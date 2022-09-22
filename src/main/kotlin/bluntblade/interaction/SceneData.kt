package bluntblade.interaction

import bluntblade.inventory.resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Position(
    @SerialName("_x") val x: Float,
    @SerialName("_y") val y: Float,
    @SerialName("_z") val z: Float
)
@Serializable
data class PointData(
    val gadgetId: Int,
    val tranPos: Position,
    val tranSceneId: Int = 0
)
@Serializable
data class ScenePointData(
    val points: Map<String, PointData>
)
fun scenePointDataFor(sceneId: Int) =
    resource<ScenePointData>("./BinOutput/Scene/Point/scene${sceneId}_point.json")

@Serializable
data class SceneData(val id: Int, val type: SceneType)
val sceneData by lazy {
    resource<List<SceneData>>("data/Scene.json")
            .associateBy(SceneData::id)
}