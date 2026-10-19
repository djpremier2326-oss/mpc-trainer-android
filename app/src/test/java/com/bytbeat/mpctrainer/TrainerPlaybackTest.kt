package com.bytbeat.mpctrainer

import org.junit.Assert.*
import org.junit.Test

class TrainerPlaybackTest {
    @Test fun velocityLevelsAreIdenticalLiveAndRecorded() {
        val levels = (1..16).map { kotlin.math.round(it * 127f / 16f).toInt() }
        val gains = levels.map { TrainerPlayback.gain(it, .8f, .5f) }
        assertEquals(.4f, gains.last(), .00001f)
        assertTrue(gains.zipWithNext().all { (a, b) -> b > a })
        assertEquals(64 / 127f * .4f, TrainerPlayback.gain(64, .8f, .5f), .00001f)
    }
    @Test fun startAndWrapPlayZeroExactlyOnce() {
        assertTrue(TrainerPlayback.crossed(-1, 0, 0))
        assertTrue(TrainerPlayback.crossed(-1, 3, 0))
        assertFalse(TrainerPlayback.crossed(0, 3, 0))
        assertTrue(TrainerPlayback.crossed(766, 2, 0))
        assertTrue(TrainerPlayback.crossed(766, 2, 767))
        assertFalse(TrainerPlayback.crossed(766, 2, 400))
    }
    @Test fun tempoChangesPreserveMusicalPositionAndLoopCount() {
        for (elapsed in listOf(0L, 1000L, 17500L)) {
            val next = TrainerPlayback.retimedElapsed(elapsed, 90, 120)
            assertEquals(elapsed * 90.0, next * 120.0, 120.0)
        }
    }
}
