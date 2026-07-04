package it.unibo.agar.rmi.view;

import it.unibo.agar.rmi.model.Player;
import it.unibo.agar.rmi.model.WorldSnapshot;
import it.unibo.agar.rmi.network.GameClientImpl;
import it.unibo.agar.rmi.network.GameServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.rmi.RemoteException;
import java.util.Optional;

public class LocalView extends JFrame implements SnapshotListener {
    private static final double SENSITIVITY = 2;
    private final GamePanel gamePanel;
    private final GameClientImpl gameClient;
    private final GameServer serverStub;
    private final String playerId;

    public LocalView(GameClientImpl gameClient, GameServer serverStub, String playerId) {
        this.gameClient = gameClient;
        this.playerId = gameClient.getPlayer().orElseThrow().getId();
        this.serverStub = serverStub;

        setTitle("Agar.io - Local View (" + playerId + ") (Java)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Dispose only this window
        setPreferredSize(new Dimension(600, 600));

        this.gamePanel = new GamePanel(gameClient, playerId);
        add(this.gamePanel, BorderLayout.CENTER);

        setupMouseControls();

        pack();
        setLocationRelativeTo(null); // Center on screen
    }

    private void setupMouseControls() {
        gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                //TODO Manage this with a call to server to check if player exists
                Optional<Player> playerOpt = gameClient.getPlayer();
                if (playerOpt.isPresent()) {
                    Point mousePos = e.getPoint();
                    // Player is always in the center of the local view
                    double viewCenterX = gamePanel.getWidth() / 2.0;
                    double viewCenterY = gamePanel.getHeight() / 2.0;

                    double dx = mousePos.x - viewCenterX;
                    double dy = mousePos.y - viewCenterY;

                    // Normalize the direction vector
                    double magnitude = Math.hypot(dx, dy);
                    try {
                        if (magnitude > 0) { // Avoid division by zero if mouse is exactly at center
                            serverStub.move(playerId, (dx / magnitude) * SENSITIVITY, (dy / magnitude) * SENSITIVITY);
                        } else {
                            serverStub.move(playerId, 0, 0); // Stop if mouse is at center
                        }
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    public void repaintView() {
        if (gamePanel != null) {
            gamePanel.repaint();
        }
    }

    @Override
    public void renderUI(WorldSnapshot world) {
        this.gamePanel.setToRender(world);
        repaintView();
    }
}
