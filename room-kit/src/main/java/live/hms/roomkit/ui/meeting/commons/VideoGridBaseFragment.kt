package live.hms.roomkit.ui.meeting.commons

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import androidx.annotation.CallSuper
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import live.hms.roomkit.R
import live.hms.roomkit.databinding.GridItemVideoBinding
import live.hms.roomkit.databinding.VideoCardBinding
import live.hms.roomkit.helpers.NetworkQualityHelper
import live.hms.roomkit.hide
import live.hms.roomkit.show
import live.hms.roomkit.ui.meeting.CustomPeerMetadata
import live.hms.roomkit.ui.meeting.MeetingTrack
import live.hms.roomkit.ui.meeting.MeetingViewMode
import live.hms.roomkit.ui.meeting.MeetingViewModel
import live.hms.roomkit.ui.meeting.pinnedvideo.StatsInterpreter
import live.hms.roomkit.ui.settings.SettingsStore
import live.hms.prebuilt_themes.HMSPrebuiltTheme
import live.hms.roomkit.ui.theme.applyTheme
import live.hms.prebuilt_themes.getColorOrDefault
import live.hms.prebuilt_themes.setBackgroundAndColor
import live.hms.roomkit.util.*
import live.hms.video.media.tracks.HMSLocalVideoTrack
import live.hms.video.media.tracks.HMSRemoteVideoTrack
import live.hms.video.media.tracks.HMSVideoTrack
import live.hms.video.sdk.models.HMSPeer
import live.hms.video.sdk.models.HMSPeerType
import live.hms.video.sdk.models.HMSSpeaker
import live.hms.video.sdk.models.enums.HMSPeerUpdate
import live.hms.videoview.HMSVideoView
import hms.webrtc.RendererCommon
import hms.webrtc.SurfaceViewRenderer
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min


/**
 * The Grid is created by building column by column.
 * Example: For 4x2 (rows x columns)
 *  - 3 videos will have 3 rows, 1 column
 *  - 5 videos will have 4 rows, 2 columns
 *  - 8 videos will have 4 rows, 2 columns
 */
abstract class VideoGridBaseFragment : Fragment() {
  companion object {
    private const val TAG = "VideoGridBase"
  }

  protected val settings: SettingsStore by lazy { SettingsStore(requireContext()) }
  protected val meetingViewModel by activityViewModels<MeetingViewModel>()

  // Determined using the onResume() and onPause()
  var isFragmentVisible = false
    private set

  private var wasLastModePip = false
  private var wasLastSpeakingViewIndex = 0
  private lateinit var gridLayout : GridLayout

  //setting init value
  //TODO put a better default value
  private var gridRowCount = 0
  private var gridColumnCount = 0

  data class RenderedViewPair(
    val binding: GridItemVideoBinding,
    val meetingTrack: MeetingTrack,
    val statsInterpreter: StatsInterpreter?,
  )

  private var lastGridColumnSpan : String? = null

  protected val renderedViews = ArrayList<RenderedViewPair>()
  private val mediaPlayerManager by lazy { MediaPlayerManager(lifecycle) }

  internal fun shouldUpdateRowOrGrid(rowCount: Int, columnCount: Int) : Boolean{
    return !(rowCount == gridRowCount && columnCount == gridColumnCount)
  }

  //Normal layout
  private fun getNormalLayoutRowCount() = min(max(1, renderedViews.size), gridRowCount)
  private fun getNormalLayoutColumnCount(): Int
    {
      val maxColumns = gridColumnCount
      val result = max(1, (renderedViews.size + getNormalLayoutRowCount() - 1) / getNormalLayoutRowCount())
      if (result > maxColumns) {
        val videos = renderedViews.map { it.meetingTrack }
        throw IllegalStateException(
          "At most ${gridRowCount * maxColumns} videos are allowed. Provided $videos"
        )
      }
      return result
    }

  private fun getPipLayoutRowCount() = max(1, ceil(renderedViews.size/2.0).toInt())
  private fun getPipLayoutColumnCount(): Int = min(renderedViews.size, 2)

  fun setVideoGridRowsAndColumns(rows: Int, columns: Int) {
    gridRowCount = rows
    gridColumnCount = columns
    Log.d("VGBF","  (screenshar : ${isScreenshare()}) grid row count ${gridRowCount} else column count ${gridColumnCount}")
  }

  protected val maxItems: Int
    get() = gridRowCount * gridColumnCount

  protected fun updateGridLayoutDimensions(layout: GridLayout) {
    if (isInPipMode()) {
      updatePipGridLayout(layout)
      return
    }

    var childIdx: Pair<Int, Int>? = null
    var colIdx = 0
    var rowIdx = 0

    layout.apply {
      fun normalLayout() {
        // The 5th video, if there are only 5, gets spread.
        val spread5thVideo = childCount == 5
        for ((index,child) in children.withIndex()) {
          childIdx = Pair(rowIdx, colIdx)

          val params = child.layoutParams as GridLayout.LayoutParams

          val size =
            if(gridRowCount == 1 && gridColumnCount == 2)
              1
            else if(index == 4 && spread5thVideo && isScreenshare().not()) {
            // The 5th video spans two spaces.
            //  if there are only 5 videos
            2
          } else 1

          if (isScreenshare().not()) {
            Log.d("VGBF","(row, coulmn) : (${rowIdx}, ${colIdx})")
          }

          params.rowSpec = GridLayout.spec(rowIdx, 1, 1f)
          params.columnSpec = GridLayout.spec(colIdx, size, 1f)

          if (colIdx + 1 == getNormalLayoutColumnCount()) {
            rowIdx += 1
            colIdx = 0
          } else {
            colIdx += 1
          }
        }
        // Forces maxIndex to be recalculated when rowCount/columnCount is set
        requestLayout()

        rowCount = getNormalLayoutRowCount()
        columnCount = getNormalLayoutColumnCount()
        lastGridColumnSpan = "${gridRowCount}x${gridColumnCount}"
      }


        normalLayout()

      }

    hideOrShowGridsForPip(wasLastSpeakingViewIndex)

  }

  private fun createVideoView(parent: ViewGroup): GridItemVideoBinding {
     val binding = GridItemVideoBinding.inflate(
      LayoutInflater.from(requireContext()),
      parent,
      false
    )
    binding.videoCard.applyTheme()
    binding.rootContainer.setBackgroundAndColor(
      HMSPrebuiltTheme.getColours()?.backgroundDim,
      HMSPrebuiltTheme.getDefaults().background_default
    )
    return binding
  }

  protected fun bindSurfaceView(
    binding: VideoCardBinding,
    item: MeetingTrack,
    scalingType: RendererCommon.ScalingType = RendererCommon.ScalingType.SCALE_ASPECT_BALANCED
  ) {
    Log.d(TAG,"bindSurfaceView for :: ${item.peer.name}")
    val earlyExit = item.video == null
            || item.video?.isMute == true
    if (earlyExit) return
    binding.hmsVideoView.let { view ->
      item.video?.let { track ->
        if (isScreenshare()) view.setScalingType( RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        view.addTrack(track)
        view.disableAutoSimulcastLayerSelect(meetingViewModel.isAutoSimulcastEnabled())
        if (item.video?.isDegraded == true ) binding.hmsVideoView.hide() else binding.hmsVideoView.show()
        // Use single click to prevent double-tap triggering two fragment transitions
        binding.hmsVideoView.setOnSingleClickListener {
          meetingViewModel.localPinnedTrack.postValue(item)
          meetingViewModel.setMeetingViewMode(MeetingViewMode.PINNED)
        }
        binding.hmsVideoView.setOnLongClickListener {
          (it as? HMSVideoView)?.let { videoView -> openDialog(videoView, item.video, item.peer.name.orEmpty()) }
          true
        }
        binding.hmsVideoView.setCameraGestureListener(item.video, {
          activity?.openShareIntent(it)
        },
        onLongPress = {(binding.hmsVideoView as? SurfaceViewRenderer)?.let { surfaceView -> openDialog(surfaceView, item.video, item.peer.name.orEmpty()) }})
      }
    }
  }

  private fun openDialog(
    surfaceView: SurfaceViewRenderer?,
    videoTrack: HMSVideoTrack?,
    peerName: String
  ) {

    contextSafe { context, activity ->
      context.showTileListDialog (
        isLocalTrack = videoTrack is HMSLocalVideoTrack,
        onScreenCapture = { captureVideoFrame(surfaceView, videoTrack) },
        onSimulcast = { context.showSimulcastDialog(videoTrack as? HMSRemoteVideoTrack) },
        onMirror = { context.showMirrorOptions(surfaceView)},
        peerName = peerName,
        onSpotlight = if (meetingViewModel.isAllowedToSpotlight() && videoTrack?.trackId != null) {
          { meetingViewModel.spotlightTrack(videoTrack.trackId) }
        } else null
      )
    }
  }

  private fun captureVideoFrame(surfaceView: SurfaceViewRenderer?, videoTrack: HMSVideoTrack?) {

    //safe check incase video
    if (videoTrack.isValid().not()){
      return
    }

    contextSafe { context, activity -> mediaPlayerManager.startPlay(R.raw.camera_shut, context )}
    surfaceView?.vibrateStrong()

    surfaceView?.onBitMap(onBitmap = { bitmap ->
      contextSafe { context, activity ->
        //stores the bitmap in local cache thus avoiding any permission
        val uri = bitmap?.saveCaptureToLocalCache(context)
        //the uri is used to open share intent
        uri?.let { activity.openShareIntent(it) }
      }
    })
  }

  protected fun bindVideo(binding: VideoCardBinding, item: MeetingTrack) {
    // FIXME: Add a shared VM with activity scope to subscribe to events
    // binding.container.setOnClickListener { viewModel.onVideoItemClick?.invoke(item) }
    binding.apply {
      applyTheme()
      // Donot update the text view if not needed, this causes redraw of the entire view leading to  flicker
      if (name.text.equals(item.peer.name).not()) {
        name.text = item.peer.name
        nameInitials.text = NameUtils.getInitials(item.peer.name)
      }
      if(item.peer.type == HMSPeerType.SIP) {
        sipImageHolder.visibility = View.VISIBLE
      } else {
        sipImageHolder.visibility = View.GONE
      }
      // Using alpha instead of visibility to stop redraw of the entire view to stop flickering
      iconScreenShare.alpha = visibilityOpacity( (item.isScreen) )
      val isAudioMute = item.isScreen.not() &&
              (item.audio == null || item.audio!!.isMute)
      iconAudioOff.alpha = visibilityOpacity(
        isAudioMute
      )

      if (isScreenshare() || item.peer.type == HMSPeerType.SIP)
        audioLevel.alpha = visibilityOpacity(false)
      else
        audioLevel.alpha = visibilityOpacity(
          isAudioMute.not()
        )

      binding.iconMaximised.alpha = visibilityOpacity(isScreenshare())
      binding.iconMaximised.setOnClickListener {
        if (isScreenshare())
        meetingViewModel.triggerScreenShareBottomSheet(item.video)
      }

      /*if (isAudioMute)
      iconAudioOff.visibility  = View.VISIBLE
      else
        iconAudioOff.visibility = View.GONE*/
      degradedView.alpha = visibilityOpacity(item.video?.isDegraded == true)
      Log.d(TAG,"bindVideo for :: ${item.peer.name} isDegraded :: ${item.video?.isDegraded} visibility :: ${item.video} ")

      /** [View.setVisibility] */
      val surfaceViewVisibility = if (item.video == null
        || item.video?.isMute == true
        || item.video?.isDegraded == true) {
        View.INVISIBLE
      } else {
        View.VISIBLE
      }
      if (hmsVideoView.visibility != surfaceViewVisibility) {
        hmsVideoView.visibility = surfaceViewVisibility
      }
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    Log.d("VGBF","  (screenshar : ${isScreenshare()}) init")
    setVideoGridRowsAndColumns(settings.videoGridRows, settings.videoGridColumns)
  }
  protected fun unbindSurfaceView(
    binding: VideoCardBinding,
    item: MeetingTrack,
    metadata: String = ""
  ) {
    Log.d(TAG,"unbindSurfaceView for :: ${item.peer.name}")

    binding.hmsVideoView.removeTrack()
    binding.hmsVideoView.setOnLongClickListener(null)
    binding.hmsVideoView.visibility = View.INVISIBLE

  }


  protected fun updateVideos(
    layout: GridLayout,
    newVideos: List<MeetingTrack?>,
    isVideoGrid: Boolean,
    isScreenShare: Boolean = false,
    isForceUpdate : Boolean = false,
  ) {
    gridLayout = layout
    var requiresGridLayoutUpdate = false
    
    val newRenderedViews = ArrayList<RenderedViewPair>()

    // Remove all the views which are not required now
    for (currentRenderedView in renderedViews) {
      val newVideo = newVideos.find { it == currentRenderedView.meetingTrack }
      if (newVideo == null) {
        requiresGridLayoutUpdate = true

        layout.apply {
          // Unbind only when view is visible to user
          if (isFragmentVisible|| isForceUpdate) unbindSurfaceView(
            currentRenderedView.binding.videoCard,
            currentRenderedView.meetingTrack
          )
//          currentRenderedView.statsInterpreter?.close()
          removeViewInLayout(currentRenderedView.binding.root)
        }
      }
    }

    for (_newVideo in newVideos) {
      _newVideo?.also { newVideo ->

        // Check if video already rendered
        val renderedViewPair = renderedViews.find { it.meetingTrack == newVideo }
        if (renderedViewPair != null) {
          newRenderedViews.add(renderedViewPair)

          if (isFragmentVisible ||isForceUpdate) {

            // This view is not yet initialized (possibly because when AudioTrack was added --
            // VideoTrack was not present, hence had to create an empty tile)
            bindSurfaceView(renderedViewPair.binding.videoCard, newVideo, if (isScreenshare()) RendererCommon.ScalingType.SCALE_ASPECT_FIT else RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
            //handling simulcast case since we are updating local reference it thinks it's an update instead of rebinding it
            renderedViewPair.statsInterpreter?.updateVideoTrack(newVideo.video)
          }

          updateNetworkQualityView(newVideo.peer,renderedViewPair.binding.videoCard.networkQuality)

          renderedViewPair.binding.videoCard.raisedHand.alpha =
            visibilityOpacity(newVideo.peer.isHandRaised)

          renderedViewPair.binding.videoCard.isBrb.alpha =
            visibilityOpacity(CustomPeerMetadata.fromJson(newVideo.peer.metadata)?.isBRBOn == true)
        } else {
          requiresGridLayoutUpdate = true

          // Create a new view
          val videoBinding = createVideoView(layout)
          var statsInterpreter: StatsInterpreter? = null
          if (!isVideoGrid) {
            statsInterpreter = StatsInterpreter(settings.showStats)
          }

          // Bind surfaceView when view is visible to user
          if (isFragmentVisible || isForceUpdate) {
            bindSurfaceView(videoBinding.videoCard, newVideo, if (isScreenShare) RendererCommon.ScalingType.SCALE_ASPECT_FIT else RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)
          }

          videoBinding.videoCard.raisedHand.alpha =
            visibilityOpacity(newVideo.peer.isHandRaised)

          videoBinding.videoCard.isBrb.alpha =
            visibilityOpacity(CustomPeerMetadata.fromJson(newVideo.peer.metadata)?.isBRBOn == true)

          layout.addView(videoBinding.root)
          newRenderedViews.add(RenderedViewPair(videoBinding, newVideo, statsInterpreter))
        }
      }
    }



    renderedViews.clear()
    renderedViews.addAll(newRenderedViews)

    // Re-bind all the videos, this handles any changes made in isMute
    for (view in renderedViews) {
      bindVideo(view.binding.videoCard, view.meetingTrack)
    }
    if (isInPipMode()) applyPipDecorations()

    if (requiresGridLayoutUpdate) {
      updateGridLayoutDimensions(layout)
    }
  }

  private fun applyMetadataUpdates(peerTypePair: Pair<HMSPeer, HMSPeerUpdate>) {
    val renderedViewPair =
      renderedViews.find { it.meetingTrack.peer.peerID == peerTypePair.first.peerID }
    if (renderedViewPair != null) {
      when (peerTypePair.second) {
        HMSPeerUpdate.METADATA_CHANGED -> {
          val isHandRaised = renderedViewPair.meetingTrack.peer.isHandRaised
          val isBRB = CustomPeerMetadata.fromJson(renderedViewPair.meetingTrack.peer.metadata)?.isBRBOn == true
          renderedViewPair.binding.videoCard.raisedHand.alpha = visibilityOpacity(isHandRaised)
          renderedViewPair.binding.videoCard.isBrb.alpha = visibilityOpacity(isBRB)
        }
        HMSPeerUpdate.HAND_RAISED_CHANGED -> {
          renderedViewPair.binding.videoCard.raisedHand.alpha = visibilityOpacity(renderedViewPair.meetingTrack.peer.isHandRaised)
        }
        HMSPeerUpdate.NAME_CHANGED -> {
          with(renderedViewPair.binding.videoCard) {
            name.text = renderedViewPair.meetingTrack.peer.name
            nameInitials.text = NameUtils.getInitials(renderedViewPair.meetingTrack.peer.name)
          }
        }
        HMSPeerUpdate.NETWORK_QUALITY_UPDATED -> {
          renderedViewPair.binding.videoCard.networkQuality.apply {
            updateNetworkQualityView(peerTypePair.first,this)
          }
        }

        // Unused updates
        HMSPeerUpdate.PEER_JOINED,
        HMSPeerUpdate.PEER_LEFT,
        HMSPeerUpdate.BECAME_DOMINANT_SPEAKER,
        HMSPeerUpdate.NO_DOMINANT_SPEAKER,
        HMSPeerUpdate.ROLE_CHANGED -> {}
      }
    }
  }

  private fun updateNetworkQualityView(peer : HMSPeer, imageView: ImageView){
    if(peer.type == HMSPeerType.SIP){
      imageView.visibility = View.GONE
    }
    else {
      val downLinkScore = peer.networkQuality?.downlinkQuality
      NetworkQualityHelper.getNetworkResource(downLinkScore, imageView.context).let { drawable ->
        if (downLinkScore == 0) {
          imageView.setColorFilter(
            getColorOrDefault(
              HMSPrebuiltTheme.getColours()?.alertErrorDefault,
              HMSPrebuiltTheme.getDefaults().error_default
            ), android.graphics.PorterDuff.Mode.SRC_IN
          );
        } else {
          imageView.colorFilter = null
        }
        if (imageView.drawable != drawable) {
          imageView.setImageDrawable(drawable)
          if (drawable == null) {
            imageView.visibility = View.GONE
          } else {
            imageView.visibility = View.VISIBLE
          }
        }
      }
    }
  }

  protected fun applySpeakerUpdates(speakers: Array<HMSSpeaker>) {
    renderedViews.forEachIndexed { index, renderedView ->
      val track = renderedView.meetingTrack.audio

      renderedView.binding.apply {
        if (track == null || track.isMute || renderedView.meetingTrack.peer.type == HMSPeerType.SIP) {
          videoCard.audioLevel.update(null)
        } else {
          val level = speakers.find { it.hmsTrack?.trackId == track.trackId }?.level ?: 0
          videoCard.audioLevel.update(level)
        }

      }
    }
  }

  protected var pipModeOverride: Boolean? = null

  protected fun isInPipMode(): Boolean =
    pipModeOverride ?: (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N &&
      activity?.isInPictureInPictureMode == true)

  private val hiddenPipViews = mutableListOf<Pair<View, Int>>()
  private val shrunkPipNames = mutableListOf<Triple<android.widget.TextView, Float, Int>>()

  /**
   * Applied by the parent screen when PiP is entered or exited. In PiP the grid shows one tile,
   * without mic or expand icons and with a smaller name label.
   */
  fun applyPipMode(inPip: Boolean) {
    pipModeOverride = inPip
    if (inPip) {
      applyPipDecorations()
    } else {
      hiddenPipViews.forEach { (child, visibility) -> child.visibility = visibility }
      hiddenPipViews.clear()
      shrunkPipNames.forEach { (name, textSizePx, maxLines) ->
        name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textSizePx)
        name.maxLines = maxLines
      }
      shrunkPipNames.clear()
    }
    if (::gridLayout.isInitialized) updateGridLayoutDimensions(gridLayout)
    if (!inPip) pipModeOverride = null
  }

  /** Hides mic and expand icons and shrinks names on the current tiles. Safe to call on every grid update. */
  private fun applyPipDecorations() {
    renderedViews.forEach { rendered ->
      val icon = rendered.binding.videoCard.iconAudioOff
      if (icon.visibility != View.GONE) {
        hiddenPipViews += icon to icon.visibility
        icon.visibility = View.GONE
      }
    }
    val roots = renderedViews.map { it.binding.root } + listOfNotNull(view)
    roots.forEach { root ->
      root.findViewById<View>(R.id.icon_maximised)?.let { expand ->
        if (expand.visibility != View.GONE) {
          hiddenPipViews += expand to expand.visibility
          expand.visibility = View.GONE
        }
      }
    }
    renderedViews.forEach { rendered ->
      rendered.binding.root.findViewById<android.widget.TextView>(R.id.name)?.let { name ->
        if (shrunkPipNames.none { it.first === name }) {
          shrunkPipNames += Triple(name, name.textSize, name.maxLines)
        }
        name.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
        name.maxLines = 1
      }
    }
  }

  /**
   * In PiP only the screen share (or the first tile when nothing is shared) is shown, in a single full-size cell.
   */
  private fun updatePipGridLayout(layout: GridLayout) {
    val mainTile = renderedViews.firstOrNull { it.meetingTrack.isScreen } ?: renderedViews.firstOrNull()
    // Move every child into the single cell first: GridLayout rejects a smaller count while children still sit in later cells.
    for (child in layout.children) {
      val params = child.layoutParams as GridLayout.LayoutParams
      params.rowSpec = GridLayout.spec(0, 1, 1f)
      params.columnSpec = GridLayout.spec(0, 1, 1f)
      child.visibility = if (child === mainTile?.binding?.root) View.VISIBLE else View.GONE
    }
    layout.rowCount = 1
    layout.columnCount = 1
    layout.requestLayout()
  }

  private fun hideOrShowGridsForPip(onlyIndexToShow : Int? = null) {
    if (isInPipMode()) return
    renderedViews.forEach { renderedViewPair ->
      if (renderedViewPair.binding.root.isVisible.not())
        renderedViewPair.binding.root.visibility = View.VISIBLE
    }
  }

  override fun onResume() {
    super.onResume()
    if (wasLastModePip) {
      //force pip mode layout refresh
      hideOrShowGridsForPip(null)
      if (::gridLayout.isInitialized)
        updateGridLayoutDimensions(gridLayout)
      wasLastModePip = false
      return
    }
    isFragmentVisible = true
    bindViews()
  }

  fun bindViews() {
    renderedViews.forEach { renderedView ->
      bindSurfaceView(renderedView.binding.videoCard, renderedView.meetingTrack, if (isScreenshare()) RendererCommon.ScalingType.SCALE_ASPECT_FIT else RendererCommon.ScalingType.SCALE_ASPECT_BALANCED)

      meetingViewModel.statsToggleLiveData.observe(this) {
        if (it) {
          renderedView.binding.videoCard.statsView.visibility = View.VISIBLE
          renderedView.statsInterpreter?.initiateStats(
            viewLifecycleOwner,
            meetingViewModel.getStats(),
            renderedView.meetingTrack.video,
            renderedView.meetingTrack.audio,
            renderedView.meetingTrack.peer.isLocal
          ) { string -> renderedView.binding.videoCard.statsView.text = string }
        } else {
          renderedView.binding.videoCard.statsView.visibility = View.GONE
        }
      }
    }
  }

  override fun onPause() {
    super.onPause()
    if (isInPipMode()) {
      // Keep the surfaces bound so the screen share keeps rendering in PiP.
      wasLastModePip = true
      if (::gridLayout.isInitialized) updateGridLayoutDimensions(gridLayout)
      return
    }
    isFragmentVisible = false
    unbindViews()
  }

  fun unbindViews() {
    renderedViews.forEach {
      unbindSurfaceView(it.binding.videoCard, it.meetingTrack)
//      it.statsInterpreter?.close()
    }
  }

  fun unbindLocalView() {
    renderedViews.forEach {
      if (it.meetingTrack.isLocal){
        unbindSurfaceView(it.binding.videoCard, it.meetingTrack)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (wasLastModePip) {
       unbindViews()
    }
    // Release all references to views
    renderedViews.clear()
  }

  @CallSuper
  open fun initViewModels() {
    meetingViewModel.peerMetadataNameUpdate.observe(viewLifecycleOwner) {
      applyMetadataUpdates(it)
    }
  }

  abstract fun isScreenshare(): Boolean
}