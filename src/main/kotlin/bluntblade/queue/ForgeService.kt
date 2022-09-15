package bluntblade.queue

import bluntblade.ForgeQueue
import bluntblade.GameItem
import bluntblade.GameSession
import bluntblade.PacketMessages.Retcode
import bluntblade.game.PlayerProperty
import bluntblade.game.WatcherTriggerType
import bluntblade.inventory.InventoryService
import bluntblade.inventory.itemParam
import bluntblade.queue.QueueMessages.*
import java.time.Instant

object ForgeQueueService {
    private fun queueCountFor(ar: Int) =
        if (ar >= 15) 4 else if (ar >= 10) 3 else if (ar >= 5) 2 else 1

    fun start(session: GameSession, req: ForgeStartReq) {
        if (session.account.forgeQueues.size >= queueCountFor(session.account.level))
            return session.send(forgeStartRsp { retcode = Retcode.RET_FORGE_QUEUE_FULL.number })

        val data: ForgeData = forgeData[req.forgeId]!!
        val requiredPoints = data.points * req.forgeCount
        if (requiredPoints > session.account.properties[PlayerProperty.FORGE_POINT])
            return session.send(forgeStartRsp { retcode = Retcode.RET_FORGE_POINT_NOT_ENOUGH.number })

        InventoryService.pay(session, data.materials, data.moraCost)
        session.account.properties.add(PlayerProperty.FORGE_POINT, -requiredPoints)

        val timestamp = Instant.now().epochSecond.toInt()
        val timestamps = (1..req.forgeCount)
            .map { timestamp + data.duration * it }
        session.account.forgeQueues.add(ForgeQueue(req.forgeId, req.avatarId, timestamps))

        session.send(forgeQueueDataNotify {
            forgeQueueMap.putAll(packetFor(session.account.forgeQueues)) })
        session.send(forgeStartRsp { retcode = Retcode.RET_SUCC.number })
    }

    fun list(session: GameSession) = session.send(forgeGetQueueDataRsp {
        retcode = Retcode.RET_SUCC.number
        maxQueueNum = queueCountFor(session.account.level)
        forgeQueueMap.putAll(packetFor(session.account.forgeQueues)) })

    fun notify(session: GameSession) = session.send(forgeDataNotify {
        forgeIdList.addAll(session.account.unlockedForgingBlueprints)
        maxQueueNum = queueCountFor(session.account.level)
        forgeQueueMap.putAll(packetFor(session.account.forgeQueues)) })

    fun manipulate(session: GameSession, req: ForgeQueueManipulateReq) {
        val id: Int = req.forgeQueueId
        val now = Instant.now().epochSecond
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
            session.send(forgeQueueDataNotify {
                if (allDone) removedForgeQueueList.add(id)
                forgeQueueMap.putAll(packetFor(session.account.forgeQueues)) })
            session.send(forgeQueueManipulateRsp {
                retcode = Retcode.RET_SUCC.number
                manipulateType = ForgeQueueManipulateType.RECEIVE
                outputItemList.add(itemParam { itemId = item.id; count = item.count }) })
        } else { // CANCEL
            if (finishCount > 0) return // Make sure there are no unfinished items.
            val remainingCount = forge.timestamps.count { it > now }

            // Return material items to the player.
            val refund: List<GameItem> = data.materials.filter { it.id > 0 }
                .map { InventoryService.add(it.id, it.count * remainingCount) } +
                    InventoryService.add(InventoryService.MORA_ID, data.moraCost * remainingCount)

            session.account.properties.add(PlayerProperty.FORGE_POINT, data.points * remainingCount)
            session.account.forgeQueues.removeAt(id - 1)

            session.send(forgeQueueDataNotify {
                removedForgeQueueList.add(id)
                forgeQueueMap.putAll(packetFor(session.account.forgeQueues)) })
            session.send(forgeQueueManipulateRsp {
                retcode = Retcode.RET_SUCC.number
                manipulateType = ForgeQueueManipulateType.CANCEL
                returnItemList.addAll(refund.map { itemParam { itemId = it.id; count = it.count } }) })
        }
    }

    fun tick(session: GameSession, lastTick: Int) {
        val now = Instant.now().epochSecond
        if (session.account.forgeQueues.flatMap { it.timestamps }
                .any { it in (lastTick + 1)..now })
            session.send(forgeQueueDataNotify {
                forgeQueueMap.putAll(packetFor(session.account.forgeQueues)) })
    }

    private fun packetFor(queues: List<ForgeQueue>): Map<Int, ForgeQueueData> {
        val now = Instant.now().epochSecond
        return queues.mapIndexed { i, q -> i to forgeQueueData {
            queueId = i + 1
            forgeId = q.id
            finishCount = q.timestamps.count { it <= now }
            unfinishCount = q.timestamps.count { it > now }
            totalFinishTimestamp = q.timestamps.last()
            nextFinishTimestamp = q.timestamps.first { it > now }
            avatarId = q.avatarId
        } }.toMap()
    }
}