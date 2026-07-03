package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.Globals;
import it.unibo.agar.rmi.model.Player;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class RunClient {

    private static final Random random = new Random();

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);
            GameServer serverService = (GameServer) registry.lookup("serverService");

            GameClient client = new GameClientImpl();
            GameClient clientStub = (GameClient) UnicastRemoteObject.exportObject(client, 0);

            Player player = new Player("p-" + random.nextInt(1000), random.nextInt(Globals.WORLD_WIDTH), random.nextInt(Globals.WORLD_HEIGHT), 120.0);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (serverService != null) {
                        serverService.leave(player.getId());
                    }
                } catch (Exception e) {
                    log("Server might be dead.");
                }
            }));

            serverService.join(player, clientStub); //some sort of observer i guess

            log("Running client with player " + player.getId());

        } catch (Exception e) {
            log("Client exception: " + e);
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ Client Main ] " + msg);
    }
}


