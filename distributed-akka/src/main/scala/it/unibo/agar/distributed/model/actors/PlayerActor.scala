package it.unibo.agar.distributed.model.actors

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import it.unibo.agar.distributed.model.Player

object PlayerActor:

  sealed trait Command

  final case class Initialize(player: Player, manager: ActorRef[PlayerManager.Command]) extends Command
  final case class Move(dx: Double, dy: Double) extends Command
  final case class ConsumeFood(mass: Double) extends Command

  final case class ConsumePlayer(mass: Double) extends Command

  case object Stop extends Command

  val TypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Player")

  private val WorldWidth = 1000.0
  private val WorldHeight = 1000.0
  private val Speed = 10.0

  def apply(id: String): Behavior[Command] =
    Behaviors.setup { _ =>
      def waiting(): Behavior[Command] =
        Behaviors.receiveMessage {
          case Initialize(player, manager) =>
            manager !
              PlayerManager.PlayerUpdated(player)
            running(player, manager)

          case Stop =>
            Behaviors.stopped

          case _ =>
            Behaviors.same
        }

      def running(
          player: Player,
          manager: ActorRef[PlayerManager.Command]
      ): Behavior[Command] =
        Behaviors.receiveMessage {

          case Move(dx, dy) =>
            val updated =
              player.copy(
                x = (player.x + dx * Speed)
                  .max(0.0)
                  .min(WorldWidth),
                y = (player.y + dy * Speed)
                  .max(0.0)
                  .min(WorldHeight)
              )

            manager !
              PlayerManager.PlayerUpdated(updated)

            running(updated, manager)

          case ConsumeFood(mass) =>
            val updated = player.copy(mass = player.mass + mass)
            manager !
              PlayerManager.PlayerUpdated(updated)

            running(updated, manager)

          case ConsumePlayer(mass) =>
            val updated = player.copy(mass = player.mass + mass)
            manager !
              PlayerManager.PlayerUpdated(updated)
            running(updated, manager)

          case Stop =>
            manager !
              PlayerManager.PlayerRemoved(player.id)
            Behaviors.stopped

          case Initialize(_, _) =>
            Behaviors.same
        }

      waiting()
    }

end PlayerActor
