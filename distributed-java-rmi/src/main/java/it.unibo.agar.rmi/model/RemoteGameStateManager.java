package it.unibo.agar.rmi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RemoteGameStateManager implements GameStateManager {
    private final Map<String, Position> playerDirections;
    private World world;
    private final List<StateListener> listeners = new ArrayList<>();

    public RemoteGameStateManager(final World initialWorld) {
        this.world = initialWorld;
        this.playerDirections = new ConcurrentHashMap<>();
        this.world.getPlayers().forEach(p -> playerDirections.put(p.getId(), Position.ZERO));
    }

    public void addListener(StateListener listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized WorldSnapshot getWorldCopy() {
        return new WorldSnapshot(List.copyOf(world.getPlayers()), List.copyOf(world.getFoods()));
    }

    @Override
    public synchronized void addPlayer(final Player player){
        this.world.addPlayer(player);
    }

    @Override
    public synchronized void removePlayer(final String id){
        this.world.removePlayer(id);
    }

    @Override
    public synchronized Player playerValid(String playerId) {
        return this.world.playerValid(playerId);
    }

    @Override
    public void setPlayerDirection(final String playerId, final double dx, final double dy) {
        // Ensure player exists before setting direction
        if (world.getPlayerById(playerId).isPresent()) {
            this.playerDirections.put(playerId, Position.of(dx, dy));
        }
    }

    public void tick() {
        this.world = handleEating(moveAllPlayers(this.world));
        cleanupPlayerDirections();
    }

    private World moveAllPlayers(final World currentWorld) {
        final Map<String, Player> updatedPlayers = currentWorld.getPlayers().stream()
            .map(player -> {
                Position direction = playerDirections.getOrDefault(player.getId(), Position.ZERO);
                final double newX = player.getX() + direction.x() * Globals.PLAYER_SPEED;
                final double newY = player.getY() + direction.y() * Globals.PLAYER_SPEED;
                return player.moveTo(newX, newY);
            })
            .collect(Collectors.toMap(
                    Player::getId,
                    player -> player
            ));

        return new World(currentWorld.getWidth(), currentWorld.getHeight(), updatedPlayers, currentWorld.getFoods());
    }

    private World handleEating(final World currentWorld) {
        final Map<String, Player> updatedPlayers = currentWorld.getPlayers().stream()
                .map(player -> growPlayer(currentWorld, player))
                .collect(Collectors.toMap(
                        Player::getId,
                        player -> player
                ));

        final List<Food> foodsToRemove = currentWorld.getPlayers().stream()
                .flatMap(player -> eatenFoods(currentWorld, player).stream())
                .distinct()
                .toList();

        final List<Player> playersToRemove = currentWorld.getPlayers().stream()
                .flatMap(player -> eatenPlayers(currentWorld, player).stream())
                .distinct()
                .toList();
        
        listeners.forEach(l -> l.eliminatePlayers(playersToRemove.stream().map(Player::getId).toList()));

        return new World(currentWorld.getWidth(), currentWorld.getHeight(), updatedPlayers, currentWorld.getFoods())
                .removeFoods(foodsToRemove)
                .removePlayers(playersToRemove);
    }

    private Player growPlayer(final World world, final Player player) {
        final Player afterFood = eatenFoods(world, player).stream()
                .reduce(player, (acc, f) -> acc.grow(f.getMass()), (p1, p2) -> p1);

        return eatenPlayers(world, afterFood).stream()
                .reduce(afterFood, (acc, p) -> acc.grow(p.getMass()), (p1, p2) -> p1);
    }

    private List<Food> eatenFoods(final World world, final Player player) {
        return world.getFoods().stream()
                .filter(food -> EatingManager.canEatFood(player, food))
                .toList();
    }

    private List<Player> eatenPlayers(final World world, final Player player) {
        return world.getPlayersExcludingSelf(player).stream()
                .filter(other -> EatingManager.canEatPlayer(player, other))
                .toList();
    }

    private void cleanupPlayerDirections() {
        List<String> currentPlayerIds = this.world.getPlayers().stream()
                .map(Player::getId)
                .collect(Collectors.toList());

        this.playerDirections.keySet().retainAll(currentPlayerIds);
        this.world.getPlayers().forEach(p ->
                playerDirections.putIfAbsent(p.getId(), Position.ZERO));
    }

}
