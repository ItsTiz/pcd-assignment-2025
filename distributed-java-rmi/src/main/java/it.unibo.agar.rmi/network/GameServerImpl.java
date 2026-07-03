package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.Food;
import it.unibo.agar.rmi.model.Player;
import it.unibo.agar.rmi.model.WorldSnapshot;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameServerImpl implements GameServer {

    private final Map<String, Player> players;
    private final Map<String, GameClient> clients;
    private final Set<Food> foods;

    public GameServerImpl() throws RemoteException {
        this.players = new ConcurrentHashMap<>();
        this.clients = new ConcurrentHashMap<>();
        this.foods = ConcurrentHashMap.newKeySet();
    }

    @Override
    public synchronized void join(final Player player, final GameClient client) throws RemoteException {
        players.put(player.getId(), player);
        clients.put(player.getId(), client);
        System.out.println(player.getId() + " joined the game.");
    }

    @Override
    public synchronized void leave(final String playerId) throws RemoteException {
        players.remove(playerId);
        clients.remove(playerId);
        System.out.println(playerId + " left the game.");
    }

    @Override
    public synchronized void move(final String playerId, final double dx, final double dy) throws RemoteException {
        final Player player = players.get(playerId);
        if (player == null) {
            return;
        }
        Player updated = player.move(dx, dy);
        players.put(playerId, updated);
    }

    @Override
    public synchronized WorldSnapshot getWorldSnapshot() throws RemoteException {
        return new WorldSnapshot(List.copyOf(players.values()), List.copyOf(foods));
    }
}