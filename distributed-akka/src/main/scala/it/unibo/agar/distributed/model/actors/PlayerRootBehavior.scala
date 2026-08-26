package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}

import it.unibo.agar.distributed.model.{Food, Player}

object PlayerRootBehavior:

  def apply(player: Player, stateChangedSlot: (Seq[Food], Seq[Player]) => Unit): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      ClusterSharding(ctx.system).init(Entity(PlayerActor.TypeKey)(entityCtx => PlayerActor(entityCtx.entityId)))

      ctx.spawn(WorldSpectator(stateChangedSlot), "WorldSpectator")
      val joinListener = ctx.spawn(
        Behaviors.receive[Receptionist.Listing] { (_, listing) =>
          listing.serviceInstances(PlayerManager.Key).headOption.foreach(_ ! PlayerManager.Join(player))
          Behaviors.stopped
        },
        "PlayerJoinListener"
      )
      ctx.system.receptionist ! Receptionist.Find(PlayerManager.Key, joinListener)

      Behaviors.empty
    }

end PlayerRootBehavior