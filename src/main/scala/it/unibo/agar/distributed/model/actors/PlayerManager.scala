package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.*
import akka.cluster.ddata.typed.scaladsl.DistributedData
import akka.cluster.ddata.typed.scaladsl.Replicator
import scala.concurrent.duration.*

import it.unibo.agar.distributed.model.{Food, Player, EatingManager}

object PlayerManager:

  sealed trait PlayerManagerMessage
  case class Join(player: Player) extends PlayerManagerMessage
  case class Leave(id: String) extends PlayerManagerMessage
  case class Move(id: String, dx: Double, dy: Double) extends PlayerManagerMessage

  private case class PlayersChanged(
                                     chg: Replicator.SubscribeResponse[LWWMap[String, Player]]
                                   ) extends PlayerManagerMessage

  private case class FoodsChanged(
                                   chg: Replicator.SubscribeResponse[ORSet[Food]]
                                 ) extends PlayerManagerMessage

  private case object Tick extends PlayerManagerMessage
  private case object Ignore extends PlayerManagerMessage

  private val playersKey = LWWMapKey[String, Player]("players-ddata")
  private val foodKey = ORSetKey[Food]("food-ddata")

  private def updatePlayerPosition(
                                    player: Player,
                                    dx: Double,
                                    dy: Double,
                                    width: Int,
                                    height: Int,
                                    speed: Double
                                  ): Player =
    val newX = (player.x + dx * speed).max(0).min(width)
    val newY = (player.y + dy * speed).max(0).min(height)
    player.copy(x = newX, y = newY)

  def apply(): Behavior[PlayerManagerMessage] =
    Behaviors.setup { context =>
      context.log.info("PlayerManager started")
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress
      DistributedData.withReplicatorMessageAdapter[PlayerManagerMessage, LWWMap[String, Player]] { playerAdapter =>
        DistributedData.withReplicatorMessageAdapter[PlayerManagerMessage, ORSet[Food]] { foodAdapter =>
          playerAdapter.subscribe(playersKey, PlayersChanged.apply)
          foodAdapter.subscribe(foodKey, FoodsChanged.apply)
          Behaviors.withTimers { timers =>timers.startTimerWithFixedDelay(Tick, Tick, 50.millis)
            def running(directions: Map[String, (Double, Double)], players: Map[String, Player], foods: Set[Food]): Behavior[PlayerManagerMessage] =
              Behaviors.receiveMessage {

                case PlayersChanged(chg @ Replicator.Changed(`playersKey`)) =>
                  running(directions, chg.get(playersKey).entries, foods)

                case FoodsChanged(chg @ Replicator.Changed(`foodKey`)) =>
                  running(directions, players, chg.get(foodKey).elements)

                case PlayersChanged(_) => Behaviors.same
                case FoodsChanged(_)   => Behaviors.same

                case Join(player) =>
                  playerAdapter.askUpdate(
                    Replicator.Update(playersKey, LWWMap.empty[String, Player], Replicator.WriteLocal) { map =>
                      map.put(node, player.id, player)
                    },
                    _ => Ignore
                  )
                  Behaviors.same

                case Leave(id) =>
                  playerAdapter.askUpdate(
                    Replicator.Update(playersKey, LWWMap.empty[String, Player], Replicator.WriteLocal) { map =>
                      map.remove(node, id)
                    },
                    _ => Ignore
                  )
                  Behaviors.same

                case Move(id, dx, dy) =>
                  running(directions.updated(id, (dx, dy)), players, foods)

                case Tick =>

                  // move players
                  val movedPlayers =
                    directions.foldLeft(players) {
                      case (acc, (id, (dx, dy))) =>
                        acc.get(id)
                          .map(p => acc.updated(id, updatePlayerPosition(p, dx, dy, 1000, 1000, 10.0)))
                          .getOrElse(acc)
                    }

                  val players0 = movedPlayers.values.toSeq.sortBy(p => (-p.mass, p.id))
                  val foods0 = foods.toSeq

                  // food winner (1 food → 1 player max)
                  val foodWinners: Map[Food, Player] =
                    foods0.flatMap { food =>
                      val contenders = players0.filter(p => EatingManager.canEatFood(p, food))
                      contenders.sortBy(p => (-p.mass, p.id)).headOption.map(food -> _)
                    }.toMap

                  val foodByPlayer: Map[String, Seq[Food]] = foodWinners.groupMap(_._2.id)(_._1)
                  val eatenFoods = foodWinners.keySet
                  val afterFood: Map[String, Player] =
                    players0.map { p =>
                      val grown = foodByPlayer.getOrElse(p.id, Seq.empty).foldLeft(p)((acc, f) => acc.grow(f))
                      p.id -> grown
                    }.toMap

                  val playersAfterFood = afterFood.values.toSeq.sortBy(p => (-p.mass, p.id))

                  // player vs player
                  val (finalPlayers, eatenPlayers) =
                    playersAfterFood.foldLeft((afterFood, Set.empty[String])) {
                      case ((acc, dead), predator) =>
                        if dead.contains(predator.id) then
                          (acc, dead)
                        else
                          val current = acc(predator.id)
                          val victims = playersAfterFood.filter { other => other.id != predator.id && !dead.contains(other.id) && EatingManager.canEatPlayer(current, other)}
                          val (newPredator, newDead) = victims.foldLeft((current, dead)) {
                            case ((pAcc, dAcc), v) =>
                              (pAcc.grow(v), dAcc + v.id)
                          }
                          val acc1 = acc.updated(predator.id, newPredator)
                          (acc1, newDead)
                    }

                  val survivors = finalPlayers.filterNot { case (id, _) => eatenPlayers.contains(id) }

                  // update players
                  playerAdapter.askUpdate(
                    Replicator.Update(playersKey, LWWMap.empty[String, Player], Replicator.WriteLocal) { map =>
                      val cleared = eatenPlayers.foldLeft(map) { (m, id) => m.remove(node, id)}
                      survivors.values.foldLeft(cleared) { (m, p) => m.put(node, p.id, p)}
                    },
                    _ => Ignore
                  )

                  // remove food
                  if eatenFoods.nonEmpty then
                    foodAdapter.askUpdate(
                      Replicator.Update(foodKey, ORSet.empty[Food], Replicator.WriteLocal) { set =>
                        eatenFoods.foldLeft(set) { (s, f) =>
                          s.remove(node, f)
                        }
                      },
                      _ => Ignore
                    )

                  // next state
                  running(directions, survivors, foods -- eatenFoods)
              }
            running(Map.empty, Map.empty, Set.empty)
          }
        }
      }
    }