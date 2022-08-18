package dullblade.queue

import dullblade.ForgeQueue
import dullblade.GameItem
import dullblade.GameSession
import dullblade.PacketMessages.Retcode
import dullblade.game.PlayerProperty
import dullblade.game.WatcherTriggerType
import dullblade.inventory.InventoryService
import dullblade.queue.QueueMessages.*

object ForgeQueueService {
    private fun queueCountFor(ar: Int) =
        if (ar >= 15) 4 else if (ar >= 10) 3 else if (ar >= 5) 2 else 1

    fun start(session: GameSession, req: ForgeStartReq) {
        if (session.account.forgeQueues.size >= queueCountFor(session.account.level))
            return session.send(PacketForgeStartRsp(Retcode.RET_FORGE_QUEUE_FULL))

        val data: ForgeData = forgeData[req.forgeId]!!
        val requiredPoints = data.points * req.forgeCount
        if (requiredPoints > session.account.properties[PlayerProperty.FORGE_POINT])
            return session.send(PacketForgeStartRsp(Retcode.RET_FORGE_POINT_NOT_ENOUGH))

        InventoryService.pay(session, data.materials, data.moraCost)
        session.account.properties.add(PlayerProperty.FORGE_POINT, -requiredPoints)

        var timestamp = (System.currentTimeMillis() / 1000).toInt()
        val timestamps = (1..req.forgeCount).map {
            timestamp += data.duration
            timestamp
        }
        session.account.forgeQueues.add(ForgeQueue(req.forgeId, req.avatarId, timestamps))

        session.send(PacketForgeQueueDataNotify(session.account.forgeQueues))
        session.send(PacketForgeStartRsp(Retcode.RET_SUCC))
    }

    fun list(session: GameSession) {
        session.send(PacketForgeGetQueueDataRsp(
            queueCountFor(session.account.level),
            session.account.forgeQueues))
    }

    fun notify(session: GameSession) {
        session.send(PacketForgeDataNotify(
            queueCountFor(session.account.level),
            session.account.forgeQueues,
            session.account.unlockedForgingBlueprints))
    }

    fun manipulate(session: GameSession, req: ForgeQueueManipulateReq) {
        val id: Int = req.forgeQueueId
        val now: Int = (System.currentTimeMillis() / 1000).toInt()
        val forge = session.account.forgeQueues[id - 1]
        val finishCount = forge.timestamps.count { it <= now }
        val data: ForgeData = forgeData[forge.id]!!

        if (req.manipulateType == ForgeQueueManipulateType.RECEIVE) {
            if (finishCount <= 0) return

            // Give finished items to the player.
            val item = InventoryService.add(data.resultItemId, data.resultItemCount * finishCount)
            BattlePassService.trigger(WatcherTriggerType.TRIGGER_DO_FORGE, 0, finishCount)

            val allDone = forge.timestamps.last() < now
            if (allDone) session.account.forgeQueues.removeAt(id - 1)
            session.send(PacketForgeQueueDataNotify(session.account.forgeQueues, if (allDone) id else null))
            session.send(PacketForgeQueueManipulateRsp(ForgeQueueManipulateType.RECEIVE, listOf(item)))
        } else { // CANCEL
            if (finishCount > 0) return // Make sure there are no unfinished items.
            val remainingCount = forge.timestamps.count { it > now }

            // Return material items to the player.
            val refund: List<GameItem> = data.materials.filter { it.id > 0 }
                .map { InventoryService.add(it.id, it.count * remainingCount) } +
                    InventoryService.add(InventoryService.MORA_ID, data.moraCost * remainingCount)

            session.account.properties.add(PlayerProperty.FORGE_POINT, data.points * remainingCount)
            session.account.forgeQueues.removeAt(id - 1)

            session.send(PacketForgeQueueDataNotify(session.account.forgeQueues, id))
            session.send(PacketForgeQueueManipulateRsp(ForgeQueueManipulateType.CANCEL, refund = refund))
        }
    }

    fun tick(session: GameSession, lastTick: Int) {
        val now = (System.currentTimeMillis() / 1000).toInt()
        if (session.account.forgeQueues.flatMap { it.timestamps }
            .any { it in (lastTick + 1)..now })
            session.send(PacketForgeQueueDataNotify(session.account.forgeQueues))
    }
}