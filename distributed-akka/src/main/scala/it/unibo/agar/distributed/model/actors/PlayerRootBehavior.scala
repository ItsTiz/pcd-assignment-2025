package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import it.unibo.agar.distributed.model.actors.backend.players.{PlayerActor, PlayerJoinListener, PlayerManager}
import it.unibo.agar.distributed.model.{Food, Player}

object PlayerRootBehavior:

  def apply(player: Player, stateChangedSlot: (Seq[Food], Seq[Player], String) => Unit): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      ClusterSharding(ctx.system).init(Entity(PlayerActor.TypeKey)(entityCtx => PlayerActor(entityCtx.entityId)))

      ctx.spawn(WorldSpectator(stateChangedSlot), "WorldSpectator")
      val joinListenerRef = ctx.spawn(PlayerJoinListener(player), "PlayerJoinListener")
      ctx.system.receptionist ! Receptionist.Subscribe(PlayerManager.Key, joinListenerRef)
      Behaviors.empty
    }

end PlayerRootBehavior
