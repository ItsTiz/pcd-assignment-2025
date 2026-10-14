package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.*;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class GameServerImpl implements GameServer, StateListener {

    private final GameStateManager gameStateManager;
    private final Map<String, GameClient> clients;

    public GameServerImpl(GameStateManager manager) throws RemoteException {
        this.gameStateManager = manager;
        this.clients = new ConcurrentHashMap<>();
    }

    @Override
    public synchronized void join(final Player player, final GameClient client) throws RemoteException {
        gameStateManager.addPlayer(player);
        clients.put(player.getId(), client);
        System.out.println(player.getId() + " joined the game.");
    }

    @Override
    public synchronized void leave(final String playerId) throws RemoteException {
        gameStateManager.removePlayer(playerId);
        clients.remove(playerId);
        System.out.println(playerId + " left the game.");
    }

    @Override
    public synchronized void move(final String playerId, final double dx, final double dy) throws RemoteException {
        gameStateManager.setPlayerDirection(playerId, dx, dy);
    }

    @Override
    public synchronized Player playerValid(final String playerId) throws RemoteException {
        return gameStateManager.playerValid(playerId);
    }

    public void signalClients() {
        final WorldSnapshot snapshot = gameStateManager.getWorldCopy();
        clients.forEach((id, client) -> {
            try {
                client.onWorldUpdate(snapshot);
            } catch (RemoteException e) {
                try {
                    leave(id);
                } catch (RemoteException eLeave) {
                    throw new RuntimeException(eLeave);
                }
            }
        });
    }

    public void eliminatePlayers(List<String> playerIds) {
        playerIds.forEach(id -> {
            final GameClient client = clients.remove(id);
            if (client == null) return;
            try {
                client.onPlayerKilled(id);
            } catch (RemoteException e) {
                log("Could not notify " + id + " of its elimination.");
            }
        });
    }

    public void runEngine() {
        log("Starting game loop.");
        final Timer timer = new Timer(true); // Use daemon thread for timer
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                gameStateManager.tick();
                signalClients();
            }
        }, 0, Globals.GAME_TICK_MS);
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ Main ] " + msg);
    }
}