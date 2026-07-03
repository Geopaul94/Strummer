package geo.strummer.domain.guitar.model

data class SavedPalette(
    val id: Long,
    val name: String,
    val chords: List<Chord>,
)
