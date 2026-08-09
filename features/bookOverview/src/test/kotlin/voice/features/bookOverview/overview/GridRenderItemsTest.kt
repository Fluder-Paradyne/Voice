package voice.features.bookOverview.overview

import androidx.compose.runtime.State
import voice.core.data.BookId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GridRenderItemsTest {

  @Test
  fun `toGridItems flattens series rows with leftover fillers`() {
    for (case in cases) {
      assertEquals(
        expected = case.expected,
        actual = case.rows.toGridItems(case.columnCount).map { it.toSnapshot() },
        message = case.name,
      )
    }
  }

  @Test
  fun `toGridItems requires at least one column`() {
    assertFailsWith<IllegalArgumentException> {
      emptyList<BookOverviewRow>().toGridItems(0)
    }
  }

  private data class Case(
    val name: String,
    val rows: List<BookOverviewRow>,
    val columnCount: Int,
    val expected: List<ItemSnapshot>,
  )

  private sealed interface ItemSnapshot {
    data class SeriesHeader(
      val series: String,
      val matchKey: String,
    ) : ItemSnapshot

    data class Book(
      val id: String,
    ) : ItemSnapshot

    data class RowFiller(
      val matchKey: String,
      val span: Int,
    ) : ItemSnapshot
  }

  private fun GridRenderItem.toSnapshot(): ItemSnapshot {
    return when (this) {
      is GridRenderItem.SeriesHeader -> ItemSnapshot.SeriesHeader(series, matchKey)
      is GridRenderItem.Book -> ItemSnapshot.Book(book.id.value)
      is GridRenderItem.RowFiller -> ItemSnapshot.RowFiller(matchKey, span)
    }
  }

  private fun header(
    bookCount: Int,
    series: String = "Harry Potter",
    matchKey: String = "harry potter",
  ): BookOverviewRow.SeriesHeader {
    return BookOverviewRow.SeriesHeader(
      series = series,
      matchKey = matchKey,
      bookCount = bookCount,
    )
  }

  private fun book(id: String): BookOverviewRow.Book {
    return BookOverviewRow.Book(
      id = BookId(id),
      item = unusedItem,
    )
  }

  private fun headerSnapshot(
    series: String = "Harry Potter",
    matchKey: String = "harry potter",
  ): ItemSnapshot.SeriesHeader {
    return ItemSnapshot.SeriesHeader(series, matchKey)
  }

  private fun bookSnapshot(id: String): ItemSnapshot.Book {
    return ItemSnapshot.Book(id)
  }

  private fun fillerSnapshot(
    span: Int,
    matchKey: String = "harry potter",
  ): ItemSnapshot.RowFiller {
    return ItemSnapshot.RowFiller(matchKey, span)
  }

  private val unusedItem = object : State<BookOverviewItemViewState> {
    override val value: BookOverviewItemViewState
      get() = error("unused")
  }

  private val cases = listOf(
    Case(
      name = "3-book series + standalone",
      rows = listOf(
        header(bookCount = 3),
        book("hp-1"),
        book("hp-2"),
        book("hp-3"),
        book("dune"),
      ),
      columnCount = 2,
      expected = listOf(
        headerSnapshot(),
        bookSnapshot("hp-1"),
        bookSnapshot("hp-2"),
        bookSnapshot("hp-3"),
        fillerSnapshot(span = 1),
        bookSnapshot("dune"),
      ),
    ),
    Case(
      name = "2-book series + standalone",
      rows = listOf(
        header(bookCount = 2),
        book("hp-1"),
        book("hp-2"),
        book("dune"),
      ),
      columnCount = 2,
      expected = listOf(
        headerSnapshot(),
        bookSnapshot("hp-1"),
        bookSnapshot("hp-2"),
        bookSnapshot("dune"),
      ),
    ),
    Case(
      name = "3-book series + next series",
      rows = listOf(
        header(bookCount = 3),
        book("hp-1"),
        book("hp-2"),
        book("hp-3"),
        header(
          bookCount = 2,
          series = "Kingkiller Chronicle",
          matchKey = "kingkiller chronicle",
        ),
        book("kk-1"),
        book("kk-2"),
      ),
      columnCount = 2,
      expected = listOf(
        headerSnapshot(),
        bookSnapshot("hp-1"),
        bookSnapshot("hp-2"),
        bookSnapshot("hp-3"),
        headerSnapshot(series = "Kingkiller Chronicle", matchKey = "kingkiller chronicle"),
        bookSnapshot("kk-1"),
        bookSnapshot("kk-2"),
      ),
    ),
    Case(
      name = "3-book series at end of category",
      rows = listOf(
        header(bookCount = 3),
        book("hp-1"),
        book("hp-2"),
        book("hp-3"),
      ),
      columnCount = 2,
      expected = listOf(
        headerSnapshot(),
        bookSnapshot("hp-1"),
        bookSnapshot("hp-2"),
        bookSnapshot("hp-3"),
      ),
    ),
    Case(
      name = "standalones only",
      rows = listOf(
        book("a"),
        book("b"),
        book("c"),
      ),
      columnCount = 2,
      expected = listOf(
        bookSnapshot("a"),
        bookSnapshot("b"),
        bookSnapshot("c"),
      ),
    ),
    Case(
      name = "4-book series + standalone",
      rows = listOf(
        header(bookCount = 4),
        book("hp-1"),
        book("hp-2"),
        book("hp-3"),
        book("hp-4"),
        book("dune"),
      ),
      columnCount = 2,
      expected = listOf(
        headerSnapshot(),
        bookSnapshot("hp-1"),
        bookSnapshot("hp-2"),
        bookSnapshot("hp-3"),
        bookSnapshot("hp-4"),
        bookSnapshot("dune"),
      ),
    ),
    Case(
      name = "3-book series + standalone, 3 columns",
      rows = listOf(
        header(bookCount = 3),
        book("hp-1"),
        book("hp-2"),
        book("hp-3"),
        book("dune"),
      ),
      columnCount = 3,
      expected = listOf(
        headerSnapshot(),
        bookSnapshot("hp-1"),
        bookSnapshot("hp-2"),
        bookSnapshot("hp-3"),
        bookSnapshot("dune"),
      ),
    ),
  )
}
