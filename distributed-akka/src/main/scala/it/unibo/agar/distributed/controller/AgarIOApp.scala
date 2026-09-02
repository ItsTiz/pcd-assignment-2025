package it.unibo.agar.distributed.controller

import it.unibo.agar.distributed.model.actors.AgarRootBehavior
import it.unibo.agar.distributed.{seeds, startupWithRole, startupBackendNode}

object AgarIOApp:

  def main(args: Array[String]): Unit =
    if args.length == 2 then
      val role = args(0)
      val port = args(1).toInt

      println(s"Starting node with role=$role port=$port")

      startupWithRole(role, port)(AgarRootBehavior())
    else
      println("No args provided → starting local cluster demo")
      seeds.foreach(port => startupBackendNode(port)(AgarRootBehavior()))