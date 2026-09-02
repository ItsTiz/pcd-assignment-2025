package it.unibo.agar.distributed.controller

import it.unibo.agar.distributed.model.actors.SpectatorRootBehavior
import it.unibo.agar.distributed.model.{DistributedWorldState, DistributedWorld, Food, Player}
import it.unibo.agar.distributed.startupWithRole
import it.unibo.agar.distributed.view.GlobalView

import java.awt.Window
import scala.swing.Swing.onEDT
import scala.swing.{Frame, SimpleSwingApplication}

object GlobalWorldSpectator extends SimpleSwingApplication:

  private val state = new DistributedWorldState(DistributedWorld(1000, 1000, Seq.empty, Seq.empty))

  private def onStateChanged(foods: Seq[Food], players: Seq[Player]): Unit = {
    state.setWorld(state.getWorld.newFoods(foods).newPlayers(players))
    onEDT(Window.getWindows.foreach(_.repaint()))
  }

  startupWithRole("spectator", 0)(SpectatorRootBehavior(onStateChanged))

  override def top: Frame =
    new GlobalView(state).open()
    new Frame {
      visible = false
    }

end GlobalWorldSpectator