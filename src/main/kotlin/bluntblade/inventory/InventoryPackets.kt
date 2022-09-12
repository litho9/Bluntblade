package bluntblade.inventory

import bluntblade.*
import bluntblade.Equip
import bluntblade.Material
import bluntblade.Weapon
import bluntblade.game.ActionReason
import bluntblade.game.PlayerProperty
import bluntblade.inventory.InventoryMessages.*

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

fun toFetterProto(avatar: Avatar) = avatarFetterInfo {
    expLevel = totalFetterExpData.indexOfFirst { it > avatar.fetterTotalExp }
    expNumber = avatar.fetterTotalExp - totalFetterExpData[expLevel - 1]
    fetterList.addAll(
        fetterIdMap[avatar.id]!!.subList(0, expLevel)
        .map { fetterData { fetterId = it; fetterState = 3 /* FINISH*/ } })
    if (expLevel == 10) rewardedFetterLevelList.add(10)
}

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
