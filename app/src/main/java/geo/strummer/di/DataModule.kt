package geo.strummer.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import geo.strummer.data.palette.PaletteDao
import geo.strummer.data.palette.StrummerDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StrummerDatabase =
        Room.databaseBuilder(context, StrummerDatabase::class.java, "strummer.db").build()

    @Provides
    fun providePaletteDao(db: StrummerDatabase): PaletteDao = db.paletteDao()
}
