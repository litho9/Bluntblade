package dullblade.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PropGrowCurvesData(val Type: String, val GrowCurve: String)
@Serializable
data class AvatarData(
    @SerialName("Id") val id: Int,
    @SerialName("AvatarPromoteId") val promoteId: Int,
    @SerialName("SkillDepotId") val skillDepotId: Int,
    val HpBase: Float,
    val AttackBase: Float,
    val DefenseBase: Float,
    val Critical: Float,
    val CriticalHurt: Float,
    val InitialWeapon: Int,
    @SerialName("PropGrowCurves") val curves: List<PropGrowCurvesData>
)
val avatarData by lazy {
    resource<List<AvatarData>>("./ExcelBinOutput/AvatarExcelConfigData.json")
        .associateBy(AvatarData::id)
}

@Serializable
data class AvatarPromoteData(
    @SerialName("AvatarPromoteId") val id: Int,
    @SerialName("CostItems") val costs: List<CostItem>,
    @SerialName("ScoinCost") val costCoin: Int,
    @SerialName("UnlockMaxLevel") val unlockMaxLevel: Int,
    @SerialName("AddProps") val addProps: List<AddProps>
)
val avatarPromoteData by lazy {
    resource<List<AvatarPromoteData>>("./ExcelBinOutput/AvatarPromoteExcelConfigData.json")
        .groupBy(AvatarPromoteData::id)
}

@Serializable
data class InherentProudSkillOpens(
    val ProudSkillGroupId: Int? = null,
    val NeedAvatarPromoteLevel: Int = 0
)
data class AvatarSkillDepot(
    @SerialName("Id") val id: Int,
    @SerialName("EnergySkill") val burstId: Int,
    @SerialName("Skills") val skillIds: List<Int>,
    @SerialName("EnergySkill") val energySkill: Int,
    val InherentProudSkillOpens: List<InherentProudSkillOpens>
)
val avatarSkillDepotData by lazy {
    resource<List<AvatarSkillDepot>>("./ExcelBinOutput/AvatarSkillDepotExcelConfigData.json")
//        .onEach { it.InherentProudSkillOpens.removeIf { r -> r.ProudSkillGroupId == null } }
        .associateBy(AvatarSkillDepot::id)
}
fun proudSkillIdFor(skillDepotId: Int, level: Int): Int? =
    avatarSkillDepotData[skillDepotId]!!.InherentProudSkillOpens.firstOrNull {
        it.NeedAvatarPromoteLevel == level && it.ProudSkillGroupId != null
    }?.ProudSkillGroupId

@Serializable
data class AvatarLevelData(val Level: Int, val Exp: Int)
val avatarLevelData by lazy {
    resource<List<AvatarLevelData>>("./ExcelBinOutput/AvatarLevelExcelConfigData.json")
        .map(AvatarLevelData::Exp)
}
val avatarTotalExpData: List<Int> by lazy {
    var totalExp = 0
    avatarLevelData.map { exp -> totalExp += exp; totalExp }
}

@Serializable
data class AvatarSkillData(
    @SerialName("Id") val id: Int,
    @SerialName("CostElemType") val element: String, // Ex.: "Ice"
    @SerialName("CostElemVal") val elementValue: Float,
    @SerialName("ProudSkillGroupId") val proudSkillGroupId: Int
)
val avatarSkillData by lazy {
    resource<List<AvatarSkillData>>("./ExcelBinOutput/AvatarSkillExcelConfigData.json")
        .associateBy(AvatarSkillData::id)
}
@Serializable
data class ProudSkillData(
    @SerialName("ProudSkillId") val id: Int,
    @SerialName("ProudSkillGroupId") val proudSkillGroupId: Int,
    @SerialName("CostItems") val costs: List<CostItem>,
    @SerialName("CoinCost") val costCoin: Int,
    @SerialName("AddProps") val props: List<AddProps>,
)
val proudSkillData by lazy {
    resource<List<ProudSkillData>>("./ExcelBinOutput/ProudSkillExcelConfigData.json")
        .groupBy(ProudSkillData::proudSkillGroupId)
}

@Serializable
data class AvatarTalentData(
    @SerialName("TalentId") val id: Int,
    @SerialName("MainCostItemId") val mainCostItemId: Int,
    @SerialName("OpenConfig") val openConfig: String,
)
val avatarTalentData by lazy {
    resource<List<AvatarTalentData>>("./ExcelBinOutput/AvatarTalentExcelConfigData.json")
        .associateBy(AvatarTalentData::id)
}

@Serializable
open class AvatarTalentDataExtra
@Serializable
data class AvatarTalentDataProud(
    @SerialName("\$type") val type: String,
    val talentIndex: Int,
    val extraLevel: Int
) : AvatarTalentDataExtra()
@Serializable
data class AvatarTalentDataCharge(
    @SerialName("\$type") val type: String,
    val skillID: Int,
    val pointDelta: Int
) : AvatarTalentDataExtra()
val extraConstellationData by lazy {
    File(AvatarTalentDataExtra::class.java.classLoader
        .getResource("./BinOutput/Talent/AvatarTalents")!!.toURI()).walk()
        .map { resource<Map<String, List<AvatarTalentDataExtra>>>(it.path) }
        .reduce { acc, b -> acc + b }
}

val fetterExpData = listOf(1000, 1550, 2050, 2600, 3175, 3750, 4350, 4975, 5650, 6325)
val totalFetterExpData = with(0) {
    var totalExp = 0
    fetterExpData.map { exp -> totalExp += exp; totalExp }
}
val maxFetterExp = totalFetterExpData[totalFetterExpData.size - 1]

@Serializable
data class FetterData(val FetterId: Int, val AvatarId: Int)
val fetterIdMap by lazy {
    resource<List<FetterData>>("./ExcelBinOutput/FettersExcelConfigData.json")
        .groupBy(FetterData::AvatarId)
        .mapValues { it.value.map(FetterData::FetterId) }
}

@Serializable
data class CurveInfosData(val Type: String, val Value: Float)
@Serializable
data class AvatarCurveData(val Level: Int, val CurveInfos: List<CurveInfosData>)
val avatarCurveData: Map<String, List<Float>> by lazy {
    val data = resource<List<AvatarCurveData>>("./ExcelBinOutput/AvatarCurveExcelConfigData.json")
    (0 until 4).associate { i -> data[0].CurveInfos[i].Type to data.map { it.CurveInfos[i].Value } }
}