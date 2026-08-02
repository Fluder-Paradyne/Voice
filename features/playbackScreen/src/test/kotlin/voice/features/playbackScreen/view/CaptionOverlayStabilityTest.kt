package voice.features.playbackScreen.view

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CaptionOverlayStabilityTest {

  @Test
  fun `incoming non-blank text replaces displayed`() {
    assertEquals(
      expected = "Hello",
      actual = stableCaptionText(incoming = "Hello", currentlyDisplayed = "Old"),
    )
  }

  @Test
  fun `blank incoming keeps currently displayed until clear`() {
    assertEquals(
      expected = "Held",
      actual = stableCaptionText(incoming = null, currentlyDisplayed = "Held"),
    )
    assertEquals(
      expected = "Held",
      actual = stableCaptionText(incoming = "  ", currentlyDisplayed = "Held"),
    )
  }

  @Test
  fun `blank with nothing displayed stays null`() {
    assertNull(stableCaptionText(incoming = null, currentlyDisplayed = null))
  }
}
