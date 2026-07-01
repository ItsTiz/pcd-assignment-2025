package it.unibo.agar.rmi.model;

import java.io.Serializable;
import java.util.List;

public record WorldSnapshot(List<Player> players, List<Food> foods) implements Serializable {}