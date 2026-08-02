package voice.features.playbackScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import voice.core.data.CaptionFont
import voice.core.data.CaptionTextSize
import voice.core.strings.R as StringsR

@Composable
internal fun CaptionsBottomSheet(
  dialogState: BookPlayDialogViewState.Captions,
  onDismiss: () -> Unit,
  onTrackSelected: (String?) -> Unit,
  onTextSizeSelected: (CaptionTextSize) -> Unit,
  onFontSelected: (CaptionFont) -> Unit,
) {
  ModalBottomSheet(
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
    onDismissRequest = onDismiss,
    content = {
      LazyColumn {
        item {
          Text(
            text = stringResource(StringsR.string.playback_captions_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
        }
        item {
          CaptionStyleSection(
            title = stringResource(StringsR.string.playback_captions_style_size),
            content = {
              CaptionTextSize.entries.forEach { size ->
                FilterChip(
                  selected = dialogState.style.size == size,
                  onClick = { onTextSizeSelected(size) },
                  label = { Text(text = size.label()) },
                )
              }
            },
          )
        }
        item {
          CaptionStyleSection(
            title = stringResource(StringsR.string.playback_captions_style_font),
            content = {
              CaptionFont.entries.forEach { font ->
                FilterChip(
                  selected = dialogState.style.font == font,
                  onClick = { onFontSelected(font) },
                  label = { Text(text = font.label()) },
                )
              }
            },
          )
        }
        item {
          CaptionTrackRow(
            label = stringResource(StringsR.string.playback_captions_off),
            selected = dialogState.selectedTrackId == null,
            onClick = { onTrackSelected(null) },
          )
        }
        items(dialogState.tracks, key = { it.id }) { track ->
          CaptionTrackRow(
            label = track.label,
            selected = dialogState.selectedTrackId == track.id,
            onClick = { onTrackSelected(track.id) },
          )
        }
      }
    },
  )
}

@Composable
private fun CaptionStyleSection(
  title: String,
  content: @Composable () -> Unit,
) {
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.padding(bottom = 8.dp),
    )
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      content()
    }
  }
}

@Composable
private fun CaptionTextSize.label(): String = when (this) {
  CaptionTextSize.Small -> stringResource(StringsR.string.playback_captions_style_size_small)
  CaptionTextSize.Medium -> stringResource(StringsR.string.playback_captions_style_size_medium)
  CaptionTextSize.Large -> stringResource(StringsR.string.playback_captions_style_size_large)
  CaptionTextSize.ExtraLarge -> stringResource(StringsR.string.playback_captions_style_size_extra_large)
}

@Composable
private fun CaptionFont.label(): String = when (this) {
  CaptionFont.Sans -> stringResource(StringsR.string.playback_captions_style_font_sans)
  CaptionFont.Serif -> stringResource(StringsR.string.playback_captions_style_font_serif)
  CaptionFont.Monospace -> stringResource(StringsR.string.playback_captions_style_font_monospace)
  CaptionFont.OpenDyslexic -> stringResource(StringsR.string.playback_captions_style_font_opendyslexic)
}

@Composable
private fun CaptionTrackRow(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
) {
  val backgroundColor = if (selected) {
    MaterialTheme.colorScheme.primaryContainer
  } else {
    Color.Transparent
  }
  ListItem(
    colors = ListItemDefaults.colors(containerColor = backgroundColor),
    modifier = Modifier
      .padding(3.dp)
      .clip(shape = RoundedCornerShape(12.dp))
      .semantics { this.selected = selected }
      .clickable(onClick = onClick),
    headlineContent = {
      Text(text = label)
    },
  )
}
