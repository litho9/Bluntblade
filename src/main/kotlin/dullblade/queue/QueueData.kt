package dullblade.queue

import dullblade.inventory.CostItem
import dullblade.inventory.resource
import kotlinx.serialization.SerialName

data class ForgeData(
    @SerialName("Id") val id: Int,
    @SerialName("ResultItemId") val resultItemId: Int,
    @SerialName("ResultItemCount") val resultItemCount: Int,
    @SerialName("MaterialItems") val materials: List<CostItem>,
    @SerialName("ScoinCost") val moraCost: Int,
    @SerialName("ForgeTime") val duration: Int,
    val points: Int = 1, // TODO "ForgePoints" not present on JSON
)
val forgeData: Map<Int, ForgeData> by lazy {
    resource<List<ForgeData>>("./ExcelBinOutput/ForgeExcelConfigData.json")
        .associateBy(ForgeData::id)
}