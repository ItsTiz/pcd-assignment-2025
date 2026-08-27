package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.LWWMap
import akka.cluster.ddata.LWWMapKey
import akka.cluster.ddata.ORSet
import akka.cluster.ddata.ORSetKey
import akka.cluster.ddata.ReplicatedData
import akka.cluster.ddata.SelfUniqueAddress
import akka.cluster.ddata.typed.scaladsl.DistributedData
import akka.cluster.ddata.typed.scaladsl.Replicator
import it.unibo.agar.distributed.model.Food
import it.unibo.agar.distributed.model.Player

object WorldSpectator:

  sealed trait SpectatorMessage
  sealed private trait InternalMessage extends SpectatorMessage

  case object Unsubscribe extends SpectatorMessage

  private case class InternalSubscribeResponse(
      rsp: Replicator.SubscribeResponse[ReplicatedData]
  ) extends InternalMessage

  private val foodKey: ORSetKey[Food] = ORSetKey("food-ddata")
  private val playersKey: LWWMapKey[String, Player] = LWWMapKey("players-ddata")

  def apply(stateChangedSlot: (Seq[Food], Seq[Player]) => Unit): Behavior[SpectatorMessage] =
    Behaviors.setup { context =>
      context.log.info("Created WorldSpectator")

      val replicator = DistributedData(context.system).replicator

      val subAdapter =
        context.messageAdapter[Replicator.SubscribeResponse[ReplicatedData]](InternalSubscribeResponse.apply)

      replicator ! Replicator.Subscribe(foodKey, subAdapter)
      replicator ! Replicator.Subscribe(playersKey, subAdapter)

      def updated(foods: Set[Food], players: Map[String, Player]): Behavior[SpectatorMessage] =
        Behaviors.receiveMessage {

          case InternalSubscribeResponse(chg @ Replicator.Changed(`foodKey`)) =>
            val newFoods = chg.get(foodKey).elements
            stateChangedSlot(newFoods.toSeq, players.values.toSeq)
            updated(newFoods, players)

          case InternalSubscribeResponse(chg @ Replicator.Changed(`playersKey`)) =>
            val newPlayers = chg.get(playersKey).entries
            stateChangedSlot(foods.toSeq, newPlayers.values.toSeq)
            updated(foods, newPlayers)

          case InternalSubscribeResponse(Replicator.Deleted(_)) =>
            Behaviors.same

          case Unsubscribe =>
            replicator ! Replicator.Unsubscribe(foodKey, subAdapter)
            replicator ! Replicator.Unsubscribe(playersKey, subAdapter)
            Behaviors.same

          case _ =>
            Behaviors.unhandled
        }

      updated(Set.empty, Map.empty)
    }

end WorldSpectator
