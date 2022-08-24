package bluntblade.interaction

//import bluntblade.BasePacket
//import bluntblade.EntityBaseGadget
//import bluntblade.game.PacketOpcodes
//import bluntblade.interaction.InteractionMessages.*
//
//class PacketGadgetInteractRsp(
//    gadget: EntityBaseGadget,
//    interact: GadgetInteractRsp.InteractType,
//    opType: InterOpType? = null
//) : BasePacket(proto(gadget, interact, opType)) {
//    companion object {
//        fun proto(
//            gadget: EntityBaseGadget,
//            interact: GadgetInteractRsp.InteractType?,
//            opType: InterOpType? = null
//        ): GadgetInteractRsp {
//            val proto = GadgetInteractRsp.newBuilder()
//                .setGadgetEntityId(gadget.gadgetId) // TODO
//                .setInteractType(interact)
//                .setGadgetId(gadget.gadgetId)
//            if (opType != null) proto.opType = opType
//            return proto.build()
//        }
//    }
//}