package it.unibo.agar.distributed.view

import it.unibo.agar.distributed.model.DistributedWorldState

import java.awt.Graphics2D
import scala.swing.{Dimension, MainFrame, Panel}

class GlobalView(manager: DistributedWorldState) extends MainFrame:

  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      val world = manager.getWorld
      AgarViewUtils.drawWorld(g, world)
