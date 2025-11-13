package live.hms.roomkit.ui.meeting.analytics

data class LiveSessionMetrics(
    var timeTakenToJoin: Double = 0.0,
    var avgNetworkQuality: Double = 0.0,
    var initialNetworkQuality: Int = 1,
    var minimumNetworkQuality: Int = 1,
    var reconnectCount: Int = 0,
    var errorMessages: MutableList<String> = mutableListOf(),
    var exitReason: String = "",
    var startTime: String? = null,
    var endTime: String? = null,
    val networkTimeline: MutableList<String> = mutableListOf()
)


