package live.hms.roomkit.ui

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HMSRoomKitMeetingStateTest {

    @After
    fun tearDown() {
        HMSRoomKit.updateMeetingState(isActive = false)
        HMSRoomKit.updatePictureInPictureState(isInPictureInPictureMode = false)
    }

    @Test
    fun `meeting state is exposed to host apps`() {
        HMSRoomKit.updateMeetingState(isActive = true)

        assertTrue(HMSRoomKit.isMeetingActive)
    }

    @Test
    fun `picture in picture state is exposed to host apps`() {
        HMSRoomKit.updatePictureInPictureState(isInPictureInPictureMode = true)

        assertTrue(HMSRoomKit.isMeetingInPictureInPictureMode)
    }

    @Test
    fun `meeting state defaults to inactive and outside picture in picture`() {
        assertFalse(HMSRoomKit.isMeetingActive)
        assertFalse(HMSRoomKit.isMeetingInPictureInPictureMode)
    }
}
