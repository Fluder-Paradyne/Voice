package voice.features.bookOverview.views

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
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
  Text(
    modifier = modifier,
    text = series,
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines = 1,
    overflow = TextOverflow.Ellipsis,
  )
}
