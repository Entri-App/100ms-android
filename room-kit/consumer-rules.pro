-dontwarn live.hms.video.virtualbackground.HMSVirtualBackground

# Keep MediaPipe classes (required by virtual-background and noise-cancellation)
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**

# Keep Protocol Buffers classes (required by MediaPipe)
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**