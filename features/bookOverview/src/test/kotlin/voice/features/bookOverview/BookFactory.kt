package voice.features.bookOverview

import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import java.time.Instant
import kotlin.uuid.Uuid

fun book(
  name: String = Uuid.random().toString(),
  id: BookId = BookId(Uuid.random().toString()),
  series: String? = null,
  part: String? = null,
  lastPlayedAt: Instant = Instant.EPOCH,
  chapters: List<Chapter> = listOf(chapter(), chapter()),
  time: Long = 42,
  currentChapter: ChapterId = chapters.first().id,
  author: String? = Uuid.random().toString(),
): Book {
  return Book(
    content = BookContent(
      author = author,
      name = name,
      positionInChapter = time,
      playbackSpeed = 1F,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = currentChapter,
      isActive = true,
      lastPlayedAt = lastPlayedAt,
      skipSilence = false,
      id = id,
      gain = 0F,
      genre = null,
      narrator = null,
      series = series,
      part = part,
    ),
    chapters = chapters,
  )
}

fun chapter(
  duration: Long = 10000,
  id: ChapterId = ChapterId(Uuid.random().toString()),
): Chapter {
  return Chapter(
    id = id,
    name = Uuid.random().toString(),
    duration = duration,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    fileSize = 0,
  )
}
