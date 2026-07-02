package geo.strummer.domain.guitar.model

// A voicing defines what each of the 6 strings plays in a chord.
// Each element is the fret number (0 = open), or -1 for muted/not played.
// Index 0 = low E (string 6), index 5 = high E (string 1).
//
// Example: G major open = [3, 2, 0, 0, 0, 3]
//          C major open = [-1, 3, 2, 0, 1, 0]
//          X = muted     = [-1, ...]
data class Voicing(val frets: List<Int>) {
    init {
        require(frets.size == 6) { "A guitar voicing must have exactly 6 entries" }
    }

    fun isMuted(stringIndex: Int): Boolean = frets[stringIndex] < 0

    // Compute the MIDI pitch for a given string, accounting for standard tuning
    // and optional capo position.
    // Standard tuning open pitches: E2=40, A2=45, D3=50, G3=55, B3=59, E4=64.
    fun midiPitch(stringIndex: Int, capo: Int = 0): Int {
        if (isMuted(stringIndex)) return -1
        return GuitarString.entries[stringIndex].openMidiPitch + frets[stringIndex] + capo
    }
}
