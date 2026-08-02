package voice.features.playbackScreen.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Holds the last non-empty cue briefly so short gaps between subtitle samples
 * do not clear and re-create the overlay (which looks like flicker).
 */
internal fun stableCaptionText(
  incoming: String?,
  currentlyDisplayed: String?,
): String? {
  return incoming?.takeIf { it.isNotBlank() } ?: currentlyDisplayed
}

@Composable
internal fun CaptionOverlay(
  text: String?,
  modifier: Modifier = Modifier,
  clearDelayMs: Long = 300,
) {
  var displayedText by remember { mutableStateOf(text?.takeIf { it.isNotBlank() }) }

  LaunchedEffect(text) {
    val next = text?.takeIf { it.isNotBlank() }
    if (next != null) {
      displayedText = next
    } else {
      delay(clearDelayMs)
      // Only clear if we are still without a cue after the hold window.
      if (text.isNullOrBlank()) {
        displayedText = null
      }
    }
  }

  // Keep a fixed slot so silence does not reflow cover/controls.
  Box(
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 56.dp),
    contentAlignment = Alignment.Center,
  ) {
    val shown = displayedText
    if (!shown.isNullOrBlank()) {
      Text(
        text = shown,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color.Black.copy(alpha = 0.72f))
          .padding(horizontal = 12.dp, vertical = 8.dp),
      )
    }
  }
}
