package bluntblade.interaction

import bluntblade.*
import bluntblade.game.ElementType
import bluntblade.game.Stat
import bluntblade.inventory.*

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
                val team = p.curTeam
                val resonances = team.groupBy {
                    val skillDepot = avatarSkillDepotData[it.avatar.skillDepotId]!!
                    val burstData = avatarSkillData[skillDepot.energySkill]!!
                    ElementType.valueOf(burstData.element)
                }.entries.filter { it.value.size > 1 }
                val resonanceEmbryos = resonances.map { it.key.configName!! }
                    .ifEmpty { listOf("TeamResonance_AllDifferent") }
                val resonanceIds = resonances.map { it.key.teamResonanceId }
                    .ifEmpty { listOf(10801) }
                sceneTeamAvatarList.addAll(team.map { entity -> sceneTeamAvatar {
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
                    avatarInfo = toProto(entity.avatar)
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
        }, BasePacket.buildHeader(11))
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
        }, BasePacket.buildHeader(11))
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
            session.curTeam.filter { it.avatar.skillExtraCharges.isNotEmpty() }
                .forEach { session.send(avatarSkillInfoNotify {
                    skillMap.putAll(it.avatar.skillExtraCharges
                        .mapValues { avatarSkillInfo { maxChargeCount = it.value } })
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
        session.send(changeGameTimeRsp {
            curGameTime = session.scene.time
        })
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
            cityInfoList.addAll((1..3).map { cityInfo {
                cityId = it
                level = 1
            } })
        }, BasePacket.buildHeader(0))
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

    // vehicleCreate
    // drown
    // teleport
    // AI sync
    // sit down
    // stand up
    // personal scene jump
    // enter pathfinding
    // set entity client data notify

    private fun add(entity: EntityAvatar) {
        TODO("Not implemented yet")
    }
}