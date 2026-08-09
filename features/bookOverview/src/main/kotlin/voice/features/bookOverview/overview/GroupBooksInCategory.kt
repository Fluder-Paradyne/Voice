package voice.features.bookOverview.overview

import voice.core.common.comparator.NaturalOrderComparator
import voice.core.data.Book
import java.util.Locale

internal sealed interface LibraryUnit {
  data class Series(
    val displayName: String,
    val matchKey: String, // seriesKey; lowercase trimmed
    val books: List<Book>, // already sorted by part, then name
  ) : LibraryUnit

  data class Standalone(
    val book: Book,
  ) : LibraryUnit
}

internal fun seriesKey(series: String?): String? {
  val trimmed = series?.trim().orEmpty()
  return trimmed.takeIf { it.isNotEmpty() }?.lowercase(Locale.ROOT)
}

internal fun groupBooksInCategory(
  books: List<Book>,
  category: BookOverviewCategory,
): List<LibraryUnit> {
  val groups = books.groupBy { seriesKey(it.content.series) }
  val seenSeries = mutableSetOf<String>()
  val units = buildList {
    for (book in books) {
      val key = seriesKey(book.content.series)
      if (key == null) {
        add(LibraryUnit.Standalone(book))
        continue
      }
      val cluster = groups.getValue(key)
      if (cluster.size < 2) {
        add(LibraryUnit.Standalone(book))
      } else if (seenSeries.add(key)) {
        val sorted = cluster.sortedWith(inSeriesComparator)
        val displayName = sorted.first().content.series!!.trim()
        add(LibraryUnit.Series(displayName = displayName, matchKey = key, books = sorted))
      }
    }
  }
  return units.sortedWith(unitComparator(category))
}

private val inSeriesComparator: Comparator<Book> =
  Comparator<Book> { left, right ->
    val leftPart = left.content.part?.trim().orEmpty()
    val rightPart = right.content.part?.trim().orEmpty()
    when {
      leftPart.isEmpty() && rightPart.isEmpty() -> 0
      leftPart.isEmpty() -> 1  // blank / null last
      rightPart.isEmpty() -> -1
      else -> NaturalOrderComparator.stringComparator.compare(leftPart, rightPart)
    }
  }.thenBy(NaturalOrderComparator.stringComparator) { it.content.name }

private fun unitComparator(category: BookOverviewCategory): Comparator<LibraryUnit> {
  return Comparator { left, right ->
    category.comparator.compare(
      left.sortBook(category),
      right.sortBook(category),
    )
  }
}

private fun LibraryUnit.sortBook(category: BookOverviewCategory): Book = when (this) {
  is LibraryUnit.Standalone -> book
  is LibraryUnit.Series -> when (category) {
    BookOverviewCategory.CURRENT,
    BookOverviewCategory.FINISHED,
    -> books.maxBy { it.content.lastPlayedAt }
    BookOverviewCategory.NOT_STARTED ->
      // In-memory sort key only; never shown or persisted.
      // name = matchKey so placement does not depend on which book's casing won displayName.
      books.first().update { it.copy(name = matchKey) }
  }
}
