package bluntblade

import bluntblade.account.AccountMessages.*
import bluntblade.account.AccountService
import bluntblade.game.PacketOpcodes
import bluntblade.interaction.*
import bluntblade.inventory.AvatarMessages.*
import bluntblade.inventory.AvatarService
import bluntblade.inventory.InventoryService
import bluntblade.inventory.InventoryMessages.*
import bluntblade.queue.ForgeQueueService
import bluntblade.queue.QueueMessages.*

object GameRouter {
    fun route(session: GameSession, opcode: PacketOpcodes, payload: ByteArray) = when(opcode) {
        // account
        PacketOpcodes.GetPlayerTokenReq -> AccountService.getToken(session, GetPlayerTokenReq.parseFrom(payload))
        PacketOpcodes.PlayerLoginReq -> AccountService.login(session, PlayerLoginReq.parseFrom(payload))
        PacketOpcodes.SetPlayerBornDataReq -> AccountService.chooseMc(session, SetPlayerBornDataReq.parseFrom(payload))

        // inventory
        PacketOpcodes.SetEquipLockStateReq -> InventoryService.lock(session, SetEquipLockStateReq.parseFrom(payload))
        PacketOpcodes.WeaponPromoteReq -> InventoryService.weaponPromote(session, WeaponPromoteReq.parseFrom(payload))
        PacketOpcodes.CalcWeaponUpgradeReturnItemsReq -> InventoryService.calcWeaponUpgradeLeftovers(session, CalcWeaponUpgradeReturnItemsReq.parseFrom(payload))
        PacketOpcodes.WeaponUpgradeReq -> InventoryService.upgradeWeapon(session, WeaponUpgradeReq.parseFrom(payload))
        PacketOpcodes.WeaponAwakenReq -> InventoryService.refineWeapon(session, WeaponAwakenReq.parseFrom(payload))
        PacketOpcodes.ReliquaryUpgradeReq -> InventoryService.upgradeRelic(session, ReliquaryUpgradeReq.parseFrom(payload))
        // material
        PacketOpcodes.UseItemReq -> InventoryService.materialUse(session, UseItemReq.parseFrom(payload))
        PacketOpcodes.DestroyMaterialReq -> InventoryService.materialDestroy(session, DestroyMaterialReq.parseFrom(payload))
        PacketOpcodes.CombineReq -> InventoryService.materialCombine(session, CombineReq.parseFrom(payload))
        // avatar
        PacketOpcodes.AvatarPromoteReq -> AvatarService.promote(session, AvatarPromoteReq.parseFrom(payload))
        PacketOpcodes.AvatarUpgradeReq -> AvatarService.upgrade(session, AvatarUpgradeReq.parseFrom(payload))
        PacketOpcodes.AvatarSkillUpgradeReq -> AvatarService.upgradeSkill(session, AvatarSkillUpgradeReq.parseFrom(payload))
        PacketOpcodes.UnlockAvatarTalentReq -> AvatarService.unlockConstellation(session, UnlockAvatarTalentReq.parseFrom(payload))
        // TODO HandlerMcoinExchangeHcoinReq HandlerBuyGoodsReq HandlerQuickUseWidgetReq

        // queue - forge
        PacketOpcodes.ForgeStartReq -> ForgeQueueService.start(session, ForgeStartReq.parseFrom(payload))
        PacketOpcodes.ForgeGetQueueDataReq -> ForgeQueueService.list(session)
        PacketOpcodes.ForgeQueueManipulateReq -> ForgeQueueService.manipulate(session, ForgeQueueManipulateReq.parseFrom(payload))

        // interaction
//        PacketOpcodes.GadgetInteractReq -> InteractionService.interactWith(session, GadgetInteractReq.parseFrom(payload))
        PacketOpcodes.SceneInitFinishReq -> SceneService.initFinish(session)
        PacketOpcodes.EnterSceneReadyReq -> SceneService.enterReady(session)
        PacketOpcodes.EnterSceneDoneReq -> SceneService.enterDone(session)
        PacketOpcodes.PostEnterSceneReq -> SceneService.enterPost(session)
        PacketOpcodes.ChangeGameTimeReq -> SceneService.changeTime(session, ChangeGameTimeReq.parseFrom(payload))
        PacketOpcodes.GetScenePointReq -> SceneService.pointGet(session, GetScenePointReq.parseFrom(payload))
        PacketOpcodes.EvtCreateGadgetNotify -> SceneService.gadgetCreated(session, EvtCreateGadgetNotify.parseFrom(payload))
        PacketOpcodes.EvtDestroyGadgetNotify -> SceneService.gadgetDestroyed(session, EvtDestroyGadgetNotify.parseFrom(payload))
        else -> null
    }
}