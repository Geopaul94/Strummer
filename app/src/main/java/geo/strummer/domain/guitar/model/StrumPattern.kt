package geo.strummer.domain.guitar.model

// A rhythm pattern that plays a held chord in time. Two flavors:
//   - STRUM: each step strums the whole chord in a direction (or rests).
//   - FINGERPICK: each step plucks specific strings of the chord.
//
// A pattern is a looping sequence of steps. Timing is derived from BPM:
//   stepDurationMs = 60_000 / bpm / stepsPerBeat
// e.g. stepsPerBeat = 2 means each step is an eighth note.

enum class PatternCategory { STRUM, FINGERPICK }

sealed interface PatternStep {
    // Strum the whole chord in a direction.
    data class Strum(val direction: StrumDirection) : PatternStep

    // Pluck specific strings (indices 0=low E .. 5=high E). Muted strings in the
    // current voicing are skipped automatically by the player.
    data class Pick(val strings: List<Int>) : PatternStep

    // Silence for this step.
    data object Rest : PatternStep
}

data class StrumPattern(
    val name: String,
    val category: PatternCategory,
    val stepsPerBeat: Int,
    val steps: List<PatternStep>,
)
