package bluntblade.queue

import bluntblade.inventory.CostItem
import bluntblade.inventory.resource
import kotlinx.serialization.SerialName

data class ForgeData(
    val id: Int,
    val resultItemId: Int,
    val resultItemCount: Int,
    @SerialName("materialItems") val materials: List<CostItem>,
    @SerialName("scoinCost") val moraCost: Int,
    @SerialName("forgeTime") val duration: Int,
    @SerialName("forgePoint") val points: Int = 1,
)
val forgeData by lazy { resource<List<ForgeData>>("data/Forge.json").associateBy(ForgeData::id) }