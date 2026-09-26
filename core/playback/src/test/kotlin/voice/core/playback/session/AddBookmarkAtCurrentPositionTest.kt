package voice.core.playback.session

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookmarkRepo
import voice.core.playback.CurrentBookResolver
import voice.core.playback.session.search.book
import java.time.Instant
import kotlin.test.Test
import kotlin.uuid.Uuid

class AddBookmarkAtCurrentPositionTest {

  private val book = book(
    chapters = listOf(
      Chapter(
        id = ChapterId("chapter-1"),
        name = "Chapter 1",
        duration = 10_000,
        fileLastModified = Instant.EPOCH,
        markData = emptyList(),
        fileSize = 0,
      ),
    ),
    positionInChapter = 1_234,
  )

  @Test
  fun `adds a bookmark at the current book position`() = runTest {
    val bookmarkRepo = mockk<BookmarkRepo> {
      coEvery { addBookmarkAtBookPosition(any(), any(), any()) } returns bookmark()
    }
    val addBookmark = AddBookmarkAtCurrentPosition(
      bookmarkRepo = bookmarkRepo,
      currentBookResolver = mockk {
        coEvery { currentBook() } returns book
      },
    )

    addBookmark.add()

    coVerify {
      bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        title = null,
        setBySleepTimer = false,
      )
    }
  }

  @Test
  fun `does nothing when no book is playing`() = runTest {
    val bookmarkRepo = mockk<BookmarkRepo>(relaxUnitFun = true)
    val addBookmark = AddBookmarkAtCurrentPosition(
      bookmarkRepo = bookmarkRepo,
      currentBookResolver = mockk {
        coEvery { currentBook() } returns null
      },
    )

    addBookmark.add()

    coVerify(exactly = 0) { bookmarkRepo.addBookmarkAtBookPosition(any(), any(), any()) }
  }

  private fun bookmark(): Bookmark {
    return Bookmark(
      bookId = book.id,
      chapterId = book.currentChapter.id,
      addedAt = Instant.EPOCH,
      setBySleepTimer = false,
      id = Bookmark.Id(Uuid.random()),
      time = book.content.positionInChapter,
      title = null,
    )
  }
}
