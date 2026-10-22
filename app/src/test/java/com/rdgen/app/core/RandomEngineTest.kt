package com.rdgen.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class RandomEngineTest {

    private fun values(min: Long, max: Long, count: Int, distinct: Boolean = false, ascending: Boolean = false): List<Long> {
        val outcome = RandomEngine.generate(min, max, count, distinct, ascending, Random(42))
        assertTrue("expected success, got $outcome", outcome is RandomEngine.Outcome.Success)
        return (outcome as RandomEngine.Outcome.Success).values
    }

    @Test
    fun `generates requested amount inside range`() {
        val result = values(1, 6, 5)
        assertEquals(5, result.size)
        assertTrue(result.all { it in 1L..6L })
    }

    @Test
    fun `distinct never repeats`() {
        val result = values(1, 10, 10, distinct = true)
        assertEquals(10, result.size)
        assertEquals(10, result.toSet().size)
    }

    @Test
    fun `distinct covers whole range exactly`() {
        val result = values(3, 12, 10, distinct = true, ascending = true)
        assertEquals((3L..12L).toList(), result)
    }

    @Test
    fun `ascending sorts output`() {
        val result = values(1, 1000, 60, ascending = true)
        assertEquals(result.sorted(), result)
    }

    @Test
    fun `single value range always returns that value`() {
        val result = values(7, 7, 20)
        assertTrue(result.all { it == 7L })
    }

    @Test
    fun `rejects non positive min`() {
        val outcome = RandomEngine.generate(0, 10, 5, false, false, Random(1))
        assertTrue(outcome is RandomEngine.Outcome.Invalid)
    }

    @Test
    fun `rejects reversed range`() {
        val outcome = RandomEngine.generate(10, 1, 5, false, false, Random(1))
        assertTrue(outcome is RandomEngine.Outcome.Invalid)
    }

    @Test
    fun `rejects count over maximum`() {
        val outcome = RandomEngine.generate(1, 1000, RandomEngine.MAX_COUNT + 1, false, false, Random(1))
        assertTrue(outcome is RandomEngine.Outcome.Invalid)
    }

    @Test
    fun `rejects impossible distinct request`() {
        val outcome = RandomEngine.generate(1, 5, 6, true, false, Random(1))
        assertTrue(outcome is RandomEngine.Outcome.Invalid)
    }

    @Test
    fun `large distinct request stays unique and in range`() {
        val result = values(1, 200, 100, distinct = true)
        assertEquals(100, result.toSet().size)
        assertTrue(result.all { it in 1L..200L })
    }

    @Test
    fun `distribution is roughly uniform`() {
        val buckets = IntArray(10)
        val random = Random(7)
        repeat(20_000) {
            buckets[(RandomEngine.nextInRange(random, 1, 10) - 1).toInt()]++
        }
        buckets.forEach { count ->
            assertTrue("bucket $count out of expected band", count in 1_600..2_400)
        }
    }

    @Test
    fun `huge range does not overflow`() {
        val result = values(1, RandomEngine.MAX_VALUE, 20, distinct = true)
        assertEquals(20, result.toSet().size)
        assertTrue(result.all { it in 1L..RandomEngine.MAX_VALUE })
    }
}
