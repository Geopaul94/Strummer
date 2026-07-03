package geo.strummer.data.billing

import geo.strummer.domain.billing.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Placeholder entitlement: everything in the honest free tier is available, and
// there are no ads. When Play Billing is wired, swap this binding in the DI module
// for a real implementation — nothing else in the app changes.
@Singleton
class StubEntitlementRepository @Inject constructor() : EntitlementRepository {
    override val isPro: Flow<Boolean> = MutableStateFlow(false)
    override suspend fun refresh() { /* no-op until billing is wired */ }
}
