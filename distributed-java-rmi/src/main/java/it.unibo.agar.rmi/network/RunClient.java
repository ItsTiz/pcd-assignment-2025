package it.unibo.agar.rmi.network;

import it.unibo.agar.rmi.model.Player;
import it.unibo.agar.rmi.view.LocalView;

import javax.swing.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RunClient {

    public static void main(String[] args) {

        String host = (args.length < 1) ? null : args[0];
        try {
            Registry registry = LocateRegistry.getRegistry(host);
            GameServer serverService = (GameServer) registry.lookup("serverService");

            GameClientImpl client = new GameClientImpl();
            GameClient clientStub = (GameClient) UnicastRemoteObject.exportObject(client, 0);

            final Player player = client.getPlayer();
            final String playerId = player.getId();

            SwingUtilities.invokeLater(() -> {
                LocalView localViewP1 = new LocalView(serverService, playerId);
                localViewP1.setVisible(true);
                client.addListener(localViewP1);
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (serverService != null) {
                        serverService.leave(playerId);
                    }
                } catch (Exception e) {
                    log("Server might be dead.");
                }
            }));

            serverService.join(player, clientStub); //some sort of observer i guess

            log("Running client with player " + playerId);

        } catch (Exception e) {
            log("Client exception: " + e);
            e.printStackTrace();
        }
    }

    private static void log(String msg) {
        System.out.println("[ " + System.currentTimeMillis() + " ][ Client Main ] " + msg);
    }
}


