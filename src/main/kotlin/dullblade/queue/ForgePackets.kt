package dullblade.queue

import dullblade.BasePacket
import dullblade.ForgeQueue
import dullblade.GameItem
import dullblade.PacketMessages.Retcode
import dullblade.inventory.itemParam

private fun packetFor(queues: List<ForgeQueue>): Map<Int, QueueMessages.ForgeQueueData> {
    val now = (System.currentTimeMillis() / 1000).toInt()
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

class PacketForgeQueueDataNotify(queues: List<ForgeQueue>, removeIdx: Int? = null) :
    BasePacket(forgeQueueDataNotify {
        if (removeIdx != null) removedForgeQueueList.add(removeIdx)
        forgeQueueMap.putAll(packetFor(queues))
    })

class PacketForgeQueueManipulateRsp(
    type: QueueMessages.ForgeQueueManipulateType,
    output: List<GameItem> = emptyList(),
    refund: List<GameItem> = emptyList(),
    retCode: Retcode = Retcode.RET_SUCC,
) : BasePacket(forgeQueueManipulateRsp {
    retcode = retCode.number
    manipulateType = type
    outputItemList.addAll(output.map { itemParam {
        itemId = it.id
        count = it.count
    } })
    returnItemList.addAll(refund.map { itemParam {
        itemId = it.id
        count = it.count
    } })
})

class PacketForgeStartRsp(retCode: Retcode)
    : BasePacket(forgeStartRsp { retcode = retCode.number })

class PacketForgeGetQueueDataRsp(
    maxQueues: Int,
    queues: List<ForgeQueue>,
    retCode: Retcode = Retcode.RET_SUCC,
) : BasePacket(forgeGetQueueDataRsp {
        retcode = retCode.number
        maxQueueNum = maxQueues
        forgeQueueMap.putAll(packetFor(queues))
    })

class PacketForgeDataNotify(
    maxQueues: Int,
    queues: List<ForgeQueue>,
    unlockedForgingBlueprints: Collection<Int>
) : BasePacket(forgeDataNotify {
    forgeIdList.addAll(unlockedForgingBlueprints)
    maxQueueNum = maxQueues
    forgeQueueMap.putAll(packetFor(queues))
})
