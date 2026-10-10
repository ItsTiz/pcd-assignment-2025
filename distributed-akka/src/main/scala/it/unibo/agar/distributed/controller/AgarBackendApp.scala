package it.unibo.agar.distributed.controller

import com.typesafe.config.ConfigFactory
import it.unibo.agar.distributed.model.actors.backend.BackendRootBehavior
import it.unibo.agar.distributed.{seeds, startupWithRoles}

import scala.jdk.CollectionConverters.*

object AgarBackendApp:
  private val baseConfig = ConfigFactory.load("agario-game")
  private val backendRoles: Seq[String] = baseConfig.getStringList("agar.roles.backend").asScala.toSeq

  def main(args: Array[String]): Unit =
    if args.nonEmpty then
      val port = args(0).toInt
      val roles = if args.length > 1 then args.drop(1).toSeq else backendRoles
      println(s"[Backend] Starting from CLI on port=$port with roles=${roles.mkString(",")}")
      startupWithRoles(port, roles:_*)(BackendRootBehavior())
    else
      println("[Backend] Starting from environment variables / agario-game.conf")
      startupWithRoles(port = 0, backendRoles:_*)(BackendRootBehavior())