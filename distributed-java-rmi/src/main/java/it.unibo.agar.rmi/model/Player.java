package it.unibo.agar.rmi.model;

public record Player(String id, double x, double y, double mass) implements Entity {
    @Override
    public String getId() {
        return id;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getRadius() {
        return Math.sqrt(mass);
    }

    public Player moveTo(final double dx, final double dy) {
        return new Player(id, dx, dy, mass);
    }

    public Player grow(final double gainedMass) {
        return new Player(id, x, y, mass + gainedMass);
    }
}