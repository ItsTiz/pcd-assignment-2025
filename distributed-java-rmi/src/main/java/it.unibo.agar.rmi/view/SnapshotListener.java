package it.unibo.agar.rmi.view;

import it.unibo.agar.rmi.model.WorldSnapshot;

public interface SnapshotListener {

    public void renderUI(WorldSnapshot world);
}
