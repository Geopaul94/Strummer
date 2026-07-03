package geo.strummer.domain.guitar.model

// Guitar tones. Each maps to a Karplus-Strong brightness value that shapes the
// string timbre and sustain. (When a real multisampled SF2 is sourced later,
// these can switch soundfonts instead — the enum stays the same.)
enum class Tone(val label: String, val brightness: Float) {
    ACOUSTIC("Acoustic", 0.55f),   // steel-string: bright, ringing
    NYLON("Nylon", 0.32f),         // classical: warm, mellow, shorter
    ELECTRIC("Electric", 0.78f),   // clean electric: brightest, longest sustain
}
