package voice.core.playback.session

import dev.zacsweers.metro.Inject
import voice.core.data.repo.BookmarkRepo
import voice.core.playback.CurrentBookResolver

@Inject
class AddBookmarkAtCurrentPosition(
  private val bookmarkRepo: BookmarkRepo,
  private val currentBookResolver: CurrentBookResolver,
) {

  suspend fun add() {
    val currentBook = currentBookResolver.currentBook() ?: return
    bookmarkRepo.addBookmarkAtBookPosition(
      book = currentBook,
      title = null,
      setBySleepTimer = false,
    )
  }
}
