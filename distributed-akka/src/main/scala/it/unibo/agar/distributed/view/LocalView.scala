package it.unibo.agar.distributed.view

import it.unibo.agar.distributed.model.GameStateManager

import java.awt.Graphics2D
import javax.swing.Timer
import scala.swing.*

class LocalView(manager: GameStateManager, playerId: String) extends MainFrame:

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(400, 400)

  private val MoveIntervalMillis = 30

  @volatile private var desiredDx: Double = 0.0
  @volatile private var desiredDy: Double = 0.0

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      val world = manager.getWorld
      val playerOpt = world.playerById(playerId)
      val (offsetX, offsetY) = playerOpt
        .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
        .getOrElse((0.0, 0.0))
      AgarViewUtils.drawWorld(g, world, offsetX, offsetY, Some(playerId))

    reactions += { case e: event.MouseMoved =>

      val mousePos = e.point
      val rawX = mousePos.x - size.width / 2.0
      val rawY = mousePos.y - size.height / 2.0
      val length = Math.hypot(rawX, rawY)

      if length > 5.0 then
        desiredDx = rawX / length
        desiredDy = rawY / length
      else
        desiredDx = 0.0
        desiredDy = 0.0
    }

  // invia il comando Move a intervalli fissi, indipendentemente dalla frequenza del mouse
  private val moveTimer = new Timer(
    MoveIntervalMillis,
    _ => {
        if desiredDx != 0.0 || desiredDy != 0.0 then {
          manager.movePlayerDirection(playerId, desiredDx, desiredDy)
        }
    }
  )
  moveTimer.setRepeats(!manager.getWorld.hasWinner)
  moveTimer.start()
