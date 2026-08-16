package voice.features.bookmark

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.featureflag.MemoryFeatureFlag
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class BookmarkViewModelTest {

  private val book = book()
  private val bookmarkFlow = MutableStateFlow<List<Bookmark>>(emptyList())

  @Test
  fun `updates the list when a bookmark is added while the screen is open`() = runTest {
    val viewModel = BookmarkViewModel(
      currentBookStore = mockk(),
      repo = mockk<BookRepository> {
        coEvery { get(book.id) } returns book
      },
      bookmarkRepo = mockk<BookmarkRepo> {
        every { flow(book.id) } returns bookmarkFlow
      },
      playStateManager = mockk(),
      playerController = mockk(),
      navigator = mockk(),
      context = ApplicationProvider.getApplicationContext(),
      kioskModeFeatureFlag = MemoryFeatureFlag(false),
      bookId = book.id,
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(emptyList(), awaitItem().bookmarks)

      val added = bookmark(book)
      bookmarkFlow.value = listOf(added)

      assertEquals(
        listOf(added.id),
        awaitItem().bookmarks.map { it.id },
      )
    }
  }
}

private fun book(): Book {
  val chapter = Chapter(
    id = ChapterId("chapter-1"),
    name = "Chapter 1",
    duration = 10_000,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    fileSize = 0,
  )
  return Book(
    content = BookContent(
      author = "Author",
      name = "Book",
      positionInChapter = 1_000,
      playbackSpeed = 1F,
      addedAt = Instant.EPOCH,
      chapters = listOf(chapter.id),
      cover = null,
      currentChapter = chapter.id,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      skipSilence = false,
      id = BookId(Uuid.random().toString()),
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
    ),
    chapters = listOf(chapter),
  )
}

private fun bookmark(book: Book): Bookmark {
  return Bookmark(
    bookId = book.id,
    chapterId = book.currentChapter.id,
    title = "From notification",
    time = 1_000L,
    addedAt = Instant.EPOCH,
    setBySleepTimer = false,
    id = Bookmark.Id.random(),
  )
}
