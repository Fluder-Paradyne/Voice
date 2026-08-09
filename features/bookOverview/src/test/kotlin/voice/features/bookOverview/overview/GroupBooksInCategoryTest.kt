package voice.features.bookOverview.overview

import voice.core.data.Book
import voice.features.bookOverview.book
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupBooksInCategoryTest {

  @Test
  fun `groups books in category`() {
    for (case in cases) {
      assertEquals(
        expected = case.expected,
        actual = groupBooksInCategory(case.books, case.category).map { it.toSnapshot() },
        message = case.name,
      )
    }
  }

  private data class Case(
    val name: String,
    val books: List<Book>,
    val category: BookOverviewCategory,
    val expected: List<UnitSnapshot>,
  )

  private sealed interface UnitSnapshot {
    data class Series(
      val displayName: String,
      val matchKey: String,
      val books: List<String>,
    ) : UnitSnapshot

    data class Standalone(
      val name: String,
    ) : UnitSnapshot
  }

  private fun LibraryUnit.toSnapshot(): UnitSnapshot {
    return when (this) {
      is LibraryUnit.Series -> UnitSnapshot.Series(
        displayName = displayName,
        matchKey = matchKey,
        books = books.map { it.content.name },
      )
      is LibraryUnit.Standalone -> UnitSnapshot.Standalone(book.content.name)
    }
  }

  private fun series(
    displayName: String,
    matchKey: String,
    vararg books: String,
  ): UnitSnapshot.Series {
    return UnitSnapshot.Series(displayName, matchKey, books.toList())
  }

  private fun standalone(name: String): UnitSnapshot.Standalone {
    return UnitSnapshot.Standalone(name)
  }

  private fun played(epochMilli: Long): Instant = Instant.ofEpochMilli(epochMilli)

  private val cases = listOf(
    Case(
      name = "empty",
      books = emptyList(),
      category = BookOverviewCategory.CURRENT,
      expected = emptyList(),
    ),
    Case(
      name = "all standalone",
      books = listOf(
        book(name = "A", lastPlayedAt = played(1)),
        book(name = "B", lastPlayedAt = played(3)),
        book(name = "C", lastPlayedAt = played(2)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(standalone("B"), standalone("C"), standalone("A")),
    ),
    Case(
      name = "lone tagged",
      books = listOf(
        book(name = "Stone", series = "Harry Potter", part = "1", lastPlayedAt = played(100)),
        book(name = "Dune", lastPlayedAt = played(200)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(standalone("Dune"), standalone("Stone")),
    ),
    Case(
      name = "two same series",
      books = listOf(
        book(name = "Chamber", series = "Harry Potter", part = "2"),
        book(name = "Stone", series = "Harry Potter", part = "1"),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(series("Harry Potter", "harry potter", "Stone", "Chamber")),
    ),
    Case(
      name = "case / trim match",
      books = listOf(
        book(name = "Chamber", series = " harry potter ", part = "2"),
        book(name = "Stone", series = "Harry Potter", part = "1"),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(series("Harry Potter", "harry potter", "Stone", "Chamber")),
    ),
    Case(
      name = "cross-casing display",
      books = listOf(
        book(name = "Chamber", series = "Harry Potter", part = "2"),
        book(name = "Stone", series = "harry potter", part = "1"),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(series("harry potter", "harry potter", "Stone", "Chamber")),
    ),
    Case(
      name = "whitespace-only series",
      books = listOf(
        book(name = "Blank", series = "   ", lastPlayedAt = played(3)),
        book(name = "Empty", series = "", lastPlayedAt = played(2)),
        book(name = "None", lastPlayedAt = played(1)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(standalone("Blank"), standalone("Empty"), standalone("None")),
    ),
    Case(
      name = "two series + standalones",
      books = listOf(
        book(name = "Stone", series = "Harry Potter", part = "1", lastPlayedAt = played(100)),
        book(name = "Chamber", series = "Harry Potter", part = "2", lastPlayedAt = played(300)),
        book(name = "Wind", series = "Kingkiller Chronicle", part = "1", lastPlayedAt = played(50)),
        book(name = "Wise Man", series = "Kingkiller Chronicle", part = "2", lastPlayedAt = played(100)),
        book(name = "Dune", lastPlayedAt = played(200)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(
        series("Harry Potter", "harry potter", "Stone", "Chamber"),
        standalone("Dune"),
        series("Kingkiller Chronicle", "kingkiller chronicle", "Wind", "Wise Man"),
      ),
    ),
    Case(
      name = "current unit order",
      books = listOf(
        book(name = "Stone", series = "Harry Potter", part = "1", lastPlayedAt = played(100)),
        book(name = "Chamber", series = "harry potter", part = "2", lastPlayedAt = played(300)),
        book(name = "Dune", lastPlayedAt = played(200)),
        book(name = "Wind", series = "Kingkiller Chronicle", part = "1", lastPlayedAt = played(250)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(
        series("Harry Potter", "harry potter", "Stone", "Chamber"),
        standalone("Wind"),
        standalone("Dune"),
      ),
    ),
    Case(
      name = "current first-seen tie",
      books = listOf(
        book(name = "Stone", series = "Harry Potter", part = "1", lastPlayedAt = played(100)),
        book(name = "Dune", lastPlayedAt = played(300)),
        book(name = "Chamber", series = "Harry Potter", part = "2", lastPlayedAt = played(300)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(
        series("Harry Potter", "harry potter", "Stone", "Chamber"),
        standalone("Dune"),
      ),
    ),
    Case(
      name = "not started unit order",
      books = listOf(
        book(name = "Chamber", series = "Harry Potter", part = "2"),
        book(name = "Stone", series = "Harry Potter", part = "1"),
        book(name = "The Hobbit"),
        book(name = "Dune"),
      ),
      category = BookOverviewCategory.NOT_STARTED,
      expected = listOf(
        standalone("Dune"),
        series("Harry Potter", "harry potter", "Stone", "Chamber"),
        standalone("The Hobbit"),
      ),
    ),
    Case(
      name = "not started series vs title",
      books = listOf(
        book(name = "Alpha 2", series = "Alpha", part = "2"),
        book(name = "Alpha 1", series = "Alpha", part = "1"),
        book(name = "Beta"),
      ),
      category = BookOverviewCategory.NOT_STARTED,
      expected = listOf(
        series("Alpha", "alpha", "Alpha 1", "Alpha 2"),
        standalone("Beta"),
      ),
    ),
    Case(
      name = "not started casing-independent unit key lowercase",
      books = listOf(
        book(name = "Chamber", series = "harry potter", part = "2"),
        book(name = "Stone", series = "harry potter", part = "1"),
        book(name = "The Hobbit"),
      ),
      category = BookOverviewCategory.NOT_STARTED,
      expected = listOf(
        series("harry potter", "harry potter", "Stone", "Chamber"),
        standalone("The Hobbit"),
      ),
    ),
    Case(
      name = "not started casing-independent unit key title case",
      books = listOf(
        book(name = "Chamber", series = "Harry Potter", part = "2"),
        book(name = "Stone", series = "Harry Potter", part = "1"),
        book(name = "The Hobbit"),
      ),
      category = BookOverviewCategory.NOT_STARTED,
      expected = listOf(
        series("Harry Potter", "harry potter", "Stone", "Chamber"),
        standalone("The Hobbit"),
      ),
    ),
    Case(
      name = "in-cluster parts",
      books = listOf(
        book(name = "Ten", series = "Harry Potter", part = "10"),
        book(name = "Two", series = "Harry Potter", part = "2"),
        book(name = "One", series = "Harry Potter", part = "1"),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(series("Harry Potter", "harry potter", "One", "Two", "Ten")),
    ),
    Case(
      name = "decimal part",
      books = listOf(
        book(name = "TwoFive", series = "Harry Potter", part = "2.5"),
        book(name = "Two", series = "Harry Potter", part = "2"),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(series("Harry Potter", "harry potter", "Two", "TwoFive")),
    ),
    Case(
      name = "blank / null part last",
      books = listOf(
        book(name = "Zebra", series = "Harry Potter", part = null),
        book(name = "Apple", series = "Harry Potter", part = ""),
        book(name = "Middle", series = "Harry Potter", part = "1"),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(series("Harry Potter", "harry potter", "Middle", "Apple", "Zebra")),
    ),
    Case(
      name = "same part different names",
      books = listOf(
        book(name = "B", series = "Harry Potter", part = "1"),
        book(name = "A", series = "Harry Potter", part = "1"),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(series("Harry Potter", "harry potter", "A", "B")),
    ),
    Case(
      name = "does not merge different series",
      books = listOf(
        book(name = "HP Book", series = "HP", lastPlayedAt = played(100)),
        book(name = "Stone", series = "Harry Potter", lastPlayedAt = played(200)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(standalone("Stone"), standalone("HP Book")),
    ),
    Case(
      name = "does not merge Cafe / Café",
      books = listOf(
        book(name = "Cafe 2", series = "Cafe", part = "2", lastPlayedAt = played(100)),
        book(name = "Cafe 1", series = "Cafe", part = "1", lastPlayedAt = played(100)),
        book(name = "Café 2", series = "Café", part = "2", lastPlayedAt = played(200)),
        book(name = "Café 1", series = "Café", part = "1", lastPlayedAt = played(200)),
      ),
      category = BookOverviewCategory.CURRENT,
      expected = listOf(
        series("Café", "café", "Café 1", "Café 2"),
        series("Cafe", "cafe", "Cafe 1", "Cafe 2"),
      ),
    ),
    Case(
      name = "finished uses lastPlayed",
      books = listOf(
        book(name = "Stone", series = "Harry Potter", part = "1", lastPlayedAt = played(100)),
        book(name = "Chamber", series = "harry potter", part = "2", lastPlayedAt = played(300)),
        book(name = "Dune", lastPlayedAt = played(200)),
        book(name = "Wind", series = "Kingkiller Chronicle", part = "1", lastPlayedAt = played(250)),
      ),
      category = BookOverviewCategory.FINISHED,
      expected = listOf(
        series("Harry Potter", "harry potter", "Stone", "Chamber"),
        standalone("Wind"),
        standalone("Dune"),
      ),
    ),
  )
}
