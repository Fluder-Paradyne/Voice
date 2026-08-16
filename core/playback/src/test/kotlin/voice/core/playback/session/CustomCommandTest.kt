package voice.core.playback.session

import android.os.Bundle
import androidx.media3.session.SessionCommand
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import voice.core.playback.misc.Decibel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class CustomCommandTest {

  @Test
  fun `parses the dedicated add-bookmark notification action`() {
    val command = SessionCommand(CustomCommand.ADD_BOOKMARK_ACTION, Bundle.EMPTY)

    assertEquals(CustomCommand.AddBookmark, CustomCommand.parse(command, Bundle.EMPTY))
  }

  @Test
  fun `parses add-bookmark from the serialized custom command extras`() {
    assertEquals(
      CustomCommand.AddBookmark,
      parseSerialized(CustomCommand.AddBookmark),
    )
  }

  @Test
  fun `parses existing serialized commands`() {
    assertEquals(CustomCommand.SetGain(Decibel(2.5f)), parseSerialized(CustomCommand.SetGain(Decibel(2.5f))))
  }

  @Test
  fun `returns null for an unknown action`() {
    assertNull(CustomCommand.parse(SessionCommand("unknown", Bundle.EMPTY), Bundle.EMPTY))
  }

  @Test
  fun `returns null when serialized extras are missing`() {
    assertNull(
      CustomCommand.parse(SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, Bundle.EMPTY), Bundle.EMPTY),
    )
  }

  private fun parseSerialized(command: CustomCommand): CustomCommand? {
    val args = Bundle().apply {
      putString(
        CustomCommand.CUSTOM_COMMAND_EXTRA,
        Json.encodeToString(CustomCommand.serializer(), command),
      )
    }
    return CustomCommand.parse(SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, Bundle.EMPTY), args)
  }
}
