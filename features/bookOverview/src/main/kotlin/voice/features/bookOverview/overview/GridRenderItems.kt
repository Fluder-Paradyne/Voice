package voice.features.bookOverview.overview

internal sealed interface GridRenderItem {
  data class SeriesHeader(
    val series: String,
    val matchKey: String,
  ) : GridRenderItem

  data class Book(
    val book: BookOverviewRow.Book,
  ) : GridRenderItem

  /** Occupies leftover cells on the last series row. span in 1 until columnCount-1. Layout-only (hidden from a11y). */
  data class RowFiller(
    val matchKey: String,
    val span: Int,
  ) : GridRenderItem
}

internal fun List<BookOverviewRow>.toGridItems(columnCount: Int): List<GridRenderItem> {
  require(columnCount >= 1)
  val items = ArrayList<GridRenderItem>(size)
  var i = 0
  while (i < this.size) {
    when (val row = this[i]) {
      is BookOverviewRow.SeriesHeader -> {
        items.add(GridRenderItem.SeriesHeader(row.series, row.matchKey))
        repeat(row.bookCount) { offset ->
          val book = this[i + 1 + offset] as BookOverviewRow.Book
          items.add(GridRenderItem.Book(book))
        }
        i += 1 + row.bookCount
        val leftover = row.bookCount % columnCount
        val nextIsStandalone = i < this.size && this[i] is BookOverviewRow.Book
        if (leftover != 0 && nextIsStandalone) {
          items.add(GridRenderItem.RowFiller(matchKey = row.matchKey, span = columnCount - leftover))
        }
      }
      is BookOverviewRow.Book -> {
        items.add(GridRenderItem.Book(row))
        i += 1
      }
    }
  }
  return items
}
