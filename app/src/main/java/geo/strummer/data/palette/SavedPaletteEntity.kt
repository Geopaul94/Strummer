package geo.strummer.data.palette

import androidx.room.Entity
import androidx.room.PrimaryKey

// A user-saved chord palette. Chords are stored as a compact string of
// "ROOT:TYPE" tokens separated by commas (e.g. "G:MAJOR,C:MAJOR,E:MINOR"),
// which we reconstruct into real voicings via ChordLibrary on load.
@Entity(tableName = "saved_palettes")
data class SavedPaletteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val chordKeys: String,
)
