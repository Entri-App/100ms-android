package live.hms.roomkit.ui.meeting.analytics

import android.util.Log
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LiveSessionAnalyticsUseCase(
    private val gson: Gson = Gson()
) {
    private val metrics = LiveSessionMetrics()

    private var currentNetworkQuality: Int = -1
    private var lastRecordedQuality: Int = -1  // Track last recorded value to avoid duplicates
    private var isTrackingStarted: Boolean = false
    private var joinStartedAt: Long? = null

    companion object {
        // Thread-safe date formatter - created once and reused
        @Volatile
        private var dateFormatter: SimpleDateFormat? = null
        
        @Volatile
        private var timelineDateFormatter: SimpleDateFormat? = null

        private fun getDateFormatter(): SimpleDateFormat {
            return dateFormatter ?: synchronized(this) {
                dateFormatter ?: SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                    dateFormatter = this
                }
            }
        }
        
        private fun getTimelineDateFormatter(): SimpleDateFormat {
            return timelineDateFormatter ?: synchronized(this) {
                timelineDateFormatter ?: SimpleDateFormat("h:mm a", Locale.ENGLISH).apply {
                    timeZone = TimeZone.getTimeZone("GMT+05:30")
                    timelineDateFormatter = this
                }
            }
        }
    }

    fun onJoinStarted(timestampMs: Long = System.currentTimeMillis()) {
        joinStartedAt = timestampMs
        metrics.startTime = convertDateToAnalytics(timestampMs) ?: "$timestampMs"
    }

    fun onJoined(timeTakenToJoin: Long) {
        metrics.timeTakenToJoin = timeTakenToJoin / 1000.0
    }

    fun onNetworkQualityUpdate(quality: Int) {
        if (quality < 0) return

        val timestamp = System.currentTimeMillis()
        val formattedTime = convertDateToTimelineFormat(timestamp) ?: timestamp.toString()

        // First valid update (initialization)
        if (!isTrackingStarted) {
            isTrackingStarted = true

            metrics.apply {
                initialNetworkQuality = quality
                minimumNetworkQuality = quality
                networkTimeline.add("$formattedTime:$quality")
            }

            currentNetworkQuality = quality
            lastRecordedQuality = quality
            return
        }

        currentNetworkQuality = quality

        // Update minimum network quality
        metrics.minimumNetworkQuality =
            minOf(metrics.minimumNetworkQuality ?: quality, quality)

        // Record only if quality changed
        if (quality != lastRecordedQuality) {
            metrics.networkTimeline.add("$formattedTime:$quality")
            lastRecordedQuality = quality
        }
    }

    fun onReconnecting() {
        metrics.reconnectCount += 1
    }

    fun onError(message: String?) {
        if (!message.isNullOrBlank()) {
            metrics.errorMessages.add(message)
        }
    }

    fun finishSession(exitReason: String): String {
        metrics.exitReason = exitReason
        metrics.endTime = convertDateToAnalytics(System.currentTimeMillis()) ?: "${System.currentTimeMillis()}"
        
        // Calculate average network quality from timeline (only if we have data)
        val allQualities = metrics.networkTimeline.mapNotNull { entry ->
            entry.substringAfterLast(":").toIntOrNull()
        }.filter { it >= 0 }
        if (allQualities.isNotEmpty()) {
            val avg = allQualities.map { it.toDouble() }.average()
            metrics.avgNetworkQuality = (avg * 10).toLong() / 10.0  // Round to 1 decimal place
        }
        // If no quality data, avgNetworkQuality remains null (don't send 0)
        val json = gson.toJson(metrics)
        return json
    }

    private fun convertDateToAnalytics(timeInMillis: Long?): String? {
        if (timeInMillis == null) return null
        return try {
            getDateFormatter().format(Date(timeInMillis))
        } catch (e: Exception) {
            null
        }
    }
    
    private fun convertDateToTimelineFormat(timeInMillis: Long?): String? {
        if (timeInMillis == null) return null
        return try {
            getTimelineDateFormatter().format(Date(timeInMillis))
        } catch (e: Exception) {
            null
        }
    }
}


