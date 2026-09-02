package it.unibo.agar.distributed.controller

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import it.unibo.agar.distributed.model.actors.{PlayerActor, PlayerRootBehavior}
import it.unibo.agar.distributed.model.{DistributedWorld, Food, GameStateManager, Player}
import it.unibo.agar.distributed.startupWithRole
import it.unibo.agar.distributed.view.LocalView

import java.awt.Window
import javax.swing.Timer
import scala.swing.Swing.onEDT
import scala.swing.{Frame, SimpleSwingApplication}
import scala.util.Random

object LocalPlayerApp extends SimpleSwingApplication:

  private val playerId = s"p${Random.nextInt(1000)}"
  private val initialPlayer = Player(playerId, Random.nextInt(1000), Random.nextInt(1000), 120.0)

  @volatile private var world: DistributedWorld = DistributedWorld(1000, 1000, Seq.empty, Seq.empty)

  private def onStateChanged(foods: Seq[Food], players: Seq[Player]): Unit = {
    world = world.newFoods(foods).newPlayers(players)
  }

  private val renderTimer = new Timer(16, _ => {
    onEDT(Window.getWindows.foreach(_.repaint()))
  })
  renderTimer.start()

  private val system = startupWithRole("player", 0)(PlayerRootBehavior(initialPlayer, onStateChanged))

  private val manager: GameStateManager = new GameStateManager:
    private val sharding = ClusterSharding(system)

    def getWorld: DistributedWorld = world
    def movePlayerDirection(id: String, dx: Double, dy: Double): Unit =
      sharding.entityRefFor(PlayerActor.TypeKey, id) ! PlayerActor.ChangeDirection(dx, dy)

  override def top: Frame =
    new LocalView(manager, playerId).open()
    new Frame { visible = false }

end LocalPlayerApp