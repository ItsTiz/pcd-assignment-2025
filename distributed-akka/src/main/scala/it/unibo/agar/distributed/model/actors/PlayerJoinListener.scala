package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.agar.distributed.model.Player

object PlayerJoinListener:
  def apply(player: Player): Behavior[Receptionist.Listing] =
    Behaviors.setup { context =>
      def waiting(): Behavior[Receptionist.Listing] =
        Behaviors.receive[Receptionist.Listing] { (ctx, listing) =>

          val results = listing.serviceInstances(PlayerManager.Key)
          if (results.isEmpty) {
            ctx.log.info("Waiting to join...")
            Behaviors.same
          } else {
            ctx.log.info("Contacting manager to join game...")
            results.headOption.foreach(_ ! PlayerManager.Join(player))
            Behaviors.stopped
          }
        }
      waiting()
    }

end PlayerJoinListener
