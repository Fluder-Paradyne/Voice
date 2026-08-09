package voice.features.bookOverview.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import voice.features.bookOverview.overview.BookOverviewCategory

@Composable
internal fun Header(
  category: BookOverviewCategory,
  modifier: Modifier = Modifier,
) {
  Text(
    modifier = modifier,
    text = stringResource(id = category.nameRes),
    style = MaterialTheme.typography.headlineSmall,
  )
}

@Composable
internal fun SeriesHeader(
  series: String,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    HorizontalDivider(
      color = MaterialTheme.colorScheme.outlineVariant,
    )
    Text(
      modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
      text = series,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
internal fun SeriesFooter(modifier: Modifier = Modifier) {
  HorizontalDivider(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.outlineVariant,
  )
}
