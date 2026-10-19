package com.bytbeat.mpctrainer

/** Shared, Android-independent playback rules used by live and sequenced audio. */
object TrainerPlayback {
    fun gain(velocity: Int, padVolume: Float, masterVolume: Float): Float =
        (velocity.coerceIn(1, 127) / 127f * padVolume.coerceIn(0f, 1f) * masterVolume.coerceIn(0f, 1f)).coerceIn(0f, 1f)

    fun crossed(previous: Long, current: Long, target: Long): Boolean = when {
        previous < 0 -> target in 0..current
        current >= previous -> target > previous && target <= current
        else -> target > previous || target <= current
    }

    fun retimedElapsed(elapsedMs: Long, oldBpm: Int, newBpm: Int): Long =
        (elapsedMs.coerceAtLeast(0).toDouble() * oldBpm.coerceAtLeast(1) / newBpm.coerceAtLeast(1)).toLong()
}
