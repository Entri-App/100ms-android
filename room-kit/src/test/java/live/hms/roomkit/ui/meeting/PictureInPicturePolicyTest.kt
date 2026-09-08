package live.hms.roomkit.ui.meeting

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PictureInPicturePolicyTest {

    @Test
    fun `PiP is supported on Android 8 and above when the device exposes the feature`() {
        assertTrue(PictureInPicturePolicy.isSupported(sdkInt = 26, hasSystemFeature = true))
        assertTrue(PictureInPicturePolicy.isSupported(sdkInt = 34, hasSystemFeature = true))
    }

    @Test
    fun `PiP is unsupported below Android 8 or without the device feature`() {
        assertFalse(PictureInPicturePolicy.isSupported(sdkInt = 25, hasSystemFeature = true))
        assertFalse(PictureInPicturePolicy.isSupported(sdkInt = 34, hasSystemFeature = false))
    }

    @Test
    fun `an active meeting enters PiP when supported and not already in PiP`() {
        assertTrue(
            PictureInPicturePolicy.shouldEnter(
                isSupported = true,
                isInActiveMeeting = true,
                isAlreadyInPictureInPicture = false
            )
        )
    }

    @Test
    fun `PiP entry is rejected outside an active meeting`() {
        assertFalse(
            PictureInPicturePolicy.shouldEnter(
                isSupported = true,
                isInActiveMeeting = false,
                isAlreadyInPictureInPicture = false
            )
        )
        assertFalse(
            PictureInPicturePolicy.shouldEnter(
                isSupported = false,
                isInActiveMeeting = true,
                isAlreadyInPictureInPicture = false
            )
        )
        assertFalse(
            PictureInPicturePolicy.shouldEnter(
                isSupported = true,
                isInActiveMeeting = true,
                isAlreadyInPictureInPicture = true
            )
        )
    }
}
