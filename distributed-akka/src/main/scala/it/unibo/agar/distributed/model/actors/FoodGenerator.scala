package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.ORSet
import akka.cluster.ddata.ORSetKey
import akka.cluster.ddata.SelfUniqueAddress
import akka.cluster.ddata.typed.scaladsl.DistributedData
import akka.cluster.ddata.typed.scaladsl.Replicator
import it.unibo.agar.distributed.model.Food
import it.unibo.agar.distributed.serviceKey

import scala.concurrent.duration.DurationLong
import scala.util.Random

object FoodGenerator:

  sealed trait FoodGeneratorMessage

  case object StopGeneration extends FoodGeneratorMessage
  case object StartGeneration extends FoodGeneratorMessage
  case object PauseGeneration extends FoodGeneratorMessage

  private case object Tick extends FoodGeneratorMessage
  private case object TimerKey

  sealed private trait InternalMessage extends FoodGeneratorMessage
  private case class InternalUpdateResponse(rsp: Replicator.UpdateResponse[ORSet[Food]]) extends InternalMessage

  private def generatingFunction(offX: Int, offY: Int, deltaX: Int, deltaY: Int): Food =
    Food(s"f${Random.nextInt(1000)}", Random.nextInt(deltaX) + offX * deltaX, Random.nextInt(deltaY) + offY * deltaY)

  def apply(id: Int): Behavior[FoodGeneratorMessage] =
    Behaviors.setup { context =>
      context.log.info(s"Created sharded actor with id: $id")

      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress
      val sKey = serviceKey[FoodGeneratorMessage]("food-gen", id)
      val updateInterval = context.system.settings.config.getInt("agar.game.food-spawn-interval")
      val mapWidth = context.system.settings.config.getInt("agar.game.map-width")
      val mapHeight = context.system.settings.config.getInt("agar.game.map-height")

      val offsetX = id % 2
      val offSetY = id / 2

      context.system.receptionist ! Receptionist.Register(sKey, context.self)
      // val foods = GameInitializer.initialFoods(numFoods, width, height)

      Behaviors.withTimers { timers =>
        timers.startTimerAtFixedRate(TimerKey, Tick, (updateInterval * 1000).toLong.millis)

        def waitTrigger: Behavior[FoodGeneratorMessage] =
          Behaviors.receiveMessage {
            case Tick =>
              DistributedData.withReplicatorMessageAdapter[FoodGeneratorMessage, ORSet[Food]] { replicatorAdapter =>
                replicatorAdapter.askUpdate(
                  Replicator.Update(ORSetKey("food-ddata"), ORSet.empty, Replicator.WriteLocal)(
                    _ :+ generatingFunction(offsetX, offSetY, mapWidth / 2, mapHeight / 2)
                  ),
                  InternalUpdateResponse.apply
                )
                Behaviors.same
              }
            case StartGeneration =>
              context.log.info("Starting food generation...")
              Behaviors.same
            case PauseGeneration =>
              context.log.info("Pausing food generation...")
              idle
            case StopGeneration =>
              context.log.info("Stopping food generation...")
              Behaviors.stopped
            case internal: InternalMessage =>
              internal match {
                case InternalUpdateResponse(_) =>
                  context.log.info("New food registered successfully in replicated data")
                  Behaviors.same
              }
          }

        def idle: Behavior[FoodGeneratorMessage] =
          Behaviors.receiveMessage {
            case StartGeneration =>
              waitTrigger
            case StopGeneration =>
              Behaviors.stopped
            case _ => Behaviors.same
          }

        waitTrigger
      }

    }

end FoodGenerator
