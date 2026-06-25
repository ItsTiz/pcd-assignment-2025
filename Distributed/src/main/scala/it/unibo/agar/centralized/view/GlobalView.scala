package it.unibo.agar.centralized.view

import it.unibo.agar.centralized.model.MockGameStateManager

import java.awt.Graphics2D

class GlobalView(manager: MockGameStateManager) extends MainFrame:

  title = "Agar.io - Global View"
  preferredSize = new Dimension(800, 800)

  contents = new Panel:
    override def paintComponent(g: Graphics2D): Unit =
      val world = manager.getWorld
      AgarViewUtils.drawWorld(g, world)
