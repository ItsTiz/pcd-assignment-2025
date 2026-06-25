package it.unibo.agar.distributed.model.actors

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