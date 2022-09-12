package bluntblade.inventory

import bluntblade.Account
import bluntblade.Avatar
import bluntblade.BasePacket
import bluntblade.game.PlayerProperty

fun prop(property: PlayerProperty, value: Long) = property.id to propValue {
    type = property.id
    ival = value
    this.value = value
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

class PacketAvatarDataNotify(account: Account)
    : BasePacket(avatarDataNotify {
    curAvatarTeamId = account.curTeamIdx
    chooseAvatarGuid = account.curTeam()[0] // TODO
    ownedFlycloakList.addAll(account.flyCloaks)
    ownedCostumeList.addAll(account.costumes)
    avatarList.addAll(account.avatars.values.map { toProto(it) })
    avatarTeamMap.putAll(account.teams.mapIndexed { idx, guids -> idx to avatarTeam {
        teamName = account.teamNames[idx]
        avatarGuidList.addAll(guids)
    } }.toMap())
}, buildHeader(2))
