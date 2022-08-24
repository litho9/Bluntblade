package bluntblade.account

import com.google.protobuf.ByteString
import bluntblade.Account
import bluntblade.BasePacket
import bluntblade.GameSession
import bluntblade.GameSessionKcp
import bluntblade.inventory.playerDataNotify
import bluntblade.inventory.propValue

// HandlerPlayerForceExitReq
// HandlerPingReq
// HandlerGetAuthkeyReq

class PacketGetPlayerTokenRsp(session: GameSession)
    : BasePacket(getPlayerTokenRsp {
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

class PacketPlayerLoginRsp : BasePacket(playerLoginRsp {
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
    : BasePacket(takeAchievementRewardReq {})

class PacketPlayerDataNotify(account: Account)
    : BasePacket(playerDataNotify {
    nickName = account.nickname
    isFirstLoginToday = true
    regionId = account.regionId
    propMap.putAll(account.properties.map { (key, value) ->
        key.id to propValue {
            type = key.id
            ival = value
            this.value = value
        }
    }.toMap())
}, buildHeader(2))