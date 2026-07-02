package geo.strummer.domain.guitar.model

// A single pluck event within a strum. The strum engine produces a list of these,
// ordered by delayMs, for the ViewModel to schedule against the audio engine.
data class StrumEvent(
    val stringIndex: Int,
    val midiPitch: Int,
    val velocity: Float,
    val delayMs: Long,
)

enum class StrumDirection { DOWN, UP }
