package it.unibo.agar.rmi.model;

public record Food(String id, double x, double y, double mass) implements Entity {

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
}