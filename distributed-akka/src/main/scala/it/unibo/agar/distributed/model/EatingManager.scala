package it.unibo.agar.distributed.model

object EatingManager:

  final case class TickResult(finalPlayer: Player, eatenFoods: Set[Food], eatenPlayers: Iterable[String])

  private val MASS_MARGIN = 1.1 // 10% bigger to eat

  // Check if two entities collide
  private def collides(e1: Entity, e2: Entity): Boolean =
    e1.distanceTo(e2) < (e1.radius + e2.radius)

  // Determines if a player can eat a food
  def canEatFood(player: Player, food: Food): Boolean =
    collides(player, food) && player.mass > food.mass

  // Determines if a player can eat another player
  def canEatPlayer(player: Player, other: Player): Boolean =
    collides(player, other) && player.mass > other.mass * MASS_MARGIN

  def evaluateCollisions(player: Player, foods: Set[Food], players: Map[String, Player]): TickResult =
    val ps = players.values.toSeq.sortBy(p => (-p.mass, p.id))
    val fs = foods.toSeq

    val eatenFoods = fs.filter(food => canEatFood(player, food)).toSet
    val massFromFood = eatenFoods.map(_.mass).sum

    val eatenPlayers = players.filter((_, victim) => canEatPlayer(player, victim))
    val massFromPlayers = eatenPlayers.values.toSeq.map(_.mass).sum

    val finalPlayer = player.copy(mass = player.mass + massFromPlayers + massFromFood)

    TickResult(finalPlayer, eatenFoods, eatenPlayers.keys)
