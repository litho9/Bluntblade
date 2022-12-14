package bluntblade.interaction

import bluntblade.*
import bluntblade.game.ElementType
import bluntblade.game.EnterReason
import bluntblade.game.Stat
import bluntblade.inventory.*
import java.time.Instant
import kotlin.random.Random

fun prop(propertyId: Int, value: Long) = propValue {
    type = propertyId
    ival = value
    this.value = value
}

val defaultEmbryos = listOf(
    "Avatar_DefaultAbility_VisionReplaceDieInvincible",
    "Avatar_DefaultAbility_AvartarInShaderChange",
    "Avatar_SprintBS_Invincible",
    "Avatar_Freeze_Duration_Reducer",
    "Avatar_Attack_ReviveEnergy",
    "Avatar_Component_Initializer",
    "Avatar_FallAnthem_Achievement_Listener"
)
fun embryosFor(avatar: Avatar): List<String> {
    val data = avatarData[avatar.id]!!
    val avatarName = data.iconName.split("_").last()
    val embryoData = avatarEmbryoMap[avatarName]!!
    val depotData = avatarSkillDepotData[avatar.skillDepotId]!!
    val mcEmbryos = mcEmbryoMap[depotData.abilityGroup]
        ?.targetAbilities?.map { it.abilityName } ?: emptyList()
    return embryoData + mcEmbryos + avatar.extraAbilityEmbryos
}

fun abilityHash(str: String) = str.chars().reduce(0) { acc, b -> b + 131 * acc }

object SceneService {
    fun initFinish(session: GameSession) {
        session.send(serverTimeNotify {
            serverTime = System.currentTimeMillis()
        })
        fun onlineInfo(it: Account) = onlinePlayerInfo {
            uid = it.uid
            nickname = it.nickname
            playerLevel = it.level
            mpSettingType = MpSettingType.MP_SETTING_TYPE_ENTER_AFTER_APPLY
            nameCardId = it.nameCardId
            signature = it.signature
            profilePicture = profilePicture { avatarId = it.headImage }
            curPlayerNumInWorld = session.world?.accounts?.size ?: 1
        }
        session.send(worldPlayerInfoNotify {
            playerInfoList.addAll(session.world!!.accounts.map { onlineInfo(it) })
        }) // 2 usages
        session.send(worldDataNotify {
            worldPropMap.put(1, prop(1, session.world!!.host.account.worldLevel.toLong()))
            worldPropMap.put(2, prop(2, if (session.world!!.isMultiplayer) 1 else 0))
        })
        session.send(playerWorldSceneInfoListNotify {
            infoList.add(playerWorldSceneInfo { sceneId = 1 })
            infoList.add(playerWorldSceneInfo {
                sceneId = 3
                sceneTagIdList.addAll(listOf(102, 113, 117))
            })
            infoList.add(playerWorldSceneInfo {
                sceneId = 4
                sceneTagIdList.addAll(listOf(106, 109, 117))
            })
            infoList.add(playerWorldSceneInfo { sceneId = 5 })
            infoList.add(playerWorldSceneInfo { sceneId = 6 })
            infoList.add(playerWorldSceneInfo { sceneId = 7 })
        })
        session.send(sceneForceUnlockNotify {})
        session.send(hostPlayerNotify {
            hostUid = session.world!!.host.account.uid
            hostPeerId = 1 // session.world!!.peerId
        })
        session.send(sceneTimeNotify {
            sceneId = session.scene.id
        })
        session.send(playerGameTimeNotify {
            gameTime = 8 * 60 // session.player.scene.time
            uid = session.account.uid
        })
        session.send(playerEnterSceneInfoNotify {
            curAvatarEntityId = session.curAvatar.id
            enterSceneToken = session.enterSceneToken
            teamEnterInfo = teamEnterSceneInfo {
                teamEntityId = session.curTeamId
                teamAbilityInfo = abilitySyncStateInfo {}
                abilityControlBlock = abilityControlBlock {}
            }
            mpLevelEntityInfo = mPLevelEntityInfo {
                entityId = session.world!!.id
                authorityPeerId = 1 // session.world!!.peerId
                abilityInfo = abilitySyncStateInfo {}
            }
            avatarEnterInfo.addAll(session.curTeam.map { entity -> avatarEnterSceneInfo {
                val avatar: Avatar = entity.avatar
                avatarGuid = avatar.guid
                avatarEntityId = entity.id
                weaponGuid = avatar.weaponGuid
                weaponEntityId = entity.wield.id
                avatarAbilityInfo = abilitySyncStateInfo {}
                weaponAbilityInfo = abilitySyncStateInfo {}
            } })
        })

        // TODO move to WorldService.updatePlayerInfos
        session.send(sceneAreaWeatherNotify {
            weatherAreaId = session.world!!.weatherId
            climateType = session.world!!.climate.value
        })
        session.send(scenePlayerInfoNotify {
            playerInfoList.addAll(session.world!!.players.map { scenePlayerInfo {
                uid = it.account.uid
                peerId = it.peerId
                name = it.account.nickname
                sceneId = it.scene.id
                onlinePlayerInfo = onlineInfo(it.account)
            }})
        })
        session.send(sceneTeamUpdateNotify {
            isInMp = session.world!!.isMultiplayer
            session.world!!.players.forEach { p ->
                val resonances = p.curTeam.groupBy {
                    val skillDepot = avatarSkillDepotData[it.avatar.skillDepotId]!!
                    val burstData = avatarSkillData[skillDepot.burstId]!!
                    ElementType.valueOf(burstData.element)
                }.entries.filter { it.value.size > 1 }
                val resonanceEmbryos = resonances.map { it.key.configName!! }
                    .ifEmpty { listOf("TeamResonance_AllDifferent") }
                val resonanceIds = resonances.map { it.key.teamResonanceId }
                    .ifEmpty { listOf(10801) }
                sceneTeamAvatarList.addAll(p.curTeam.map { entity -> sceneTeamAvatar {
                    playerUid = p.account.uid
                    avatarGuid = entity.avatar.guid
                    sceneId = p.scene.id
                    entityId = entity.id
                    sceneEntityInfo = entity.toProto()
                    weaponGuid = entity.wield.weapon.guid
                    weaponEntityId = entity.wield.id
                    isPlayerCurAvatar = p.curAvatar == entity
                    isOnScene = p.curAvatar == entity
                    avatarAbilityInfo = abilitySyncStateInfo {}
                    weaponAbilityInfo = abilitySyncStateInfo {}
                    abilityControlBlock = abilityControlBlock {
                        val embryos = defaultEmbryos + resonanceEmbryos + embryosFor(entity.avatar)
                        abilityEmbryoList.addAll(embryos.mapIndexed { i, e -> abilityEmbryo {
                            abilityId = i + 1
                            abilityNameHash = abilityHash(e)
                        } })
                    }
                    avatarInfo = AvatarService.toProto(entity.avatar)
                    sceneAvatarInfo = sceneAvatarInfo {
                        uid = p.account.uid
                        avatarId = entity.avatar.id
                        guid = entity.avatar.guid
                        peerId = p.peerId
                        talentIdList.addAll(entity.avatar.constellations)
                        coreProudSkillLevel = entity.avatar.constellations.size
                        skillLevelMap.putAll(entity.avatar.skillLevels)
                        skillDepotId = entity.avatar.skillDepotId
                        inherentProudSkillList.addAll(entity.avatar.proudSkillIds)
                        proudSkillExtraLevelMap.putAll(entity.avatar.proudSkillBonusMap)
                        teamResonanceList.addAll(resonanceIds)
                        wearingFlycloakId = entity.avatar.flyCloakId
                        costumeId = entity.avatar.costumeId
                        bornTime = (entity.avatar.createdAt / 1000).toInt()
                    }
                } })
            }
        })
        session.send(syncTeamEntityNotify {
            sceneId = session.scene.id
            teamEntityInfoList.addAll(session.world!!.guests.map { teamEntityInfo {
                teamEntityId = session.curTeamId
                authorityPeerId = session.peerId
                teamAbilityInfo = abilitySyncStateInfo {}
            } })
        })
        session.send(syncScenePlayTeamEntityNotify {
            sceneId = session.scene.id
        })
        session.send(sceneInitFinishRsp {
            enterSceneToken = session.enterSceneToken
        }, buildHeader(11))
    }

    fun enterReady(session: GameSession) {
        session.send(enterScenePeerNotify {
            destSceneId = session.scene.id
            peerId = session.peerId
            hostPeerId = session.world!!.host.peerId
            enterSceneToken = session.enterSceneToken
        })
        session.send(enterSceneReadyRsp {
            enterSceneToken = session.enterSceneToken
        }, buildHeader(11))
    }

    fun enterDone(session: GameSession) {
        session.send(enterSceneDoneRsp {
            enterSceneToken = session.enterSceneToken
        })
        session.send(playerTimeNotify {
            isPaused = session.isPaused
            playerTime = session.clientTime
            serverTime = System.currentTimeMillis()
        }) // Probably not the right place

        if (!session.scene.entities.containsKey(session.curAvatar.id)) {
            if (session.curAvatar.avatar.prop(Stat.CUR_HP) <= 0f)
                session.curAvatar.avatar.prop(Stat.CUR_HP, 1f)
            add(session.curAvatar)
            session.curTeam.map { it.avatar.skillExtraCharges }.filter { it.isNotEmpty() }
                .forEach { session.send(avatarSkillInfoNotify {
                    skillMap.putAll(it.mapValues { avatarSkillInfo { maxChargeCount = it.value } })
                }) }
        }
        val entities = session.scene.entities.filter { it.key != session.curAvatar.id }
        session.send(sceneEntityAppearNotify {
            appearType = VisionType.VISION_TYPE_MEET
            entityList.addAll(entities.values.map { it.toProto() })
        })

        session.send(worldPlayerLocationNotify {
            playerWorldLocList.addAll(session.world!!.players
                .map { playerWorldLocationInfo {
                    sceneId = it.scene.id
                    playerLoc = it.locationInfo
                } })
        })
        session.send(scenePlayerLocationNotify {
            sceneId = session.scene.id
            playerLocList.addAll(session.world!!.players
                .filter { it.scene.id == session.scene.id }
                .map { it.locationInfo })
        })
        session.send(worldPlayerRTTNotify {
            playerRttList.addAll(session.world!!.players.map {
                playerRTTInfo {
                    uid = it.account.uid
                    rtt = it.srtt
                }
            })
        })
        session.lastLocationNotify = System.currentTimeMillis()
    }

    fun enterPost(session: GameSession) {
        session.send(postEnterSceneRsp {
            enterSceneToken = session.enterSceneToken
        })
    }

    fun changeTime(session: GameSession, req: ChangeGameTimeReq) {
        session.scene.time = req.gameTime % 1440
        session.send(changeGameTimeRsp { curGameTime = session.scene.time })
    }

    fun pointGet(session: GameSession, req: GetScenePointReq) {
        val points = scenePointDataFor(req.sceneId).points.keys.map { it.toInt() }
        session.send(getScenePointRsp {
            sceneId = req.sceneId
            unlockedPointList.addAll(points)
            unlockAreaList.addAll(1..8)
        })
    }

    fun areaGet(session: GameSession, req: GetSceneAreaReq) {
        val areaIds = (1..14) + (17..19) + (100..103) + 200 + 210 + 300
        session.send(getSceneAreaRsp {
            sceneId = req.sceneId
            areaIdList.addAll(areaIds)
            cityInfoList.addAll((1..3).map { cityInfo { cityId = it; level = 1 } })
        }, buildHeader(0))
    }

    fun gadgetCreated(session: GameSession, req: EvtCreateGadgetNotify) {
        session.scene.entities[req.entityId]?.let { return } // don't add duplicates
        val entity = EntityClientGadget(session.account.uid, session.peerId, req)
        session.scene.entities[req.entityId] = entity
//        gadget.owner.teamManager.gadgets.add(gadget) // TODO Add to owner's gadget list
        session.world?.players?.filter { it != session }?.forEach {
            it.send(sceneEntityAppearNotify {
                appearType = VisionType.VISION_TYPE_BORN
                entityList.add(entity.toProto())
            })
        }
    }

    fun gadgetDestroyed(session: GameSession, req: EvtDestroyGadgetNotify) {
        val gadget = (session.scene.entities[req.entityId] ?: return) as EntityClientGadget
        session.scene.entities.remove(gadget.id)
//        entity.owner.teamManager.gadgets.remove(entity) // TODO Remove from owner's gadget list
        session.world?.players?.filter { it.account.uid != gadget.ownerId }?.forEach {
            it.send(sceneEntityDisappearNotify {
                disappearType = VisionType.VISION_TYPE_DIE
                entityList.add(gadget.id)
            })
        }
    }

    fun drown(session: GameSession, req: SceneEntityDrownReq) {
        val entity = session.scene.entities[req.entityId] ?: return // EntityMonster || EntityAvatar
        if (entity is EntityMonster) entity.fightProps[Stat.CUR_HP] = 0f
        if (entity is EntityAvatar) entity.avatar.fightProperties[Stat.CUR_HP.id] = 0f
        // TODO: make a list somewhere of all entities to remove per tick rather than one by one
        killEntity(session, entity)
        session.broadcast(sceneEntityDrownRsp { entityId = req.entityId })
    }

    private fun killEntity(session: GameSession, target: GameEntity, attackerId: Int = 0) {
        session.broadcast(lifeStateChangeNotify {
            entityId = target.id
            lifeState = AvatarService.LifeState.DEAD.value
            sourceEntityId = attackerId
        })
        if (target is EntityMonster && session.scene.type != SceneType.SCENE_DUNGEON)
            DropService.drop(session, target)

        session.scene.entities.remove(target.id)
        session.broadcast(sceneEntityDisappearNotify {
            disappearType = VisionType.VISION_TYPE_DIE
            entityList.add(target.id)
        })

        if (target is EntityMonster) onDeath(session, target, attackerId)
    }

    private fun onDeath(session: GameSession, entity: EntityMonster, killerId: Int) {
        entity.spawnEntry?.let { session.scene.deadSpawnedEntities.add(it) }

        val challenge = session.scene.challenge
        challenge?.triggers?.filter { it.type == "MONSTER" }
                ?.forEach { _ -> onMonsterDeath(session, challenge) }

//        if (getScene().getScriptManager().isInit() && this.getGroupId() > 0) {
//            if (getScene().getScriptManager().getScriptMonsterSpawnService() != null) {
//                getScene().getScriptManager().getScriptMonsterSpawnService().onMonsterDead(this)
//            }
//            // prevent spawn monster after success
//            if (getScene().getChallenge() == null || getScene().getChallenge() != null && getScene().getChallenge().inProgress())
//                getScene().getScriptManager().callEvent(EventType.EVENT_ANY_MONSTER_DIE, ScriptArgs().setParam1(this.getConfigId()))
//        }

//        session.world?.players?.forEach {
//            BattlePassService.trigger(it, WatcherTriggerType.TRIGGER_MONSTER_DIE, entity.monster.id, 1) }
    }

    private fun onMonsterDeath(session: GameSession, challenge: WorldChallenge) {
        session.broadcast(challengeDataNotify {
            challengeIndex = challenge.index
            paramIndex = 1
            value = ++challenge.score
        })
        if (challenge.score >= challenge.goal)
            challenge.status = ChallengeStatus.SUCCESS
    }

    // vehicleCreate
//    fun teleport(session: GameSession)

    fun personalSceneJump(session: GameSession, req: PersonalSceneJumpReq) {
        val point = scenePointDataFor(session.scene.id)
                .points[req.pointId.toString()] ?: return
        val pos = point.tranPos
        val sceneId: Int = point.tranSceneId

//        player.getWorld().transferPlayerToScene(player, sceneId, pos)
        val data = sceneData[sceneId]!!
        val oldScene = session.scene
        val oldPos = session.pos!!
        // oldScene.removePlayer(player);
        session.scene = Scene(sceneId, data.type) // TODO see if someone else is on the scene before creating
        session.pos = pos
        val enterType = /*if (dungeonData != null) EnterType.ENTER_TYPE_DUNGEON
                else */ if (data.type == SceneType.SCENE_HOME_WORLD) EnterType.ENTER_TYPE_SELF_HOME
                else if (oldScene.id == sceneId) EnterType.ENTER_TYPE_GOTO
                else EnterType.ENTER_TYPE_JUMP
        val enterReason = when (enterType) {
            EnterType.ENTER_TYPE_DUNGEON -> EnterReason.DungeonEnter
            EnterType.ENTER_TYPE_SELF_HOME -> EnterReason.EnterHome
            else -> EnterReason.TransPoint
        }
        enterSceneNotify(session, sceneId, oldPos, enterType, enterReason)

        session.send(personalSceneJumpRsp {
            destSceneId = sceneId
            destPos = vector { x = pos.x; y = pos.y; z = pos.z; } })
    }

    // AI sync
    // sit down
    // stand up
    // enter pathfinding
    // set entity client data notify

    private fun add(entity: EntityAvatar) {
        TODO("Not implemented yet")
    }

    fun enterSceneNotify(session: GameSession, sceneId: Int, oldPos: Position, enterType: EnterType,
            enterReason: EnterReason, target: Account = session.account) {
        session.enterSceneToken = Random.nextInt(1000, 99999)
        val newPos = session.pos!!
        session.send(playerEnterSceneNotify {
            this.sceneId = sceneId
            prevPos = vector { x = oldPos.x; y = oldPos.y; z = oldPos.z; }
            pos = vector { x = newPos.x; y = newPos.y; z = newPos.z; }
            this.type = enterType
            sceneBeginTime = System.currentTimeMillis()
            targetUid = target.uid
            enterSceneToken = session.enterSceneToken
            worldLevel = target.worldLevel
            this.enterReason = enterReason.value
            worldType = 1
            sceneTransaction = "$sceneId-${target.uid}-${Instant.now().epochSecond}-18402"
            if (enterReason != EnterReason.Login)
                sceneTagIdList.addAll(0..2999)
        })
    }
}

enum class SceneType(val value: Int) {
    SCENE_NONE(0),
    SCENE_WORLD(1),
    SCENE_DUNGEON(2),
    SCENE_ROOM(3),
    SCENE_HOME_WORLD(4),
    SCENE_HOME_ROOM(5),
    SCENE_ACTIVITY(6);
}
