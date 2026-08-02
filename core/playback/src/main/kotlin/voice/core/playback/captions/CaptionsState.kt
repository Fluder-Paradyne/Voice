package voice.core.playback.captions

data class CaptionsState(
  val tracks: List<CaptionTrack>,
  val selectedTrackId: String?,
  val currentCueText: String?,
) {
  companion object {
    val Empty = CaptionsState(
      tracks = emptyList(),
      selectedTrackId = null,
      currentCueText = null,
    )
  }
}
