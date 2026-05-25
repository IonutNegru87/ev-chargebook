package io.github.inegru.chargebook.backend.auth

import io.github.inegru.chargebook.backend.volvo.DEFAULT_USER
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

fun Route.authRoutes(
    oauth: VolvoOAuthClient,
    state: OAuthStateStore,
    tokens: TokenStore,
) {
    route("/auth") {

        get("/start") {
            val verifier = Pkce.generateVerifier()
            val csrf = Pkce.generateState()
            state.put(csrf, verifier)
            val url = oauth.buildAuthorizeUrl(
                state = csrf,
                codeChallenge = Pkce.challenge(verifier),
                scopes = DEFAULT_SCOPES,
            )
            call.response.header(HttpHeaders.Location, url)
            call.respond(HttpStatusCode.Found)
        }

        get("/callback") {
            val params = call.request.queryParameters
            val error = params["error"]
            if (error != null) {
                val description = params["error_description"]
                call.respondText(
                    "OAuth error: $error${description?.let { " — $it" }.orEmpty()}",
                    status = HttpStatusCode.BadRequest,
                )
                return@get
            }
            val code = params["code"]
                ?: return@get call.respondText("missing code", status = HttpStatusCode.BadRequest)
            val csrf = params["state"]
                ?: return@get call.respondText("missing state", status = HttpStatusCode.BadRequest)
            val entry = state.consume(csrf)
                ?: return@get call.respondText(
                    "unknown or expired state",
                    status = HttpStatusCode.BadRequest,
                )

            val token = oauth.exchangeCode(code, entry.verifier)
            tokens.put(
                userId = DEFAULT_USER,
                token = StoredToken(
                    vehicleVin = null,
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    expiresAt = token.expiresAt,
                ),
            )
            call.respondText(
                "Signed in. You can now call /api/snapshot/me.",
                status = HttpStatusCode.OK,
            )
        }
    }
}
