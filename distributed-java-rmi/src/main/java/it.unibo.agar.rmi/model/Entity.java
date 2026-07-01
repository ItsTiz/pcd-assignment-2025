package it.unibo.agar.rmi.model;

import java.io.Serializable;

public interface Entity extends Serializable {

    String getId();

    double getMass();

    double getX();

    double getY();

    double getRadius();

    default double distanceTo(final Entity other) {
        final double dx = getX() - other.getX();
        final double dy = getY() - other.getY();
        return Math.hypot(dx, dy);
    }
}