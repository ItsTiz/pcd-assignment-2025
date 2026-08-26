package it.unibo.agar.rmi.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class World {
    private final int width;
    private final int height;
    private final Map<String, Player> players;
    private final Set<Food> foods;

    public World(int width, int height, Map<String, Player> players, Set<Food> foods) {
        this.width = width;
        this.height = height;
        this.players = new ConcurrentHashMap<>();
        this.foods = ConcurrentHashMap.newKeySet();
        this.foods.addAll(foods);
        this.players.putAll(players);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public synchronized void addPlayer(final Player player){
        this.players.put(player.getId(), player);
    }

    public synchronized void removePlayer(final String id){
        this.players.remove(id);
    }

    public synchronized Player playerValid(final String playerId) {
        return this.players.get(playerId);
    }

    public List<Player> getPlayers() {
        return players.values().stream().toList();
    }

    public Set<Food> getFoods() {
        return foods;
    }

    public List<Player> getPlayersExcludingSelf(final Player player) {
        return players.values().stream()
                .filter(p -> !p.getId().equals(player.getId()))
                .collect(Collectors.toList());
    }

    public Optional<Player> getPlayerById(final String id) {
        return players.containsKey(id) ? Optional.of(players.get(id)) : Optional.empty();
    }

    public World removePlayers(final List<Player> playersToRemove) {
        playersToRemove.forEach(player -> players.remove(player.getId()));
        return this;
    }

    public World removeFoods(List<Food> foodsToRemove) {
        foodsToRemove.forEach(this.foods::remove);
        return this;
    }
}
