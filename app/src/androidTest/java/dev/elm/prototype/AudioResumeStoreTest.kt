package dev.elm.prototype

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioResumeStoreTest {
    @Test fun overlappingRecordingIdsDoNotShareSelectionOrPosition() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("audio-course-scope-test", 0)
        prefs.edit().clear().commit()
        val store = AudioResumeStore(prefs)
        store.chooseDocument("content://local/first", "controls-course:tone")
        val firstKey = store.key(AudioSource.Document, "content://local/first", false,
            "controls-course:tone-all")
        val secondKey = store.key(AudioSource.Document, "content://local/first", false,
            "second-course:tone-all")
        store.save(firstKey, 4200)
        assertNull(store.documentUri("second-course:tone"))
        assertEquals(AudioSource.Tone, store.selected(false, "second-course:tone"))
        assertEquals(0, store.position(secondKey, ""))
        assertEquals(4200, store.position(firstKey, ""))
        prefs.edit().clear().commit()
    }

    @Test fun explicitSelectionAndPositionsSurviveStoreRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("audio-resume-test", 0)
        prefs.edit().clear().commit()
        val first = AudioResumeStore(prefs)
        val injected = first.key(AudioSource.Representative, "file:///representative.mp3", false)
        val documentA = first.key(AudioSource.Document, "content://local/one", false)
        val documentB = first.key(AudioSource.Document, "content://local/two", false)
        val fullA = first.key(AudioSource.Document, "content://local/one", true)
        assertEquals(4, setOf(injected, documentA, documentB, fullA).size)
        first.save(injected, 2400)
        first.save(documentA, 8500)
        first.save(fullA, 17000)
        first.chooseDocument("content://local/one")
        val reopened = AudioResumeStore(prefs)
        assertEquals(AudioSource.Document, reopened.selected(true))
        assertEquals("content://local/one", reopened.documentUri())
        assertEquals(8500, reopened.position(documentA, "unused"))
        assertEquals(8500, reopened.position(
            reopened.key(AudioSource.Document, "content://local/one", false, "tone-all"),
            "unused", documentA))
        assertEquals(17000, reopened.position(fullA, "unused"))
        assertEquals(0, reopened.position(documentB, "unused"))
        assertEquals(2400, reopened.position(injected, "unused"))
        prefs.edit().clear().commit()
    }
}
