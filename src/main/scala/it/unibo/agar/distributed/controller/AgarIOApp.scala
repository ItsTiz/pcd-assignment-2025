package it.unibo.agar.distributed.controller

import it.unibo.agar.distributed.model.actors.AgarRootBehavior
import it.unibo.agar.distributed.{seeds, startupWithRole}

object AgarIOApp:

  def main(args: Array[String]): Unit =
    args match
      case args if args.nonEmpty =>
        require(args.length == 2, "Usage: role port")
        startupWithRole(args(0), args(1).toInt)(AgarRootBehavior())
      case _ =>
        print("Called main with empty args.")
        seeds.map(seed => startupWithRole("food-mgr, player-mgr", seed)(AgarRootBehavior()))
        startupWithRole("food-mgr, player-mgr", 0)(AgarRootBehavior())
end AgarIOApp

