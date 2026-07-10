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
    public synchronized WorldSnapshot getWorldSnapshot() throws RemoteException {
        return gameStateManager.getWorldCopy();
    }

    @Override
    public synchronized Player playerValid(final String playerId) throws RemoteException {
        return gameStateManager.playerValid(playerId);
    }

    public void signalClients() {
        clients.forEach((s, gameClient) -> {
            try {
                gameClient.onWorldUpdate(gameStateManager.getWorldCopy());
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void eliminatePlayers(List<String> playerIds) {
        playerIds.forEach(id -> {
            try {
                clients.get(id).onPlayerKilled(id);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
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