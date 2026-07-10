package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.Globals;
import it.unibo.agar.rmi.model.Player;
import it.unibo.agar.rmi.model.WorldSnapshot;
import it.unibo.agar.rmi.view.SnapshotListener;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameClientImpl implements GameClient {
    private final Player player;
    private static final Random random = new Random();
    private final List<SnapshotListener> listeners = new ArrayList<>();

    public GameClientImpl() {
        this.player = new Player(
                "p-" + random.nextInt(1000),
                random.nextInt(Globals.WORLD_WIDTH),
                random.nextInt(Globals.WORLD_HEIGHT),
                120.0);
    }

    public Player getPlayer() {
        return player;
    }

    public void addListener(SnapshotListener listener) {
        listeners.add(listener);
    }

    @Override
    public void onWorldUpdate(WorldSnapshot snapshot) throws RemoteException {
        for(SnapshotListener listener : listeners){
            listener.renderUI(snapshot);
        }
    }

    @Override
    public void onPlayerKilled(String playerId) throws RemoteException {
        if(playerId.equals(player.getId())){
            for(SnapshotListener listener : listeners){
                listener.endGame();
            }
        }
    }
}
