package it.unibo.agar.rmi.view;

import it.unibo.agar.rmi.model.Player;
import it.unibo.agar.rmi.model.WorldSnapshot;
import it.unibo.agar.rmi.network.GameServer;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;

public class GamePanel extends JPanel {

    private final String focusedPlayerId; // Null for global view
    private WorldSnapshot toRender;
    private final GameServer serverStub;

    public GamePanel(GameServer serverStub, String focusedPlayerId) {
        this.focusedPlayerId = focusedPlayerId;
        this.serverStub = serverStub;
        this.setFocusable(true); // Important for receiving keyboard/mouse events if needed directly
    }

    public void setToRender(WorldSnapshot toRender) {
        this.toRender = toRender;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (focusedPlayerId != null) {
            try {
                Player player = serverStub.playerValid(focusedPlayerId);
                if (player != null && toRender != null) {
                    final double offsetX = player.getX() - getWidth() / 2.0;
                    final double offsetY = player.getY() - getHeight() / 2.0;
                    AgarViewUtils.drawWorld(g2d, toRender, offsetX, offsetY);
                }
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }

        } else {
            AgarViewUtils.drawWorld(g2d, toRender, 0, 0);
        }
    }
}
