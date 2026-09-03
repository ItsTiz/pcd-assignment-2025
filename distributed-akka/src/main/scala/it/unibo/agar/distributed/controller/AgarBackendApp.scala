package it.unibo.agar.distributed.controller

import com.typesafe.config.ConfigFactory
import it.unibo.agar.distributed.model.actors.backend.BackendRootBehavior
import it.unibo.agar.distributed.{seeds, startupWithRoles}

import scala.jdk.CollectionConverters.*

object AgarBackendApp:
  private val baseConfig = ConfigFactory.load("agario-game")
  private val backendRoles: Seq[String] = baseConfig.getStringList("agar.roles.backend").asScala.toSeq

  def main(args: Array[String]): Unit =
    if args.length == 2 then
      val role = args(0)
      val port = args(1).toInt
      println(s"Starting node with role=$role port=$port")
      startupWithRoles(port, role)(BackendRootBehavior())
    else
      println("No args provided. Starting default backend cluster.")
      seeds.foreach(port => startupWithRoles(port, backendRoles: _*)(BackendRootBehavior()))