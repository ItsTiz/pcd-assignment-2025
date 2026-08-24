package it.unibo.agar.distributed.controller

import it.unibo.agar.distributed.model.{DistributedWorld, Food, GameStateManager, Player}
import it.unibo.agar.distributed.model.actors.{PlayerActor, PlayerRootBehavior}
import it.unibo.agar.distributed.startupWithRole
import it.unibo.agar.distributed.view.LocalView

import akka.cluster.sharding.typed.scaladsl.ClusterSharding

import java.awt.Window
import scala.swing.{Frame, SimpleSwingApplication}
import scala.swing.Swing.onEDT
import scala.util.Random

object LocalPlayerApp extends SimpleSwingApplication:

  private trait LocalGameStateManager extends GameStateManager:
    def updateWorld(foods: Seq[Food], players: Seq[Player]): Unit

  private val playerId = s"p${Random.nextInt(1000)}"
  private val initialPlayer = Player(playerId, Random.nextInt(1000), Random.nextInt(1000), 120.0)

  private val manager: LocalGameStateManager = new LocalGameStateManager:
    @volatile private var world: DistributedWorld = DistributedWorld(1000, 1000, Seq.empty, Seq.empty)
    private lazy val sharding = ClusterSharding(system)

    def getWorld: DistributedWorld = world
    def updateWorld(foods: Seq[Food], players: Seq[Player]): Unit =
      world = world.newFoods(foods).newPlayers(players)
    def movePlayerDirection(id: String, dx: Double, dy: Double): Unit =
      sharding.entityRefFor(PlayerActor.TypeKey, id) ! PlayerActor.Move(dx, dy)

  private def onStateChanged(foods: Seq[Food], players: Seq[Player]): Unit = {
    manager.updateWorld(foods, players)
    onEDT(Window.getWindows.foreach(_.repaint()))
  }

  private val system = startupWithRole("player", 0)(PlayerRootBehavior(initialPlayer, onStateChanged))

  override def top: Frame =
    new LocalView(manager, playerId).open()
    new Frame { visible = false }

end LocalPlayerApp