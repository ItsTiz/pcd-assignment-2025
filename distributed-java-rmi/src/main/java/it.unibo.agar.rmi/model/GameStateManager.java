package it.unibo.agar.rmi.model;

public interface GameStateManager {
    World getWorld();
    void setPlayerDirection(final String playerId, final double dx, final double dy);
    void tick();
}
