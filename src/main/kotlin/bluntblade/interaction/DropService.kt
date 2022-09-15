package bluntblade.interaction

import bluntblade.GameSession
import bluntblade.inventory.resource
import kotlinx.serialization.Serializable

@Serializable
data class MonsterData(
    val id: Int,
)
val monsterData by lazy { resource<List<MonsterData>>("data/Monster.json").associateBy(MonsterData::id) }

object DropService {
    fun drop(session: GameSession, entity: EntityMonster) {
//        val data = monsterData[entity.monster.id]
//        val players = data.share
    }
}