package it.unibo.agar.distributed.model

import it.unibo.agar.distributed.model.serializables.CborSerializable

sealed trait Entity:

  def id: String
  def mass: Double
  def x: Double
  def y: Double
  def radius: Double = math.sqrt(mass / math.Pi)

  def distanceTo(other: Entity): Double =
    val dx = x - other.x
    val dy = y - other.y
    math.hypot(dx, dy)

case class Player(id: String, x: Double, y: Double, mass: Double) extends Entity with CborSerializable:

  def grow(entity: Entity): Player =
    copy(mass = mass + entity.mass)

case class Food(id: String, x: Double, y: Double, mass: Double = 100.0) extends Entity with CborSerializable

case class DistributedWorld(
    width: Int,
    height: Int,
    players: Seq[Player],
    foods: Seq[Food]
):

  type T = DistributedWorld

  def newFoods(elements: Seq[Food]): DistributedWorld =
    copy(foods = elements)

  def newPlayers(elements: Seq[Player]): DistributedWorld =
    copy(players = elements)

  def playerById(id: String): Option[Player] =
    players.find(_.id == id)
