package live.hms.roomkit.ui.meeting.chat.combined

import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.textview.MaterialTextView
import live.hms.roomkit.R
import live.hms.roomkit.drawableStart
import live.hms.roomkit.ui.meeting.chat.Recipient
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.prebuilt_themes.getColorOrDefault

class ChatRbacRecipientHandling {
    fun updateChipRecipientUI(
        sendToChipText: MaterialTextView,
        recipient: Recipient?
    ) {

        sendToChipText.text = recipient?.toString()
            ?: sendToChipText.resources.getString(R.string.chat_rbac_picker_send_to)
        // Set the drawable next to it
        val chevron = when (recipient) {
            Recipient.Everyone -> R.drawable.tiny_chip_everyone
            is Recipient.Role -> R.drawable.tiny_chip_roles
            is Recipient.Peer -> R.drawable.tiny_chip_dm
            null -> null
        }

        sendToChipText.drawableStart = if (chevron == null) {
            null
        } else {
            AppCompatResources.getDrawable(
                sendToChipText.context, chevron
            )?.apply {
                setTint(
                    getColorOrDefault(
                        HMSPrebuiltTheme.getColours()?.onSurfaceMedium,
                        HMSPrebuiltTheme.getDefaults().onsurface_med_emp
                    )
                )
            }
        }
    }
}