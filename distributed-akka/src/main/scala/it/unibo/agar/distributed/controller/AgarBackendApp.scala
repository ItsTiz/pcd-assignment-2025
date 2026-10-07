package it.unibo.agar.distributed.controller

import com.typesafe.config.ConfigFactory
import it.unibo.agar.distributed.model.actors.backend.BackendRootBehavior
import it.unibo.agar.distributed.{seeds, startupWithRoles}

import scala.jdk.CollectionConverters.*

object AgarBackendApp:
  private val baseConfig = ConfigFactory.load("agario-game")
  private val backendRoles: Seq[String] = baseConfig.getStringList("agar.roles.backend").asScala.toSeq

  def main(args: Array[String]): Unit =
    if args.length > 0 then
      val port = args.head.toInt
      val roles = args.tail
      println(s"Starting node with roles=${roles.mkString("Array(", ", ", ")")} port=$port")
      startupWithRoles(port, roles: _*)(BackendRootBehavior())
    else
      println("No args provided. Starting default backend cluster.")
      seeds.foreach(port => startupWithRoles(port, backendRoles: _*)(BackendRootBehavior()))