package dullblade.inventory

import dullblade.game.EquipType
import dullblade.game.FightProperty
import dullblade.game.MaterialType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileInputStream

val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> resource(path: String) = FileInputStream(
    object {}.javaClass.classLoader.getResource(path)!!.path)
    .use { json.decodeFromStream<T>(it) }

val unlockMaxLevel = arrayOf(20, 40, 50, 60, 70, 80)

@Serializable
data class WeaponLevelData(val Level: Int, val RequiredExps: List<Int>)

val weaponTotalExpData: List<List<Int>> by lazy {
    val data = resource<List<WeaponLevelData>>("./ExcelBinOutput/WeaponLevelExcelConfigData.json")
    val newData = (0..4).map { i -> data.map { it.RequiredExps[i] } }

    newData.map { rankData ->
        var totalExp = 0
        rankData.map { xp ->
            totalExp += xp
            totalExp
        }
    }
}

@Serializable
data class ReliquaryAffixData(
    val Id: Int,
    val DepotId: Int,
    val GroupId: Int,
    val PropType: FightProperty,
    val PropValue: Float,
    val Weight: Double,
    val UpgradeWeight: Int
)

val reliquaryAffixData: List<ReliquaryAffixData> =
    resource("./ExcelBinOutput/ReliquaryAffixExcelConfigData.json")

@Serializable
data class ReliquaryMainPropData /*: GameResource()*/ (
    val Id: Int,
    val PropDepotId: Int,
    val PropType: FightProperty,
    val AffixName: String,
    val Weight: Int = 1
)

val reliquaryMainPropData: List<ReliquaryMainPropData> =
    resource("./ExcelBinOutput/ReliquaryMainPropExcelConfigData.json")

@Serializable
data class AddProps(val PropType: FightProperty = FightProperty.FIGHT_PROP_NONE, val Value: Float = 0f)
@Serializable
data class ReliquaryLevelData(val Rank: Int = 0, val Level: Int, val Exp: Int = 0, val AddProps: List<AddProps>)

val relicLevelData: Map<Int, List<ReliquaryLevelData>> by lazy {
    resource<List<ReliquaryLevelData>>("./ExcelBinOutput/ReliquaryLevelExcelConfigData.json")
        .groupBy { it.Rank }
}
val relicTotalExpData: List<List<Int>> by lazy {
    relicLevelData.values.map { rankData ->
        var totalExp = 0
        rankData.map { data -> totalExp += data.Exp; totalExp }
    }
}

@Serializable
data class WeaponPropertyData(
    val PropType: FightProperty = FightProperty.FIGHT_PROP_NONE,
    val InitValue: Float = 0f,
    val Type: String
)
@Serializable
data class WeaponData(
    @SerialName("Id") val id: Int,
    @SerialName("RankLevel") val rank: Int,
    @SerialName("WeaponPromoteId") val promoteId: Int,
    @SerialName("SkillAffix") val skillAffixes: List<Int>,
    @SerialName("WeaponBaseExp") val baseExp: Int,
    // private val storyId = 0
    @SerialName("AwakenCosts") val awakenCosts: List<Int>,
    @SerialName("WeaponProp") val props: List<WeaponPropertyData>
)

val weaponData: Map<Int, WeaponData> by lazy {
    resource<List<WeaponData>>("./ExcelBinOutput/WeaponExcelConfigData.json")
        .associateBy(WeaponData::id)
}

@Serializable
data class CostItem(
    @SerialName("Id") val id: Int = 0,
    @SerialName("Count") val count: Int = 0)
@Serializable
data class WeaponPromoteData(
    @SerialName("WeaponPromoteId") val id: Int,
    @SerialName("CostItems") val costItems: List<CostItem>,
    @SerialName("CoinCost") val costCoin: Int = 0,
    @SerialName("UnlockMaxLevel") val unlockMaxLevel: Int,
    @SerialName("AddProps") val props: List<AddProps>,
)

val weaponPromoteData by lazy {
    resource<List<WeaponPromoteData>>("./ExcelBinOutput/WeaponPromoteExcelConfigData.json")
        .groupBy(WeaponPromoteData::id)
}

@Serializable
data class WeaponCurveInfoData(val Type: String, val Value: Float)
@Serializable
data class WeaponCurveData(
    val CurveInfos: List<WeaponCurveInfoData>,
)
val weaponCurveData by lazy {
    resource<List<WeaponCurveData>>("./ExcelBinOutput/WeaponCurveExcelConfigData.json")
}

@Serializable
data class RelicData(
    @SerialName("Id") val id: Int,
    @SerialName("EquipType") val type: EquipType,
    @SerialName("RankLevel") val rank: Int,
    @SerialName("SetId") val setId: Int = 0,
    @SerialName("MaxLevel") val maxLevel: Int,
    @SerialName("MainPropDepotId") val mainPropDepotId: Int,
    @SerialName("AppendPropDepotId") val depotId: Int,
    @SerialName("BaseConvExp") val baseConvExp: Int
)
//        "ShowPic": "Eff_UI_RelicIcon_10000_4",
//        "AddPropLevels": [],
//        "DestroyReturnMaterial": [],
//        "DestroyReturnMaterialCount": [],
//        "NameTextMapHash": 980293684,
//        "DescTextMapHash": 2642558472,
//        "Icon": "UI_RelicIcon_10000_4",
//        "ItemType": "ITEM_RELIQUARY",
//        "Weight": 1,
//        "Rank": 10,
//        "GadgetId": 70600041

val relicData by lazy {
    resource<List<RelicData>>("./ExcelBinOutput/ReliquaryExcelConfigData.json")
        .associateBy(RelicData::id)
}

data class RelicSetData(
    @SerialName("Id") val id: Int,
    @SerialName("EquipAffixId") val equipAffixId: Int,
    @SerialName("SetNeedNum") val setNeedNums: List<Int>
)
val reliquarySetData by lazy {
    resource<List<RelicSetData>>("./ExcelBinOutput/ReliquarySetExcelConfigData.json")
        .associateBy { it.id }
}

data class EquipAffixData(
    @SerialName("Id") val id: Int,
    @SerialName("EquipAffixId") val equipAffixId: Int,
    @SerialName("AddProps") val props: List<AddProps>,
    @SerialName("OpenConfig") val openConfig: String
)
val equipAffixData by lazy {
    resource<List<EquipAffixData>>("./ExcelBinOutput/EquipAffixExcelConfigData.json")
        .groupBy { it.id }
}

data class ItemUse(
    @SerialName("UseOp") val op: String = "",
    @SerialName("UseParam") val params: List<String>,
)
data class MaterialData(
    @SerialName("Id") val id: Int,
    @SerialName("MaterialType") val type: MaterialType,
    @SerialName("UseTarget") val useTarget: String,
    @SerialName("SatiationParams") val satiationParams: List<Int>,
    @SerialName("ItemUse") val itemUse: List<ItemUse>,
    @SerialName("DestroyReturnMaterial") val destroyReturnMaterial: List<Int>,
    @SerialName("DestroyReturnMaterialCount") val destroyReturnMaterialCount: List<Int>,
)
val materialData by lazy {
    resource<List<MaterialData>>("./ExcelBinOutput/MaterialExcelConfigData.json")
        .associateBy { it.id }
}
fun destroyMaterialsFor(itemId: Int): List<CostItem> {
    val data = materialData[itemId]!!
    return data.destroyReturnMaterial.zip(data.destroyReturnMaterialCount)
        .map { CostItem(it.first, it.second) }
}

data class CombineData(
    @SerialName("CombineId") val id: Int,
    @SerialName("PlayerLevel") val playerLevel: Int,
    @SerialName("MaterialItems") val materials: List<CostItem>,
    @SerialName("ScoinCost") val moraCost: Int,
    @SerialName("ResultItemId") val resultItemId: Int,
    @SerialName("ResultItemCount") val resultItemCount: Int,
)
val combineData by lazy {
    resource<List<CombineData>>("./ExcelBinOutput/CombineExcelConfigData.json")
        .associateBy { it.id }
}