package io.github.inegru.chargebook.backend.location

import io.github.inegru.chargebook.shared.model.GeoPoint

/**
 * Resolves a coordinate pair to a short human-readable label
 * ("Strada Foo, Iași" or similar). Returns `null` when the lookup fails or the
 * provider has no useful label for the location.
 */
interface LocationLabeler {
    suspend fun label(point: GeoPoint): String?
}
