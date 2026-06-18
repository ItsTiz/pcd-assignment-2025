package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.ORSet
import akka.cluster.ddata.ORSetKey
import akka.cluster.ddata.SelfUniqueAddress
import akka.cluster.ddata.typed.scaladsl.DistributedData
import akka.cluster.ddata.typed.scaladsl.Replicator
import it.unibo.agar.distributed.model.Food
import it.unibo.agar.distributed.model.GameStateManager
object WorldSpectator:

  sealed trait SpectatorMessage
  sealed private trait InternalMessage extends SpectatorMessage

  case object Unsubscribe extends SpectatorMessage
  private case class InternalSubscribeResponse(chg: Replicator.SubscribeResponse[ORSet[Food]]) extends InternalMessage
  private val key: ORSetKey[Food] = ORSetKey("food-ddata")

  def apply(stateChangedSlot: Seq[Food] => Unit): Behavior[SpectatorMessage] = Behaviors.setup { context =>
    context.log.info(s"Created spectator actor")

    implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress

    DistributedData.withReplicatorMessageAdapter[SpectatorMessage, ORSet[Food]] { replicatorAdapter =>
      replicatorAdapter.subscribe(key, InternalSubscribeResponse.apply)

      def updated(cachedValues: Set[Food]): Behavior[SpectatorMessage] =
        Behaviors.receiveMessage[SpectatorMessage] {
          case Unsubscribe =>
            replicatorAdapter.unsubscribe(key)
            Behaviors.same

          case internal: InternalMessage =>
            internal match
              case InternalSubscribeResponse(chg @ Replicator.Changed(`key`)) =>
                val elements = chg.get(key).elements
                stateChangedSlot(elements.toSeq)
                updated(elements)

              case InternalSubscribeResponse(Replicator.Deleted(_)) =>
                Behaviors.unhandled // no deletes

              case InternalSubscribeResponse(_) => // changed but wrong key
                Behaviors.unhandled
        }

      updated(Set.empty)
    }
  }

end WorldSpectator
