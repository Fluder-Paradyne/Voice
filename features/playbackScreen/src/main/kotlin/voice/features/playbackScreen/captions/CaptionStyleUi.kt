package voice.features.playbackScreen.captions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import voice.core.data.CaptionFont
import voice.core.data.CaptionStylePreference
import voice.core.data.CaptionTextSize
import voice.core.ui.R as UiR

private val OpenDyslexicFamily = FontFamily(Font(UiR.font.opendyslexic_regular))

internal fun CaptionFont.toFontFamily(): FontFamily = when (this) {
  CaptionFont.Sans -> FontFamily.SansSerif
  CaptionFont.Serif -> FontFamily.Serif
  CaptionFont.Monospace -> FontFamily.Monospace
  CaptionFont.OpenDyslexic -> OpenDyslexicFamily
}

internal fun CaptionTextSize.overlayFontSize(): TextUnit = when (this) {
  CaptionTextSize.Small -> 14.sp
  CaptionTextSize.Medium -> 16.sp
  CaptionTextSize.Large -> 20.sp
  CaptionTextSize.ExtraLarge -> 24.sp
}

internal fun CaptionTextSize.fullscreenFontSize(): TextUnit = when (this) {
  CaptionTextSize.Small -> 22.sp
  CaptionTextSize.Medium -> 28.sp
  CaptionTextSize.Large -> 34.sp
  CaptionTextSize.ExtraLarge -> 42.sp
}

internal fun CaptionTextSize.overlayLineHeight(): TextUnit = when (this) {
  CaptionTextSize.Small -> 18.sp
  CaptionTextSize.Medium -> 22.sp
  CaptionTextSize.Large -> 26.sp
  CaptionTextSize.ExtraLarge -> 30.sp
}

internal fun CaptionTextSize.fullscreenLineHeight(): TextUnit = when (this) {
  CaptionTextSize.Small -> 28.sp
  CaptionTextSize.Medium -> 36.sp
  CaptionTextSize.Large -> 44.sp
  CaptionTextSize.ExtraLarge -> 52.sp
}

@Composable
internal fun CaptionStylePreference.overlayTextStyle(): TextStyle {
  return MaterialTheme.typography.bodyLarge.copy(
    fontSize = size.overlayFontSize(),
    lineHeight = size.overlayLineHeight(),
    fontFamily = font.toFontFamily(),
  )
}

@Composable
internal fun CaptionStylePreference.fullscreenTextStyle(): TextStyle {
  return MaterialTheme.typography.headlineMedium.copy(
    fontSize = size.fullscreenFontSize(),
    lineHeight = size.fullscreenLineHeight(),
    fontFamily = font.toFontFamily(),
  )
}
