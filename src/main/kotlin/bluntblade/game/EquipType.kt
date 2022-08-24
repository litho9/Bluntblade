package bluntblade.game

//import java.util.HashMap
//import java.util.stream.Stream

enum class EquipType(val value: Int) {
    EQUIP_NONE(0),
    EQUIP_BRACER(1),
    EQUIP_NECKLACE(2),
    EQUIP_SHOES(3),
    EQUIP_RING(4),
    EQUIP_DRESS(5),
    EQUIP_WEAPON(6);

//    companion object {
//        private val map: Int2ObjectMap<EquipType> = Int2ObjectOpenHashMap()
//        private val stringMap: MutableMap<String, EquipType> = HashMap()
//
//        init {
//            Stream.of(*values()).forEach { e: EquipType ->
//                map.put(e.value, e)
//                stringMap[e.name] = e
//            }
//        }
//
//        fun getTypeByValue(value: Int): EquipType = map.getOrDefault(value, EQUIP_NONE)
//
//        fun getTypeByName(name: String) = stringMap.getOrDefault(name, EQUIP_NONE)
//    }
}