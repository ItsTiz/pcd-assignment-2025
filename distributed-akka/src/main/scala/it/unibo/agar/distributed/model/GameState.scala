package it.unibo.agar.distributed.model

trait GameStateManager:

  def getWorld: DistributedWorld
  def movePlayerDirection(id: String, dx: Double, dy: Double): Unit

class DistributedWorldState(var world: DistributedWorld):

  def getWorld: DistributedWorld = world
  def setWorld(newWorld: DistributedWorld): Unit = world = newWorld

end DistributedWorldState
