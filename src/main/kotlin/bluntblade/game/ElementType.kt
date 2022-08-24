package bluntblade.game

enum class ElementType(
    val value: Int,
    val curEnergyProp: Stat,
    val maxEnergyProp: Stat,
    val teamResonanceId: Int = 0,
    val configName: String? = null,
    depotValue: Int = 0
) {
    None(0, Stat.CUR_FIRE_ENERGY, Stat.MAX_FIRE_ENERGY),
    Fire(1, Stat.CUR_FIRE_ENERGY, Stat.MAX_FIRE_ENERGY, 10101, "TeamResonance_Fire_Lv2", 2),
    Water(2, Stat.CUR_WATER_ENERGY, Stat.MAX_WATER_ENERGY, 10201, "TeamResonance_Water_Lv2", 3),
    Grass(3, Stat.CUR_GRASS_ENERGY, Stat.MAX_GRASS_ENERGY),
    Electric(4, Stat.CUR_ELEC_ENERGY, Stat.MAX_ELEC_ENERGY, 10401, "TeamResonance_Electric_Lv2", 7),
    Ice(5, Stat.CUR_ICE_ENERGY, Stat.MAX_ICE_ENERGY, 10601, "TeamResonance_Ice_Lv2", 5),
    Frozen(6, Stat.CUR_ICE_ENERGY, Stat.MAX_ICE_ENERGY),
    Wind(7, Stat.CUR_WIND_ENERGY, Stat.MAX_WIND_ENERGY, 10301, "TeamResonance_Wind_Lv2", 4),
    Rock(8, Stat.CUR_ROCK_ENERGY, Stat.MAX_ROCK_ENERGY, 10701, "TeamResonance_Rock_Lv2", 6),
    AntiFire(9, Stat.CUR_FIRE_ENERGY, Stat.MAX_FIRE_ENERGY),
    Default(255, Stat.CUR_FIRE_ENERGY, Stat.MAX_FIRE_ENERGY, 10801, "TeamResonance_AllDifferent");
}