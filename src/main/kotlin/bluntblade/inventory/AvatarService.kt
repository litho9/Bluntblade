package bluntblade.inventory

import bluntblade.GameSession
import bluntblade.*
import bluntblade.game.ElementType
import bluntblade.game.FightProperty
import bluntblade.game.FightProperty.*
import bluntblade.game.PlayerProperty
import bluntblade.game.Stat
import bluntblade.game.Stat.*
import bluntblade.inventory.AvatarMessages.*
import java.util.*

object AvatarService {
    fun promote(session: GameSession, req: AvatarPromoteReq) {
        val avatar = session.account.avatars[req.guid]!!
        val promoteData = avatarPromoteData[avatar.promoteId]!![avatar.promoteLevel++]

        InventoryService.pay(session, promoteData.costs, promoteData.costCoin)
        proudSkillIdFor(avatar.skillDepotId, avatar.promoteLevel)?.let {
            avatar.proudSkillIds.add(it)
            session.send(proudSkillChangeNotify {
                avatarGuid = avatar.guid
//    entityId = avatar.entityId // TODO
                skillDepotId = avatar.skillDepotId
                proudSkillList.addAll(avatar.proudSkillIds)
            })
        }

        session.send(propNotify(avatar))
        session.send(avatarPromoteRsp { guid = avatar.guid })
        // TODO Send entity prop update packet to world
        recalcStats(session, avatar)// , forceSendAbilityChange=true
//        avatar.save()
    }

    fun upgrade(session: GameSession, req: AvatarUpgradeReq) {
        val avatar = session.account.avatars[req.avatarGuid]!!
        val promoteData = avatarPromoteData[avatar.promoteId]!![avatar.promoteLevel]
        val expGain = req.count * (bookExpMap[req.itemId] ?: 0)
        if (expGain <= 0) return // Sanity check
        val moraCost = expGain / 5
        InventoryService.pay(session, listOf(CostItem(req.itemId, req.count)), moraCost)

        avatarUpgrade(session, avatar, promoteData, expGain) // Level up
    }

    private fun avatarUpgrade(session: GameSession, avatar: Avatar, promoteData: AvatarPromoteData, expGain: Int) {
        val oldLevel = avatar.level
        val oldPropMap = avatar.fightProperties.toMap()
        avatar.totalExp = (avatar.totalExp + expGain)
            .coerceAtMost(avatarTotalExpData[promoteData.unlockMaxLevel - 1])
        avatar.level = avatarTotalExpData.indexOfFirst { it > avatar.totalExp }
        avatar.exp = avatar.totalExp - avatarTotalExpData[avatar.level - 1]

        recalcStats(session, avatar)
//        avatar.save()
        // TODO Send entity prop update packet to world
        session.send(propNotify(avatar))
        session.send(avatarUpgradeRsp {
            avatarGuid = avatar.guid
            this.oldLevel = oldLevel
            curLevel = avatar.level
            oldFightPropMap.putAll(oldPropMap)
            curFightPropMap.putAll(avatar.fightProperties)
        })
    }

    fun upgradeFetterLevel(session: GameSession, avatar: Avatar, expGain: Int) {
        avatar.fetterTotalExp = (avatar.fetterTotalExp + expGain).coerceAtMost(maxFetterExp)
//        avatar.save()
        session.send(propNotify(avatar))
        session.send(avatarFetterDataNotify {
            fetterInfoMap[avatar.guid] = toFetterProto(avatar) })
    }

    fun upgradeSkill(session: GameSession, req: AvatarSkillUpgradeReq) {
        val avatar = session.account.avatars[req.avatarGuid]!!
        val oldLevel = avatar.skillLevels[req.avatarSkillId] ?: 1
        val level = oldLevel + 1
        val proudSkill = proudSkillData[avatarSkillData[req.avatarSkillId]!!.proudSkillGroupId]!![level - 1]

        InventoryService.pay(session, proudSkill.costs, proudSkill.costCoin)
        avatar.skillLevels[req.avatarSkillId] = level
//        avatar.save()

        session.send(avatarSkillChangeNotify {
            avatarGuid = avatar.guid
//    entityId = avatar.entityId // TODO
            skillDepotId = avatar.skillDepotId
            avatarSkillId = req.avatarSkillId
            this.oldLevel = oldLevel
            curLevel = level
        })
        session.send(avatarSkillUpgradeRsp {
            avatarGuid = avatar.guid
            avatarSkillId = req.avatarSkillId
            this.oldLevel = oldLevel
            curLevel = level
        })
    }

    fun unlockConstellation(session: GameSession, req: UnlockAvatarTalentReq) {
        val avatar = session.account.avatars[req.avatarGuid]!!
        val talentData = avatarTalentData[req.talentId]!!
        InventoryService.pay(session, listOf(CostItem(talentData.mainCostItemId)))
        avatar.constellations.add(req.talentId)
        session.send(avatarUnlockTalentNotify {
            avatarGuid = avatar.guid
//    entityId = avatar.entityId // TODO
            talentId = req.talentId
            skillDepotId = avatar.skillDepotId
        })
        session.send(unlockAvatarTalentRsp { avatarGuid = avatar.guid; talentId = req.talentId })

        extraConstellationData[talentData.openConfig]!!.forEach {
            if (it is AvatarTalentDataCharge) {
                // Check if new constellation adds skill charges
                avatar.skillExtraCharges[it.skillID] = it.pointDelta
                session.send(avatarSkillMaxChargeCountNotify {
                    avatarGuid = avatar.guid
                    skillId = it.skillID
                    maxChargeCount = it.pointDelta
                })
            } else if (it is AvatarTalentDataProud) {
                // Check if new constellation adds +3 to a skill level
                val proudSkillGroupId = avatar.promoteId * 100 + 30 + it.talentIndex
                avatar.proudSkillBonusMap[proudSkillGroupId] = 3
                session.send(proudSkillExtraLevelNotify {
                    avatarGuid = avatar.guid
                    talentType = 3 // Talent type = 3 "AvatarSkill"
                    talentIndex = it.talentIndex
                    extraLevel = 3
                })
            }
        }
        recalcStats(session, avatar)// , forceSendAbilityChange=true
//        avatar.save()
    }

    fun create(account: Account, id: Int) {
        val data = avatarData[id]!!
        val skillDepotId = when (id) {
            MC_MALE_ID -> 504
            MC_FEMALE_ID -> 704
            else -> data.skillDepotId
        }
        val weapon = Weapon(account.newGuid(), data.initialWeapon)
        account.inventory.weapons[weapon.guid] = weapon
        val avatar = Avatar(account.newGuid(), id, skillDepotId, data.promoteId, weapon.guid)
        weapon.equipCharacterId = avatar.guid
        account.avatars[avatar.guid] = avatar
        account.curTeam()[0] = avatar.guid

        val skillDepot = avatarSkillDepotData[skillDepotId]!!
        avatar.skillLevels.putAll(skillDepot.skillIds.filter { it > 0 }.associateBy { it })
        avatar.skillLevels[skillDepot.burstId] = 1
        avatar.proudSkillIds.add(proudSkillIdFor(skillDepotId, 0)!!)
    }

    fun revive(session: GameSession, guid: Long): Boolean {
        val avatar = session.account.avatars[guid]!!
        if (avatar.prop(CUR_HP) > 0f) return false
        avatar.prop(CUR_HP, avatar.prop(MAX_HP) * .1f)
        return lifeStateNotify(session, avatar)
    }

    fun heal(session: GameSession, guid: Long, satiationParams: List<Int>): Boolean {
        val avatar = session.account.avatars[guid]!!
        val maxHp = avatar.prop(MAX_HP)
        val curHp = avatar.prop(CUR_HP)
        if (curHp == 0f) return false
        val (healRate, healAmount) = satiationParams
        val heal = maxHp * healRate / 100 + healAmount / 100
        avatar.prop(CUR_HP, (curHp + heal).coerceAtMost(maxHp))
        return lifeStateNotify(session, avatar)
    }

    private fun lifeStateNotify(session: GameSession, avatar: Avatar): Boolean {
        session.send(avatarFightPropUpdateNotify {
            avatarGuid = avatar.guid
            fightPropMap.put(CUR_HP.id, avatar.prop(CUR_HP)) })
        session.send(avatarLifeStateChangeNotify {
            avatarGuid = avatar.guid
            lifeState = LifeState.ALIVE.value })
        return true
    }

    fun recalcStats(session: GameSession, guid: Long?) {
        session.account.avatars[guid]?.let { recalcStats(session, it) }
    }

    private fun recalcStats(session: GameSession, avatar: Avatar) {
        avatar.extraAbilityEmbryos.clear()

        val data = avatarData[avatar.id]!!
        val (hpCurve, atkCurve, defCurve) = data.curves
            .map { avatarCurveData[it.Type]!![avatar.level - 1] }
        val props = StatMap(mutableMapOf(
            BASE_HP to data.hpBase * hpCurve,
            BASE_ATK to data.attackBase * atkCurve,
            BASE_DEF to data.defenseBase * defCurve,
            CRITICAL to data.critical,
            CRITICAL_HURT to data.criticalHurt,
            ER to 1f,
        ))

        val promoteData = avatarPromoteData[avatar.promoteId]!![avatar.promoteLevel]
        promoteData.addProps.forEach { props.add(it.propType, it.value) }

        val skillDepot = avatarSkillDepotData[avatar.skillDepotId]
        val burstData = avatarSkillData[skillDepot?.burstId]
        if (burstData != null) {
            val elementType = ElementType.valueOf(burstData.element)
            val curEr = elementType.curEnergyProp
            props[elementType.maxEnergyProp] = burstData.elementValue
            props[curEr] = avatar.fightProperties[curEr.id] ?: 0f
        }

        val relics = avatar.relicGuids.mapNotNull { session.account.inventory.relics[it] }
        relics.forEach { relic ->
            val relicData = relicData[relic.id]!!
            val mainPropData = reliquaryMainPropData.find { it.id == relic.mainPropId }!!
            val mainProp = relicLevelData[relicData.rank]!![relic.level - 1].addProps
                .find { it.propType == mainPropData.propType }!!
            props.add(mainProp.propType, mainProp.value)

            relic.appendPropIdList.forEach {
                val affixes = reliquaryAffixData[it]
                props.add(affixes.propType, affixes.propValue)
            }
        }
        relics.groupBy { relicData[it.id]?.setId }.forEach { (setId, u) ->
            val relicSetData = reliquarySetData[setId]!!
            val affixes = List(relicSetData.setNeedNums.filter { it <= u.size }.size) { idx ->
                equipAffixData[relicSetData.equipAffixId]!![idx] }
            affixes.flatMap { it.props }.forEach { props.add(it.propType, it.value) }
            avatar.extraAbilityEmbryos.addAll(affixes.map { it.openConfig }.filter { it.isNotEmpty() })
        }

        val weapon = session.account.inventory.weapons[avatar.weaponGuid]!!
        val weaponData = weaponData[weapon.id]!!
        val curveInfos = weaponCurveData[weapon.level - 1].curveInfos
        weaponData.props.forEach { prop ->
            val info = curveInfos.find { it.type == prop.type }!!
            props.add(prop.propType, prop.initValue * info.value)
        }
        weaponPromoteData[weaponData.promoteId]!![weapon.promoteLevel - 1]
            .props.filter { it.value > 0 }
            .forEach { props.add(it.propType, it.value) }
        val affixes = weapon.affixIds.map { equipAffixData[it]!![weapon.refinement - 1] }
        affixes.flatMap { it.props }.forEach { props.add(it.propType, it.value) }
        avatar.extraAbilityEmbryos.addAll(affixes.map { it.openConfig }.filter { it.isNotEmpty() })

        val proudSkillList = skillDepot?.inherentProudSkillOpens
            ?.filter { it.proudSkillGroupId != null && it.needAvatarPromoteLevel <= avatar.promoteLevel }
            ?.map { proudSkillData[it.proudSkillGroupId]!![0] }
        proudSkillList?.flatMap { it.props }?.forEach { props.add(it.propType, it.value) }
        avatar.extraAbilityEmbryos.addAll(affixes.map { it.openConfig }.filter { it.isNotEmpty() })

        avatar.extraAbilityEmbryos.addAll(avatar.constellations
            .map { avatarTalentData[it]!!.openConfig })

        props[MAX_HP] = props[BASE_HP] * (1f + props[HP_PERCENT]) + props[HP]
        props[CUR_ATK] = props[BASE_ATK] * (1f + props[ATK_PERCENT]) + props[ATK]
        props[CUR_DEF] = props[BASE_DEF] * (1f + props[DEF_PERCENT]) + props[DEF]

        // Get hp percent, set to 100% if none
        val maxHp = avatar.fightProperties[MAX_HP.id] ?: 0f
        val curHp = avatar.fightProperties[CUR_HP.id] ?: 0f
        val hpPercent: Float = if (maxHp <= 0) 1f else curHp / maxHp
        props[CUR_HP] = props[MAX_HP] * hpPercent

        avatar.fightProperties.clear()
        avatar.fightProperties.putAll(props.map { it.key.id to it.value })
        session.send(avatarFightPropNotify {
            avatarGuid = avatar.guid
            fightPropMap.putAll(avatar.fightProperties)
        })

//        // Update client abilities
//        val entity: EntityAvatar? = player!!.teamManager.activeTeam.firstOrNull { it.avatar === this }
//        if (entity != null && (extraAbilityEmbryos != prevExtraAbilityEmbryos || forceSendAbilityChange)) {
//            player!!.sendPacket(PacketAbilityChangeNotify(entity))
//        }
    }

    fun toProto(avatar: Avatar) = avatarInfo {
        avatarId = avatar.id
        guid = avatar.guid
        lifeState = 1
        talentIdList.addAll(avatar.constellations)
        fightPropMap.putAll(avatar.fightProperties)
        skillDepotId = avatar.skillDepotId
        coreProudSkillLevel = avatar.constellations.size
        skillLevelMap.putAll(avatar.skillLevels)
        inherentProudSkillList.addAll(avatar.proudSkillIds)
        proudSkillExtraLevelMap.putAll(avatar.proudSkillBonusMap)
        avatarType = 1
        bornTime = avatar.createdAt.toInt()
        fetterInfo = toFetterProto(avatar)
        wearingFlycloakId = avatar.flyCloakId
        costumeId = avatar.costumeId
        skillMap.putAll(avatar.skillExtraCharges.mapValues { (_, value) ->
            avatarSkillInfo { maxChargeCount = value } })
        equipGuidList.add(avatar.weaponGuid)
        equipGuidList.addAll(avatar.relicGuids)
        propMap.putAll(setOf(
            prop(PlayerProperty.PROP_LEVEL, avatar.level.toLong()),
            prop(PlayerProperty.PROP_EXP, avatar.exp.toLong()),
            prop(PlayerProperty.PROP_BREAK_LEVEL, avatar.promoteLevel.toLong()),
            prop(PlayerProperty.PROP_SATIATION_VAL, 0L),
            prop(PlayerProperty.PROP_SATIATION_PENALTY_TIME, 0L),
        ).toMap())
    }

    private fun toFetterProto(avatar: Avatar) = avatarFetterInfo {
        expLevel = totalFetterExpData.indexOfFirst { it > avatar.fetterTotalExp }
        expNumber = avatar.fetterTotalExp - totalFetterExpData[expLevel - 1]
        fetterList.addAll(fetterIdMap[avatar.id]!!.subList(0, expLevel)
            .map { fetterData { fetterId = it; fetterState = 3 /* FINISH*/ } })
        if (expLevel == 10) rewardedFetterLevelList.add(10)
    }

    fun propNotify(avatar: Avatar) = avatarPropNotify {
        avatarGuid = avatar.guid
        propMap[PlayerProperty.PROP_LEVEL.id] = avatar.level.toLong()
        propMap[PlayerProperty.PROP_EXP.id] = avatar.exp.toLong()
        propMap[PlayerProperty.PROP_BREAK_LEVEL.id] = avatar.promoteLevel.toLong()
        propMap[PlayerProperty.PROP_SATIATION_VAL.id] = 0
        propMap[PlayerProperty.PROP_SATIATION_PENALTY_TIME.id] = 0
    }

    private fun prop(property: PlayerProperty, value: Long) = property.id to propValue {
        type = property.id
        ival = value
        this.value = value
    }

    private const val MC_MALE_ID = 10000005
    private const val MC_FEMALE_ID = 10000007
    private val bookExpMap = mapOf(104001 to 1000, 104002 to 5000, 104003 to 20000)

    enum class LifeState(val value: Int) {
        NONE(0), ALIVE(1), DEAD(2), REVIVE(3);
    }
}

class StatMap(
    private val underlying: MutableMap<Stat, Float> = EnumMap(Stat::class.java)
) : MutableMap<Stat, Float> by underlying {
    override fun get(key: Stat) = underlying[key] ?: 0f
    fun add(type: FightProperty, value: Float) {
        val stat = Stat.from(type)
        if (type != FIGHT_PROP_NONE)
            underlying[stat] = (underlying[stat] ?: 0f) + value
    }
}