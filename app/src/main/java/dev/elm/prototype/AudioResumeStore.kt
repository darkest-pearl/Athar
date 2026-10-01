package dev.elm.prototype

import android.content.SharedPreferences
import java.security.MessageDigest

enum class AudioSource { Tone, Document, Representative }

/** Selection is independent of an injected representative file. Positions belong to a
 * recording identity and playback mode, so a full source never inherits a clipped offset. */
class AudioResumeStore(private val prefs: SharedPreferences) {
    fun selected(representativeExists: Boolean): AudioSource {
        val saved = prefs.getString("active-audio-source", null)
        if (saved != null) return AudioSource.entries.firstOrNull { it.name == saved } ?: AudioSource.Tone
        // Migrate the prototype preference once. A selected document wins over test injection.
        return if (prefs.getBoolean("source-audio", false)) {
            if (documentUri() != null) AudioSource.Document
            else if (representativeExists) AudioSource.Representative
            else AudioSource.Tone
        } else AudioSource.Tone
    }
    fun documentUri(): String? = prefs.getString("audio-uri", null)
    fun select(source: AudioSource) {
        prefs.edit().putString("active-audio-source", source.name).commit()
    }
    fun chooseDocument(uri: String) {
        prefs.edit().putString("audio-uri", uri).putString("active-audio-source", AudioSource.Document.name).commit()
    }
    fun key(source: AudioSource, identity: String, full: Boolean): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("${source.name}|$identity|${if (full) "full" else "range"}".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
    fun position(key: String, legacyMediaKey: String): Long {
        val stored = prefs.getLong("resume-$key", Long.MIN_VALUE)
        if (stored != Long.MIN_VALUE) return stored.coerceAtLeast(0)
        // Preserve the old single-source offset only for the exact prior media key.
        return if (prefs.getString("media-key", null) == legacyMediaKey)
            prefs.getLong("audio-position", 0).coerceAtLeast(0) else 0
    }
    fun save(key: String?, position: Long) {
        if (key != null) prefs.edit().putLong("resume-$key", position.coerceAtLeast(0)).commit()
    }
}
