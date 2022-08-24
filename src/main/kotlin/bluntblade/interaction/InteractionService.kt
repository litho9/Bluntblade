package bluntblade.interaction

//import bluntblade.packets.PacketGadgetInteractRsp
import bluntblade.Scene

//abstract class EntityBaseGadget(scene: Scene) : GameEntity(scene) {
//    abstract val gadgetId: Int
//}
//
//class EntityItem(
//    scene: Scene,
//    override val gadgetId: Int,
//    val item: GameItem,
//    val guid: Long,
//    val isShare: Boolean = true
//) : EntityBaseGadget(scene) {
////    override fun toProto(): SceneEntityInfo? {
////        TODO("Not yet implemented")
////    }
//}

class InteractionManager(
    val scene: Scene,
) {
//    fun interactWith(session: GameSession, opType: GadgetInteractReq): List<BasePacket>? {
//        val entity: GameEntity = scene.entities[opType.gadgetEntityId] ?: return null
//        if (entity is EntityItem) {
//            // check drop owner to avoid someone picked up item in others' world
//            if (!entity.isShare && (entity.guid shr 32).toInt() != session.account.uid) return null
//            scene.entities.remove(opType.gadgetEntityId) // Pick item
//            val success = session.account.inventory.add(entity.item) // Add to inventory
//            if (success) {
//                val id = if (entity.item is Weapon) entity.item.id else
//                    if (entity.item is Relic) entity.item.id else null
//                val packets = mutableListOf(PacketItemAddHintNotify(
//                    id!!,
//                    entity.item.count, ActionReason.SubfieldDrop))
//                return if (!entity.isShare) // not shared drop
//                    packets + PacketGadgetInteractRsp(entity, InteractType.PICK_ITEM)
//                else
//                    listOf(scene.broadcastPacket(
//                        PacketGadgetInteractRsp(entity, InteractType.PICK_ITEM)))
//            }
////        } else if (entity is EntityGadget) {
////            if (entity.content?.onInteract(this, opType) ?: return)
////                scene.removeEntity(entity)
////        } else if (entity is EntityMonster) {
////            insectCaptureManager.arrestSmallCreature(entity)
////        } else if (entity is EntityVehicle) {// try to arrest it, example: glowworm
////            insectCaptureManager.arrestSmallCreature(entity)
////        } else {
////            scene.removeEntity(entity) // Delete directly
//        }
//        return null
//    }
}