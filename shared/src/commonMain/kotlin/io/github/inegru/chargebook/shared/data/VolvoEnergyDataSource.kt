package io.github.inegru.chargebook.shared.data

import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.model.ChargingSnapshot
import io.github.inegru.chargebook.shared.result.Result

/**
 * Domain interface for the Volvo Energy API. Lives in `:shared` (core:domain) so
 * future Android targets can swap in their own implementation (direct Volvo call
 * or a call into this project's backend) without changing call sites.
 *
 * Implementations are responsible for mapping the Volvo DTOs to [ChargingSnapshot]
 * and for catching transport / serialization errors into [DataError.Network].
 */
interface VolvoEnergyDataSource {

    /**
     * Fetches the composite recharge-status snapshot for [vin] and maps it to a
     * domain [ChargingSnapshot] without a `sessionId` (the session detector
     * assigns one once a snapshot is associated with a session).
     */
    suspend fun rechargeStatus(vin: String): Result<ChargingSnapshot, DataError.Network>
}
