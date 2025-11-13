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
        if (quality < 0) return  // Skip invalid quality values
        
        val currentTime = System.currentTimeMillis()
        
        // Set initial network quality and minimum (first value received)
        if (!isTrackingStarted) {
            metrics.initialNetworkQuality = quality
            metrics.minimumNetworkQuality = quality  // Initialize minimum with first value
            isTrackingStarted = true
            lastRecordedQuality = quality  // Initialize last recorded value
        }
        
        // Update current quality
        currentNetworkQuality = quality
        
        // Update minimum network quality (lowest value seen)
        if (quality < metrics.minimumNetworkQuality) {
            metrics.minimumNetworkQuality = quality
        }
        
        // Only record to timeline if value is different from last recorded value
        if (quality != lastRecordedQuality) {
            val formattedCurrentTime = convertDateToTimelineFormat(currentTime) ?: "$currentTime"
            metrics.networkTimeline.add("$formattedCurrentTime:$quality")
            lastRecordedQuality = quality  // Update last recorded value
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
        
        // Calculate average network quality from timeline
        val allQualities = metrics.networkTimeline.mapNotNull { entry ->
            entry.substringAfterLast(":").toIntOrNull()
        }.filter { it >= 0 }
        if (allQualities.isNotEmpty()) {
            metrics.avgNetworkQuality = allQualities.map { it.toDouble() }.average()
        } else {
            // If no quality data, use defaults
            metrics.avgNetworkQuality = currentNetworkQuality.toDouble().takeIf { it >= 0 } ?: 0.0
        }
        
        return gson.toJson(metrics)
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


