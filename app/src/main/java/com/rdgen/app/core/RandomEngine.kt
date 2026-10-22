package com.rdgen.app.core

import java.security.SecureRandom
import java.util.Random

/**
 * 纯逻辑的随机数引擎 —— 不依赖任何 Android API，可以直接被 JVM 单元测试覆盖。
 *
 * 产品口径：
 *  - 只生成**正整数**（≥ 1）；
 *  - 一次最多生成 [MAX_COUNT] 个；
 *  - 可选「不重复」与「升序」。
 */
object RandomEngine {

    /** 一次生成的个数上限。 */
    const val MAX_COUNT: Int = 100

    /** 单个数值的上限（9 位，够用且不会让界面爆掉）。 */
    const val MAX_VALUE: Long = 999_999_999L

    sealed interface Outcome {
        data class Success(val values: List<Long>) : Outcome
        data class Invalid(val message: String) : Outcome
    }

    private val secureRandom: Random by lazy { SecureRandom() }

    /**
     * @param min 最小值，必须 ≥ 1
     * @param max 最大值，必须 ≥ [min]
     * @param count 个数，1..[MAX_COUNT]
     * @param distinct 是否不重复
     * @param ascending 是否升序排列
     */
    fun generate(
        min: Long,
        max: Long,
        count: Int,
        distinct: Boolean,
        ascending: Boolean,
        random: Random = secureRandom,
    ): Outcome {
        if (min < 1L || max < 1L) return Outcome.Invalid("只能生成正整数（≥ 1）")
        if (max < min) return Outcome.Invalid("最小值不能大于最大值")
        if (count !in 1..MAX_COUNT) return Outcome.Invalid("生成个数需要在 1..$MAX_COUNT 之间")

        val span = spanSize(min, max)
        if (distinct && span != null && span < count.toLong()) {
            return Outcome.Invalid("该范围内只有 $span 个整数，取不了 $count 个不重复的数")
        }

        val values = if (distinct) {
            distinctValues(random, min, max, count, span)
        } else {
            List(count) { nextInRange(random, min, max) }
        }
        return Outcome.Success(if (ascending) values.sorted() else values)
    }

    /** 区间内整数个数；null 表示溢出（视为「大到不可能不够取」）。 */
    private fun spanSize(min: Long, max: Long): Long? {
        val span = max - min + 1L
        return if (span <= 0L) null else span
    }

    private fun distinctValues(
        random: Random,
        min: Long,
        max: Long,
        count: Int,
        span: Long?,
    ): List<Long> {
        // 范围不大时直接构造候选池再部分洗牌：比「随机 + 重试」稳定，
        // 也避免 count 逼近区间长度时的长尾等待。
        if (span != null && span <= count.toLong() * 8L) {
            val size = span.toInt()
            val pool = LongArray(size) { min + it }
            for (i in 0 until count) {
                val j = i + random.nextInt(size - i)
                val tmp = pool[i]
                pool[i] = pool[j]
                pool[j] = tmp
            }
            return pool.take(count)
        }

        val picked = LinkedHashSet<Long>(count * 2)
        while (picked.size < count) {
            picked.add(nextInRange(random, min, max))
        }
        return picked.toList()
    }

    /**
     * 取 [min, max] 内的均匀随机整数。
     *
     * 用「拒绝采样」消除取模偏差：先把 63 位随机数截到非负，再拒绝掉
     * 不能整除区间长度的那一小段尾部。跨度过大（超过 Long 上限）时退化为
     * 整段采样后重试。
     */
    internal fun nextInRange(random: Random, min: Long, max: Long): Long {
        if (min == max) return min
        val bound = max - min + 1L
        if (bound <= 0L) {
            while (true) {
                val candidate = random.nextLong()
                if (candidate in min..max) return candidate
            }
        }
        val limit = Long.MAX_VALUE - (Long.MAX_VALUE % bound)
        var candidate: Long
        do {
            candidate = random.nextLong() ushr 1
        } while (candidate >= limit)
        return min + candidate % bound
    }
}
