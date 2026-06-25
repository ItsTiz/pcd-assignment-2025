package it.unibo.agar.distributed.model.actors

import it.unibo.agar.distributed.model.{Food, Player}

object WorldSpectator:

  sealed trait SpectatorMessage
  private sealed trait InternalMessage extends SpectatorMessage

  case object Unsubscribe extends SpectatorMessage

  private case class InternalFood(
                                   rsp: Replicator.SubscribeResponse[ORSet[Food]]
                                 ) extends InternalMessage

  private case class InternalPlayer(
                                     rsp: Replicator.SubscribeResponse[LWWMap[String, Player]]
                                   ) extends InternalMessage

  private val foodKey: ORSetKey[Food] = ORSetKey("food-ddata")

  private val playersKey: LWWMapKey[String, Player] = LWWMapKey("players-ddata")

  def apply(stateChangedSlot: (Seq[Food], Seq[Player]) => Unit): Behavior[SpectatorMessage] =
    Behaviors.setup { context =>
      context.log.info("Created WorldSpectator")
      implicit val node: SelfUniqueAddress = DistributedData(context.system).selfUniqueAddress
      DistributedData.withReplicatorMessageAdapter[SpectatorMessage, ORSet[Food]] { foodAdapter =>
        DistributedData.withReplicatorMessageAdapter[SpectatorMessage, LWWMap[String, Player]] { playerAdapter =>
          foodAdapter.subscribe(foodKey, InternalFood.apply)
          playerAdapter.subscribe(playersKey, InternalPlayer.apply)
          def updated(foods: Set[Food], players: Map[String, Player]): Behavior[SpectatorMessage] =
            Behaviors.receiveMessage {
              case InternalFood(chg @ Replicator.Changed(`foodKey`)) =>
                val newFoods = chg.get(foodKey).elements
                stateChangedSlot(newFoods.toSeq, players.values.toSeq)
                updated(newFoods, players)

              case InternalPlayer(chg @ Replicator.Changed(`playersKey`)) =>
                val newPlayers = chg.get(playersKey).entries
                stateChangedSlot(foods.toSeq, newPlayers.values.toSeq)
                updated(foods, newPlayers)

              case Unsubscribe =>
                foodAdapter.unsubscribe(foodKey)
                playerAdapter.unsubscribe(playersKey)
                Behaviors.same

              case _ =>
                Behaviors.unhandled
            }

          updated(Set.empty, Map.empty)
        }
      }
    }