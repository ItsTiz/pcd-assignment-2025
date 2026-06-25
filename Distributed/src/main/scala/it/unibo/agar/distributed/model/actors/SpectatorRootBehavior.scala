package it.unibo.agar.distributed.model.actors

import it.unibo.agar.distributed.model.Food
import it.unibo.agar.distributed.serviceKey

object SpectatorRootBehavior:

  def apply(stateChangedSlot: Seq[Food]  => Unit): Behavior[Nothing] =
    Behaviors.setup[Nothing] {
      ctx =>
        val cluster = Cluster(ctx.system)
        if (cluster.selfMember.hasRole("spectator"))
          val spectator = ctx.spawn(WorldSpectator(stateChangedSlot), "WorldSpectator")
          ctx.system.receptionist ! Receptionist.Register(serviceKey[SpectatorMessage]("world-spectator"), spectator)

      Behaviors.empty
    }

end SpectatorRootBehavior
