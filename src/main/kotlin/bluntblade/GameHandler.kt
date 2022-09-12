package bluntblade

import bluntblade.game.PropMap
import bluntblade.game.Stat
import bluntblade.interaction.*
import bluntblade.inventory.AvatarService
import bluntblade.inventory.vector
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap

fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

@Serializable
data class Inventory(
    val weapons: MutableMap<Long, Weapon> = HashMap(),
    val relics: MutableMap<Long, Relic> = HashMap(),
    val materials: MutableMap<Int, Material> = HashMap(),
    val furniture: MutableMap<Int, Material> = HashMap(),
    var mora: Int = 0,
)

@Serializable
data class ForgeQueue(
    val id: Int,
    val avatarId: Int,
    val timestamps: List<Int>,
)

@Serializable
data class Account(
    val uid: Int,
    val login: String,
    val token: String,
    val regionId: Int = 3,
    var mainCharacterId: Int = 10000007,

    // profile
    var nickname: String = "temp",
    var headImage: Int = 10000007,
    var nameCardId: Int = 10000007, // TODO
    var signature: String = "",

    val inventory: Inventory = Inventory(),
    val avatars: MutableMap<Long, Avatar> = HashMap(),

    var curTeamIdx: Int = 0,
    val teams: List<MutableList<Long>> = listOf((0 until 4).map { 0L }.toMutableList()),
    val teamNames: List<String> = mutableListOf("Team 1", "Team 2", "Team 3", "Team 4"),

    val properties: PropMap = PropMap(),
    var nextGuid: Long = 0L,
    val flyCloaks: Set<Int> = setOf(),
    val costumes: Set<Int> = setOf(),

    val level: Int = 1, // AR
    val worldLevel: Int = 1,
    val forgeQueues: MutableList<ForgeQueue> = ArrayList(),
    val unlockedForgingBlueprints: MutableSet<Int> = HashSet(),
    val unlockedCombines: MutableSet<Int> = HashSet(),
    val unlockedFurniture: MutableSet<Int> = HashSet(),
    val unlockedFurnitureSuites: MutableSet<Int> = HashSet(),

    var moonCardEnd: String? = null, // Ex.: "2020-09-26", Java is big dumb and doesn't serialize LocalDate
    var moonCardLastGet: String? = null, // Ex.: "2020-09-26"
) {
    fun newGuid() = (uid.toLong() shl 32) + ++nextGuid
    fun curTeam() = teams[curTeamIdx]
}

interface GameItem {
    val guid: Long
    val id: Int
    val count: Int
}

@Serializable
abstract class Equip(
    var level: Int = 0,
    var locked: Boolean = false,
    var exp: Int = 0,
    var equipCharacterId: Long = 0,
    var totalExp: Int = 0,
    override val count: Int = 1
) : GameItem

@Serializable
data class Weapon(
    override val guid: Long,
    override val id: Int,
    var promoteLevel: Int = 0,
    var refinement: Int = 0, // 0..4
    val affixIds: MutableList<Int> = mutableListOf()
) : Equip(1 /*data.rank > 3 TODO */)

@Serializable
data class Relic(
    override val guid: Long,
    override val id: Int,
    val mainPropId: Int,
    val appendPropIdList: MutableList<Int>,
) : Equip()

@Serializable
class Material( // MaterialStack
    override val guid: Long,
    override val id: Int,
    override var count: Int
) : GameItem

@Serializable
class Avatar(
    val guid: Long,
    val id: Int,
    var skillDepotId: Int, // it changes for main character
    val promoteId: Int,
    var weaponGuid: Long,
    val relicGuids: MutableList<Long> = mutableListOf(),
    var level: Int = 1,
    var exp: Int = 0,
    var totalExp: Int = 0,
    var promoteLevel: Int = 0,
    var fetterTotalExp: Int = 0,

    val createdAt: Long = System.currentTimeMillis() / 1000,
    val flyCloakId: Int = 140001,
    val costumeId: Int = 0,
    val proudSkillIds: MutableList<Int> = mutableListOf(),
    val fightProperties: MutableMap<Int, Float> = mutableMapOf(),
    val extraAbilityEmbryos: MutableSet<String> = HashSet(),
    val skillLevels: MutableMap<Int, Int> = mutableMapOf(),
    val constellations: MutableList<Int> = mutableListOf(),
    val skillExtraCharges: MutableMap<Int, Int> = mutableMapOf(),
    val proudSkillBonusMap: MutableMap<Int, Int> = mutableMapOf()
) {
    fun prop(stat: Stat) = fightProperties[stat.id] ?: 0f
    fun prop(stat: Stat, value: Float) { fightProperties[stat.id] = value }
}

abstract class GameEntity(
    val id: Int,
    var lastMoveSceneTimeMs: Int = 0,
    val lastMoveReliableSeq: Int = 0
) {
    open fun toProto() : SceneEntityInfo {
        TODO()
    }
}
class EntityWeapon(
    id: Int,
    val weapon: Weapon,
) : GameEntity(id)
class EntityAvatar(
    id: Int,
    val avatar: Avatar,
    val wield: EntityWeapon,
    val lifeState: AvatarService.LifeState,
) : GameEntity(id) {
    override fun toProto() = sceneEntityInfo {
        entityId = id
        entityType = ProtEntityType.PROT_ENTITY_TYPE_AVATAR
        animatorParaList.add(animatorParameterValueInfoPair {})
        entityClientData = entityClientData {}
        entityAuthorityInfo = entityAuthorityInfo {
            abilityInfo = abilitySyncStateInfo {}
            rendererChangedInfo = entityRendererChangedInfo {}
            aiInfo = sceneEntityAiInfo {
                isAiOpen = true
                bornPos = vector {}
            }
            bornPos = vector {}
        }
        lastMoveSceneTimeMs = this@EntityAvatar.lastMoveSceneTimeMs
        lastMoveReliableSeq = this@EntityAvatar.lastMoveReliableSeq
        lifeState = this@EntityAvatar.lifeState.value
    }
}

class Scene(
    val id: Int,
    var time: Int
) {
//    fun broadcastPacket(packetGadgetInteractRsp: PacketGadgetInteractRsp): PacketGadgetInteractRsp {
//        return packetGadgetInteractRsp // TODO("Not yet implemented")
//    }


    val entities: MutableMap<Int, GameEntity> = ConcurrentHashMap()
}