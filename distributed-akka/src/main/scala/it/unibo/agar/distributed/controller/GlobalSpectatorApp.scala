package it.unibo.agar.distributed.controller

import com.typesafe.config.ConfigFactory
import it.unibo.agar.distributed.controller.LocalPlayerApp.world
import it.unibo.agar.distributed.model.actors.SpectatorRootBehavior
import it.unibo.agar.distributed.model.{DistributedWorld, DistributedWorldState, Food, Player}
import it.unibo.agar.distributed.startupWithRoles
import it.unibo.agar.distributed.view.GlobalView

import java.awt.Window
import scala.jdk.CollectionConverters.*
import scala.swing.Swing.onEDT
import scala.swing.{Frame, SimpleSwingApplication}

object GlobalSpectatorApp extends SimpleSwingApplication:

  private val config = ConfigFactory.load("agario-game")
  private val spectatorRoles: Seq[String] = config.getStringList("agar.roles.spectator").asScala.toSeq
  private val mapWidth = config.getInt("agar.game.map-width")
  private val mapHeight = config.getInt("agar.game.map-height")
  private val nodePort = 0

  @volatile private var world = DistributedWorld(mapWidth, mapHeight, Seq.empty, Seq.empty)

  private def onStateChanged(foods: Seq[Food], players: Seq[Player], winner: String): Unit =
    world = world.newFoods(foods).newPlayers(players)
    if (winner.nonEmpty)
      world = world.withWinner(winner)
    onEDT(Window.getWindows.foreach(_.repaint()))

  startupWithRoles(nodePort, spectatorRoles: _*)(SpectatorRootBehavior(onStateChanged))

  override def top: Frame = new GlobalView(world)

end GlobalSpectatorApp
