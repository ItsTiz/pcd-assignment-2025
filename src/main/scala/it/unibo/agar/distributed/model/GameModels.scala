package it.unibo.agar.distributed.model
import akka.cluster.ddata.{ORSet, ReplicatedData, SelfUniqueAddress}

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

case class Player(id: String, x: Double, y: Double, mass: Double) extends Entity:

  def grow(entity: Entity): Player =
    copy(mass = mass + entity.mass)

case class Food(id: String, x: Double, y: Double, mass: Double = 100.0) extends Entity

case class DistributedWorld(
    width: Int,
    height: Int,
    players: Seq[Player],
    foods: ORSet[Food]
) extends ReplicatedData:

  type T = DistributedWorld

  def addFood(element: Food)(implicit node: SelfUniqueAddress): DistributedWorld =
    copy(foods = foods :+ element)

  def removeFood(element: Food)(implicit node: SelfUniqueAddress): DistributedWorld =
    copy(foods = foods.remove(element))

  def foodElements: Set[Food] = foods.elements

  override def merge(that: DistributedWorld): DistributedWorld =
    copy(foods = this.foods.merge(that.foods))
