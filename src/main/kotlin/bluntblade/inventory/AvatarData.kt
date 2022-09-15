package bluntblade.inventory

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.util.regex.Pattern

@Serializable
data class PropGrowCurvesData(val Type: String, val GrowCurve: String)
@Serializable
data class AvatarData(
    val id: Int,
    @SerialName("AvatarPromoteId") val promoteId: Int,
    val skillDepotId: Int,
    val iconName: String,
    val hpBase: Float,
    val attackBase: Float,
    val defenseBase: Float,
    val critical: Float,
    val criticalHurt: Float,
    val initialWeapon: Int,
    @SerialName("propGrowCurves") val curves: List<PropGrowCurvesData>
)
val avatarData by lazy {
    resource<List<AvatarData>>("data/Avatar.json")
        .associateBy(AvatarData::id)
}

@Serializable
data class AvatarPromoteData(
    @SerialName("avatarPromoteId") val id: Int,
    @SerialName("costItems") val costs: List<CostItem>,
    @SerialName("scoinCost") val costCoin: Int,
    val unlockMaxLevel: Int,
    val addProps: List<AddProps>
)
val avatarPromoteData by lazy {
    resource<List<AvatarPromoteData>>("data/AvatarPromote.json")
        .groupBy(AvatarPromoteData::id)
}

@Serializable
data class InherentProudSkillOpens(
    val proudSkillGroupId: Int? = null,
    val needAvatarPromoteLevel: Int = 0
)
data class AvatarSkillDepot(
    val id: Int,
    @SerialName("energySkill") val burstId: Int,
    @SerialName("skills") val skillIds: List<Int>,
    val inherentProudSkillOpens: List<InherentProudSkillOpens>,
    @SerialName("skillDepotAbilityGroup") val abilityGroup: String, // only for MC
)
val avatarSkillDepotData by lazy {
    resource<List<AvatarSkillDepot>>("data/AvatarSkillDepot.json")
//        .onEach { it.InherentProudSkillOpens.removeIf { r -> r.ProudSkillGroupId == null } }
        .associateBy(AvatarSkillDepot::id)
}
fun proudSkillIdFor(skillDepotId: Int, level: Int): Int? =
    avatarSkillDepotData[skillDepotId]!!.inherentProudSkillOpens.firstOrNull {
        it.needAvatarPromoteLevel == level && it.proudSkillGroupId != null
    }?.proudSkillGroupId

@Serializable
data class AvatarLevelData(val Level: Int, val Exp: Int)
val avatarLevelData by lazy {
    resource<List<AvatarLevelData>>("data/AvatarLevel.json")
        .map(AvatarLevelData::Exp)
}
val avatarTotalExpData: List<Int> by lazy {
    var totalExp = 0
    avatarLevelData.map { exp -> totalExp += exp; totalExp }
}

@Serializable
data class AvatarSkillData(
    val id: Int,
    @SerialName("costElemType") val element: String, // Ex.: "Ice"
    @SerialName("costElemVal") val elementValue: Float,
    val proudSkillGroupId: Int
)
val avatarSkillData by lazy {
    resource<List<AvatarSkillData>>("data/AvatarSkill.json")
        .associateBy(AvatarSkillData::id)
}
@Serializable
data class ProudSkillData(
    @SerialName("proudSkillId") val id: Int,
    val proudSkillGroupId: Int,
    @SerialName("costItems") val costs: List<CostItem>,
    @SerialName("coinCost") val costCoin: Int,
    @SerialName("addProps") val props: List<AddProps>,
)
val proudSkillData by lazy {
    resource<List<ProudSkillData>>("data/ProudSkill.json")
        .groupBy(ProudSkillData::proudSkillGroupId)
}

@Serializable
data class AvatarTalentData(
    @SerialName("talentId") val id: Int,
    val mainCostItemId: Int,
    val openConfig: String,
)
val avatarTalentData by lazy {
    resource<List<AvatarTalentData>>("data/AvatarTalent.json")
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
        .getResource("BinOutput/Talent/AvatarTalents")!!.toURI()).walk()
        .map { resource<Map<String, List<AvatarTalentDataExtra>>>(it.path) }
        .reduce { acc, b -> acc + b }
}

val loader: ClassLoader = AvatarTalentDataExtra::class.java.classLoader
fun walk(pathName: String) = File(loader.getResource(pathName)!!.toURI()).walk()

@Serializable
data class AbilityEmbryoData(
    val abilityID: String,
    val abilityName: String,
    val abilityOverride: String,
)
@Serializable
data class AvatarEmbryoData(val abilities: List<AbilityEmbryoData>)
@Serializable
data class McEmbryoData(val targetAbilities: List<AbilityEmbryoData>)

val avatarEmbryoPattern: Pattern = Pattern.compile("ConfigAvatar_(?<name>[^\\W_]+)\\.json")
val avatarEmbryoMap: Map<String, List<String>> by lazy {
    walk("./BinOutput/Avatar/").mapNotNull {
        val matcher = avatarEmbryoPattern.matcher(it.path)
        if (!matcher.find()) return@mapNotNull null
        val name = matcher.group("name")
        val res = resource<AvatarEmbryoData>(it.path)
        name to res.abilities.map(AbilityEmbryoData::abilityName)
    }.toMap()
}
val mcEmbryoMap by lazy {
    resource<Map<String, McEmbryoData>>("BinOutput/AbilityGroup/AbilityGroup_Other_PlayerElementAbility.json")
}

val fetterExpData = listOf(1000, 1550, 2050, 2600, 3175, 3750, 4350, 4975, 5650, 6325)
val totalFetterExpData = with(0) {
    var totalExp = 0
    fetterExpData.map { exp -> totalExp += exp; totalExp }
}
val maxFetterExp = totalFetterExpData[totalFetterExpData.size - 1]

@Serializable
data class FetterData(val fetterId: Int, val avatarId: Int)
val fetterIdMap by lazy {
    resource<List<FetterData>>("data/Fetters.json")
        .groupBy(FetterData::avatarId)
        .mapValues { it.value.map(FetterData::fetterId) }
}

@Serializable
data class CurveInfosData(val type: String, val value: Float)
@Serializable
data class AvatarCurveData(val level: Int, val curveInfos: List<CurveInfosData>)
val avatarCurveData: Map<String, List<Float>> by lazy {
    val data = resource<List<AvatarCurveData>>("data/AvatarCurve.json")
    (0 until 4).associate { i -> data[0].curveInfos[i].type to data.map { it.curveInfos[i].value } }
}