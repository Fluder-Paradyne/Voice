package voice.features.playbackScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import voice.core.strings.R as StringsR

@Composable
internal fun CaptionsBottomSheet(
  dialogState: BookPlayDialogViewState.Captions,
  onDismiss: () -> Unit,
  onTrackSelected: (String?) -> Unit,
) {
  ModalBottomSheet(
    sheetState = rememberBottomSheetState(
      initialValue = Hidden,
      enabledValues = setOf(Hidden, Expanded),
    ),
    onDismissRequest = onDismiss,
    content = {
      Text(
        text = stringResource(StringsR.string.playback_captions_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      )
      LazyColumn {
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
