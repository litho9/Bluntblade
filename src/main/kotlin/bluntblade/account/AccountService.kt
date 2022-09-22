package bluntblade.account

import bluntblade.inventory.*
import bluntblade.GameSession
import bluntblade.GameSessionKcp
import bluntblade.account.AccountMessages.*
import bluntblade.buildHeader
import bluntblade.db
import bluntblade.game.EnterReason
import bluntblade.interaction.EnterType
import bluntblade.interaction.SceneService
import bluntblade.queue.ForgeQueueService
import com.google.protobuf.ByteString
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection

// HandlerPlayerForceExitReq
// HandlerPingReq
// HandlerGetAuthkeyReq

object AccountService {
    fun getToken(session: GameSession, req: GetPlayerTokenReq) {
        val accounts = db.getCollection<bluntblade.Account>()
        session.account = accounts.findOne(bluntblade.Account::login eq req.accountUid)
            ?: bluntblade.Account(0 /*TODO*/, req.accountUid, req.accountToken)
                .also { accounts.insertOne(it) }
        // TODO check token, index login&token, check banned, existing session, server max players

//        session.player = DatabaseHelper.getOrCreatePlayer(session) // Set player object for session
//        session.secretKey = Crypto.ENCRYPT_KEY
//        session.state = SessionState.WAITING_FOR_LOGIN
        session.send(getPlayerTokenRsp {
            uid = session.account.uid
            token = session.account.token
            accountType = 1
            isProficientPlayer = session.account.avatars.isNotEmpty() // Not sure where this goes
            secretKeySeed = GameSessionKcp.ENCRYPT_SEED
            securityCmdBuffer = ByteString.copyFrom(GameSessionKcp.ENCRYPT_SEED_BUFFER)
            platformType = 3
            channelId = 1
            countryCode = "US"
            clientVersionRandomKey = "c25-314dd05b0b5f"
            regPlatform = 3
            clientIpStr = session.getHostAddress()
        }, buildHeader(++session.lastClientSeq), GameSessionKcp.DISPATCH_KEY)
    }

    fun login(session: GameSession, req: PlayerLoginReq) {
        if (session.account.avatars.isEmpty()) {
//            session.state = SessionState.PICKING_CHARACTER // Pick main character
            // Show opening cutscene if player has no avatars
            session.send(doSetPlayerBornDataNotify {})
        } else {
            onLogin(session)
        }

        // Final packet to tell client logging in is done
        session.send(playerLoginRsp {
            isUseAbilityHash = true // true
            abilityHashCode = 1844674 // 1844674
            gameBiz = "hk4e_global"
            clientDataVersion = regionCache.clientDataVersion
            clientSilenceDataVersion = regionCache.clientSilenceDataVersion
            clientMd5 = regionCache.clientDataMd5
            clientSilenceMd5 = regionCache.clientSilenceDataMd5
            resVersionConfig = regionCache.resVersionConfig
            clientVersionSuffix = regionCache.clientVersionSuffix
            clientSilenceVersionSuffix = regionCache.clientSilenceVersionSuffix
            isScOpen = false //.setScInfo(ByteString.copyFrom(new byte[] {}))
            registerCps = "mihoyo"
            countryCode = "US"
        }, buildHeader(1), GameSessionKcp.DISPATCH_KEY)
        session.send(takeAchievementRewardReq {})
    }

    fun chooseMc(session: GameSession, req: SetPlayerBornDataReq) {
        val avatarId = req.avatarId
        session.account.mainCharacterId = avatarId
        session.account.nickname = req.nickName
        session.account.headImage = avatarId
        AvatarService.create(session.account, avatarId)
//        player.save() TODO

        onLogin(session)
        session.send(setPlayerBornDataRsp {})
        // TODO welcome mail
    }

    private fun onLogin(session: GameSession) {
//        // Create world
//        val world = World(this)
//        world.addPlayer(this)
//
//        // Multiplayer setting
//        this.setProperty(PlayerProperty.PROP_PLAYER_MP_SETTING_TYPE, mpSetting.number, false)
//        this.setProperty(PlayerProperty.PROP_IS_MP_MODE_AVAILABLE, 1, false)
//
//        // Execute daily reset logic if this is a new day.
//        doDailyReset()

        // Packets
        session.send(playerDataNotify {
            nickName = session.account.nickname
            isFirstLoginToday = true
            regionId = session.account.regionId
            propMap.putAll(session.account.properties.map { (key, value) ->
                key.id to propValue {
                    type = key.id
                    ival = value
                    this.value = value
                }
            }.toMap())
        }, buildHeader(2))
        session.send(storeWeightLimitNotify {
            storeType = InventoryMessages.StoreType.PACK
            weightLimit = InventoryService.LIMIT_ALL
            weaponCountLimit = InventoryService.LIMIT_WEAPONS
            reliquaryCountLimit = InventoryService.LIMIT_RELICS
            materialCountLimit = InventoryService.LIMIT_MATERIALS
            furnitureCountLimit = InventoryService.LIMIT_FURNITURE
        })
//        session.send(PacketPlayerStoreNotify(session.account.inventory))
        session.send(playerStoreNotify {
            storeType = InventoryMessages.StoreType.PACK
            weightLimit = InventoryService.LIMIT_ALL
            itemList.addAll(session.account.inventory.weapons.map { InventoryService.toProto(it.value) })
            itemList.addAll(session.account.inventory.relics.map { InventoryService.toProto(it.value) })
            itemList.addAll(session.account.inventory.materials.map { (id, material) -> item {
                guid = material.guid
                itemId = id
                this.material = material { count = material.count }
            } })
            itemList.addAll(session.account.inventory.furniture.map { (id, material) -> item {
                guid = material.guid
                itemId = id
                this.furniture = furniture { count = material.count }
            } })
        })
//        session.send(PacketFinishedParentQuestNotify(this)) TODO
//        session.send(PacketBattlePassAllDataNotify(this))
//        session.send(PacketQuestListNotify(this))
//        session.send(PacketCodexDataFullNotify(this))
//        session.send(PacketAllWidgetDataNotify(this))
//        session.send(PacketWidgetGadgetAllDataNotify())
        session.send(avatarDataNotify {
            curAvatarTeamId = session.account.curTeamIdx
            chooseAvatarGuid = session.account.curTeam()[0] // TODO
            ownedFlycloakList.addAll(session.account.flyCloaks)
            ownedCostumeList.addAll(session.account.costumes)
            avatarList.addAll(session.account.avatars.values.map { AvatarService.toProto(it) })
            avatarTeamMap.putAll(session.account.teams.mapIndexed { idx, guids -> idx to avatarTeam {
                teamName = session.account.teamNames[idx]
                avatarGuidList.addAll(guids)
            } }.toMap())
        }, buildHeader(2))
        session.send(combineDataNotify { combineIdList.addAll(session.account.unlockedCombines) })
        ForgeQueueService.notify(session)
//        resinManager.onPlayerLogin()
//        todayMoonCard() // The timer works at 0:0, some users log in after that, use this method to check if they have received a reward today or not. If not, send the reward.
//
//        // Battle Pass trigger
//        battlePassManager!!.triggerMission(WatcherTriggerType.TRIGGER_LOGIN)
//        furnitureManager.onLogin()
//        // Home
//        home = GameHome.getByUid(uid)
//        home!!.onOwnerLogin(this)
        SceneService.enterSceneNotify(session, 3, session.pos!!,
                EnterType.ENTER_TYPE_SELF, EnterReason.Login) // Enter game world
//        session.send(PacketPlayerLevelRewardUpdateNotify(rewardedLevels))
//        session.send(PacketOpenStateUpdateNotify())
//
//        hasSentAvatarDataNotify = true // First notify packets sent
//
//        session.state = SessionState.ACTIVE // Set session state
//
//        // register
//        server.registerPlayer(this)
//        profile.player = this // Set online
    }

    private val regionCache by lazy {
        regionInfo {
            gateserverIp = "127.0.0.1"
            gateserverPort = 22102
            secretKey = ByteString.copyFrom(GameSessionKcp.DISPATCH_SEED)
        }
    }
}