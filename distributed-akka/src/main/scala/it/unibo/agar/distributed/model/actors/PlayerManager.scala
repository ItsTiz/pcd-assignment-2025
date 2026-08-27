package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.LWWMap
import akka.cluster.ddata.LWWMapKey
import akka.cluster.ddata.ORSet
import akka.cluster.ddata.ORSetKey
import akka.cluster.ddata.ReplicatedData
import akka.cluster.ddata.SelfUniqueAddress
import akka.cluster.ddata.typed.scaladsl.DistributedData
import akka.cluster.ddata.typed.scaladsl.Replicator
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import it.unibo.agar.distributed.model.EatingManager
import it.unibo.agar.distributed.model.Food
import it.unibo.agar.distributed.model.Player

import scala.concurrent.duration.DurationInt
import it.unibo.agar.distributed.model.serializables.CborSerializable
import it.unibo.agar.distributed.serviceKey

object PlayerManager:

  sealed trait Command extends CborSerializable

  final case class Join(player: Player) extends Command
  final case class Leave(id: String) extends Command

  final case class PlayerUpdated(player: Player) extends Command
  final case class PlayerRemoved(id: String) extends Command

  private case object Tick extends Command

  private case class InternalSubscribeResponse(rsp: Replicator.SubscribeResponse[ORSet[Food]]) extends Command
  private case class InternalUpdateResponse(rsp: Replicator.UpdateResponse[LWWMap[String, Player]]) extends Command
  private case class InternalFoodUpdateResponse(rsp: Replicator.UpdateResponse[ORSet[Food]]) extends Command

  private val foodKey = ORSetKey[Food]("food-ddata")
  private val playersKey = LWWMapKey[String, Player]("players-ddata")

  val Key: ServiceKey[Command] = serviceKey[Command]("player-manager")

  private def playerRef(sharding: ClusterSharding, id: String) = sharding.entityRefFor(PlayerActor.TypeKey, id)

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val sharding = ClusterSharding(context.system)
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress

      context.system.receptionist ! Receptionist.Register(Key, context.self)

      val replicator = DistributedData(context.system).replicator

      val updateAdapterFood = context.messageAdapter[Replicator.UpdateResponse[ORSet[Food]]](InternalFoodUpdateResponse.apply)
      val subAdapter = context.messageAdapter[Replicator.SubscribeResponse[ORSet[Food]]](InternalSubscribeResponse.apply)
      val updateAdapter = context.messageAdapter[Replicator.UpdateResponse[LWWMap[String, Player]]](InternalUpdateResponse.apply)

      replicator ! Replicator.Subscribe(foodKey, subAdapter)

      def publishPlayer(p: Player): Unit =
        replicator ! Replicator.Update(
          playersKey,
          LWWMap.empty[String, Player],
          Replicator.WriteLocal,
          updateAdapter
        )(_.:+ (p.id, p))

      def retractPlayer(id: String): Unit =
        replicator ! Replicator.Update(
          playersKey,
          LWWMap.empty[String, Player],
          Replicator.WriteLocal,
          updateAdapter
        )(_.remove(node, id))

      def removeFoods(food: Set[Food]): Unit =
        replicator ! Replicator.Update(
          foodKey,
          ORSet.empty[Food],
          Replicator.WriteLocal,
          updateAdapterFood
        ) { currentOrSet =>
          food.foldLeft(currentOrSet) { (accSet, foodToRemove) =>
            accSet.remove(node, foodToRemove)
          }
        }

      Behaviors.withTimers { timers => timers.startTimerWithFixedDelay(Tick, Tick, 50.millis)

        def running(players: Map[String, Player], foods: Set[Food]): Behavior[Command] =
          Behaviors.receiveMessage {

            case InternalSubscribeResponse(chg @ Replicator.Changed(`foodKey`)) =>
              running(players, chg.get(foodKey).elements)

            case InternalSubscribeResponse(_) =>
              Behaviors.same

            case InternalUpdateResponse(_) =>
              Behaviors.same

            case InternalFoodUpdateResponse(_) =>
              Behaviors.same

            case Join(player) =>
              context.log.info(s"Player joined the game with id ${player.id}")
              playerRef(sharding, player.id) ! PlayerActor.Initialize(player, context.self)
              publishPlayer(player)
              running(players + (player.id -> player), foods)

            case Leave(id) =>
              context.log.info(s"Player leaving the game with id $id")
              playerRef(sharding, id) ! PlayerActor.Stop
              retractPlayer(id)
              running(players - id, foods)

            case PlayerUpdated(p) =>
              publishPlayer(p)
              running(players.updated(p.id, p), foods)

            case PlayerRemoved(id) =>
              context.log.info(s"Player retracting from the game with id $id")
              retractPlayer(id)
              running(players - id, foods)

            case Tick =>
              val ps = players.values.toSeq.sortBy(p => (-p.mass, p.id))
              val fs = foods.toSeq
              val foodWinners =
                fs.flatMap { food =>
                  ps.filter(p => EatingManager.canEatFood(p, food))
                    .sortBy(p => (-p.mass, p.id))
                    .headOption
                    .map(food -> _)
                }.toMap

              val foodByPlayer = foodWinners.groupMap(_._2.id)(_._1)
              val eatenFoods = foodWinners.keySet

              foodByPlayer.foreach { (pid, eaten) =>
                val mass = eaten.map(_.mass).sum
                playerRef(sharding, pid) ! PlayerActor.ConsumeFood(mass)
              }

              val eatenPlayers =
                ps.foldLeft(Set.empty[String]) { (dead, predator) =>
                  if dead.contains(predator.id) then dead
                  else
                    val victims = ps.filter(o =>
                      o.id != predator.id && !dead.contains(o.id) && EatingManager.canEatPlayer(predator, o)
                    )
                    if victims.nonEmpty then
                      playerRef(sharding, predator.id) ! PlayerActor.ConsumePlayer(victims.map(_.mass).sum)
                    victims.foreach(v => playerRef(sharding, v.id) ! PlayerActor.Stop)
                    dead ++ victims.map(_.id)
                }

              eatenPlayers.foreach(retractPlayer)
              removeFoods(eatenFoods)
              running(players -- eatenPlayers, foods -- eatenFoods)
          }

        running(Map.empty, Set.empty)
      }
    }

end PlayerManager
