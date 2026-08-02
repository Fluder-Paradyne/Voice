package voice.core.playback.captions

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup

object CaptionsMapper {

  fun trackId(
    groupIndex: Int,
    trackIndex: Int,
  ): String = "$groupIndex:$trackIndex"

  fun parseTrackId(id: String): Pair<Int, Int>? {
    val parts = id.split(':')
    if (parts.size != 2) return null
    val groupIndex = parts[0].toIntOrNull() ?: return null
    val trackIndex = parts[1].toIntOrNull() ?: return null
    return groupIndex to trackIndex
  }

  fun trackLabel(
    label: String?,
    language: String?,
    fallbackIndex: Int,
  ): String {
    return label?.takeIf { it.isNotBlank() }
      ?: language?.takeIf { it.isNotBlank() }
      ?: "Track $fallbackIndex"
  }

  fun textTracks(tracks: Tracks): List<CaptionTrack> {
    var fallbackIndex = 1
    val result = ArrayList<CaptionTrack>()
    tracks.groups.forEachIndexed { groupIndex, group ->
      if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
      for (trackIndex in 0 until group.length) {
        val format = group.getTrackFormat(trackIndex)
        result += CaptionTrack(
          id = trackId(groupIndex, trackIndex),
          label = trackLabel(
            label = format.label,
            language = format.language,
            fallbackIndex = fallbackIndex,
          ),
        )
        fallbackIndex++
      }
    }
    return result
  }

  /** First text track currently selected on the player, if any. */
  fun selectedTextTrackIdFromPlayer(tracks: Tracks): String? {
    tracks.groups.forEachIndexed { groupIndex, group ->
      if (group.type != C.TRACK_TYPE_TEXT) return@forEachIndexed
      for (trackIndex in 0 until group.length) {
        if (group.isTrackSelected(trackIndex)) {
          return trackId(groupIndex, trackIndex)
        }
      }
    }
    return null
  }

  fun cueText(cueGroup: CueGroup): String? {
    val text = cueGroup.cues
      .mapNotNull { cue -> cue.text?.toString()?.takeIf { it.isNotBlank() } }
      .joinToString(separator = "\n")
    return text.ifBlank { null }
  }

  fun selectedTrackStillAvailable(
    tracks: List<CaptionTrack>,
    selectedTrackId: String?,
  ): String? {
    if (selectedTrackId == null) return null
    return selectedTrackId.takeIf { id -> tracks.any { it.id == id } }
  }

  /**
   * Resolves the effective selection for UI / player sync.
   *
   * When [tracks] is temporarily empty (e.g. player reconnect after rotation), keep the preferred
   * selection so it can be re-applied once tracks are available again. Only drop it when tracks
   * are present and the preferred id is no longer among them.
   */
  fun resolveSelectedTrackId(
    tracks: List<CaptionTrack>,
    preferredTrackId: String?,
  ): ResolveSelection {
    if (preferredTrackId == null) {
      return ResolveSelection(selectedTrackId = null, clearPreference = false)
    }
    if (tracks.isEmpty()) {
      return ResolveSelection(selectedTrackId = preferredTrackId, clearPreference = false)
    }
    val stillAvailable = selectedTrackStillAvailable(tracks, preferredTrackId)
    return if (stillAvailable != null) {
      ResolveSelection(selectedTrackId = stillAvailable, clearPreference = false)
    } else {
      ResolveSelection(selectedTrackId = null, clearPreference = true)
    }
  }

  data class ResolveSelection(
    val selectedTrackId: String?,
    val clearPreference: Boolean,
  )
}

fun Player.applyCaptionsTrackSelection(trackId: String?) {
  val builder = trackSelectionParameters.buildUpon()
    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
  if (trackId == null) {
    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
  } else {
    val parsed = CaptionsMapper.parseTrackId(trackId)
    if (parsed == null) {
      builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
      trackSelectionParameters = builder.build()
      return
    }
    val (groupIndex, trackIndex) = parsed
    val group = currentTracks.groups.getOrNull(groupIndex)
    if (group == null || group.type != C.TRACK_TYPE_TEXT || trackIndex !in 0 until group.length) {
      builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
      trackSelectionParameters = builder.build()
      return
    }
    builder
      .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
      .addOverride(TrackSelectionOverride(group.mediaTrackGroup, listOf(trackIndex)))
  }
  trackSelectionParameters = builder.build()
}
