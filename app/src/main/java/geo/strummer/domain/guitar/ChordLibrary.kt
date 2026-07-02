package geo.strummer.domain.guitar

import geo.strummer.domain.guitar.model.Chord
import geo.strummer.domain.guitar.model.ChordType
import geo.strummer.domain.guitar.model.NoteName
import geo.strummer.domain.guitar.model.Voicing

// Chord fingerings are factual/computable data — these are standard guitar
// voicings any guitarist would know. Not copyrighted, not scraped.
//
// Two approaches combined:
// 1. Hand-authored OPEN shapes for common first-position chords (sound best open).
// 2. Moveable BARRE shapes (E-shape and A-shape) that transpose to any root by
//    shifting up the neck. This gives complete coverage of all 12 keys without
//    needing to hand-author every voicing.
object ChordLibrary {

    // ── Open chord shapes (first position, most natural voicings) ──────────
    // These are the chords every beginner learns first. They use open strings
    // and sound fuller/richer than barre equivalents at the same pitches.
    // Frets: [lowE, A, D, G, B, highE], -1 = muted/not played.
    private val openChords: List<Chord> = listOf(
        // Major
        chord(NoteName.C, ChordType.MAJOR, -1, 3, 2, 0, 1, 0),
        chord(NoteName.D, ChordType.MAJOR, -1, -1, 0, 2, 3, 2),
        chord(NoteName.E, ChordType.MAJOR, 0, 2, 2, 1, 0, 0),
        chord(NoteName.G, ChordType.MAJOR, 3, 2, 0, 0, 0, 3),
        chord(NoteName.A, ChordType.MAJOR, -1, 0, 2, 2, 2, 0),

        // Minor
        chord(NoteName.D, ChordType.MINOR, -1, -1, 0, 2, 3, 1),
        chord(NoteName.E, ChordType.MINOR, 0, 2, 2, 0, 0, 0),
        chord(NoteName.A, ChordType.MINOR, -1, 0, 2, 2, 1, 0),

        // Dominant 7th
        chord(NoteName.A, ChordType.DOMINANT_7, -1, 0, 2, 0, 2, 0),
        chord(NoteName.B, ChordType.DOMINANT_7, -1, 2, 1, 2, 0, 2),
        chord(NoteName.D, ChordType.DOMINANT_7, -1, -1, 0, 2, 1, 2),
        chord(NoteName.E, ChordType.DOMINANT_7, 0, 2, 0, 1, 0, 0),
        chord(NoteName.G, ChordType.DOMINANT_7, 3, 2, 0, 0, 0, 1),

        // Major 7th
        chord(NoteName.C, ChordType.MAJOR_7, -1, 3, 2, 0, 0, 0),
        chord(NoteName.D, ChordType.MAJOR_7, -1, -1, 0, 2, 2, 2),
        chord(NoteName.G, ChordType.MAJOR_7, 3, 2, 0, 0, 0, 2),

        // Minor 7th
        chord(NoteName.A, ChordType.MINOR_7, -1, 0, 2, 0, 1, 0),
        chord(NoteName.E, ChordType.MINOR_7, 0, 2, 0, 0, 0, 0),
        chord(NoteName.D, ChordType.MINOR_7, -1, -1, 0, 2, 1, 1),

        // Sus2
        chord(NoteName.A, ChordType.SUS2, -1, 0, 2, 2, 0, 0),
        chord(NoteName.D, ChordType.SUS2, -1, -1, 0, 2, 3, 0),
        chord(NoteName.E, ChordType.SUS2, 0, 2, 4, 4, 0, 0),

        // Sus4
        chord(NoteName.A, ChordType.SUS4, -1, 0, 2, 2, 3, 0),
        chord(NoteName.D, ChordType.SUS4, -1, -1, 0, 2, 3, 3),
        chord(NoteName.E, ChordType.SUS4, 0, 2, 2, 2, 0, 0),
    )

    // ── Moveable barre shapes ──────────────────────────────────────────────
    // These are the "E-shape" and "A-shape" barre chord forms. By sliding them
    // up the neck (adding a fret offset), any root note is reachable.
    //
    // E-shape barre: root on the low E string (string 0).
    //   The "base" shape is an open E chord at fret 0. At fret N, the root
    //   becomes E + N semitones. E.g. fret 1 = F, fret 3 = G, fret 5 = A.
    //
    // A-shape barre: root on the A string (string 1).
    //   The "base" shape is an open A chord at fret 0. At fret N, the root
    //   becomes A + N semitones. E.g. fret 2 = B, fret 3 = C, fret 5 = D.

    // E-shape templates (fret offsets relative to the barre position).
    // When barred at fret N: each fret value = template + N.
    private data class BarreTemplate(
        val type: ChordType,
        val shape: String, // "E" or "A" — which string carries the root
        val frets: List<Int>, // relative fret values (0-based from barre)
        val muted: Set<Int> = emptySet(), // which strings are muted
    )

    private val barreTemplates = listOf(
        // E-shape major: 0-2-2-1-0-0 (the open E major shape)
        BarreTemplate(ChordType.MAJOR, "E", listOf(0, 2, 2, 1, 0, 0)),
        // E-shape minor: 0-2-2-0-0-0
        BarreTemplate(ChordType.MINOR, "E", listOf(0, 2, 2, 0, 0, 0)),
        // E-shape dom7: 0-2-0-1-0-0
        BarreTemplate(ChordType.DOMINANT_7, "E", listOf(0, 2, 0, 1, 0, 0)),
        // E-shape maj7: 0-2-1-1-0-0
        BarreTemplate(ChordType.MAJOR_7, "E", listOf(0, 2, 1, 1, 0, 0)),
        // E-shape min7: 0-2-0-0-0-0
        BarreTemplate(ChordType.MINOR_7, "E", listOf(0, 2, 0, 0, 0, 0)),

        // A-shape major: x-0-2-2-2-0
        BarreTemplate(ChordType.MAJOR, "A", listOf(0, 0, 2, 2, 2, 0), setOf(0)),
        // A-shape minor: x-0-2-2-1-0
        BarreTemplate(ChordType.MINOR, "A", listOf(0, 0, 2, 2, 1, 0), setOf(0)),
        // A-shape dom7: x-0-2-0-2-0
        BarreTemplate(ChordType.DOMINANT_7, "A", listOf(0, 0, 2, 0, 2, 0), setOf(0)),
        // A-shape min7: x-0-2-0-1-0
        BarreTemplate(ChordType.MINOR_7, "A", listOf(0, 0, 2, 0, 1, 0), setOf(0)),
    )

    // Root note of each barre shape at fret 0.
    // E-shape: open low E string = E (semitone 4 in NoteName).
    // A-shape: open A string = A (semitone 9 in NoteName).
    private val eShapeBaseOffset = NoteName.E.semitoneOffset  // 4
    private val aShapeBaseOffset = NoteName.A.semitoneOffset  // 9

    private fun generateBarreChords(): List<Chord> {
        val result = mutableListOf<Chord>()
        for (template in barreTemplates) {
            val baseOffset = if (template.shape == "E") eShapeBaseOffset else aShapeBaseOffset
            // Generate for all 12 roots by shifting up the neck.
            for (root in NoteName.entries) {
                // How many frets to shift: distance from the shape's base root to target root.
                val shift = (root.semitoneOffset - baseOffset + 12) % 12
                // Skip fret 0 for barre chords — those are already covered by open shapes
                // (and open shapes sound better at fret 0).
                if (shift == 0) continue
                // Skip very high fret positions (above fret 12) — impractical.
                if (shift > 12) continue

                val frets = template.frets.mapIndexed { idx, fret ->
                    if (idx in template.muted) -1
                    else fret + shift
                }
                result.add(Chord(root, template.type, Voicing(frets)))
            }
        }
        return result
    }

    // ── Public API ─────────────────────────────────────────────────────────

    private val chordCache: List<Chord> by lazy { openChords + generateBarreChords() }

    fun getChord(root: NoteName, type: ChordType): Chord? {
        return openChords.find { it.root == root && it.type == type }
            ?: chordCache.find { it.root == root && it.type == type }
    }

    fun getChordsForRoot(root: NoteName): List<Chord> {
        return chordCache.filter { it.root == root }.distinctBy { it.type }
    }

    fun getAllChords(): List<Chord> = chordCache.distinctBy { "${it.root}_${it.type}" }

    // A sensible default palette for the strum screen — common campfire chords.
    fun defaultPalette(): List<Chord> = listOfNotNull(
        getChord(NoteName.G, ChordType.MAJOR),
        getChord(NoteName.C, ChordType.MAJOR),
        getChord(NoteName.D, ChordType.MAJOR),
        getChord(NoteName.E, ChordType.MINOR),
        getChord(NoteName.A, ChordType.MINOR),
        getChord(NoteName.E, ChordType.MAJOR),
        getChord(NoteName.A, ChordType.MAJOR),
        getChord(NoteName.D, ChordType.MINOR),
    )

    private fun chord(root: NoteName, type: ChordType, vararg frets: Int): Chord =
        Chord(root, type, Voicing(frets.toList()))
}
