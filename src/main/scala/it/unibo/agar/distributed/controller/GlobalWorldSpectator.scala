package it.unibo.agar.distributed.controller

import it.unibo.agar.distributed.view.GlobalView
import it.unibo.agar.distributed.model.{DistributedStateManager, DistributedWorld, Food, Player}
import it.unibo.agar.distributed.model.actors.SpectatorRootBehavior
import it.unibo.agar.distributed.startupWithRole

import java.awt.Window
import scala.swing.Frame
import scala.swing.SimpleSwingApplication
import scala.swing.Swing.onEDT

object GlobalWorldSpectator extends SimpleSwingApplication:

  private val manager = new DistributedStateManager(DistributedWorld(1000, 1000, Seq.empty, Seq.empty))

  private def onStateChanged(foods: Seq[Food], players: Seq[Player]): Unit = {
    manager.setWorld(manager.getWorld.newFoods(foods).newPlayers(players))
    onEDT(Window.getWindows.foreach(_.repaint()))
  }

  startupWithRole("spectator", 0)(SpectatorRootBehavior(onStateChanged))

  override def top: Frame =
    new GlobalView(manager).open()
    new Frame {
      visible = false
    }

end GlobalWorldSpectator