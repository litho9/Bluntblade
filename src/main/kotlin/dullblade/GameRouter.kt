package dullblade

import dullblade.account.AccountMessages.*
import dullblade.account.AccountService
import dullblade.game.PacketOpcodes
import dullblade.interaction.SceneService
import dullblade.inventory.AvatarMessages.*
import dullblade.inventory.AvatarService
import dullblade.inventory.InventoryService
import dullblade.inventory.InventoryMessages.*
import dullblade.queue.ForgeQueueService
import dullblade.queue.QueueMessages.*

object GameRouter {
    fun route(session: GameSession, opcode: PacketOpcodes, payload: ByteArray) = when(opcode) {
        // account
        PacketOpcodes.GetPlayerTokenReq -> AccountService.getToken(session, GetPlayerTokenReq.parseFrom(payload))
        PacketOpcodes.PlayerLoginReq -> AccountService.login(session, PlayerLoginReq.parseFrom(payload))
        PacketOpcodes.SetPlayerBornDataReq -> AccountService.chooseMc(session, SetPlayerBornDataReq.parseFrom(payload))

        // inventory
        PacketOpcodes.SetEquipLockStateReq -> InventoryService.lock(session, SetEquipLockStateReq.parseFrom(payload))
        PacketOpcodes.WeaponPromoteReq -> InventoryService.promoteWeapon(session, WeaponPromoteReq.parseFrom(payload))
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
        else -> null
    }
}