package com.xeboki.sdk

import com.xeboki.sdk.products.*

/**
 * Main entry point for the Xeboki SDK.
 *
 * ```kotlin
 * val xeboki = XebokiClient("xbk_live_...")
 * val orders = xeboki.pos.listOrders()
 * println(xeboki.lastRateLimit?.remaining)
 * ```
 */
class XebokiClient(
    apiKey: String,
    baseUrl: String = "https://api.xeboki.com",
) {
    private val http = XebokiHttpClient(apiKey, baseUrl)
    private var _lastRateLimit: RateLimitInfo? = null

    /** Rate limit info from the most recent API call. */
    val lastRateLimit: RateLimitInfo? get() = _lastRateLimit

    private val rateLimitCallback: (RateLimitInfo) -> Unit = { _lastRateLimit = it }

    val pos       = PosClient(http, rateLimitCallback)
    val chat      = ChatClient(http, rateLimitCallback)
    val link      = LinkClient(http, rateLimitCallback)
    val removebg  = RemoveBGClient(http, rateLimitCallback)
    val analytics = AnalyticsClient(http, rateLimitCallback)
    val account   = AccountClient(http, rateLimitCallback)
    val launchpad = LaunchpadClient(http, rateLimitCallback)

    /** Release underlying HTTP client resources. */
    fun close() = http.close()
}
