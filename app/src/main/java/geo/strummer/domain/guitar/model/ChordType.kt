package geo.strummer.domain.guitar.model

enum class ChordType(val label: String, val suffix: String) {
    MAJOR("Major", ""),
    MINOR("Minor", "m"),
    DOMINANT_7("Dominant 7th", "7"),
    MAJOR_7("Major 7th", "maj7"),
    MINOR_7("Minor 7th", "m7"),
    SUS2("Suspended 2nd", "sus2"),
    SUS4("Suspended 4th", "sus4"),
}
