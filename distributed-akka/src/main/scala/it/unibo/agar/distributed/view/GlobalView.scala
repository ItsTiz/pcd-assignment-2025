package it.unibo.agar.distributed.view

import it.unibo.agar.distributed.model.DistributedWorld

import java.awt.Graphics2D
import scala.swing.{Dimension, MainFrame, Panel}

class GlobalView(world: DistributedWorld) extends MainFrame:

  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      AgarViewUtils.drawWorld(g, world)
