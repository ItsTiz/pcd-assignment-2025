package it.unibo.agar.distributed.controller

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import it.unibo.agar.distributed.model.actors.PlayerRootBehavior
import it.unibo.agar.distributed.model.{DistributedWorld, Food, GameStateManager, Player}
import it.unibo.agar.distributed.startupWithRoles
import it.unibo.agar.distributed.view.LocalView

import java.awt.Window
import javax.swing.Timer
import scala.swing.Swing.onEDT
import scala.swing.{Frame, SimpleSwingApplication}
import scala.util.Random
import com.typesafe.config.ConfigFactory
import it.unibo.agar.distributed.controller.GlobalSpectatorApp.config
import it.unibo.agar.distributed.model.actors.backend.players.PlayerActor

import scala.jdk.CollectionConverters.*

object LocalPlayerApp extends SimpleSwingApplication:

  private val config = ConfigFactory.load("agario-game")
  private val clientRoles: Seq[String] = config.getStringList("agar.roles.client").asScala.toSeq
  private val mapWidth = config.getInt("agar.game.map-width")
  private val mapHeight = config.getInt("agar.game.map-height")
  private val initialPlayerMass = config.getInt("agar.game.map-height")
  private val nodePort = 0
  private val playerMaxId = 1000

  private val playerId = s"p${Random.nextInt(playerMaxId)}"
  private val initialPlayer = Player(playerId, Random.nextInt(mapWidth), Random.nextInt(mapHeight), initialPlayerMass)

  @volatile private var world: DistributedWorld = DistributedWorld(mapWidth, mapHeight, Seq.empty, Seq.empty)

  private def onStateChanged(foods: Seq[Food], players: Seq[Player]): Unit =
    world = world.newFoods(foods).newPlayers(players)
    onEDT(Window.getWindows.foreach(_.repaint()))

  private val system = startupWithRoles(nodePort, clientRoles: _*)(PlayerRootBehavior(initialPlayer, onStateChanged))

  private val manager: GameStateManager = new GameStateManager:
    private val sharding = ClusterSharding(system)

    def getWorld: DistributedWorld = world
    def movePlayerDirection(id: String, dx: Double, dy: Double): Unit =
      sharding.entityRefFor(PlayerActor.TypeKey, id) ! PlayerActor.ChangeDirection(dx, dy)

  override def top: Frame =
    new LocalView(manager, playerId)

end LocalPlayerApp