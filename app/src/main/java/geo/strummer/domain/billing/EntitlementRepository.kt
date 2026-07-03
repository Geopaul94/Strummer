package geo.strummer.domain.billing

import kotlinx.coroutines.flow.Flow

// Abstraction over "is the user Pro?". Billing (Play Billing / AdMob) is wired in
// a later release; for now a stub grants the honest free tier. Keeping this behind
// an interface means the UI can gate Pro-only extras today without any billing SDK.
interface EntitlementRepository {
    val isPro: Flow<Boolean>
    suspend fun refresh()
}
