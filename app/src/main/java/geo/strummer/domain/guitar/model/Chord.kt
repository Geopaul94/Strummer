package geo.strummer.domain.guitar.model

data class Chord(
    val root: NoteName,
    val type: ChordType,
    val voicing: Voicing,
) {
    val displayName: String get() = "${root.label}${type.suffix}"
}
