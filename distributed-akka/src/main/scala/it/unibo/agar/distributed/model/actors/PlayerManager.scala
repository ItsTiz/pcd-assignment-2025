package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ddata.*
import akka.cluster.ddata.typed.scaladsl.{DistributedData, Replicator}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import it.unibo.agar.distributed.model.Player
import it.unibo.agar.distributed.model.serializables.CborSerializable
import it.unibo.agar.distributed.serviceKey

object PlayerManager:

  sealed trait Command extends CborSerializable

  final case class Join(player: Player) extends Command
  final case class Leave(id: String) extends Command
  final case class PlayerUpdated(player: Player) extends Command

  private case class InternalUpdateResponse(rsp: Replicator.UpdateResponse[LWWMap[String, Player]]) extends Command

  private val playersKey = LWWMapKey[String, Player]("players-ddata")

  val Key: ServiceKey[Command] = serviceKey[Command]("player-manager")

  private def playerRef(sharding: ClusterSharding, id: String) = sharding.entityRefFor(PlayerActor.TypeKey, id)

  def apply(): Behavior[Command] =
    Behaviors.setup { context =>
      val sharding = ClusterSharding(context.system)
      val replicator = DistributedData(context.system).replicator
      val updateAdapter = context.messageAdapter[Replicator.UpdateResponse[LWWMap[String, Player]]](InternalUpdateResponse.apply)
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress

      context.system.receptionist ! Receptionist.Register(Key, context.self)

      def publishPlayer(p: Player): Unit =
        replicator ! Replicator.Update(
          playersKey,
          LWWMap.empty[String, Player],
          Replicator.WriteLocal,
          updateAdapter
        )(_.:+ (p.id, p))

      def retractPlayer(id: String): Unit =
        replicator ! Replicator.Update(
          playersKey,
          LWWMap.empty[String, Player],
          Replicator.WriteLocal,
          updateAdapter
        )(_.remove(node, id))

      def running(): Behavior[Command] =
        Behaviors.receiveMessage {

          case InternalUpdateResponse(_) =>
            Behaviors.same

          case Join(player) =>
            context.log.info(s"Player joined the game with id ${player.id}")
            playerRef(sharding, player.id) ! PlayerActor.Initialize(player, context.self)
            publishPlayer(player)
            running()

          case Leave(id) =>
            context.log.info(s"Player leaving the game with id $id")
            playerRef(sharding, id) ! PlayerActor.Stop
            retractPlayer(id)
            running()

          case PlayerUpdated(p) =>
            publishPlayer(p)
            running()
        }

      running()

    }

end PlayerManager
