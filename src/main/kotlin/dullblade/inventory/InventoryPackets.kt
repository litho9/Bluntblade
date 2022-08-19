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

class PacketItemAddHintNotify(itemId: Int, count: Int, reason: ActionReason)
    : BasePacket(itemAddHintNotify {
    itemList.add(itemHint { this.itemId = itemId; this.count = count })
    this.reason = reason.value
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

class PacketCombineDataNotify(unlockedCombines: Set<Int>) : BasePacket(combineDataNotify {
    combineIdList.addAll(unlockedCombines)
})
