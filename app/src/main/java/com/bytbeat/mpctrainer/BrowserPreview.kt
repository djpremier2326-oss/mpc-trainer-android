package com.bytbeat.mpctrainer

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/** Streaming preview for Browser selections. Unlike SoundPool this is suitable for long MP3/WAV music samples. */
class BrowserPreview(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(uri: Uri, onError: (String) -> Unit = {}) {
        stop()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                setOnPreparedListener { it.start() }
                setOnCompletionListener { stop() }
                setOnErrorListener { _, what, extra ->
                    onError("Preview error $what/$extra")
                    stop()
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            stop()
            onError(e.message ?: "Preview unavailable")
        }
    }

    fun stop() {
        val old = player
        player = null
        if (old != null) {
            try { old.stop() } catch (_: Exception) {}
            try { old.reset() } catch (_: Exception) {}
            old.release()
        }
    }

    fun release() = stop()
}
