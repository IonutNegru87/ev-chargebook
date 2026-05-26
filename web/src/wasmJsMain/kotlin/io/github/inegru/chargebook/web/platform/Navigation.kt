package io.github.inegru.chargebook.web.platform

import kotlinx.browser.window

/**
 * Navigates the current browser tab to [url] (replacing the page rather than
 * opening a new one). [androidx.compose.ui.platform.LocalUriHandler.openUri]
 * defaults to `window.open(_blank)` on web, which is wrong for OAuth redirects
 * — Volvo's callback would land in a stranded tab.
 */
fun navigateSameWindow(url: String) {
    window.location.href = url
}
