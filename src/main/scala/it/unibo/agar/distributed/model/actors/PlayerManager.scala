package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object PlayerManager:

  sealed trait PlayerManagerMessage

  def apply(): Behavior[PlayerManagerMessage] =
    Behaviors.empty;

end PlayerManager