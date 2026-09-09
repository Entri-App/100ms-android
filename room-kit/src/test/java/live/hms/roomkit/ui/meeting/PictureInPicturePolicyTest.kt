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
    fun `an active meeting can enter PiP when supported and not already in PiP`() {
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

    @Test
    fun `resuming a host activity while an active meeting is in PiP restores the meeting task`() {
        assertTrue(
            PictureInPicturePolicy.shouldRestoreMeetingTask(
                isMeetingActivity = false,
                isInActiveMeeting = true,
                isInPictureInPicture = true
            )
        )
    }

    @Test
    fun `meeting task is not restored outside active PiP or for the meeting activity itself`() {
        assertFalse(
            PictureInPicturePolicy.shouldRestoreMeetingTask(
                isMeetingActivity = false,
                isInActiveMeeting = false,
                isInPictureInPicture = true
            )
        )
        assertFalse(
            PictureInPicturePolicy.shouldRestoreMeetingTask(
                isMeetingActivity = false,
                isInActiveMeeting = true,
                isInPictureInPicture = false
            )
        )
        assertFalse(
            PictureInPicturePolicy.shouldRestoreMeetingTask(
                isMeetingActivity = true,
                isInActiveMeeting = true,
                isInPictureInPicture = true
            )
        )
    }
}
