package bluntblade.inventory

import bluntblade.game.EquipType
import bluntblade.game.FightProperty
import bluntblade.game.MaterialType
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
data class WeaponLevelData(val level: Int, val requiredExps: List<Int>)

val weaponTotalExpData: List<List<Int>> by lazy {
    val data = resource<List<WeaponLevelData>>("data/WeaponLevel.json")
    val newData = (0..4).map { i -> data.map { it.requiredExps[i] } }

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
    val id: Int,
    val depotId: Int,
    val groupId: Int,
    val propType: FightProperty,
    val propValue: Float,
    val weight: Double,
    val upgradeWeight: Int
)

val reliquaryAffixData: List<ReliquaryAffixData> =
    resource("data/ReliquaryAffix.json")

@Serializable
data class ReliquaryMainPropData /*: GameResource()*/ (
    val id: Int,
    val propDepotId: Int,
    val propType: FightProperty,
    val affixName: String,
    val weight: Int = 1
)

val reliquaryMainPropData: List<ReliquaryMainPropData> =
    resource("data/ReliquaryMainProp.json")

@Serializable
data class AddProps(val propType: FightProperty = FightProperty.FIGHT_PROP_NONE, val value: Float = 0f)
@Serializable
data class ReliquaryLevelData(val rank: Int = 0, val level: Int, val exp: Int = 0, val addProps: List<AddProps>)

val relicLevelData: Map<Int, List<ReliquaryLevelData>> by lazy {
    resource<List<ReliquaryLevelData>>("data/ReliquaryLevel.json")
        .groupBy { it.rank } }
val relicTotalExpData: List<List<Int>> by lazy {
    relicLevelData.values.map { rankData ->
        var totalExp = 0
        rankData.map { data -> totalExp += data.exp; totalExp }
    }
}

@Serializable
data class WeaponPropertyData(
    val propType: FightProperty = FightProperty.FIGHT_PROP_NONE,
    val initValue: Float = 0f,
    val type: String
)
@Serializable
data class WeaponData(
    val id: Int,
    @SerialName("rankLevel") val rank: Int,
    @SerialName("weaponPromoteId") val promoteId: Int,
    @SerialName("skillAffix") val skillAffixes: List<Int>,
    @SerialName("weaponBaseExp") val baseExp: Int,
    // private val storyId = 0
    val awakenCosts: List<Int>,
    @SerialName("weaponProp") val props: List<WeaponPropertyData>
)

val weaponData: Map<Int, WeaponData> by lazy {
    resource<List<WeaponData>>("data/Weapon.json")
        .associateBy(WeaponData::id)
}

@Serializable
data class CostItem(val id: Int = 0, val count: Int = 1)
@Serializable
data class WeaponPromoteData(
    @SerialName("weaponPromoteId") val id: Int,
    val costItems: List<CostItem>,
    @SerialName("coinCost") val costCoin: Int = 0,
    val unlockMaxLevel: Int,
    @SerialName("addProps") val props: List<AddProps>,
)

val weaponPromoteData by lazy {
    resource<List<WeaponPromoteData>>("data/WeaponPromote.json")
        .groupBy(WeaponPromoteData::id)
}

@Serializable
data class WeaponCurveInfoData(val type: String, val value: Float)
@Serializable
data class WeaponCurveData(val curveInfos: List<WeaponCurveInfoData>)
val weaponCurveData by lazy {
    resource<List<WeaponCurveData>>("data/WeaponCurve.json") }

@Serializable
data class RelicData(
    val id: Int,
    @SerialName("equipType") val type: EquipType,
    @SerialName("rankLevel") val rank: Int,
    val setId: Int = 0,
    val maxLevel: Int,
    val mainPropDepotId: Int,
    @SerialName("appendPropDepotId") val depotId: Int,
    val baseConvExp: Int
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
    resource<List<RelicData>>("data/Reliquary.json")
        .associateBy(RelicData::id)
}

@Serializable
data class RelicSetData(val id: Int, val equipAffixId: Int, @SerialName("setNeedNum") val setNeedNums: List<Int>)
val reliquarySetData by lazy {
    resource<List<RelicSetData>>("data/ReliquarySet.json")
        .associateBy { it.id }
}

@Serializable
data class EquipAffixData(
    val id: Int,
    val equipAffixId: Int,
    @SerialName("addProps") val props: List<AddProps>,
    val openConfig: String
)
val equipAffixData by lazy {
    resource<List<EquipAffixData>>("data/EquipAffix.json")
        .groupBy { it.id }
}

@Serializable
data class ItemUse(
    @SerialName("useOp") val op: String = "",
    @SerialName("useParam") val params: List<String>,
)
@Serializable
data class MaterialData(
    val id: Int,
    @SerialName("materialType") val type: MaterialType,
    val useTarget: String,
    val satiationParams: List<Int>,
    val itemUse: List<ItemUse>,
    val destroyReturnMaterial: List<Int>,
    val destroyReturnMaterialCount: List<Int>,
)
val materialData by lazy {
    resource<List<MaterialData>>("data/Material.json")
        .associateBy { it.id }
}
fun destroyMaterialsFor(itemId: Int): List<CostItem> {
    val data = materialData[itemId]!!
    return data.destroyReturnMaterial.zip(data.destroyReturnMaterialCount)
        .map { CostItem(it.first, it.second) }
}

@Serializable
data class CombineData(
    @SerialName("combineId") val id: Int,
    val playerLevel: Int,
    @SerialName("materialItems") val materials: List<CostItem>,
    @SerialName("scoinCost") val moraCost: Int,
    val resultItemId: Int,
    val resultItemCount: Int,
)
val combineData by lazy {
    resource<List<CombineData>>("data/Combine.json")
        .associateBy { it.id }
}