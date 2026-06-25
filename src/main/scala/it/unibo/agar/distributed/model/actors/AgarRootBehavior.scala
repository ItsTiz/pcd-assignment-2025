package it.unibo.agar.distributed.model.actors

import akka.actor.typed.{Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}

object AgarRootBehavior:

  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>

      val cluster = Cluster(ctx.system)
      val sharding = ClusterSharding(ctx.system)

      sharding.init(Entity(PlayerActor.TypeKey)(PlayerActor(_)))

      if cluster.selfMember.hasRole("food-mgr") then
        val numberOfGenerators =
          ctx.system.settings.config.getInt("agar.core.generators")

        ctx.spawn(FoodManager(numberOfGenerators), "FoodManager")

      if cluster.selfMember.hasRole("player-mgr") then
        ctx.spawn(PlayerManager(), "PlayerManager")

      Behaviors.empty
    }