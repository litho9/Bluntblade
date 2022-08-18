package dullblade.account

import com.google.protobuf.ByteString
import dullblade.Account
import dullblade.BasePacket
import dullblade.GameSession
import dullblade.GameSessionKcp
import dullblade.game.PacketOpcodes.*
import dullblade.inventory.playerDataNotify
import dullblade.inventory.propValue

// HandlerPlayerForceExitReq
// HandlerPingReq
// HandlerGetAuthkeyReq

class PacketGetPlayerTokenRsp(session: GameSession)
    : BasePacket(GetPlayerTokenRsp, getPlayerTokenRsp {
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

class PacketPlayerLoginRsp : BasePacket(PlayerLoginRsp, playerLoginRsp {
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
}, buildHeader(1), GameSessionKcp.DISPATCH_KEY) {
    companion object {
        private val regionCache by lazy {
            regionInfo {
                gateserverIp = "127.0.0.1"
                gateserverPort = 22102
                secretKey = ByteString.copyFrom(GameSessionKcp.DISPATCH_SEED)
            }
        }
    }
}

class PacketTakeAchievementRewardReq
    : BasePacket(TakeAchievementRewardReq,
        takeAchievementRewardReq {})

class PacketPlayerDataNotify(account: Account)
    : BasePacket(PlayerDataNotify, playerDataNotify {
    nickName = account.nickname
    isFirstLoginToday = true
    regionId = account.regionId
    propMap.putAll(account.properties.mapValues { (key, value) ->
        propValue {
            type = key
            ival = value
            this.value = value
        }
    })
}, buildHeader(2))