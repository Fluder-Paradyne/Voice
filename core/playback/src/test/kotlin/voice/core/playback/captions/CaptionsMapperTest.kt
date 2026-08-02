package voice.core.playback.captions

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CaptionsMapperTest {

  @Test
  fun `parseTrackId reads group and track index`() {
    assertThat(CaptionsMapper.parseTrackId("2:1")).isEqualTo(2 to 1)
    assertThat(CaptionsMapper.parseTrackId("bad")).isNull()
    assertThat(CaptionsMapper.parseTrackId("a:b")).isNull()
  }

  @Test
  fun `trackLabel prefers label then language then fallback`() {
    assertThat(CaptionsMapper.trackLabel("English", "en", 1)).isEqualTo("English")
    assertThat(CaptionsMapper.trackLabel(null, "de", 2)).isEqualTo("de")
    assertThat(CaptionsMapper.trackLabel("  ", "", 3)).isEqualTo("Track 3")
  }

  @Test
  fun `textTracks returns empty for no text groups`() {
    val audio = format(MimeTypes.AUDIO_AAC, language = null, label = null)
    val tracks = Tracks(
      listOf(
        Tracks.Group(
          TrackGroup(audio),
          false,
          intArrayOf(C.FORMAT_HANDLED),
          booleanArrayOf(true),
        ),
      ),
    )
    assertThat(CaptionsMapper.textTracks(tracks)).isEmpty()
  }

  @Test
  fun `textTracks maps label language and fallback ids`() {
    val audio = format(MimeTypes.AUDIO_AAC, language = null, label = null)
    val english = format(MimeTypes.TEXT_VTT, language = "en", label = "English")
    val unlabeled = format(MimeTypes.TEXT_VTT, language = null, label = null)
    // Media3 requires formats in a single TrackGroup to be compatible, so use separate groups.
    val tracks = Tracks(
      listOf(
        Tracks.Group(
          TrackGroup(audio),
          false,
          intArrayOf(C.FORMAT_HANDLED),
          booleanArrayOf(true),
        ),
        Tracks.Group(
          TrackGroup(english),
          false,
          intArrayOf(C.FORMAT_HANDLED),
          booleanArrayOf(false),
        ),
        Tracks.Group(
          TrackGroup(unlabeled),
          false,
          intArrayOf(C.FORMAT_HANDLED),
          booleanArrayOf(false),
        ),
      ),
    )

    assertThat(CaptionsMapper.textTracks(tracks)).containsExactly(
      CaptionTrack(id = "1:0", label = "English"),
      CaptionTrack(id = "2:0", label = "Track 2"),
    ).inOrder()
  }

  @Test
  fun `cueText joins non-blank cues`() {
    val cueGroup = CueGroup(
      listOf(
        Cue.Builder().setText("Hello").build(),
        Cue.Builder().setText("  ").build(),
        Cue.Builder().setText("World").build(),
      ),
      0L,
    )
    assertThat(CaptionsMapper.cueText(cueGroup)).isEqualTo("Hello\nWorld")
  }

  @Test
  fun `cueText returns null when empty`() {
    val cueGroup = CueGroup(emptyList(), 0L)
    assertThat(CaptionsMapper.cueText(cueGroup)).isNull()
  }

  @Test
  fun `selectedTrackStillAvailable clears missing selection`() {
    val tracks = listOf(CaptionTrack("0:0", "English"))
    assertThat(CaptionsMapper.selectedTrackStillAvailable(tracks, "0:0")).isEqualTo("0:0")
    assertThat(CaptionsMapper.selectedTrackStillAvailable(tracks, "9:9")).isNull()
    assertThat(CaptionsMapper.selectedTrackStillAvailable(tracks, null)).isNull()
  }

  private fun format(
    mimeType: String,
    language: String?,
    label: String?,
  ): Format {
    return Format.Builder()
      .setSampleMimeType(mimeType)
      .setLanguage(language)
      .setLabel(label)
      .build()
  }
}
