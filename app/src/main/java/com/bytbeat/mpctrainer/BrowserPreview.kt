package com.bytbeat.mpctrainer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/** Streaming audition for long WAV/MP3 sources. Pad one-shots remain on SoundPool. */
class BrowserPreview(private val context: Context) {
    private var player: MediaPlayer? = null
    private var generation = 0

    @Synchronized
    fun play(uri: Uri, onReady: () -> Unit = {}, onError: (String) -> Unit = {}) {
        val token = ++generation
        releaseCurrent()
        try {
            val next = MediaPlayer()
            player = next
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            next.setDataSource(context, uri)
            next.setOnPreparedListener {
                if (token != generation || player !== it) return@setOnPreparedListener
                onReady()
                it.start()
            }
            next.setOnCompletionListener {
                if (player === it) stop()
            }
            next.setOnErrorListener { mp, what, extra ->
                if (player === mp) {
                    onError("Ошибка PREVIEW $what/$extra")
                    stop()
                }
                true
            }
            next.prepareAsync()
        } catch (e: Exception) {
            if (token == generation) {
                releaseCurrent()
                onError(e.message ?: "PREVIEW недоступен")
            }
        }
    }

    @Synchronized
    fun stop() {
        generation++
        releaseCurrent()
    }

    @Synchronized
    fun release() = stop()

    @Synchronized
    fun isActive(): Boolean = player != null

    private fun releaseCurrent() {
        val old = player
        player = null
        if (old != null) {
            try { old.setOnPreparedListener(null) } catch (_: Exception) {}
            try { old.setOnCompletionListener(null) } catch (_: Exception) {}
            try { old.setOnErrorListener(null) } catch (_: Exception) {}
            try { old.stop() } catch (_: Exception) {}
            try { old.reset() } catch (_: Exception) {}
            try { old.release() } catch (_: Exception) {}
        }
    }
}
