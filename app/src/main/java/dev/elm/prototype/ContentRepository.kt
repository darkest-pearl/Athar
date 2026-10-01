package dev.elm.prototype

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

data class PackQuestion(
    val id: String, val version: Int, val conceptId: String, val prompt: String,
    val choices: List<String>, val correctIndex: Int, val explanation: String,
    val sourceRef: String
)
data class PackLesson(
    val id: String, val version: Int, val title: String, val language: String,
    val notes: String, val segmentRef: String, val permissionRef: String,
    val questions: List<PackQuestion>, val prerequisiteConceptIds: List<String>
)
data class PackRecording(val id: String, val relativePath: String, val durationMs: Long)
data class PackSegment(val id: String, val recordingId: String, val startMs: Long, val endMs: Long)
data class ContentPack(
    val courseId: String, val courseTitle: String, val contentVersion: Int,
    val developmentOnly: Boolean, val language: String, val lessons: List<PackLesson>,
    val recordings: Map<String, PackRecording>, val segments: Map<String, PackSegment>,
    val canonicalJson: String
)

object ContentPackParser {
    const val MAX_BYTES = 256 * 1024
    private val idPattern = Regex("[a-z][a-z0-9-]{0,63}")
    private val states = setOf("draft", "in-review", "approved", "published")

    fun parse(bytes: ByteArray): ContentPack {
        require(bytes.size <= MAX_BYTES) { "Pack exceeds 256 KiB" }
        val root = try { JSONObject(bytes.toString(Charsets.UTF_8)) }
        catch (e: JSONException) { throw IllegalArgumentException("Malformed JSON: ${e.message}") }
        require(number(root, "schemaVersion", "pack") == 1) { "Unsupported schema version (expected 1)" }
        val contentVersion = number(root, "contentVersion", "pack", 1)
        val developmentOnly = root.opt("developmentOnly")
        require(developmentOnly is Boolean) { "pack.developmentOnly: boolean required" }
        val language = string(root, "language", "pack")
        val course = objectField(root, "course", "pack")
        val courseId = id(course, "pack.course")
        val courseTitle = string(course, "title", "pack.course")
        val permissions = linkedMapOf<String, String>()
        entries(root, "permissions", "pack", 100).forEachIndexed { index, item ->
            val where = "permissions[$index]"
            val key = id(item, where)
            unique(permissions, key, where)
            val status = string(item, "status", where)
            require(status in setOf("authorized", "pending", "revoked")) { "$where.status: invalid status" }
            permissions[key] = status
        }
        val recordings = linkedMapOf<String, PackRecording>()
        entries(root, "recordings", "pack", 100).forEachIndexed { index, item ->
            val where = "recordings[$index]"
            val key = id(item, where)
            unique(recordings, key, where)
            val path = string(item, "relativePath", where)
            require(!path.startsWith("/") && !path.contains("..") && !path.contains('\\')) {
                "$where.relativePath: unsafe path"
            }
            val duration = longNumber(item, "durationMs", where, 1)
            authorized(permissions, string(item, "permissionRef", where), where)
            recordings[key] = PackRecording(key, path, duration)
        }
        val segments = linkedMapOf<String, PackSegment>()
        entries(root, "segments", "pack", 500).forEachIndexed { index, item ->
            val where = "segments[$index]"
            val key = id(item, where)
            unique(segments, key, where)
            val recordingId = string(item, "recordingId", where)
            val recording = recordings[recordingId] ?: throw IllegalArgumentException("$where: unknown recording")
            val start = longNumber(item, "startMs", where)
            val end = longNumber(item, "endMs", where, 1)
            require(start < end && end <= recording.durationMs) { "$where: invalid source timestamps" }
            segments[key] = PackSegment(key, recordingId, start, end)
        }
        val concepts = linkedSetOf<String>()
        entries(root, "concepts", "pack", 500).forEachIndexed { index, item ->
            val key = id(item, "concepts[$index]")
            require(concepts.add(key)) { "concepts: duplicate ID $key" }
        }
        val lessonIds = linkedSetOf<String>()
        val lessons = entries(root, "lessons", "pack", 100).mapIndexed { lessonIndex, item ->
            val where = "lessons[$lessonIndex]"
            val key = id(item, where)
            require(lessonIds.add(key)) { "lessons: duplicate ID $key" }
            val version = number(item, "version", where, 1)
            val title = string(item, "title", where)
            val lessonLanguage = string(item, "language", where)
            require(string(item, "state", where) in states) { "$where.state: invalid state" }
            val segmentRef = string(item, "segmentRef", where)
            require(segmentRef in segments) { "$where: unknown source segment" }
            val permissionRef = string(item, "permissionRef", where)
            authorized(permissions, permissionRef, where)
            val notes = optionalString(item, "notes", where)
            val prerequisites = optionalIds(item, "prerequisiteConceptIds", where, 20)
            require(prerequisites.all { it in concepts }) { "$where: unknown prerequisite concept" }
            val questionIds = linkedSetOf<String>()
            val questions = entries(item, "questions", where, 20).mapIndexed { questionIndex, question ->
                val qwhere = "$where.questions[$questionIndex]"
                val qid = id(question, qwhere)
                require(questionIds.add(qid)) { "$where.questions: duplicate ID $qid" }
                val qversion = number(question, "version", qwhere, 1)
                val conceptId = string(question, "conceptId", qwhere)
                require(conceptId in concepts) { "$qwhere: unknown concept" }
                val prompt = string(question, "prompt", qwhere)
                val explanation = string(question, "explanation", qwhere)
                val choicesArray = array(question, "choices", qwhere, 6)
                require(choicesArray.length() >= 2) { "$qwhere.choices: 2-6 choices required" }
                val choices = (0 until choicesArray.length()).map { choiceIndex ->
                    val raw = choicesArray.opt(choiceIndex)
                    require(raw is String && raw.isNotBlank() && raw.length <= 4096) {
                        "$qwhere.choices[$choiceIndex]: nonblank string required"
                    }
                    raw.trim()
                }
                require(choices.size == choices.toSet().size) { "$qwhere.choices: duplicate choices" }
                val correctIndex = number(question, "correctIndex", qwhere)
                require(correctIndex < choices.size) { "$qwhere.correctIndex: out of range" }
                val sourceRef = objectField(question, "sourceRef", qwhere)
                if (sourceRef.optString("kind") == "fixture-text") {
                    require(sourceRef.optString("lessonId") == key) { "$qwhere: invalid fixture source lesson" }
                    number(sourceRef, "contentVersion", "$qwhere.sourceRef", 1)
                    string(sourceRef, "passage", "$qwhere.sourceRef")
                } else {
                    val sourceSegment = segments[sourceRef.optString("segmentId")]
                        ?: throw IllegalArgumentException("$qwhere: unknown source passage segment")
                    val sourceStart = longNumber(sourceRef, "startMs", "$qwhere.sourceRef")
                    val sourceEnd = longNumber(sourceRef, "endMs", "$qwhere.sourceRef", 1)
                    require(sourceSegment.startMs <= sourceStart && sourceStart < sourceEnd &&
                        sourceEnd <= sourceSegment.endMs) { "$qwhere: invalid source passage" }
                    number(sourceRef, "contentVersion", "$qwhere.sourceRef", 1)
                }
                PackQuestion(qid, qversion, conceptId, prompt, choices, correctIndex,
                    explanation, canonical(sourceRef))
            }
            PackLesson(key, version, title, lessonLanguage, notes, segmentRef,
                permissionRef, questions, prerequisites)
        }
        require(lessons.sumOf { it.questions.size } <= 1000) { "pack: too many questions" }
        return ContentPack(courseId, courseTitle, contentVersion, developmentOnly as Boolean,
            language, lessons, recordings, segments, canonical(root))
    }

    private fun unique(map: Map<String, *>, key: String, where: String) {
        require(key !in map) { "$where: duplicate ID $key" }
    }
    private fun authorized(permissions: Map<String, String>, ref: String, where: String) {
        require(permissions[ref] == "authorized") { "$where: collection authorization missing or revoked" }
    }
    private fun id(item: JSONObject, where: String): String {
        val value = string(item, "id", where)
        require(idPattern.matches(value)) { "$where.id: invalid stable ID" }
        return value
    }
    private fun string(item: JSONObject, field: String, where: String): String {
        val raw = item.opt(field)
        require(raw is String && raw.isNotBlank() && raw.length <= 4096) {
            "$where.$field: nonblank string required"
        }
        return raw.trim()
    }
    private fun optionalString(item: JSONObject, field: String, where: String): String {
        if (!item.has(field) || item.isNull(field)) return ""
        return string(item, field, where)
    }
    private fun number(item: JSONObject, field: String, where: String, minimum: Int = 0): Int {
        val raw = item.opt(field)
        require(raw is Number && raw.toDouble().isFinite() &&
            raw.toDouble() == raw.toLong().toDouble() &&
            raw.toLong() in minimum.toLong()..Int.MAX_VALUE.toLong()) {
            "$where.$field: integer >= $minimum required"
        }
        return raw.toInt()
    }
    private fun longNumber(item: JSONObject, field: String, where: String, minimum: Long = 0): Long {
        val raw = item.opt(field)
        require(raw is Number && raw.toDouble().isFinite() &&
            raw.toDouble() == raw.toLong().toDouble() && raw.toLong() >= minimum) {
            "$where.$field: integer >= $minimum required"
        }
        return raw.toLong()
    }
    private fun objectField(item: JSONObject, field: String, where: String): JSONObject {
        return item.opt(field) as? JSONObject
            ?: throw IllegalArgumentException("$where.$field: object required")
    }
    private fun array(item: JSONObject, field: String, where: String, maximum: Int): JSONArray {
        val result = item.opt(field) as? JSONArray
            ?: throw IllegalArgumentException("$where.$field: nonempty collection required")
        require(result.length() in 1..maximum) { "$where.$field: 1-$maximum items required" }
        return result
    }
    private fun entries(item: JSONObject, field: String, where: String, maximum: Int): List<JSONObject> {
        val values = array(item, field, where, maximum)
        return (0 until values.length()).map { index ->
            values.opt(index) as? JSONObject
                ?: throw IllegalArgumentException("$where.$field[$index]: object required")
        }
    }
    private fun optionalIds(item: JSONObject, field: String, where: String, maximum: Int): List<String> {
        if (!item.has(field)) return emptyList()
        val values = item.opt(field) as? JSONArray
            ?: throw IllegalArgumentException("$where.$field: array required")
        require(values.length() <= maximum) { "$where.$field: too many items" }
        val ids = (0 until values.length()).map { index ->
            val raw = values.opt(index)
            require(raw is String && idPattern.matches(raw)) { "$where.$field[$index]: invalid ID" }
            raw as String
        }
        require(ids.size == ids.toSet().size) { "$where.$field: duplicate ID" }
        return ids
    }
    internal fun canonical(value: Any?): String = when (value) {
        is JSONObject -> value.keys().asSequence().toList().sorted()
            .joinToString(",", "{", "}") { key -> JSONObject.quote(key) + ":" + canonical(value.opt(key)) }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { canonical(value.opt(it)) }
        is String -> JSONObject.quote(value)
        JSONObject.NULL, null -> "null"
        else -> value.toString()
    }
}

class ContentRepository(private val context: Context) {
    private val activeFile = File(context.filesDir, "active-pack.json")
    private val ledgerFile = File(context.filesDir, "content-identity-ledger.json")
    fun load(): ContentPack {
        val bundled = ContentPackParser.parse(readBounded(context.assets.open("fixture-pack.json")))
        if (!activeFile.exists()) return bundled
        return try { ContentPackParser.parse(readBounded(activeFile.inputStream())) }
        catch (_: Exception) { bundled }
    }
    fun import(uri: Uri, current: ContentPack): ContentPack {
        val bytes = context.contentResolver.openInputStream(uri)?.use(::readBounded)
            ?: throw IllegalArgumentException("Selected content pack cannot be opened")
        return importBytes(bytes, current)
    }
    fun importBytes(bytes: ByteArray, current: ContentPack): ContentPack {
        val candidate = ContentPackParser.parse(bytes)
        val ledger = if (ledgerFile.exists()) JSONObject(ledgerFile.readText()) else JSONObject()
        val known = identities(current)
        known.forEach { (key, fingerprint) ->
            if (!ledger.has(key)) ledger.put(key, fingerprint)
        }
        val proposed = identities(candidate)
        val packKey = "${candidate.courseId}/pack/v${candidate.contentVersion}"
        val latest = ledger.keys().asSequence().mapNotNull { key ->
            if (key.startsWith("${candidate.courseId}/pack/v"))
                key.substringAfterLast('v').toIntOrNull() else null
        }.maxOrNull() ?: 0
        require(candidate.contentVersion >= latest || ledger.optString(packKey) == proposed[packKey]) {
            "Older unrecognized content version for this course"
        }
        proposed.forEach { (key, fingerprint) ->
            require(!ledger.has(key) || ledger.getString(key) == fingerprint) {
                "Immutable content identity changed: $key"
            }
            ledger.put(key, fingerprint)
        }
        writeAtomic(ledgerFile, ledger.toString().toByteArray(Charsets.UTF_8))
        writeAtomic(activeFile, bytes)
        return candidate
    }
    private fun identities(pack: ContentPack): Map<String, String> {
        val root = JSONObject(pack.canonicalJson)
        val prefix = pack.courseId
        val result = linkedMapOf("$prefix/pack/v${pack.contentVersion}" to pack.canonicalJson)
        fun addEntries(field: String, key: (JSONObject) -> String) {
            val array = root.getJSONArray(field)
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                result["$prefix/${key(item)}"] = ContentPackParser.canonical(item)
            }
        }
        addEntries("recordings") { "recording/${it.getString("id")}" }
        addEntries("segments") { "segment/${it.getString("id")}" }
        val lessons = root.getJSONArray("lessons")
        for (index in 0 until lessons.length()) {
            val lesson = lessons.getJSONObject(index)
            val lessonId = lesson.getString("id")
            val version = lesson.getInt("version")
            val material = JSONObject(lesson.toString())
            material.remove("state")
            material.remove("reviews")
            val materialQuestions = material.getJSONArray("questions")
            for (qIndex in 0 until materialQuestions.length())
                materialQuestions.getJSONObject(qIndex).remove("reviews")
            result["$prefix/lesson/$lessonId/v$version"] = ContentPackParser.canonical(material)
            val questions = lesson.getJSONArray("questions")
            for (qIndex in 0 until questions.length()) {
                val question = questions.getJSONObject(qIndex)
                val questionMaterial = JSONObject(question.toString())
                questionMaterial.remove("reviews")
                result["$prefix/question/$lessonId/${question.getString("id")}/v${question.getInt("version")}"] =
                    ContentPackParser.canonical(questionMaterial)
            }
        }
        return result
    }
    private fun writeAtomic(file: File, bytes: ByteArray) {
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try { output.write(bytes); atomic.finishWrite(output) }
        catch (e: Exception) { atomic.failWrite(output); throw e }
    }
    private fun readBounded(input: InputStream): ByteArray = input.use {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val size = it.read(buffer)
            if (size < 0) break
            require(out.size() + size <= ContentPackParser.MAX_BYTES) { "Pack exceeds 256 KiB" }
            out.write(buffer, 0, size)
        }
        out.toByteArray()
    }
}
