package bluntblade.game

enum class MaterialType(val value: Int) {
    MATERIAL_NONE(0),
    MATERIAL_FOOD(1),
    MATERIAL_QUEST(2),
    MATERIAL_EXCHANGE(4),
    MATERIAL_CONSUME(5),
    MATERIAL_EXP_FRUIT(6),
    MATERIAL_AVATAR(7),
    MATERIAL_ADSORBATE(8),
    MATERIAL_CRICKET(9),
    MATERIAL_ELEM_CRYSTAL(10),
    MATERIAL_WEAPON_EXP_STONE(11),
    MATERIAL_CHEST(12),
    MATERIAL_RELIQUARY_MATERIAL(13),
    MATERIAL_AVATAR_MATERIAL(14),
    MATERIAL_NOTICE_ADD_HP(15),
    MATERIAL_SEA_LAMP(16),
    MATERIAL_SELECTABLE_CHEST(17),
    MATERIAL_FLYCLOAK(18),
    MATERIAL_NAMECARD(19),
    MATERIAL_TALENT(20),
    MATERIAL_WIDGET(21),
    MATERIAL_CHEST_BATCH_USE(22),
    MATERIAL_FAKE_ABSORBATE(23),
    MATERIAL_CONSUME_BATCH_USE(24),
    MATERIAL_WOOD(25),
    MATERIAL_FURNITURE_FORMULA(27),
    MATERIAL_CHANNELLER_SLAB_BUFF(28),
    MATERIAL_FURNITURE_SUITE_FORMULA(29),
    MATERIAL_COSTUME(30);

//    companion object {
//        private val map: Int2ObjectMap<MaterialType> = Int2ObjectOpenHashMap()
//        private val stringMap: MutableMap<String, MaterialType> = HashMap()
//
//        init {
//            Stream.of(*emu.grasscutter.game.inventory.MaterialType.values()).forEach(Consumer { e: MaterialType ->
//                map.put(e.value, e)
//                stringMap[e.name] = e
//            })
//        }
//
//        fun getTypeByValue(value: Int): MaterialType {
//            return map.getOrDefault(value, MATERIAL_NONE)
//        }
//
//        fun getTypeByName(name: String): MaterialType {
//            return stringMap.getOrDefault(name, MATERIAL_NONE)
//        }
//    }
}