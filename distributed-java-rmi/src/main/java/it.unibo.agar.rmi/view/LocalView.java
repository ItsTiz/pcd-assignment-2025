package it.unibo.agar.rmi.view;

import it.unibo.agar.rmi.model.Player;
import it.unibo.agar.rmi.model.WorldSnapshot;
import it.unibo.agar.rmi.network.GameServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;

public class LocalView extends JFrame implements SnapshotListener {
    private static final double SENSITIVITY = 2;
    private final GamePanel gamePanel;
    private final GameServer serverStub;
    private final String playerId;

    private boolean isGameOver = false;

    public LocalView(GameServer serverStub, String playerId) {
        this.playerId = playerId;
        this.serverStub = serverStub;

        setTitle("Agar.io - Local View (" + playerId + ") (Java)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(600, 600));

        this.gamePanel = new GamePanel(serverStub, playerId);
        add(this.gamePanel, BorderLayout.CENTER);

        setupWindowEvents();
        setupMouseControls();
        pack();
        setLocationRelativeTo(null);
    }

    private void setupMouseControls() {
        gamePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isGameOver) return;

                try {
                    Player player = serverStub.playerValid(playerId);
                    if (player != null) {
                        Point mousePos = e.getPoint();
                        double viewCenterX = gamePanel.getWidth() / 2.0;
                        double viewCenterY = gamePanel.getHeight() / 2.0;

                        double dx = mousePos.x - viewCenterX;
                        double dy = mousePos.y - viewCenterY;

                        double magnitude = Math.hypot(dx, dy);

                        if (magnitude > 0) {
                            serverStub.move(playerId, (dx / magnitude) * SENSITIVITY, (dy / magnitude) * SENSITIVITY);
                        } else {
                            serverStub.move(playerId, 0, 0);
                        }
                    }
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void setupWindowEvents() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    if (serverStub != null) {
                        serverStub.leave(playerId);
                    }
                    System.exit(0);
                } catch (Exception ex) {
                    ex.printStackTrace();
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
        if (isGameOver || world == null) return;

        this.gamePanel.setToRender(world);
        repaintView();
    }

    @Override
    public void endGame() {
        if (isGameOver) return;
        isGameOver = true;

        SwingUtilities.invokeLater(() -> {
            getContentPane().removeAll();

            JLabel gameOverLabel = new JLabel("Game Over! You've been eaten.", SwingConstants.CENTER);
            gameOverLabel.setFont(new Font("Arial", Font.BOLD, 24));
            gameOverLabel.setForeground(Color.RED);

            add(gameOverLabel, BorderLayout.CENTER);

            revalidate();
            repaint();
        });
    }
}