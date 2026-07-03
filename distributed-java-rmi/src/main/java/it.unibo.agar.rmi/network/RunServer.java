package it.unibo.agar.rmi.network;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RunServer {

    public static void main(String[] args) {

        try {
            GameServer serverService = new GameServerImpl();
            GameServer serverServiceStub = (GameServer) UnicastRemoteObject.exportObject(serverService, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("serverService", serverServiceStub);

            log("Game server object registered successfully.");
        } catch (Exception e) {
            log("Server exception: " + e);
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ Main ] " + msg);
    }
}
