package it.unibo.agar.distributed.controller

import it.unibo.agar.distributed.model.actors.{FoodManagementRoot, PlayerManagementRoot}
import it.unibo.agar.distributed.{seeds, startup, startupWithRole}

object AgarIOCluster:

  def main(args: Array[String]): Unit = args match
    case args if args.isEmpty =>
      seeds.map(seed => startupWithRole("food-management", seed)(FoodManagementRoot()))
    case _ =>
      require(args.length == 2, "Usage: role port")
       args(0) match
        case food @ "food-management" =>
          startupWithRole(food, args(1).toInt)(FoodManagementRoot())
        case player @ "player-management" =>
          startupWithRole(player, args(1).toInt)(PlayerManagementRoot())

end AgarIOCluster

