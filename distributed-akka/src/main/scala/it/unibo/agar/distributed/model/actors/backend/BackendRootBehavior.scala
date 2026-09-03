package it.unibo.agar.distributed.model.actors.backend

import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.Cluster
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, Entity}
import akka.cluster.typed.{ClusterSingleton, SingletonActor}
import it.unibo.agar.distributed.model.actors.backend.food.FoodManager
import it.unibo.agar.distributed.model.actors.backend.players.{PlayerActor, PlayerManager}

object BackendRootBehavior:

  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      val sharding = ClusterSharding(ctx.system)
      val singletonManager = ClusterSingleton(ctx.system)

      sharding.init(Entity(PlayerActor.TypeKey)(ctx => PlayerActor(ctx.entityId)).withRole("backend"))

      if cluster.selfMember.hasRole("food-mgr") then
        val numberOfGenerators = ctx.system.settings.config.getInt("agar.core.generators")
        ctx.spawn(FoodManager(numberOfGenerators), "FoodManager")
      if cluster.selfMember.hasRole("player-mgr") then
        val proxy = singletonManager.init(
          SingletonActor(
            Behaviors.supervise(PlayerManager()).onFailure(SupervisorStrategy.restart),
            "GlobalPlayerManager"
          )
        )

      Behaviors.empty
    }
