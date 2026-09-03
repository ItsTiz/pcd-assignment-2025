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
  
  ActorSystem(root, file, config)

def startupWithRoles[X](port: Int, roles: String*)(root: => Behavior[X]): ActorSystem[X] =
  val rolesHocon = roles.map(r => s""""$r"""").mkString("[", ", ", "]")
  val config = ConfigFactory
    .parseString(s"""
      akka.remote.artery.canonical.port = $port
      akka.cluster.roles = $rolesHocon
      """)
    .withFallback(ConfigFactory.load("agario-game"))
  
  ActorSystem(root, "agario-cluster", config)

def serviceKey[T](nameTag: String, id: Int = 0)(implicit classTag: ClassTag[T]) = ServiceKey[T](s"$nameTag-$id")
