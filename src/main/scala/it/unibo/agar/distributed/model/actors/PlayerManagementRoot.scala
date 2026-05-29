package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object PlayerManagementRoot:

  def apply(): Behavior[Unit] =
    Behaviors.empty;

end PlayerManagementRoot