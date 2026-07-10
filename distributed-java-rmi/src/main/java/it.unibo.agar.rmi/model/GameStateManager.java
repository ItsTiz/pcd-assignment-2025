package it.unibo.agar.rmi.model;

public interface GameStateManager {
    void addPlayer(final Player player);
    void removePlayer(final String id);
    Player playerValid(final String playerId);
    void setPlayerDirection(final String playerId, final double dx, final double dy);
    void tick();
    WorldSnapshot getWorldCopy();
}
