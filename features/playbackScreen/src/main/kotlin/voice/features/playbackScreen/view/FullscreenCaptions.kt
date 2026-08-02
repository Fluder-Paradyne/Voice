package voice.features.playbackScreen.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import voice.core.data.CaptionStylePreference
import voice.core.ui.icons.VoiceIcons
import voice.features.playbackScreen.captions.fullscreenTextStyle
import voice.core.strings.R as StringsR

/**
 * Captions-only surface: black background, large cue text, no player chrome.
 * Tap anywhere or use close / system back to exit.
 */
@Composable
internal fun FullscreenCaptions(
  captionText: String?,
  style: CaptionStylePreference = CaptionStylePreference.Default,
  onClose: () -> Unit,
  onPlayPause: () -> Unit,
) {
  BackHandler(onBack = onClose)

  var displayedText by remember { mutableStateOf(captionText?.takeIf { it.isNotBlank() }) }
  LaunchedEffect(captionText) {
    val next = captionText?.takeIf { it.isNotBlank() }
    if (next != null) {
      displayedText = next
    } else {
      delay(300)
      if (captionText.isNullOrBlank()) {
        displayedText = null
      }
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = onPlayPause,
      ),
  ) {
    IconButton(
      onClick = onClose,
      modifier = Modifier
        .align(Alignment.TopStart)
        .statusBarsPadding()
        .padding(8.dp),
    ) {
      Icon(
        imageVector = VoiceIcons.Close,
        contentDescription = stringResource(StringsR.string.playback_captions_fullscreen_close),
        tint = Color.White.copy(alpha = 0.85f),
      )
    }

    Text(
      text = displayedText.orEmpty(),
      style = style.fullscreenTextStyle(),
      color = Color.White,
      textAlign = TextAlign.Center,
      modifier = Modifier
        .align(Alignment.Center)
        .fillMaxWidth()
        .padding(horizontal = 32.dp, vertical = 48.dp),
    )
  }
}
