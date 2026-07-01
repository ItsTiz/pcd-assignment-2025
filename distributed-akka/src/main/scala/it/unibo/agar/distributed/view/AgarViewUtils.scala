package it.unibo.agar.distributed.view

import it.unibo.agar.distributed.model.DistributedWorld

import java.awt.{Color, Graphics2D}

object AgarViewUtils:

  private val WorldWidth = 1000
  private val WorldHeight = 1000

  private val playerBorderColor = Color.black
  private val playerLabelOffsetX = 20
  private val playerLabelOffsetY = 0

  private val playerInnerOffset = 2
  private val playerInnerBorder = 4

  private val playerPalette: Array[Color] =
    Array(
      Color.blue,
      Color.orange,
      Color.cyan,
      Color.pink,
      Color.yellow,
      Color.red,
      Color.green,
      Color.lightGray
    )

  private def playerColor(id: String): Color =
      id match
        case pid if pid.startsWith("p") =>
          val idx = pid.drop(1).toIntOption.getOrElse(0)
          playerPalette(idx % playerPalette.length)
        case _ =>
          Color.gray

  def drawWorld(g: Graphics2D, world: DistributedWorld, offsetX: Double = 0, offsetY: Double = 0, localPlayerId: Option[String] = None): Unit =

    def toScreenCenter(x: Double, y: Double, radius: Int): (Int, Int) =
      ((x - offsetX - radius).toInt, (y - offsetY - radius).toInt)

    def toScreenLabel(x: Double, y: Double): (Int, Int) =
      ((x - offsetX + playerLabelOffsetX).toInt, (y - offsetY + playerLabelOffsetY).toInt)

  // Draw foods
    g.setColor(Color.green)
    world.foods.foreach: food =>
      val radius = food.radius.toInt
      val diameter = radius * 2
      val (foodX, foodY) = toScreenCenter(food.x, food.y, radius)
      g.fillOval(foodX, foodY, diameter, diameter)


    // Draw players
    world.players.foreach { player =>
      val radius = player.radius.toInt
      val diameter = radius * 2
      val (borderX, borderY) = toScreenCenter(player.x, player.y, radius)

      g.setColor(playerBorderColor)
      g.drawOval(borderX, borderY, diameter, diameter)
      g.setColor(playerColor(player.id))

      val (innerX, innerY) = toScreenCenter(player.x, player.y, radius - playerInnerOffset)
      g.fillOval(innerX, innerY, diameter - playerInnerBorder, diameter - playerInnerBorder)
      g.setColor(playerBorderColor)
      val (labelX, labelY) = toScreenLabel(player.x, player.y)
      g.drawString(s"${player.id} (${player.mass.toInt})", labelX, labelY)

      // local player highlight
      if localPlayerId.contains(player.id) then
        g.setColor(Color.white)
        g.drawOval(borderX - 3, borderY - 3, diameter + 6, diameter + 6)
    }

    val leaderboard = world.players.sortBy(p => -p.mass).take(5)
    g.setColor(Color.black)
    g.drawString("Leaderboard", 10, 20)
    leaderboard.zipWithIndex.foreach {
      case (player, idx) =>
        g.drawString(
          s"${idx + 1}. ${player.id}: ${player.mass.toInt}",
          10,
          40 + idx * 18
        )
    }