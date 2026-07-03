package geo.strummer.data.palette

import geo.strummer.domain.guitar.ChordLibrary
import geo.strummer.domain.guitar.model.Chord
import geo.strummer.domain.guitar.model.ChordType
import geo.strummer.domain.guitar.model.NoteName
import geo.strummer.domain.guitar.model.SavedPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaletteRepository @Inject constructor(
    private val dao: PaletteDao,
) {
    fun observePalettes(): Flow<List<SavedPalette>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun save(name: String, chords: List<Chord>) {
        dao.insert(SavedPaletteEntity(name = name, chordKeys = chords.encode()))
    }

    suspend fun delete(palette: SavedPalette) {
        dao.delete(SavedPaletteEntity(palette.id, palette.name, palette.chords.encode()))
    }

    // ── Serialization: chords ↔ "ROOT:TYPE,ROOT:TYPE" ───────────────────────
    private fun List<Chord>.encode(): String =
        joinToString(",") { "${it.root.name}:${it.type.name}" }

    private fun SavedPaletteEntity.toDomain(): SavedPalette {
        val chords = chordKeys.split(",").mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size != 2) return@mapNotNull null
            val root = runCatching { NoteName.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
            val type = runCatching { ChordType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
            ChordLibrary.getChord(root, type)
        }
        return SavedPalette(id = id, name = name, chords = chords)
    }
}
