package geo.strummer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import geo.strummer.data.audio.NativeGuitarEngine
import geo.strummer.domain.audio.GuitarEngine
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindGuitarEngine(impl: NativeGuitarEngine): GuitarEngine
}
