package it.unibo.agar.distributed

import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.{ActorSystem, Behavior}
import com.typesafe.config.ConfigFactory

import scala.reflect.ClassTag

val seeds = List(25251, 25252) // seed used in the configuration

def startup[X](file: String = "base-cluster", port: Int)(root: => Behavior[X]): ActorSystem[X] =
  // Override the configuration of the port
  val config = ConfigFactory
    .parseString(s"""akka.remote.artery.canonical.port=$port""")
    .withFallback(ConfigFactory.load(file))

  // Create an Akka system
  ActorSystem(root, file, config)

def startupWithRole[X](role: String, port: Int)(root: => Behavior[X]): ActorSystem[X] =
  val config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
    .withFallback(ConfigFactory.load("agario-game"))

  // Create an Akka system
  ActorSystem(root, "agario-cluster", config)

def serviceKey[T](nameTag: String, id: Int = 0)(implicit classTag: ClassTag[T]) = ServiceKey[T](s"$nameTag-$id")
