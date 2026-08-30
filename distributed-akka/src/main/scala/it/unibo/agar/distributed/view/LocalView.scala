package it.unibo.agar.distributed.view

import it.unibo.agar.distributed.model.GameStateManager
import java.awt.Graphics2D
import javax.swing.Timer
import scala.swing.*

class LocalView(manager: GameStateManager, playerId: String) extends MainFrame:

  title = s"Agar.io - Local View ($playerId)"
  preferredSize = new Dimension(400, 400)

  private val MoveIntervalMillis = 50
  
  @volatile private var desiredDx: Double = 0.0
  @volatile private var desiredDy: Double = 0.0

  contents = new Panel:
    listenTo(keys, mouse.moves)
    focusable = true
    requestFocusInWindow()

    override def paintComponent(g: Graphics2D): Unit =
      val world = manager.getWorld
      val playerOpt = world.players.find(_.id == playerId)
      val (offsetX, offsetY) = playerOpt
        .map(p => (p.x - size.width / 2.0, p.y - size.height / 2.0))
        .getOrElse((0.0, 0.0))
      AgarViewUtils.drawWorld(g, world, offsetX, offsetY, Some(playerId))

    reactions += { case e: event.MouseMoved =>
    
      val mousePos = e.point
      desiredDx = (mousePos.x - size.width / 2) * 0.01
      desiredDy = (mousePos.y - size.height / 2) * 0.01
    }

  // invia il comando Move a intervalli fissi, indipendentemente dalla frequenza del mouse
  private val moveTimer = new Timer(MoveIntervalMillis, _ => {
    if desiredDx != 0.0 || desiredDy != 0.0 then
      manager.movePlayerDirection(playerId, desiredDx, desiredDy)
  })
  moveTimer.start()