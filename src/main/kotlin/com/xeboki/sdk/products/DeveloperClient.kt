package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Models ───────────────────────────────────────────────────────────────────

@Serializable
data class ApiKey(
    val id: String,
    val name: String,
    @SerialName("key_prefix") val keyPrefix: String,
    val scopes: List<String>,
    @SerialName("location_ids") val locationIds: List<String>? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)

@Serializable
data class CreatedApiKey(
    val id: String,
    val name: String,
    /** Full key — returned ONCE at creation. Store securely. */
    val key: String,
    @SerialName("key_prefix") val keyPrefix: String,
    val scopes: List<String>,
    @SerialName("location_ids") val locationIds: List<String>? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    val warning: String = "",
)

@Serializable
data class DeveloperWebhook(
    val id: String,
    val url: String,
    val events: List<String>,
    val description: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("secret_prefix") val secretPrefix: String = "",
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_triggered_at") val lastTriggeredAt: String? = null,
    @SerialName("failure_count") val failureCount: Int = 0,
)

@Serializable
data class CreateApiKeyParams(
    val name: String,
    val scopes: List<String>,
    @SerialName("location_ids") val locationIds: List<String>? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)

@Serializable
data class RegisterWebhookParams(
    val url: String,
    val events: List<String>,
    val description: String? = null,
)

@Serializable
data class TestWebhookParams(val event: String = "order.created")

@Serializable
data class TestWebhookResponse(val status: String, val event: String, val url: String)

@Serializable
data class ScopesResponse(val scopes: List<String>)

@Serializable
data class EventsResponse(val events: List<String>)

// ─── Client ───────────────────────────────────────────────────────────────────

/**
 * Manage API keys and webhook endpoints for a subscriber.
 *
 * Requires a POS JWT issued to an admin-role user.
 * All calls are scoped to the authenticated subscriber.
 *
 * ```kotlin
 * val xeboki = XebokiClient(apiKey = "xbk_live_...")
 *
 * // List API keys
 * val keys = xeboki.developer.listApiKeys()
 *
 * // Create a key (key is shown ONCE — store securely)
 * val created = xeboki.developer.createApiKey(
 *     CreateApiKeyParams(name = "Mobile Storefront", scopes = listOf("pos:read", "orders:write"))
 * )
 *
 * // Register a webhook
 * xeboki.developer.registerWebhook(
 *     RegisterWebhookParams(url = "https://example.com/hook", events = listOf("order.created"))
 * )
 * ```
 */
class DeveloperClient(
    private val http: XebokiHttpClient,
    private val onRateLimit: (RateLimitInfo) -> Unit,
) {
    private suspend inline fun <reified T> call(
        method: HttpMethod, path: String,
        query: Map<String, String?> = emptyMap(),
        body: Any? = null,
    ): T {
        val r = http.request<T>(method, path, query, body)
        onRateLimit(r.rateLimit)
        return r.data
    }

    // ── API Keys ──────────────────────────────────────────────────────────────

    suspend fun listApiKeys(): List<ApiKey> =
        call(HttpMethod.Get, "/v1/developer/api-keys")

    suspend fun createApiKey(params: CreateApiKeyParams): CreatedApiKey =
        call(HttpMethod.Post, "/v1/developer/api-keys", body = params)

    suspend fun revokeApiKey(keyId: String): Unit =
        call(HttpMethod.Delete, "/v1/developer/api-keys/$keyId")

    // ── Webhooks ──────────────────────────────────────────────────────────────

    suspend fun listWebhooks(): List<DeveloperWebhook> =
        call(HttpMethod.Get, "/v1/developer/webhooks")

    suspend fun registerWebhook(params: RegisterWebhookParams): DeveloperWebhook =
        call(HttpMethod.Post, "/v1/developer/webhooks", body = params)

    suspend fun deleteWebhook(webhookId: String): Unit =
        call(HttpMethod.Delete, "/v1/developer/webhooks/$webhookId")

    suspend fun testWebhook(
        webhookId: String,
        event: String = "order.created",
    ): TestWebhookResponse =
        call(HttpMethod.Post, "/v1/developer/webhooks/$webhookId/test",
            body = TestWebhookParams(event = event))

    // ── Discovery ─────────────────────────────────────────────────────────────

    suspend fun listScopes(): List<String> =
        call<ScopesResponse>(HttpMethod.Get, "/v1/developer/scopes").scopes

    suspend fun listEvents(): List<String> =
        call<EventsResponse>(HttpMethod.Get, "/v1/developer/events").events
}
