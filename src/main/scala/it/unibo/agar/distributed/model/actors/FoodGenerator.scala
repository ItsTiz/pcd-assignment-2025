package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.SelfUniqueAddress
import akka.cluster.ddata.typed.scaladsl.DistributedData
import it.unibo.agar.distributed.model.GameInitializer
import it.unibo.agar.distributed.serviceKey

import java.time.InstantSource.system

object FoodGenerator:

  sealed trait FoodGeneratorMessage

  final case class StopGeneration() extends FoodGeneratorMessage
  final case class StartGeneration() extends FoodGeneratorMessage

  def apply(id: Int): Behavior[FoodGeneratorMessage] =
    Behaviors.setup { context =>
      println(s"Created sharded actor with id: $id")

      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress
      val sKey = serviceKey[FoodGeneratorMessage]("food-gen", id)

      context.system.receptionist ! Receptionist.Register(sKey, context.self)
      //val foods = GameInitializer.initialFoods(numFoods, width, height)

      def generating(): Behavior[FoodGeneratorMessage] =
        Behaviors.receiveMessage {
          case StartGeneration() =>
            Behaviors.empty
          case StopGeneration() =>
            Behaviors.stopped
        }

      def idle(): Behavior[FoodGeneratorMessage] =
        Behaviors.receiveMessage {
          case StopGeneration() =>
            Behaviors.empty
          case StartGeneration() =>
            Behaviors.empty
        }

      generating()
    }

end FoodGenerator
