package voice.core.data

import kotlinx.serialization.Serializable

@Serializable
public data class CaptionStylePreference(
  val size: CaptionTextSize = CaptionTextSize.Medium,
  val font: CaptionFont = CaptionFont.Sans,
) {
  public companion object {
    public val Default: CaptionStylePreference = CaptionStylePreference()
  }
}

@Serializable
public enum class CaptionTextSize {
  Small,
  Medium,
  Large,
  ExtraLarge,
}

@Serializable
public enum class CaptionFont {
  Sans,
  Serif,
  Monospace,
  OpenDyslexic,
}
