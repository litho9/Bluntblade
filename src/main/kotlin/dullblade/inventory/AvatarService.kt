package dullblade.inventory

import dullblade.*
import dullblade.game.ElementType
import dullblade.game.FightProperty
import dullblade.game.FightProperty.*
import dullblade.game.Stat
import dullblade.game.Stat.*
import dullblade.inventory.AvatarMessages.*
import java.util.*

object AvatarService {
    fun promote(session: GameSession, req: AvatarPromoteReq) {
        val avatar = session.account.avatars[req.guid]!!
        val promoteData = avatarPromoteData[avatar.promoteId]!![avatar.promoteLevel++]

        InventoryService.pay(session, promoteData.costs, promoteData.costCoin)
        proudSkillIdFor(avatar.skillDepotId, avatar.promoteLevel)?.let {
            avatar.proudSkillIds.add(it)
            session.send(PacketProudSkillChangeNotify(avatar))
        }

        session.send(PacketAvatarPropNotify(avatar),
            PacketAvatarPromoteRsp(avatar))
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
        session.send(PacketAvatarPropNotify(avatar),
            PacketAvatarUpgradeRsp(avatar, oldLevel, oldPropMap))
    }

    fun upgradeFetterLevel(session: GameSession, avatar: Avatar, expGain: Int) {
        avatar.fetterTotalExp = (avatar.fetterTotalExp + expGain).coerceAtMost(maxFetterExp)
//        avatar.save()
        session.send(PacketAvatarPropNotify(avatar),
            PacketAvatarFetterDataNotify(avatar))
    }

    fun upgradeSkill(session: GameSession, req: AvatarSkillUpgradeReq) {
        val avatar = session.account.avatars[req.avatarGuid]!!
        val oldLevel = avatar.skillLevels[req.avatarSkillId] ?: 1
        val level = oldLevel + 1
        val proudSkill = proudSkillData[avatarSkillData[req.avatarSkillId]!!.proudSkillGroupId]!![level - 1]

        InventoryService.pay(session, proudSkill.costs, proudSkill.costCoin)
        avatar.skillLevels[req.avatarSkillId] = level
//        avatar.save()

        session.send(PacketAvatarSkillChangeNotify(avatar, req.avatarSkillId, oldLevel, level),
            PacketAvatarSkillUpgradeRsp(avatar, req.avatarSkillId, oldLevel, level))
    }

    fun unlockConstellation(session: GameSession, req: UnlockAvatarTalentReq) {
        val avatar = session.account.avatars[req.avatarGuid]!!
//        val nextTalentId = avatar.promoteId * 10 + avatar.constellations.size + 1
        val talentData = avatarTalentData[req.talentId]!!

        InventoryService.pay(session, listOf(CostItem(talentData.mainCostItemId, 1)), 0)
        avatar.constellations.add(req.talentId)

        session.send(PacketAvatarUnlockTalentNotify(avatar, req.talentId),
            PacketUnlockAvatarTalentRsp(avatar, req.talentId))

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
                session.send(PacketProudSkillExtraLevelNotify(avatar, it.talentIndex))
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
        val weapon = Weapon(account.newGuid(), data.InitialWeapon)
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
        session.send(PacketAvatarFightPropUpdateNotify(avatar, CUR_HP))
        session.send(PacketAvatarLifeStateChangeNotify(avatar, LifeState.ALIVE.value))
        return true
    }

    fun heal(session: GameSession, guid: Long, satiationParams: List<Int>): Boolean {
        val avatar = session.account.avatars[guid]!!
        val maxHp = avatar.prop(MAX_HP)
        val curHp = avatar.prop(CUR_HP)
        if (curHp == 0f) return false
        val (healRate, healAmount) = satiationParams
        val heal = maxHp * healRate / 100 + healAmount / 100
        avatar.prop(CUR_HP, (curHp + heal).coerceAtMost(maxHp))
        session.send(PacketAvatarFightPropUpdateNotify(avatar, CUR_HP))
        session.send(PacketAvatarLifeStateChangeNotify(avatar, LifeState.ALIVE.value))
        return true
    }

    fun recalcStats(session: GameSession, guid: Long?) {
        session.account.avatars[guid]?.let { recalcStats(session, it) }
    }

    fun recalcStats(session: GameSession, avatar: Avatar) {
        avatar.extraAbilityEmbryos.clear()

        val data = avatarData[avatar.id]!!
        val (hpCurve, atkCurve, defCurve) = data.curves
            .map { avatarCurveData[it.Type]!![avatar.level - 1] }
        val props = StatMap(mutableMapOf(
            BASE_HP to data.HpBase * hpCurve,
            BASE_ATK to data.AttackBase * atkCurve,
            BASE_DEF to data.DefenseBase * defCurve,
            CRITICAL to data.Critical,
            CRITICAL_HURT to data.CriticalHurt,
            ER to 1f,
        ))

        val promoteData = avatarPromoteData[avatar.promoteId]!![avatar.promoteLevel]
        promoteData.addProps.forEach { props.add(it.PropType, it.Value) }

        val skillDepot = avatarSkillDepotData[avatar.skillDepotId]
        val burstData = avatarSkillData[skillDepot?.energySkill]
        if (burstData != null) {
            val elementType = ElementType.valueOf(burstData.element)
            val curEr = elementType.curEnergyProp
            props[elementType.maxEnergyProp] = burstData.elementValue
            props[curEr] = avatar.fightProperties[curEr.id] ?: 0f
        }

        val relics = avatar.relicGuids.mapNotNull { session.account.inventory.relics[it] }
        relics.forEach { relic ->
            val relicData = relicData[relic.id]!!
            val mainPropData = reliquaryMainPropData.find { it.Id == relic.mainPropId }!!
            val mainProp = relicLevelData[relicData.rank]!![relic.level - 1].AddProps
                .find { it.PropType == mainPropData.PropType }!!
            props.add(mainProp.PropType, mainProp.Value)

            relic.appendPropIdList.forEach {
                val affixes = reliquaryAffixData[it]
                props.add(affixes.PropType, affixes.PropValue)
            }
        }
        relics.groupBy { relicData[it.id]?.setId }.forEach { (setId, u) ->
            val relicSetData = reliquarySetData[setId]!!
            val affixes = List(relicSetData.setNeedNums.filter { it <= u.size }.size) { idx ->
                equipAffixData[relicSetData.equipAffixId]!![idx] }
            affixes.flatMap { it.props }.forEach { props.add(it.PropType, it.Value) }
            avatar.extraAbilityEmbryos.addAll(affixes.map { it.openConfig }.filter { it.isNotEmpty() })
        }

        val weapon = session.account.inventory.weapons[avatar.weaponGuid]!!
        val weaponData = weaponData[weapon.id]!!
        val curveInfos = weaponCurveData[weapon.level - 1].CurveInfos
        weaponData.props.forEach { prop ->
            val info = curveInfos.find { it.Type == prop.Type }!!
            props.add(prop.PropType, prop.InitValue * info.Value)
        }
        weaponPromoteData[weaponData.promoteId]!![weapon.promoteLevel - 1]
            .props.filter { it.Value > 0 }
            .forEach { props.add(it.PropType, it.Value) }
        val affixes = weapon.affixIds.map { equipAffixData[it]!![weapon.refinement - 1] }
        affixes.flatMap { it.props }.forEach { props.add(it.PropType, it.Value) }
        avatar.extraAbilityEmbryos.addAll(affixes.map { it.openConfig }.filter { it.isNotEmpty() })

        val proudSkillList = skillDepot?.InherentProudSkillOpens
            ?.filter { it.ProudSkillGroupId != null && it.NeedAvatarPromoteLevel <= avatar.promoteLevel }
            ?.map { proudSkillData[it.ProudSkillGroupId]!![0] }
        proudSkillList?.flatMap { it.props }?.forEach { props.add(it.PropType, it.Value) }
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


        session.send(PacketAvatarFightPropNotify(avatar.guid, avatar.fightProperties))
//        // Update client abilities
//        val entity: EntityAvatar? = player!!.teamManager.activeTeam.firstOrNull { it.avatar === this }
//        if (entity != null && (extraAbilityEmbryos != prevExtraAbilityEmbryos || forceSendAbilityChange)) {
//            player!!.sendPacket(PacketAbilityChangeNotify(entity))
//        }
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