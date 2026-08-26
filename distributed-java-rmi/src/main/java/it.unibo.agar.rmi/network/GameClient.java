package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.WorldSnapshot;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameClient extends Remote {

    void onWorldUpdate(WorldSnapshot snapshot) throws RemoteException;
    void onPlayerKilled(String playerId) throws RemoteException;
}