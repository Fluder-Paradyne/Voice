package voice.core.playback

import android.content.ComponentName
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.guava.asDeferred
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.logging.api.Logger
import voice.core.playback.captions.CaptionTrack
import voice.core.playback.captions.CaptionsMapper
import voice.core.playback.captions.CaptionsState
import voice.core.playback.captions.applyCaptionsTrackSelection
import voice.core.playback.misc.Decibel
import voice.core.playback.session.CustomCommand
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.PlaybackService
import voice.core.playback.session.bookId
import voice.core.playback.session.playbackItemForPosition
import voice.core.playback.session.positionInMediaItem
import voice.core.playback.session.sendCustomCommand
import voice.core.playback.session.toMediaIdOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SingleIn(AppScope::class)
@Inject
class PlayerController(
  private val context: Context,
  @CurrentBookStore
  private val currentBookStoreId: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val mediaItemProvider: MediaItemProvider,
) {

  private var _controller: Deferred<MediaController> = newControllerAsync()

  private fun newControllerAsync() = MediaController
    .Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
    .buildAsync()
    .asDeferred()

  private val controller: Deferred<MediaController>
    get() {
      if (_controller.isCompleted) {
        val completedController = _controller.getCompleted()
        if (!completedController.isConnected) {
          completedController.release()
          _controller = newControllerAsync()
        }
      }
      return _controller
    }
  private val scope = CoroutineScope(Dispatchers.Main.immediate)
  private val selectedCaptionTrackId = MutableStateFlow<String?>(null)

  fun setCaptionsTrack(trackId: String?) = executeAfterPrepare { controller ->
    selectedCaptionTrackId.value = trackId
    controller.applyCaptionsTrackSelection(trackId)
  }

  fun captionsStateFlow(): Flow<CaptionsState> = callbackFlow {
    val controller = awaitConnect()
    if (controller == null) {
      trySend(CaptionsState.Empty)
      close()
      return@callbackFlow
    }

    var lastMediaItemId: String? = controller.currentMediaItem?.mediaId

    fun syncCaptionSelectionToPlayer(playerTracks: Tracks) {
      val textTracks = CaptionsMapper.textTracks(playerTracks)
      // Adopt whatever the player already has selected (survives UI process recreation).
      val fromPlayer = CaptionsMapper.selectedTextTrackIdFromPlayer(playerTracks)
      if (selectedCaptionTrackId.value == null && fromPlayer != null) {
        selectedCaptionTrackId.value = fromPlayer
      }
      val preferred = selectedCaptionTrackId.value ?: return
      if (textTracks.isEmpty()) return
      val resolved = CaptionsMapper.resolveSelectedTrackId(textTracks, preferred)
      if (resolved.clearPreference) {
        selectedCaptionTrackId.value = null
        controller.applyCaptionsTrackSelection(null)
      } else {
        // Re-apply after rotation / controller reconnect when tracks become available again.
        controller.applyCaptionsTrackSelection(resolved.selectedTrackId)
      }
    }

    fun emitState(reapplySelection: Boolean = false) {
      val playerTracks = controller.currentTracks
      if (reapplySelection) {
        syncCaptionSelectionToPlayer(playerTracks)
      }
      val tracks = CaptionsMapper.textTracks(playerTracks)
      val preferred = selectedCaptionTrackId.value
        ?: CaptionsMapper.selectedTextTrackIdFromPlayer(playerTracks)
      val resolved = CaptionsMapper.resolveSelectedTrackId(tracks, preferred)
      if (resolved.clearPreference && selectedCaptionTrackId.value != null) {
        selectedCaptionTrackId.value = null
        controller.applyCaptionsTrackSelection(null)
      } else if (selectedCaptionTrackId.value == null && resolved.selectedTrackId != null) {
        selectedCaptionTrackId.value = resolved.selectedTrackId
      }
      val selected = resolved.selectedTrackId
      // Always surface active cues when a track is selected (don't hide real player cues).
      val cueText = if (selected != null) {
        CaptionsMapper.cueText(controller.currentCues)
      } else {
        null
      }
      trySend(
        CaptionsState(
          tracks = tracks,
          selectedTrackId = selected,
          currentCueText = cueText,
        ),
      )
    }

    val listener = object : Player.Listener {
      override fun onTracksChanged(tracks: Tracks) {
        emitState(reapplySelection = true)
      }

      override fun onCues(cueGroup: CueGroup) {
        emitState()
      }

      override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
      ) {
        val newId = mediaItem?.mediaId
        // Reconnect / rotation can re-emit the same item; only reset on a real chapter change.
        if (newId != null && newId == lastMediaItemId) {
          emitState(reapplySelection = true)
          return
        }
        lastMediaItemId = newId
        selectedCaptionTrackId.value = null
        controller.applyCaptionsTrackSelection(null)
        emitState()
      }
    }

    controller.addListener(listener)
    val selectionJob = launch {
      selectedCaptionTrackId.collect {
        emitState(reapplySelection = true)
      }
    }
    // Restore selection after Activity recreation re-subscribes this flow.
    emitState(reapplySelection = true)
    awaitClose {
      selectionJob.cancel()
      controller.removeListener(listener)
    }
  }.distinctUntilChanged()

  fun setPosition(
    time: Long,
    id: ChapterId,
  ) = executeAfterPrepare { controller ->
    val bookId = currentBookStoreId.data.first() ?: return@executeAfterPrepare
    val book = bookRepository.get(bookId) ?: return@executeAfterPrepare
    val playbackItem = book.playbackItemForPosition(
      chapterId = id,
      positionInChapterMs = time,
    )
    if (playbackItem != null) {
      controller.seekTo(playbackItem.index, playbackItem.positionInMediaItem(time))
    }
  }

  fun pauseIfCurrentBookDifferentFrom(id: BookId) {
    scope.launch {
      val controller = awaitConnect() ?: return@launch
      val currentBookId = controller.currentBookId()
      if (currentBookId != null && currentBookId != id) {
        controller.pause()
      }
    }
  }

  fun skipSilence(skip: Boolean) = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.SetSkipSilence(skip))
  }

  fun fastForward() = executeAfterPrepare { controller ->
    controller.seekForward()
  }

  fun rewind() = executeAfterPrepare { controller ->
    controller.seekBack()
  }

  fun previous() = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.ForceSeekToPrevious)
  }

  fun next() = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.ForceSeekToNext)
  }

  fun play() = executeAfterPrepare { controller ->
    controller.play()
  }

  fun playPause() = executeAfterPrepare { controller ->
    if (controller.isPlaying) {
      controller.pause()
    } else {
      controller.play()
    }
  }

  private suspend fun maybePrepare(controller: MediaController): Boolean {
    val bookId = currentBookStoreId.data.first() ?: return false
    if (controller.currentBookId() == bookId &&
      controller.playbackState in listOf(Player.STATE_READY, Player.STATE_BUFFERING)
    ) {
      return true
    }
    val book = bookRepository.get(bookId) ?: return false
    controller.setMediaItem(mediaItemProvider.mediaItem(book))
    controller.prepare()
    return true
  }

  private fun MediaController.currentBookId(): BookId? {
    val currentMediaItem = currentMediaItem ?: return null
    val mediaId = currentMediaItem.mediaId.toMediaIdOrNull() ?: return null
    return mediaId.bookId
  }

  fun pauseWithRewind(rewind: Duration) = executeAfterPrepare { controller ->
    controller.pause()
    controller.seekBackBy(
      rewind = rewind,
      crossMediaItems = false,
    )
  }

  private fun MediaController.seekBackBy(
    rewind: Duration,
    crossMediaItems: Boolean,
  ) {
    var currentPosition = currentPosition.takeUnless { it == C.TIME_UNSET }
      ?.milliseconds
      ?: return
    var remaining = rewind
    var mediaItemIndex = currentMediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: return

    while (remaining > currentPosition) {
      if (!crossMediaItems) {
        seekTo(mediaItemIndex, 0)
        return
      }
      remaining -= currentPosition
      val previousMediaItemIndex = mediaItemIndex - 1
      if (previousMediaItemIndex < 0) {
        seekTo(0)
        return
      }
      currentPosition = getMediaItemAt(previousMediaItemIndex).mediaMetadata.durationMs?.milliseconds ?: return
      mediaItemIndex = previousMediaItemIndex
    }

    seekTo(mediaItemIndex, (currentPosition - remaining).inWholeMilliseconds)
  }

  fun setSpeed(speed: Float) = executeAfterPrepare { controller ->
    controller.setPlaybackSpeed(speed)
  }

  fun setGain(gain: Decibel) = executeAfterPrepare { controller ->
    controller.sendCustomCommand(CustomCommand.SetGain(gain))
  }

  fun setVolume(volume: Float) = executeAfterPrepare {
    require(volume in 0F..1F)
    it.volume = volume
  }

  suspend fun livePlaybackState(bookId: BookId? = null): LivePlaybackState? {
    val controller = awaitConnect() ?: return null
    return controller.livePlaybackStateSnapshot(bookId)
  }

  fun livePlaybackStateFlow(bookId: BookId? = null): Flow<LivePlaybackState?> = callbackFlow {
    val controller = awaitConnect()
    if (controller == null) {
      trySend(null)
      close()
      return@callbackFlow
    }

    fun emitSnapshot() {
      trySend(controller.livePlaybackStateSnapshot(bookId))
    }

    var tickJob: Job? = null
    fun updateTicking() {
      if (!controller.isPlaying) {
        tickJob?.cancel()
        return
      }
      if (tickJob?.isActive == true) {
        return
      }
      tickJob = launch {
        while (isActive) {
          delay(250.milliseconds)
          emitSnapshot()
        }
      }
    }

    val listener = object : Player.Listener {
      override fun onEvents(
        player: Player,
        events: Player.Events,
      ) {
        if (events.containsAny(
            Player.EVENT_PLAY_WHEN_READY_CHANGED,
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_PLAYBACK_STATE_CHANGED,
          )
        ) {
          emitSnapshot()
          updateTicking()
        }
        if (events.containsAny(
            Player.EVENT_POSITION_DISCONTINUITY,
            Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
          )
        ) {
          emitSnapshot()
        }
      }
    }

    controller.addListener(listener)
    emitSnapshot()
    updateTicking()
    awaitClose {
      tickJob?.cancel()
      controller.removeListener(listener)
    }
  }

  private inline fun executeAfterPrepare(crossinline action: suspend (MediaController) -> Unit) {
    scope.launch {
      val controller = awaitConnect() ?: return@launch
      if (maybePrepare(controller)) {
        action(controller)
      }
    }
  }

  @IgnorableReturnValue
  suspend fun awaitConnect(): MediaController? {
    return try {
      controller.await()
    } catch (e: Exception) {
      if (e is CancellationException) currentCoroutineContext().ensureActive()
      Logger.w(e, "Error while connecting to media controller")
      null
    }
  }
}
