package dullblade.account

import dullblade.BasePacket
import dullblade.GameSession
import dullblade.account.AccountMessages.*
import dullblade.db
import dullblade.game.PacketOpcodes
import dullblade.inventory.*
import dullblade.queue.ForgeQueueService
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection

object AccountService {
    fun getToken(session: GameSession, req: GetPlayerTokenReq) {
        val accounts = db.getCollection<dullblade.Account>()
        session.account = accounts.findOne(dullblade.Account::login eq req.accountUid)
            ?: dullblade.Account(0 /*TODO*/, req.accountUid, req.accountToken)
                .also { accounts.insertOne(it) }
        // TODO check token, index login&token, check banned, existing session, server max players

//        session.player = DatabaseHelper.getOrCreatePlayer(session) // Set player object for session
//        session.secretKey = Crypto.ENCRYPT_KEY
//        session.state = SessionState.WAITING_FOR_LOGIN
        session.send(PacketGetPlayerTokenRsp(session))
    }

    fun login(session: GameSession, req: PlayerLoginReq) {
        if (session.account.avatars.isEmpty()) {
//            session.state = SessionState.PICKING_CHARACTER // Pick main character
            // Show opening cutscene if player has no avatars
            session.send(BasePacket(PacketOpcodes.DoSetPlayerBornDataNotify, null))
        } else {
            onLogin(session)
        }

        // Final packet to tell client logging in is done
        session.send(PacketPlayerLoginRsp(), PacketTakeAchievementRewardReq())
    }

    fun chooseMc(session: GameSession, req: SetPlayerBornDataReq) {
        val avatarId = req.avatarId
        session.account.mainCharacterId = avatarId
        session.account.nickname = req.nickName
        session.account.headImage = avatarId
        AvatarService.create(session.account, avatarId)
//        player.save() TODO

        onLogin(session)
        session.send(BasePacket(PacketOpcodes.SetPlayerBornDataRsp, null))
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
        session.send(PacketPlayerDataNotify(session.account),
            PacketStoreWeightLimitNotify(),
            PacketPlayerStoreNotify(session.account.inventory),
            PacketAvatarDataNotify(session.account),
//        session.send(PacketFinishedParentQuestNotify(this)) TODO
//        session.send(PacketBattlePassAllDataNotify(this))
//        session.send(PacketQuestListNotify(this))
//        session.send(PacketCodexDataFullNotify(this))
//        session.send(PacketAllWidgetDataNotify(this))
//        session.send(PacketWidgetGadgetAllDataNotify())
            PacketCombineDataNotify(session.account.unlockedCombines)
        )
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
//        session.send(PacketPlayerEnterSceneNotify(this)) // Enter game world
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
}