package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ChapterId
import voice.core.data.repo.internals.AppDb
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class BookmarkRepoImplTest {

  @Test
  fun `flow emits a bookmark added for the book`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDb::class.java,
    ).build()
    val repo = BookmarkRepoImpl(db.bookmarkDao(), db)
    val bookId = BookId("book-1")
    val bookmark = bookmark(bookId = bookId, title = "From notification")

    repo.addBookmark(bookmark(bookId = BookId("other-book"), title = "Other"))
    repo.addBookmark(bookmark)

    val emitted = repo.flow(bookId).first { it.any { item -> item.id == bookmark.id } }
    assertEquals(listOf(bookmark), emitted)
    db.close()
  }

  @Test
  fun `flow starts empty when the book has no bookmarks`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDb::class.java,
    ).build()
    val repo = BookmarkRepoImpl(db.bookmarkDao(), db)

    assertTrue(repo.flow(BookId("missing")).first().isEmpty())
    db.close()
  }

  private fun bookmark(
    bookId: BookId,
    title: String,
  ): Bookmark {
    return Bookmark(
      bookId = bookId,
      chapterId = ChapterId("chapter-1"),
      title = title,
      time = 1_000L,
      addedAt = Instant.EPOCH,
      setBySleepTimer = false,
      id = Bookmark.Id.random(),
    )
  }
}
