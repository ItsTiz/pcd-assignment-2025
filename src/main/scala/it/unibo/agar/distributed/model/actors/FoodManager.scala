package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.sharding.typed.scaladsl.ShardedDaemonProcess
import it.unibo.agar.distributed.model.actors.FoodGenerator.FoodGeneratorMessage

object FoodManager:

  sealed trait FoodManagerMessage

  def apply(nGenerators: Int): Behavior[FoodManagerMessage] =
    Behaviors.setup { context =>

      val system = context.system
      val mapSectors = 4

      context.log.info(s"Starting $nGenerators generators...")

      ShardedDaemonProcess(system).init[FoodGeneratorMessage](
        name = "food-generation",
        numberOfInstances = mapSectors,
        behaviorFactory = id => FoodGenerator(id),
        stopMessage = FoodGenerator.StopGeneration()
      )
      
      Behaviors.empty;
    }



end FoodManager
