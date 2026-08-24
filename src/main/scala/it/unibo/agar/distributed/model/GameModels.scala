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

  def addFood(element: Food): DistributedWorld =
    copy(foods = foods :+ element)

  def newFoods(elements: Seq[Food]): DistributedWorld =
    copy(foods = elements)

//  def removeFood(element: Food): DistributedWorld =
//    copy(foods = foods.(element))

  def removeFoods(ids: Seq[Food]): DistributedWorld =
    copy(foods = foods.filterNot(f => ids.contains(f)))

//  override def merge(that: DistributedWorld): DistributedWorld =
//    copy(foods = this.foods.merge(that.foods))

  def updatePlayer(player: Player): DistributedWorld =
    copy(
      players =
        players.map(p =>
          if p.id == player.id then player
          else p
        )
    )

  def addPlayer(player: Player): DistributedWorld =
    copy(players = players :+ player)

  def removePlayers(playersToRemove: Seq[Player]): DistributedWorld =
    val toRemove = playersToRemove.toSet
    copy(
      players = players.filterNot(p => toRemove.contains(p))
    )

  def playerById(id: String): Option[Player] =
    players.find(_.id == id)

  def playersExcludingSelf(player: Player): Seq[Player] =
    players.filterNot(_.id == player.id)

  def newPlayers(elements: Seq[Player]): DistributedWorld =
    copy(players = elements)