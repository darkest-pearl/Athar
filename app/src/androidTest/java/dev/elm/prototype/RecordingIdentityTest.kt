package dev.elm.prototype

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream

@RunWith(AndroidJUnit4::class)
class RecordingIdentityTest {
    @Test fun checksumAndByteCountMustBothMatch() {
        val expected = PackRecording("test", "test.wav", 1000,
            sha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            bytes = 3)
        assertTrue(verifyRecordingBytes(ByteArrayInputStream("abc".toByteArray()), expected))
        assertFalse(verifyRecordingBytes(ByteArrayInputStream("abd".toByteArray()), expected))
        assertFalse(verifyRecordingBytes(ByteArrayInputStream("abcd".toByteArray()), expected))
        assertFalse(verifyRecordingBytes(ByteArrayInputStream("abc".toByteArray()),
            expected.copy(bytes = 4)))
    }
}
