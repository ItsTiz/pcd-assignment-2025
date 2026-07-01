package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.Player;
import it.unibo.agar.rmi.model.WorldSnapshot;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameServer extends Remote {

    void join(Player player, GameClient client) throws RemoteException;
    void leave(String playerId) throws RemoteException;
    void move(String playerId, double dx, double dy) throws RemoteException;
    WorldSnapshot getWorldSnapshot() throws RemoteException;
}