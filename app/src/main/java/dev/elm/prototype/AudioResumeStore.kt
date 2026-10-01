package dev.elm.prototype

import android.content.SharedPreferences
import java.security.MessageDigest

enum class AudioSource { Tone, Document, Representative }

/** Selection is independent of an injected representative file. Positions belong to a
 * recording identity and playback mode, so a full source never inherits a clipped offset. */
class AudioResumeStore(private val prefs: SharedPreferences) {
    private fun legacyId(recordingId: String): String? =
        recordingId.removePrefix("controls-course:").takeIf {
            recordingId.startsWith("controls-course:")
        }
    fun selected(representativeExists: Boolean, recordingId: String = "tone"): AudioSource {
        val saved = prefs.getString("active-audio-source-$recordingId", null)
            ?: legacyId(recordingId)?.let { prefs.getString("active-audio-source-$it", null) }
            ?: if (recordingId == "tone" || recordingId == "controls-course:tone")
                prefs.getString("active-audio-source", null) else null
        if (saved != null) return AudioSource.entries.firstOrNull { it.name == saved } ?: AudioSource.Tone
        // Migrate the prototype preference only for its original tone recording.
        if (recordingId != "tone" && recordingId != "controls-course:tone") return AudioSource.Tone
        return if (prefs.getBoolean("source-audio", false)) {
            if (documentUri(recordingId) != null) AudioSource.Document
            else if (representativeExists) AudioSource.Representative
            else AudioSource.Tone
        } else AudioSource.Tone
    }
    fun documentUri(recordingId: String = "tone"): String? =
        prefs.getString("audio-uri-$recordingId", null)
            ?: legacyId(recordingId)?.let { prefs.getString("audio-uri-$it", null) }
            ?: if (recordingId == "tone" || recordingId == "controls-course:tone")
                prefs.getString("audio-uri", null) else null
    fun select(source: AudioSource, recordingId: String = "tone") {
        prefs.edit().putString("active-audio-source-$recordingId", source.name).commit()
    }
    fun chooseDocument(uri: String, recordingId: String = "tone") {
        prefs.edit().putString("audio-uri-$recordingId", uri)
            .putString("active-audio-source-$recordingId", AudioSource.Document.name).commit()
    }
    fun key(source: AudioSource, identity: String, full: Boolean, scopeId: String = ""): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("${source.name}|$identity|${if (full) "full" else "range"}|$scopeId".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
    fun position(key: String, legacyMediaKey: String, previousKey: String? = null): Long {
        val stored = prefs.getLong("resume-$key", Long.MIN_VALUE)
        if (stored != Long.MIN_VALUE) return stored.coerceAtLeast(0)
        if (previousKey != null) {
            val previous = prefs.getLong("resume-$previousKey", Long.MIN_VALUE)
            if (previous != Long.MIN_VALUE) return previous.coerceAtLeast(0)
        }
        // Preserve the old single-source offset only for the exact prior media key.
        return if (prefs.getString("media-key", null) == legacyMediaKey)
            prefs.getLong("audio-position", 0).coerceAtLeast(0) else 0
    }
    fun save(key: String?, position: Long) {
        if (key != null) prefs.edit().putLong("resume-$key", position.coerceAtLeast(0)).commit()
    }
}
