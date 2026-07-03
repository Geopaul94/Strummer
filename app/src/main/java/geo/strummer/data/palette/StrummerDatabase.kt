package geo.strummer.data.palette

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SavedPaletteEntity::class], version = 1, exportSchema = false)
abstract class StrummerDatabase : RoomDatabase() {
    abstract fun paletteDao(): PaletteDao
}
