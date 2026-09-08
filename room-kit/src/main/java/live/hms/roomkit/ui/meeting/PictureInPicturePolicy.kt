package live.hms.roomkit.ui.meeting

import android.os.Build

internal object PictureInPicturePolicy {

    fun isSupported(sdkInt: Int, hasSystemFeature: Boolean): Boolean =
        sdkInt >= Build.VERSION_CODES.O && hasSystemFeature

    fun shouldEnter(
        isSupported: Boolean,
        isInActiveMeeting: Boolean,
        isAlreadyInPictureInPicture: Boolean
    ): Boolean = isSupported && isInActiveMeeting && !isAlreadyInPictureInPicture
}
