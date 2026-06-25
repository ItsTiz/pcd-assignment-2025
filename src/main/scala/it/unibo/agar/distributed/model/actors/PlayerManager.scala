package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.*
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding

import scala.concurrent.duration.*
import it.unibo.agar.distributed.model.{EatingManager, Food, Player}

object PlayerManager:

  sealed trait Command

  final case class Join(player: Player) extends Command
  final case class Leave(id: String) extends Command

  final case class PlayerUpdated(player: Player) extends Command
  final case class PlayerRemoved(id: String) extends Command

  private case object Tick extends Command
  private case object Ignore extends Command

  private case class FoodsChanged(
                                   chg: Replicator.SubscribeResponse[ORSet[Food]]
                                 ) extends Command

  private val foodKey = ORSetKey[Food]("food-ddata")

  private def playerRef(sharding: ClusterSharding, id: String) = sharding.entityRefFor(PlayerActor.TypeKey, id)

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val sharding = ClusterSharding(context.system)
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress
      DistributedData.withReplicatorMessageAdapter[Command, ORSet[Food]] { foodAdapter =>
        foodAdapter.subscribe(foodKey, FoodsChanged.apply)
        Behaviors.withTimers { timers =>
          timers.startTimerWithFixedDelay(Tick, Tick, 50.millis)
          def running(
                       players: Map[String, Player],
                       foods: Set[Food]
                     ): Behavior[Command] =
            Behaviors.receiveMessage {
              case FoodsChanged(chg @ Replicator.Changed(`foodKey`)) =>
                running(players, chg.get(foodKey).elements)

              case FoodsChanged(_) =>
                Behaviors.same

              case Join(player) =>
                playerRef(sharding, player.id) !
                  PlayerActor.Initialize(player, context.self)
                running(players + (player.id -> player), foods)

              case Leave(id) =>
                playerRef(sharding, id) !
                  PlayerActor.Stop
                running(players - id, foods)

              case PlayerUpdated(p) =>
                running(players.updated(p.id, p), foods)

              case PlayerRemoved(id) =>
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
                  playerRef(sharding, pid) !
                    PlayerActor.ConsumeFood(mass)
                }

                val eatenPlayers =
                  ps.foldLeft(Set.empty[String]) { (dead, predator) =>
                    if dead.contains(predator.id) then dead
                    else
                      val victims =
                        ps.filter(o =>
                          o.id != predator.id &&
                            !dead.contains(o.id) &&
                            EatingManager.canEatPlayer(predator, o)
                        )

                      if victims.nonEmpty then
                        playerRef(sharding, predator.id) !
                          PlayerActor.ConsumePlayer(victims.map(_.mass).sum)

                      victims.foreach(v =>
                        playerRef(sharding, v.id) ! PlayerActor.Stop
                      )

                      dead ++ victims.map(_.id)
                  }

                running(players -- eatenPlayers, foods -- eatenFoods)
              case Ignore =>
                Behaviors.same
            }

          running(Map.empty, Set.empty)
        }
      }
    }