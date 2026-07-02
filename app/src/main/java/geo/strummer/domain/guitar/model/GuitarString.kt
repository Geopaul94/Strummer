package geo.strummer.domain.guitar.model

// Standard guitar tuning: string index 0 (low E) to 5 (high E).
// Each string has a name, its open MIDI pitch, and its index.
enum class GuitarString(val stringName: String, val openMidiPitch: Int, val index: Int) {
    LOW_E("E2", 40, 0),
    A("A2", 45, 1),
    D("D3", 50, 2),
    G("G3", 55, 3),
    B("B3", 59, 4),
    HIGH_E("E4", 64, 5);
}
