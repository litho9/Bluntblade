package dullblade.interaction

import dullblade.inventory.resource
import kotlinx.serialization.Serializable

@Serializable
data class PointData(val gadgetId: Int)
@Serializable
data class ScenePointData(
    val points: Map<String, PointData>
)
fun scenePointDataFor(sceneId: Int) =
    resource<ScenePointData>("./BinOutput/Scene/Point/scene${sceneId}_point.json")