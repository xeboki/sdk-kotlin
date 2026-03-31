package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable data class UserProfile(
    val id: Int, val email: String, val fullName: String,
    val products: List<String>, val createdAt: String, val updatedAt: String,
)
@Serializable data class UpdateProfileParams(
    val fullName: String? = null, val email: String? = null,
)
@Serializable data class AccountSubscription(
    val id: String, val product: String, val plan: String, val status: String,
    val currentPeriodStart: String, val currentPeriodEnd: String,
    val cancelAtPeriodEnd: Boolean,
)
@Serializable data class AccountInvoice(
    val id: String, val amount: Double, val currency: String,
    val status: String, val description: String? = null,
    val pdfUrl: String? = null, val createdAt: String, val paidAt: String? = null,
)
@Serializable data class PaymentMethod(
    val id: String, val type: String, val brand: String? = null,
    val last4: String? = null, val expMonth: Int? = null,
    val expYear: Int? = null, val isDefault: Boolean,
)

class AccountClient internal constructor(
    private val http: XebokiHttpClient,
    private val onRateLimit: (RateLimitInfo) -> Unit,
) {
    private suspend inline fun <reified T> call(
        method: HttpMethod, path: String,
        query: Map<String, String?> = emptyMap(), body: Any? = null,
    ): T {
        val r = http.request<T>(method, path, query, body)
        onRateLimit(r.rateLimit)
        return r.data
    }

    suspend fun getProfile() = call<UserProfile>(HttpMethod.Get, "/v1/account/me")

    suspend fun updateProfile(params: UpdateProfileParams) =
        call<UserProfile>(HttpMethod.Patch, "/v1/account/me", body = params)

    suspend fun listSubscriptions() =
        call<ListResponse<AccountSubscription>>(HttpMethod.Get, "/v1/account/subscriptions")

    suspend fun listInvoices(limit: Int? = null, offset: Int? = null) =
        call<ListResponse<AccountInvoice>>(HttpMethod.Get, "/v1/account/invoices",
            mapOf("limit" to limit?.toString(), "offset" to offset?.toString()))

    suspend fun listPaymentMethods() =
        call<ListResponse<PaymentMethod>>(HttpMethod.Get, "/v1/account/payment-methods")
}
