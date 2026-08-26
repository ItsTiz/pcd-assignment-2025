package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.*;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import static it.unibo.agar.rmi.model.Globals.WORLD_HEIGHT;

public class RunServer {

    public static void main(String[] args) {

        final Set<Food> initialFoods = GameInitializer.initialFoods(Globals.NUM_FOODS, Globals.WORLD_WIDTH, WORLD_HEIGHT);
        final World initialWorld = new World(Globals.WORLD_WIDTH, WORLD_HEIGHT, Map.of(), initialFoods);
        final RemoteGameStateManager gameManager = new RemoteGameStateManager(initialWorld);

        try {
            GameServerImpl serverService = new GameServerImpl(gameManager);
            GameServer serverServiceStub = (GameServer) UnicastRemoteObject.exportObject(serverService, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("serverService", serverServiceStub);

            gameManager.addListener(serverService);
            
            log("Game server object registered successfully.");

            serverService.runEngine();
        } catch (Exception e) {
            log("Server exception: " + e);
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ Main ] " + msg);
    }
}
