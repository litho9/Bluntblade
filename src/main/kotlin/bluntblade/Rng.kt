package bluntblade

import java.util.*
import kotlin.random.Random

class WeightedMap<E>(items: Collection<E>, weightFn: (i: E) -> Double) {
    private val map: NavigableMap<Double, E> = run {
        val map = TreeMap<Double, E>()
        var total = 0.0
        items.forEach {
            total += weightFn(it)
            map[total] = it
        }
        map
    }
    private val total: Double = map.keys.sum()

    operator fun next(): E = map.higherEntry(Random.nextDouble(total)).value
}

object Rng {
    var mode: Mode = Mode.DEFAULT

    fun relicRate(): Int {
        if (mode == Mode.UNLUCKIEST) return 1
        val boost = Random.nextInt(1, 100)
        return if (boost == 100) 5 else if (boost > 89) 2 else 1
    }

    fun <E> weighted(items: Collection<E>, weightFn: (i: E) -> Double) =
        if (mode == Mode.UNLUCKIEST) items.first()
        else WeightedMap(items, weightFn).next()

    enum class Mode { DEFAULT, UNLUCKIEST }
}