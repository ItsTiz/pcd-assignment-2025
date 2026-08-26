package it.unibo.agar.rmi.model;

public final class EatingManager {

    private EatingManager() {}

    public static boolean canEatFood(final Player player, final Food food) {
        return player.distanceTo(food) <= player.getRadius();
    }

    public static boolean canEatPlayer(final Player predator, final Player victim) {
        return predator.getMass() > victim.getMass()
                && predator.distanceTo(victim)
                <= predator.getRadius();
    }
}