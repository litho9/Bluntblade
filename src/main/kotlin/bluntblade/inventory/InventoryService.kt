package bluntblade.inventory

import bluntblade.*
import bluntblade.Material
import bluntblade.Weapon
import bluntblade.game.ActionReason
import bluntblade.game.MaterialType
import bluntblade.game.WatcherTriggerType
import bluntblade.inventory.InventoryMessages.*
import bluntblade.queue.BattlePassService
import java.time.LocalDate

object InventoryService {
    fun lock(session: GameSession, req: SetEquipLockStateReq) {
        val inv = session.account.inventory
        val equip = inv.weapons[req.targetEquipGuid] ?: inv.relics[req.targetEquipGuid]!!
        equip.locked = req.isLocked
//        equip.save()
        session.send(PacketStoreItemChangeNotify(equip))
        session.send(setEquipLockStateRsp {
            isLocked = equip.locked
            targetEquipGuid = equip.guid
        }, BasePacket.buildHeader())
    }

    fun weaponPromote(session: GameSession, req: WeaponPromoteReq) {
        val weapon = session.account.inventory.weapons[req.targetWeaponGuid]!!
        if (weapon.level != unlockMaxLevel[weapon.promoteLevel])
            throw IllegalArgumentException("Invalid weapon level")
        val data = weaponData[weapon.id]!!
        val nextPromoteData = weaponPromoteData[data.promoteId]!![weapon.promoteLevel]
        pay(session, nextPromoteData.costItems, nextPromoteData.costCoin)
        val oldLevel = weapon.promoteLevel++
//        db.save(weapon)
        AvatarService.recalcStats(session, weapon.equipCharacterId)

        session.send(PacketStoreItemChangeNotify(weapon))
        session.send(weaponPromoteRsp {
            targetWeaponGuid = weapon.guid
            curPromoteLevel = weapon.promoteLevel
            oldPromoteLevel = oldLevel
        })
    }

    fun calcWeaponUpgradeLeftovers(session: GameSession, req: CalcWeaponUpgradeReturnItemsReq) {
        val weapons = session.account.inventory.weapons
        val fodderWeapons = req.foodWeaponGuidListList.map { weapons[it]!! }
            .filter { it.equipCharacterId == 0L && !it.locked }.toSet()
        val expGain = fodderWeapons.sumOf { weaponData[it.id]!!.baseExp } +
                req.itemParamListList.sumOf { it.count * oreExpMap[it.itemId]!! } +
                fodderWeapons.sumOf { it.totalExp * 4 / 5 }

        val weapon = weapons[req.targetWeaponGuid]!!
        val leftoverExp = weapon.totalExp + expGain - unlockExpFor(weapon)
        val leftovers: List<Pair<Int, Int>> = getLeftoverOres(leftoverExp)
        session.send(calcWeaponUpgradeReturnItemsRsp {
            if (leftovers.isEmpty()) {
                retcode = PacketMessages.Retcode.RET_SVR_ERROR_VALUE
            } else {
                targetWeaponGuid = req.targetWeaponGuid
                itemParamList.addAll(leftovers.map {
                    itemParam { itemId = it.first; count = it.second }
                })
            }
        })
    }

    fun upgradeWeapon(session: GameSession, req: WeaponUpgradeReq) {
        val weapons = session.account.inventory.weapons
        val fodderWeapons = req.foodWeaponGuidListList.map { weapons[it]!! }
            .filter { it.equipCharacterId == 0L && !it.locked }.toSet()
        val paidExpGain = fodderWeapons.sumOf { weaponData[it.id]!!.baseExp } +
                req.itemParamListList.sumOf { it.count * oreExpMap[it.itemId]!! }
        val expGainFree = fodderWeapons.sumOf { it.totalExp * 4 / 5 } // No tax :D
        val expGain = paidExpGain + expGainFree
        if (expGain <= 0) throw IllegalArgumentException("Invalid experience gain (0)")

        val weapon = weapons[req.targetWeaponGuid]!!
        val data = weaponData[weapon.id]!!
        val unlockExp = unlockExpFor(weapon)
        val leftoverExp = weapon.totalExp + expGain - unlockExp
        val leftovers = getLeftoverOres(leftoverExp)

        pay(session, req.itemParamListList.map { CostItem(it.itemId, it.count) }, paidExpGain / 10)
        weapons.values.removeAll(fodderWeapons)
        leftovers.forEach { materialAdd(session, it.first, it.second) }

        val oldLevel = weapon.level
        weapon.totalExp = if (leftoverExp > 0) unlockExp else weapon.totalExp + expGain
        val totalExpArray = weaponTotalExpData[data.rank - 1]
        weapon.level = totalExpArray.indexOfFirst { it > weapon.totalExp }
        weapon.exp = weapon.totalExp - totalExpArray[weapon.level - 1]
//        weapon.save() TODO

        if (oldLevel != weapon.level)
            AvatarService.recalcStats(session, weapon.equipCharacterId)

        session.send(PacketStoreItemChangeNotify(weapon))
        session.send(weaponUpgradeRsp {
            targetWeaponGuid = weapon.guid
            curLevel = weapon.level
            this.oldLevel = oldLevel
            itemParamList.addAll(leftovers.map {
                itemParam { itemId = it.first; count = it.second }
            })
        })
    }

    fun refineWeapon(session: GameSession, req: WeaponAwakenReq) {
        val weapons = session.account.inventory.weapons
        val weapon = weapons[req.targetWeaponGuid]!!
        val data = weaponData[weapon.id]!!
        val feed = weapons[req.itemGuid]!!
        if (feed.locked || feed.equipCharacterId > 0 || weapon.id != feed.id
//            && weapon.data.awakenMaterialId != feed.itemId TODO
            || weapon.refinement >= 4
        ) throw IllegalArgumentException()

        val moraCost = data.awakenCosts.getOrNull(weapon.refinement) ?: 0
        if (session.account.inventory.mora < moraCost)
            throw IllegalArgumentException("Not enough mora")
        session.account.inventory.mora -= moraCost
        weapons.remove(req.itemGuid)
        val oldRefineLevel = weapon.refinement
        weapon.refinement = (oldRefineLevel + feed.refinement + 1).coerceAtMost(4)
//        weapon.save() TODO

        AvatarService.recalcStats(session, weapon.equipCharacterId)

        session.send(PacketStoreItemChangeNotify(weapon))
        session.send(weaponAwakenRsp {
            targetWeaponGuid = weapon.guid
            targetWeaponAwakenLevel = weapon.refinement
            avatarGuid = weapon.equipCharacterId
            weaponData[weapon.id]!!.skillAffixes.forEach {
                oldAffixLevelMap[it] = oldRefineLevel
                curAffixLevelMap[it] = weapon.refinement
            }
        })
    }

    fun upgradeRelic(session: GameSession, req: ReliquaryUpgradeReq) {
        val relics = session.account.inventory.relics
        val fodderRelics = req.foodReliquaryGuidListList.map { relics[it]!! }
            .filter { it.equipCharacterId == 0L && !it.locked }.toSet()
        val moraCost = fodderRelics.sumOf { relicData[it.id]!!.baseConvExp } +
                req.itemParamListList.sumOf { it.count * relicExpMap[it.itemId]!! }
        val rate = Rng.relicRate()
        val expGain = (moraCost + fodderRelics.sumOf { it.totalExp * 4 / 5 }) * rate
        if (expGain <= 0) throw IllegalArgumentException("Invalid experience gain (0)")

        pay(session, req.itemParamListList.map { CostItem(it.itemId, it.count) }, moraCost)
        relics.values.removeAll(fodderRelics)

        // Now we upgrade
        val relic = relics[req.targetReliquaryGuid]!!
        val data = relicData[relic.id]!!
        val oldAppendPropIdList = relic.appendPropIdList.toList()
        val oldLevel = relic.level
        val totalExpArray = relicTotalExpData[data.rank]
        relic.totalExp = (relic.totalExp + expGain)
            .coerceAtMost(totalExpArray[data.maxLevel - 2])
        relic.level = totalExpArray.indexOfFirst { it > relic.totalExp }
        relic.exp = relic.totalExp - totalExpArray[relic.level - 1]
        (oldLevel / 4 until relic.level / 4)
            .forEach { _ -> rollSubStat(relic, data) }
//        relic.save()

        if (oldLevel != relic.level)
            AvatarService.recalcStats(session, relic.equipCharacterId)

        session.send(PacketStoreItemChangeNotify(relic))
        session.send(reliquaryUpgradeRsp {
            targetReliquaryGuid = relic.guid
            this.oldLevel = oldLevel
            curLevel = relic.level
            powerUpRate = rate
            oldAppendPropList.addAll(oldAppendPropIdList)
            curAppendPropList.addAll(relic.appendPropIdList)
        })
    }

    private fun unlockExpFor(weapon: Weapon): Int {
        val data = weaponData[weapon.id]!!
        val promoteData = weaponPromoteData[data.promoteId]!![weapon.promoteLevel]
        return weaponTotalExpData[data.rank - 1][promoteData.unlockMaxLevel - 1]
    }

    private fun rollSubStat(relic: Relic, data: RelicData) {
        val affixList = if (relic.appendPropIdList.size == 4)
            relic.appendPropIdList.map { id -> reliquaryAffixData.find { it.id == id }!! }
        else {
            val mainProp = reliquaryMainPropData.find { it.id == relic.mainPropId }!!
            reliquaryAffixData.filter { it.depotId == data.depotId
                    && it.propType != mainProp.propType
                    && !relic.appendPropIdList.contains(it.id) }
        }
        val affix = Rng.weighted(affixList, ReliquaryAffixData::weight)
        relic.appendPropIdList.add(affix.id)
    }

    fun pay(session: GameSession, costItems: List<CostItem>, coinCost: Int = 0, count: Int = 1) {
        val costs = costItems.filter { it.id > 0 }
        val inv = session.account.inventory
        if (costs.any { inv.materials[it.id]!!.count < it.count * count } || coinCost > inv.mora)
            throw IllegalArgumentException("Can't pay cost items")
        inv.mora -= coinCost
        for (cost in costs) {
            val material = inv.materials[cost.id]!!
            BattlePassService.trigger(
                WatcherTriggerType.TRIGGER_COST_MATERIAL,
                cost.id, cost.count * count)
            material.count -= cost.count * count
            if (material.count > 0) {
                session.send(PacketStoreItemChangeNotify(material))
            } else {
//                inv.materials.remove(cost.id)
                // item.save() // Update in db TODO
                session.send(PacketStoreItemDelNotify(material.guid))
            }
        }
    }

    private fun materialAdd(session: GameSession, id: Int, count: Int = 1) {
        val materials = session.account.inventory.materials
        if (!materials.contains(id)) {
            // Item type didn't exist before, we will add it to main inventory map if there is enough space
            if (materials.size >= LIMIT_MATERIALS) return
//            player.codex.checkAddedMaterial(id) TODO
            materials[id] = Material(session.account.newGuid(), id, 0)
        }
        materials[id]!!.count += count
    }

    fun materialUse(session: GameSession, req: UseItemReq) {
        val material = session.account.inventory.materials.values.find { it.guid == req.guid }!!
        val data = materialData[material.id]
        var used = 0
        var useSuccess = false
        if (data?.type == MaterialType.MATERIAL_FOOD
            && data.useTarget == "ITEM_USE_TARGET_SPECIFY_DEAD_AVATAR") {
            used = if (AvatarService.revive(session, req.targetGuid)) 1 else 0
        } else if (data?.type == MaterialType.MATERIAL_NOTICE_ADD_HP
            && data.useTarget == "ITEM_USE_TARGET_SPECIFY_ALIVE_AVATAR") {
            used = if (AvatarService.heal(session, req.targetGuid, data.satiationParams)) 1 else 0
        } else if (data?.type == MaterialType.MATERIAL_CONSUME && data.itemUse[0].op == "ITEM_USE_UNLOCK_FORGE") {
            val forgeId = data.itemUse[0].params[0].toInt()
            session.account.inventory.materials.remove(material.id)
            session.send(PacketStoreItemDelNotify(material.guid))
            session.account.unlockedForgingBlueprints.add(forgeId)
            session.send(forgeFormulaDataNotify {
                this.forgeId = forgeId
                isLocked = false
            })
            useSuccess = true
        } else if (data?.type == MaterialType.MATERIAL_CONSUME && data.itemUse[0].op == "ITEM_USE_UNLOCK_COMBINE") {
            val combineId = data.itemUse[0].params[0].toInt()
            session.account.inventory.materials.remove(material.id)
            session.send(PacketStoreItemDelNotify(material.guid))
            session.account.unlockedCombines.add(combineId)
            session.send(combineFormulaDataNotify {
                this.combineId = combineId
                isLocked = false
            })
            useSuccess = true
        } else if (data?.type == MaterialType.MATERIAL_FURNITURE_FORMULA) {
            val fId = data.itemUse[0].params[0].toInt()
            session.account.inventory.materials.remove(material.id)
            session.send(PacketStoreItemDelNotify(material.guid))
            session.account.unlockedFurniture.add(fId)
            session.send(unlockedFurnitureFormulaDataNotify {
                furnitureIdList.addAll(session.account.unlockedFurniture)
                isAll = true
            })
            useSuccess = true
        } else if (data?.type == MaterialType.MATERIAL_FURNITURE_SUITE_FORMULA) {
            val fId = data.itemUse[0].params[0].toInt()
            session.account.inventory.materials.remove(material.id)
            session.account.unlockedFurnitureSuites.add(fId)
            session.send(unlockedFurnitureSuiteDataNotify {
                furnitureSuiteIdList.addAll(session.account.unlockedFurnitureSuites)
                isAll = true
            })
            useSuccess = true
        } else if (material.id == RESIN_FRAGILE || material.id == RESIN_TRANSIENT) {
            materialAdd(session, RESIN_ID, 60 * req.count)
            session.send(PacketItemAddHintNotify(material.id, req.count, ActionReason.PlayerUseItem))
            used = req.count // Set used amount.
        } else if (material.id == WELKIN_ID) {
            materialAdd(session, CRYSTAL_ID, 300)
            val prevEnd = if (session.account.moonCardEnd == null) today() else
                LocalDate.parse(session.account.moonCardEnd)
            session.account.moonCardEnd = prevEnd.plusDays(30).toString()
            session.account.moonCardLastGet = today().toString()
            used = 1
        }
        // TODO MATERIAL_CHEST MATERIAL_CHEST_BATCH_USE

        if (used > 0) pay(session, listOf(CostItem(material.id, 1)), count=used)
        session.send(useItemRsp {
            if (used > 0 || useSuccess) {
                itemId = material.id
                guid = material.guid
            } else
                retcode = PacketMessages.Retcode.RET_SVR_ERROR_VALUE
        })
    }

    fun materialDestroy(session: GameSession, req: DestroyMaterialReq) {
        // TODO could be relic or weapon too
        val inv = session.account.inventory
        val costs = req.materialListList.map { info ->
            val material = inv.materials.values.find { it.guid == info.guid }!!
            CostItem(material.id, info.count)
        }
        pay(session, costs, 0)
        val returnMaterial = costs.flatMap { destroyMaterialsFor(it.id) }
//        materialAdd(returnMaterial) TODO
        session.send(destroyMaterialRsp {
            returnMaterial.forEach { ret ->
                itemIdList.add(ret.id)
                itemCountList.add(ret.count)
            }
        })
    }

    fun materialCombine(session: GameSession, req: CombineReq) { // combine through alchemy
        val data = combineData[req.combineId]!!
        pay(session, data.materials, data.moraCost, req.combineCount)
        materialAdd(session, data.resultItemId, data.resultItemCount)
        // TODO lucky characters
        session.send(combineRsp {
            retcode = PacketMessages.Retcode.RET_SUCC_VALUE
            combineId = req.combineId
            combineCount = req.combineCount
            avatarGuid = req.avatarGuid
            resultItemList.add(itemParam {
                itemId = data.resultItemId
                count = data.resultItemCount * req.combineCount
            })
        })
    }

    const val LIMIT_WEAPONS = 2000
    const val LIMIT_RELICS = 2000
    const val LIMIT_MATERIALS = 2000
    const val LIMIT_FURNITURE = 2000
    const val LIMIT_ALL = 30000

    const val MORA_ID: Int = 202
    private const val CRYSTAL_ID = 203
    private const val WELKIN_ID = 1202
    private const val RESIN_ID = 106
    private const val RESIN_FRAGILE = 107009
    private const val RESIN_TRANSIENT = 107012
    private val oreExpMap = mapOf(104011 to 400, 104012 to 2000, 104013 to 10000)
    private val relicExpMap = mapOf(105002 to 2500, 105003 to 10000)
    private fun getLeftoverOres(leftover: Int) = arrayOf(
        104013 to leftover / 10000,
        104012 to leftover % 10000 / 2000,
        104011 to leftover % 10000 % 2000 / 400
    ).filter { (_, count) -> count > 0 }

    fun add(id: Int, count: Int): GameItem {
        TODO("Not yet implemented")
    }
}
