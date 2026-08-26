package it.unibo.agar.rmi.view;

import it.unibo.agar.rmi.model.WorldSnapshot;

public interface SnapshotListener {

    void renderUI(WorldSnapshot world);
    
    void endGame();
}
