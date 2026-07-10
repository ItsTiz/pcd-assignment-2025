package it.unibo.agar.rmi.model;

import java.util.Objects;

public record Food(String id, double x, double y, double mass) implements Entity {

    public static final double DEFAULT_MASS = 100.0;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Food food = (Food) o;
        return Double.compare(x, food.x) == 0 && Double.compare(y, food.y) == 0 && Double.compare(mass, food.mass) == 0 && Objects.equals(id, food.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y, mass);
    }
}