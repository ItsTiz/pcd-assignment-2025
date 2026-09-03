package it.unibo.agar.distributed.model.actors.backend.food

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.ShardedDaemonProcessSettings
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import it.unibo.agar.distributed.model.actors.backend.food.FoodGenerator.FoodGeneratorMessage

object FoodManager:

  sealed trait FoodManagerMessage

  def apply(nGenerators: Int): Behavior[FoodManagerMessage] =
    Behaviors.setup { context =>
      val system = context.system
      context.log.info(s"Starting $nGenerators generators...")

      val settings = ShardedDaemonProcessSettings(system).withRole("backend")

      ShardedDaemonProcess(system).init(
        name = "food-generation",
        numberOfInstances = 4,
        behaviorFactory = id => FoodGenerator(id),
        settings = settings,
        stopMessage = Some(FoodGenerator.StopGeneration)
      )

      Behaviors.empty;
    }

end FoodManager
