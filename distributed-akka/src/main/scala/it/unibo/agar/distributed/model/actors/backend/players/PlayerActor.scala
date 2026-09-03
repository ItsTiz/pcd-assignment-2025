package it.unibo.agar.distributed.model.actors.backend.players

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.*
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import it.unibo.agar.distributed.model.{EatingManager, Food, Player}
import it.unibo.agar.distributed.model.actors.backend.players.PlayerManager.Command
import it.unibo.agar.distributed.model.serializables.CborSerializable

import scala.concurrent.duration.DurationInt

object PlayerActor:

  sealed trait Command extends CborSerializable

  final case class Initialize(player: Player, manager: ActorRef[PlayerManager.Command]) extends Command
  final case class ChangeDirection(dx: Double, dy: Double) extends Command
  private case class InternalSubscribeResponse(rsp: Replicator.SubscribeResponse[ReplicatedData]) extends Command
  private case class InternalFoodUpdateResponse(rsp: Replicator.UpdateResponse[ORSet[Food]]) extends Command
  private case class InternalWinCondUpdateResponse(rsp: Replicator.UpdateResponse[LWWRegister[? <: String]]) extends Command
  private case object Tick extends Command
  case object Stop extends Command

  private val foodKey = ORSetKey[Food]("food-ddata")
  private val playersKey = LWWMapKey[String, Player]("players-ddata")
  private val winKey = LWWRegisterKey("winner")

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Player")

  def apply(id: String): Behavior[Command] = Behaviors.setup { context =>

    val mapWidth = context.system.settings.config.getInt("agar.game.map-width")
    val mapHeight = context.system.settings.config.getInt("agar.game.map-height")
    val speed = context.system.settings.config.getInt("agar.game.player-speed")
    val winMass = context.system.settings.config.getInt("agar.game.endgame-player-mass")

    val replicator = DistributedData(context.system).replicator
    implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress

    val foodUpdateAdapter =
      context.messageAdapter[Replicator.UpdateResponse[ORSet[Food]]](InternalFoodUpdateResponse.apply)

    val winUpdateAdapter =
      context.messageAdapter[Replicator.UpdateResponse[LWWRegister[? <: String]]](InternalWinCondUpdateResponse.apply)

    val subAdapter =
      context.messageAdapter[Replicator.SubscribeResponse[ReplicatedData]](InternalSubscribeResponse.apply)

    // Pass the exact same adapter to both subscriptions
    replicator ! Replicator.Subscribe(foodKey, subAdapter)
    replicator ! Replicator.Subscribe(playersKey, subAdapter)
    replicator ! Replicator.Subscribe(winKey, subAdapter)

    def removeFoods(food: Set[Food]): Unit =
      replicator ! Replicator.Update(
        foodKey,
        ORSet.empty[Food],
        Replicator.WriteLocal,
        foodUpdateAdapter
      ) { currentOrSet =>
        food.foldLeft(currentOrSet) { (accSet, foodToRemove) =>
          accSet.remove(node, foodToRemove)
        }
      }

    def updateWinCondition(playerId: String): Unit =
      replicator ! Replicator.Update(
        winKey,
        LWWRegister.create(""),
        Replicator.WriteLocal,
        winUpdateAdapter
      )(_ => LWWRegister(node, playerId))

    def waiting(): Behavior[Command] =
      Behaviors.receiveMessage {
        case Initialize(player, manager) =>
          manager ! PlayerManager.PlayerUpdated(player)
          run(player, manager)
        case Stop =>
          Behaviors.stopped
        case _ =>
          Behaviors.same
      }

    def run(player: Player, manager: ActorRef[PlayerManager.Command]): Behavior[Command] =
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(Tick, Tick, 30.millis)

        def running(
            player: Player,
            dx: Double,
            dy: Double,
            foods: Set[Food],
            otherPlayers: Map[String, Player]
        ): Behavior[Command] =
          Behaviors.receiveMessage {

            case InternalSubscribeResponse(chg @ Replicator.Changed(`foodKey`)) =>
              running(player, dx, dy, chg.get(foodKey).elements, otherPlayers)

            case InternalSubscribeResponse(chg @ Replicator.Changed(`playersKey`)) =>
              val allPlayers = chg.get(playersKey).entries
              val enemies = allPlayers - id
              running(player, dx, dy, foods, enemies)

            case InternalSubscribeResponse(chg @ Replicator.Changed(`winKey`)) =>
              context.log.info(s"Stopping. ${chg.get(winKey).value} won the game.")
              context.self ! Stop
              Behaviors.same

            case InternalWinCondUpdateResponse(_) =>
              Behaviors.same

            case InternalSubscribeResponse(_) =>
              Behaviors.same

            case InternalFoodUpdateResponse(_) =>
              Behaviors.same

            case ChangeDirection(newDx, newDy) =>
              running(player, newDx, newDy, foods, otherPlayers)

            case Tick =>
              val movedPlayer =
                player.copy(
                  x = (player.x + dx * speed).max(0.0).min(mapWidth),
                  y = (player.y + dy * speed).max(0.0).min(mapHeight)
                )

              val result: EatingManager.TickResult = EatingManager.evaluateCollisions(movedPlayer, foods, otherPlayers)

              // Send exactly ONE Replicator.Update for our new position/mass
              manager ! PlayerManager.PlayerUpdated(result.finalPlayer)
              // Send Replicator.Update to remove
              removeFoods(result.eatenFoods)
              // Send Actor messages to deadPlayers to kill them
              result.eatenPlayers.foreach(id => manager ! PlayerManager.Leave(id))

              if result.finalPlayer.mass >= winMass then
                updateWinCondition(result.finalPlayer.id)
                context.self ! Stop

              running(result.finalPlayer, dx, dy, foods -- result.eatenFoods, otherPlayers -- result.eatenPlayers)

            case Stop =>
              context.log.info("Stopped.")
              Behaviors.stopped

            case Initialize(_, _) =>
              Behaviors.same

          }

        running(player, 0, 0, Set.empty, Map.empty)
      }

    waiting()

  }

end PlayerActor
