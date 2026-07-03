package geo.strummer.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import geo.strummer.data.audio.NativeGuitarEngine
import geo.strummer.data.billing.StubEntitlementRepository
import geo.strummer.domain.audio.GuitarEngine
import geo.strummer.domain.billing.EntitlementRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {

    @Binds
    @Singleton
    abstract fun bindGuitarEngine(impl: NativeGuitarEngine): GuitarEngine

    @Binds
    @Singleton
    abstract fun bindEntitlementRepository(impl: StubEntitlementRepository): EntitlementRepository
}
