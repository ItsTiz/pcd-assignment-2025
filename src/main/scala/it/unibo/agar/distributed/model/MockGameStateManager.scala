package it.unibo.agar.distributed.model

trait GameStateManager:

  def getWorld: DistributedWorld
  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit

class DistributedStateManager(
  var world: DistributedWorld,
  val speed: Double = 10.0
) extends GameStateManager:

  def getWorld: DistributedWorld = world

  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit = ???

end DistributedStateManager

//class MockGameStateManager(
//    var world: DistributedWorld,
//    val speed: Double = 10.0
//) extends GameStateManager:
//
//  private var directions: Map[String, (Double, Double)] = Map.empty
//  def getWorld: DistributedWorld = world
//
//  // Move a player in a given direction (dx, dy)
//  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit =
//    directions = directions.updated(id, (dx, dy))

//  def tick(): Unit =
//    directions.foreach:
//      case (id, (dx, dy)) =>
//        world.playerById(id) match
//          case Some(player) =>
//            world = updateWorldAfterMovement(updatePlayerPosition(player, dx, dy))
//          case None =>
//          // Player not found, ignore movement
//
//  private def updatePlayerPosition(player: Player, dx: Double, dy: Double): Player =
//    val newX = (player.x + dx * speed).max(0).min(world.width)
//    val newY = (player.y + dy * speed).max(0).min(world.height)
//    player.copy(x = newX, y = newY)
//
//  private def updateWorldAfterMovement(player: Player): DistributedWorld =
//    val foodEaten = world.foods.elements.filter(food => EatingManager.canEatFood(player, food))
//    val playerEatsFood = foodEaten.foldLeft(player)((p, food) => p.grow(food))
//    val playersEaten = world
//      .playersExcludingSelf(player)
//      .filter(player => EatingManager.canEatPlayer(playerEatsFood, player))
//    val playerEatPlayers = playersEaten.foldLeft(playerEatsFood)((p, other) => p.grow(other))
//    world
//      .updatePlayer(playerEatPlayers)
//      .removePlayers(playersEaten)
//      .removeFoods(foodEaten)