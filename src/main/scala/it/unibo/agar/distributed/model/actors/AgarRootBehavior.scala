package it.unibo.agar.distributed.model.actors

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import it.unibo.agar.distributed.model.GameStateManager
import it.unibo.agar.distributed.model.actors.FoodManager.FoodManagerMessage
import it.unibo.agar.distributed.model.actors.PlayerManager.PlayerManagerMessage
import it.unibo.agar.distributed.model.actors.WorldSpectator.SpectatorMessage
import it.unibo.agar.distributed.serviceKey
import scala.swing.Window

object AgarRootBehavior:

  def apply(manager: Option[GameStateManager] = None, views: Option[Seq[Window]] = None): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)

      if (cluster.selfMember.hasRole("food-mgr"))
        val numberOfGenerators = ctx.system.settings.config.getInt("agar.core.generators")
        val foodManager = ctx.spawn(FoodManager(numberOfGenerators), s"FoodManager")
        ctx.system.receptionist ! Receptionist.Register(serviceKey[FoodManagerMessage]("food-manager"), foodManager)

      if (cluster.selfMember.hasRole("player-mgr"))
        val playerManager = ctx.spawn(PlayerManager(), "PlayerManager")
        ctx.system.receptionist ! Receptionist.Register(serviceKey[PlayerManagerMessage]("player-manager"), playerManager)

      Behaviors.empty
  }

end AgarRootBehavior
