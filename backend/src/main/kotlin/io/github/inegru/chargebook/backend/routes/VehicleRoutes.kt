package io.github.inegru.chargebook.backend.routes

import io.github.inegru.chargebook.shared.data.VolvoEnergyDataSource
import io.github.inegru.chargebook.shared.data.VolvoVehiclesDataSource
import io.github.inegru.chargebook.shared.error.DataError
import io.github.inegru.chargebook.shared.result.Result
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.vehicleRoutes(
    vehicles: VolvoVehiclesDataSource,
    energy: VolvoEnergyDataSource,
) {
    route("/api") {

        get("/vehicles") {
            handleResult(vehicles.list())
        }

        get("/snapshot/me") {
            when (val v = vehicles.list()) {
                is Result.Error -> respondNetworkError(v.error)
                is Result.Success -> {
                    val vin = v.data.firstOrNull()
                        ?: return@get call.respondText(
                            "No vehicles on this account",
                            status = HttpStatusCode.NotFound,
                        )
                    handleResult(energy.rechargeStatus(vin))
                }
            }
        }
    }
}

private suspend inline fun <reified T : Any> io.ktor.server.routing.RoutingContext.handleResult(
    result: Result<T, DataError.Network>,
) {
    when (result) {
        is Result.Success -> call.respond(result.data)
        is Result.Error -> respondNetworkError(result.error)
    }
}

private suspend fun io.ktor.server.routing.RoutingContext.respondNetworkError(
    error: DataError.Network,
) {
    val status = when (error) {
        DataError.Network.UNAUTHORIZED -> HttpStatusCode.Unauthorized
        DataError.Network.FORBIDDEN -> HttpStatusCode.Forbidden
        DataError.Network.NOT_FOUND -> HttpStatusCode.NotFound
        DataError.Network.TOO_MANY_REQUESTS -> HttpStatusCode.TooManyRequests
        DataError.Network.NO_INTERNET,
        DataError.Network.SERVICE_UNAVAILABLE -> HttpStatusCode.ServiceUnavailable
        else -> HttpStatusCode.BadGateway
    }
    call.respondText("Upstream error: $error", status = status)
}
