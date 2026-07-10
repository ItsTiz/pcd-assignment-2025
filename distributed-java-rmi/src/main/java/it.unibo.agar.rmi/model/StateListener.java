package it.unibo.agar.rmi.model;

import java.util.List;

public interface StateListener {

    void eliminatePlayers(List<String> playerIds);

}
