package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.WorldSnapshot;

import java.rmi.RemoteException;

public class GameClientImpl implements GameClient {
    
    @Override
    public void onWorldUpdate(WorldSnapshot snapshot) throws RemoteException {
        
    }

    @Override
    public void onPlayerKilled(String playerId) throws RemoteException {

    }
}
