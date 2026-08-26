package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.agar.distributed.model.{Food, Player}
import it.unibo.agar.distributed.model.actors.WorldSpectator.SpectatorMessage
import it.unibo.agar.distributed.serviceKey

object SpectatorRootBehavior:

  def apply(stateChangedSlot: (Seq[Food], Seq[Player]) => Unit): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      if cluster.selfMember.hasRole("spectator") then
        val spectator = ctx.spawn(WorldSpectator(stateChangedSlot), "WorldSpectator")
        ctx.system.receptionist ! Receptionist.Register(serviceKey[SpectatorMessage]("world-spectator"), spectator)
      Behaviors.empty
    }

end SpectatorRootBehavior