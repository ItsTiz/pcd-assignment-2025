package it.unibo.agar.distributed.model

object GameManager:

  final case class TickResult(
                               foodEatenByPlayer: Map[String, Set[Food]],
                               eatenFoods: Set[Food],
                               massGainedFromPlayers: Map[String, Double],
                               eatenPlayers: Set[String]
                             )

  def resolveTick(players: Map[String, Player], foods: Set[Food]): TickResult =
    val ps = players.values.toSeq.sortBy(p => (-p.mass, p.id))
    val fs = foods.toSeq

    val foodWinners =
      fs.flatMap { food =>
        ps.filter(p => EatingManager.canEatFood(p, food))
          .sortBy(p => (-p.mass, p.id))
          .headOption
          .map(food -> _)
      }.toMap

    val foodByPlayer =
      foodWinners.groupMap(_._2.id)(_._1).view.mapValues(_.toSet).toMap
    val eatenFoods = foodWinners.keySet

    val (eatenPlayers, massGained) =
      ps.foldLeft((Set.empty[String], Map.empty[String, Double])) { case ((dead, gained), predator) =>
        if dead.contains(predator.id) then (dead, gained)
        else
          val victims =
            ps.filter(o => o.id != predator.id && !dead.contains(o.id) && EatingManager.canEatPlayer(predator, o))
          if victims.nonEmpty then
            val mass = victims.map(_.mass).sum
            (dead ++ victims.map(_.id), gained.updated(predator.id, gained.getOrElse(predator.id, 0.0) + mass))
          else (dead, gained)
      }

    TickResult(foodByPlayer, eatenFoods, massGained, eatenPlayers)

end GameManager