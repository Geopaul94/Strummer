package geo.strummer.domain.guitar

import geo.strummer.domain.guitar.model.PatternCategory
import geo.strummer.domain.guitar.model.PatternStep
import geo.strummer.domain.guitar.model.StrumDirection
import geo.strummer.domain.guitar.model.StrumPattern

// Preset rhythm patterns. These are generic rhythmic figures (not tied to any
// copyrighted song) — the kind any guitar method book teaches.
object PatternLibrary {

    private val D = PatternStep.Strum(StrumDirection.DOWN)
    private val U = PatternStep.Strum(StrumDirection.UP)
    private val R = PatternStep.Rest

    // ── Strum patterns (8 eighth-note steps per 4/4 bar, stepsPerBeat = 2) ──

    // The universal "D-DU-UDU" folk/pop strum.
    private val folk = StrumPattern(
        name = "Folk",
        category = PatternCategory.STRUM,
        stepsPerBeat = 2,
        steps = listOf(D, R, D, U, R, U, D, U),
    )

    // Steady pop: down on the beat, up on the off-beat.
    private val pop = StrumPattern(
        name = "Pop",
        category = PatternCategory.STRUM,
        stepsPerBeat = 2,
        steps = listOf(D, U, D, U, D, U, D, U),
    )

    // Slow ballad: quarter-note downstrokes (stepsPerBeat = 1).
    private val ballad = StrumPattern(
        name = "Ballad",
        category = PatternCategory.STRUM,
        stepsPerBeat = 1,
        steps = listOf(D, D, D, D),
    )

    // Driving rock: all eighth-note downstrokes.
    private val rock = StrumPattern(
        name = "Rock",
        category = PatternCategory.STRUM,
        stepsPerBeat = 2,
        steps = listOf(D, D, D, D, D, D, D, D),
    )

    // ── Fingerpicking patterns ──────────────────────────────────────────────
    // String indices: 0 = low E, 5 = high E. Muted strings are skipped by the player.

    // Classic Travis-style alternating bass + treble (simplified).
    private val travis = StrumPattern(
        name = "Travis",
        category = PatternCategory.FINGERPICK,
        stepsPerBeat = 2,
        steps = listOf(
            PatternStep.Pick(listOf(0)),   // bass root
            PatternStep.Pick(listOf(3)),   // G string
            PatternStep.Pick(listOf(1)),   // alternate bass (A)
            PatternStep.Pick(listOf(4)),   // B string
            PatternStep.Pick(listOf(0)),   // bass root
            PatternStep.Pick(listOf(3)),   // G string
            PatternStep.Pick(listOf(1)),   // alternate bass
            PatternStep.Pick(listOf(5)),   // high E
        ),
    )

    // Ascending arpeggio (P-i-m-a rolling up the chord).
    private val arpeggioUp = StrumPattern(
        name = "Arpeggio",
        category = PatternCategory.FINGERPICK,
        stepsPerBeat = 2,
        steps = listOf(
            PatternStep.Pick(listOf(0)),
            PatternStep.Pick(listOf(2)),
            PatternStep.Pick(listOf(3)),
            PatternStep.Pick(listOf(4)),
            PatternStep.Pick(listOf(5)),
            PatternStep.Pick(listOf(4)),
            PatternStep.Pick(listOf(3)),
            PatternStep.Pick(listOf(2)),
        ),
    )

    val strumPatterns: List<StrumPattern> = listOf(folk, pop, ballad, rock)
    val fingerpickPatterns: List<StrumPattern> = listOf(travis, arpeggioUp)
    val all: List<StrumPattern> = strumPatterns + fingerpickPatterns
}
