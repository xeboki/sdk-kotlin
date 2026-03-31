package com.xeboki.sdk

/**
 * Thrown when the Xeboki API returns a non-2xx response.
 *
 * @param status      HTTP status code
 * @param message     Human-readable error message from the API
 * @param requestId   Value of X-Request-Id header (for support tickets)
 * @param retryAfter  Seconds until the rate limit window resets (only on 429)
 */
class XebokiError(
    val status: Int,
    override val message: String,
    val requestId: String? = null,
    val retryAfter: Int? = null,
) : Exception(message)
