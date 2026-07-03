package geo.strummer.data.palette

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PaletteDao {
    @Query("SELECT * FROM saved_palettes ORDER BY id DESC")
    fun observeAll(): Flow<List<SavedPaletteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(palette: SavedPaletteEntity): Long

    @Delete
    suspend fun delete(palette: SavedPaletteEntity)
}
