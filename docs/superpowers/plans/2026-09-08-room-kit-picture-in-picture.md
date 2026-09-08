# Room Kit Picture-in-Picture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make active Room Kit calls enter picture-in-picture from Back, Home, Recents, or a manual option in both normal and debug prebuilt modes.

**Architecture:** `MeetingActivity` owns PiP capability checks, parameters, automatic entry, and the actual platform call. `MeetingFragment` and both option sheets request PiP through that owner so actions and Android 12 auto-entry cannot overwrite each other's parameters.

**Tech Stack:** Kotlin, Android picture-in-picture APIs, AndroidX activity back dispatcher, JUnit 4

---

### Task 1: Define and test PiP eligibility

**Files:**
- Create: `room-kit/src/main/java/live/hms/roomkit/ui/meeting/PictureInPicturePolicy.kt`
- Create: `room-kit/src/test/java/live/hms/roomkit/ui/meeting/PictureInPicturePolicyTest.kt`

- [x] **Step 1: Write a failing unit test**

Test that PiP requires Android 8, the system feature, an active joined meeting, and a non-PiP activity.

- [x] **Step 2: Run the test and verify RED**

Run: `./gradlew -Dorg.gradle.java.home=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home :room-kit:testDebugUnitTest --tests live.hms.roomkit.ui.meeting.PictureInPicturePolicyTest`

Expected: compilation fails because `PictureInPicturePolicy` does not exist.

- [x] **Step 3: Add the minimal policy**

Implement pure `isSupported` and `shouldEnter` functions so the platform-independent behavior remains unit testable.

- [x] **Step 4: Run the test and verify GREEN**

Run the targeted test again. Expected: all policy tests pass.

### Task 2: Centralize PiP in MeetingActivity

**Files:**
- Modify: `room-kit/src/main/java/live/hms/roomkit/ui/meeting/MeetingActivity.kt`
- Modify: `room-kit/src/main/java/live/hms/roomkit/ui/meeting/MeetingFragment.kt`

- [x] **Step 1: Add activity-owned PiP state**

Retain the current remote actions, rebuild parameters with `setAutoEnterEnabled` on Android 12+, and expose a safe Boolean-returning entry method.

- [x] **Step 2: Add Home and Recents behavior**

Use Android 12 automatic entry while joined. On Android 8–11, enter from `onUserLeaveHint()`.

- [x] **Step 3: Add Back behavior**

Have the meeting fragment request PiP first and retain the existing leave flow only when PiP is unsupported or entry fails.

- [ ] **Step 4: Preserve existing PiP actions**

Route mute and leave action updates through `MeetingActivity` so rebuilding parameters does not disable automatic entry.

### Task 3: Expose manual PiP in normal prebuilt mode

**Files:**
- Modify: `room-kit/src/main/java/live/hms/roomkit/ui/meeting/SessionOptionBottomSheet.kt`
- Modify: `room-kit/src/main/java/live/hms/roomkit/ui/meeting/MeetingFragment.kt`
- Modify: `room-kit/src/main/java/live/hms/roomkit/ui/meeting/SettingsBottomSheet.kt`

- [x] **Step 1: Add the normal-mode option**

Add a Picture in Picture grid item to `SessionOptionBottomSheet` and wire it to the activity-owned entry method.

- [x] **Step 2: Make the debug option safe**

Route the existing debug settings button through the same supported-device and active-meeting checks.

### Task 4: Verify and prepare Entri integration

**Files:**
- Modify after publishing: `/Users/daryl/work/entriandroid/gradle/libs.versions.toml`

- [x] **Step 1: Run Room Kit verification**

Run targeted unit tests and `:room-kit:assembleDebug` with JDK 17. Expected: BUILD SUCCESSFUL.

- [x] **Step 2: Inspect the final diff**

Run `git diff --check` and verify only PiP behavior, tests, and this plan changed.

- [ ] **Step 3: Prepare the Entri version bump**

After the Room Kit artifact has a published tag, replace `v1.3.9.0` with that tag in Entri and run its relevant Gradle compilation.
