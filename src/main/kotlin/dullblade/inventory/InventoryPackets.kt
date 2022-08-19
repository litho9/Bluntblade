package dullblade.inventory

import dullblade.*
import dullblade.Equip
import dullblade.Material
import dullblade.Weapon
import dullblade.game.ActionReason
import dullblade.game.PlayerProperty
import dullblade.inventory.InventoryMessages.*

class PacketStoreItemChangeNotify(data: StoreItemChangeNotify)
    : BasePacket(data) {
    constructor(material: Material) : this(storeItemChangeNotify {
        storeType = StoreType.PACK
        itemList.add(item {
            guid = material.guid
            itemId = material.id
            this.material = material { count = material.count }
        })
    })

    constructor(equip: Equip) : this(storeItemChangeNotify {
        storeType = StoreType.PACK
        itemList.add(if (equip is Weapon) itemWeapon(equip) else itemRelic(equip as Relic))
    })
}

fun itemWeapon(weapon: Weapon) = item {
    guid = weapon.guid
    itemId = weapon.id
    this.equip = equip {
        this.weapon = weapon {
            level = weapon.level
            exp = weapon.exp
            promoteLevel = weapon.promoteLevel
            affixMap.putAll(weaponData[weapon.id]!!.skillAffixes.associateWith { weapon.refinement }) // TODO
        }
    }
}

fun itemRelic(relic: Relic) = item {
    guid = relic.guid
    itemId = relic.id
    this.equip = equip {
        this.reliquary = reliquary {
            level = relic.level
            exp = relic.exp
            // TODO
        }
    }
}

class PacketStoreItemDelNotify(guid: Long)
    : BasePacket(storeItemDelNotify {
            storeType = StoreType.PACK
            guidList.add(guid)
        })

class PacketWeaponPromoteRsp(data: WeaponPromoteRsp)
    : BasePacket(data) {
    constructor(weapon: Weapon, oldLevel: Int) : this(
        weaponPromoteRsp {
            targetWeaponGuid = weapon.guid
            curPromoteLevel = weapon.promoteLevel
            oldPromoteLevel = oldLevel
        }
    )
}

class PacketCalcWeaponUpgradeReturnItemsRsp(
    itemGuid: Long, returnItems: List<Pair<Int, Int>>
) : BasePacket(calcWeaponUpgradeReturnItemsRsp {
        if (returnItems.isEmpty()) {
            retcode = PacketMessages.Retcode.RET_SVR_ERROR_VALUE
        } else {
            targetWeaponGuid = itemGuid
            itemParamList.addAll(returnItems.map {
                itemParam { itemId = it.first; count = it.second }
            })
        }
    })

class PacketWeaponUpgradeRsp(data: WeaponUpgradeRsp)
    : BasePacket(data) {
    constructor(weapon: Weapon, oldLevel: Int, leftoverOres: List<Pair<Int, Int>>) : this(
        weaponUpgradeRsp {
            targetWeaponGuid = weapon.guid
            curLevel = weapon.level
            this.oldLevel = oldLevel
            itemParamList.addAll(leftoverOres.map {
                itemParam { itemId = it.first; count = it.second }
            })
        }
    )
}

class PacketWeaponAwakenRsp(weapon: Weapon, oldRefineLevel: Int)
    : BasePacket(weaponAwakenRsp {
    targetWeaponGuid = weapon.guid
    targetWeaponAwakenLevel = weapon.refinement
    avatarGuid = weapon.equipCharacterId
    weaponData[weapon.id]!!.skillAffixes.forEach {
        oldAffixLevelMap[it] = oldRefineLevel
        curAffixLevelMap[it] = weapon.refinement
    }
})

class PacketItemAddHintNotify(itemId: Int, count: Int, reason: ActionReason)
    : BasePacket(itemAddHintNotify {
    itemList.add(itemHint { this.itemId = itemId; this.count = count })
    this.reason = reason.value
})

class PacketSetEquipLockStateRsp(locked: Boolean, guid: Long)
    : BasePacket(setEquipLockStateRsp {
    isLocked = locked
    targetEquipGuid = guid
}, buildHeader())

class PacketReliquaryUpgradeRsp(relic: Relic, rate: Int, oldLevel: Int, oldAppendPropIdList: List<Int>)
    : BasePacket(reliquaryUpgradeRsp {
        targetReliquaryGuid = relic.guid
        this.oldLevel = oldLevel
        curLevel = relic.level
        powerUpRate = rate
        oldAppendPropList.addAll(oldAppendPropIdList)
        curAppendPropList.addAll(relic.appendPropIdList)
})

class PacketProudSkillChangeNotify(avatar: Avatar)
    : BasePacket(proudSkillChangeNotify {
    avatarGuid = avatar.guid
//    entityId = avatar.entityId // TODO
    skillDepotId = avatar.skillDepotId
    proudSkillList.addAll(avatar.proudSkillIds)
})

class PacketAvatarPropNotify(avatar: Avatar)
    : BasePacket(avatarPropNotify {
    avatarGuid = avatar.guid
    propMap[PlayerProperty.PROP_LEVEL.id] = avatar.level.toLong()
    propMap[PlayerProperty.PROP_EXP.id] = avatar.exp.toLong()
    propMap[PlayerProperty.PROP_BREAK_LEVEL.id] = avatar.promoteLevel.toLong()
    propMap[PlayerProperty.PROP_SATIATION_VAL.id] = 0
    propMap[PlayerProperty.PROP_SATIATION_PENALTY_TIME.id] = 0
})

class PacketAvatarPromoteRsp(avatar: Avatar)
    : BasePacket(avatarPromoteRsp { guid = avatar.guid })

class PacketAvatarUpgradeRsp(
    avatar: Avatar,
    oldLevel: Int,
    oldFightPropMap: Map<Int, Float>
) : BasePacket(avatarUpgradeRsp {
    avatarGuid = avatar.guid
    this.oldLevel = oldLevel
    curLevel = avatar.level
    this.oldFightPropMap.putAll(oldFightPropMap)
    curFightPropMap.putAll(avatar.fightProperties)
})

class PacketAvatarSkillChangeNotify(
    avatar: Avatar, skillId: Int, oldLevel: Int, curLevel: Int
) : BasePacket(avatarSkillChangeNotify {
    avatarGuid = avatar.guid
//    entityId = avatar.entityId // TODO
    skillDepotId = avatar.skillDepotId
    avatarSkillId = skillId
    this.oldLevel = oldLevel
    this.curLevel = curLevel
})

class PacketAvatarSkillUpgradeRsp(
    avatar: Avatar, skillId: Int, oldLevel: Int, curLevel: Int
) : BasePacket(avatarSkillUpgradeRsp {
    avatarGuid = avatar.guid
    avatarSkillId = skillId
    this.oldLevel = oldLevel
    this.curLevel = curLevel
})

class PacketAvatarUnlockTalentNotify(avatar: Avatar, talentId: Int) :
    BasePacket(avatarUnlockTalentNotify {
    avatarGuid = avatar.guid
//    entityId = avatar.entityId // TODO
    this.talentId = talentId
    skillDepotId = avatar.skillDepotId
})

class PacketUnlockAvatarTalentRsp(avatar: Avatar, talentId: Int)
    : BasePacket(unlockAvatarTalentRsp { avatarGuid = avatar.guid; this.talentId = talentId })

class PacketProudSkillExtraLevelNotify(
    avatar: Avatar, talentIndex: Int
) : BasePacket(proudSkillExtraLevelNotify {
    avatarGuid = avatar.guid
    talentType = 3 // Talent type = 3 "AvatarSkill"
    this.talentIndex = talentIndex
    extraLevel = 3
})

class PacketAvatarSkillMaxChargeCountNotify(
    avatar: Avatar, skillId: Int, maxCharges: Int
) : BasePacket(avatarSkillMaxChargeCountNotify {
    avatarGuid = avatar.guid
    this.skillId = skillId
    maxChargeCount = maxCharges
})

fun toFetterProto(avatar: Avatar) = avatarFetterInfo {
    expLevel = totalFetterExpData.indexOfFirst { it > avatar.fetterTotalExp }
    expNumber = avatar.fetterTotalExp - totalFetterExpData[expLevel - 1]
    fetterList.addAll(fetterIdMap[avatar.id]!!.subList(0, expLevel)
        .map { fetterData { fetterId = it; fetterState = 3 /* FINISH*/ } })
    if (expLevel == 10) rewardedFetterLevelList.add(10)
}

class PacketAvatarFetterDataNotify(avatar: Avatar)
    : BasePacket(avatarFetterDataNotify {
            fetterInfoMap[avatar.guid] = toFetterProto(avatar) })

class PacketStoreWeightLimitNotify
    : BasePacket(storeWeightLimitNotify {
    storeType = StoreType.PACK
    weightLimit = InventoryService.LIMIT_ALL
    weaponCountLimit = InventoryService.LIMIT_WEAPONS
    reliquaryCountLimit = InventoryService.LIMIT_RELICS
    materialCountLimit = InventoryService.LIMIT_MATERIALS
    furnitureCountLimit = InventoryService.LIMIT_FURNITURE
})

class PacketPlayerStoreNotify(inventory: Inventory)
    : BasePacket(playerStoreNotify {
    storeType = StoreType.PACK
    weightLimit = InventoryService.LIMIT_ALL
    itemList.addAll(inventory.weapons.map { itemWeapon(it.value) })
    itemList.addAll(inventory.relics.map { itemRelic(it.value) })
    itemList.addAll(inventory.materials.map { (id, material) -> item {
        guid = material.guid
        itemId = id
        this.material = material { count = material.count }
    } })
    itemList.addAll(inventory.furniture.map { (id, material) -> item {
        guid = material.guid
        itemId = id
        this.furniture = furniture { count = material.count }
    } })
})

class PacketUseItemRsp(useItem: Material?) : BasePacket(useItemRsp {
    if (useItem != null) {
        itemId = useItem.id
        guid = useItem.guid
    } else
        retcode = PacketMessages.Retcode.RET_SVR_ERROR_VALUE
})

class PacketDestroyMaterialRsp(returnMaterial: List<CostItem>)
    : BasePacket(destroyMaterialRsp {
    returnMaterial.forEach { ret ->
        itemIdList.add(ret.id)
        itemCountList.add(ret.count)
    }
})

class PacketForgeFormulaDataNotify(itemId: Int)
    : BasePacket(forgeFormulaDataNotify {
            forgeId = itemId
            isLocked = false
        })

class PacketCombineFormulaDataNotify(itemId: Int)
    : BasePacket(combineFormulaDataNotify {
            combineId = itemId
            isLocked = false
        })

class PacketCombineRsp(req: CombineReq, result: List<CostItem>)
    : BasePacket(combineRsp {
    retcode = PacketMessages.Retcode.RET_SUCC_VALUE
    combineId = req.combineId
    combineCount = req.combineCount
    avatarGuid = req.avatarGuid
    resultItemList.addAll(result.map {
        itemParam { itemId = it.id; count = it.count }
    })
})

class PacketCombineDataNotify(unlockedCombines: Set<Int>) : BasePacket(combineDataNotify {
    combineIdList.addAll(unlockedCombines)
})

class PacketUnlockedFurnitureFormulaDataNotify(unlocks: Set<Int>) :
    BasePacket(unlockedFurnitureFormulaDataNotify {
            furnitureIdList.addAll(unlocks)
            isAll = true
        })

class PacketUnlockedFurnitureSuiteDataNotify(unlocks: Set<Int>) :
    BasePacket(unlockedFurnitureSuiteDataNotify {
            furnitureSuiteIdList.addAll(unlocks)
            isAll = true
        })