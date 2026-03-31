package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable data class LaunchpadCustomer(
    val id: String, val name: String, val email: String,
    val phone: String? = null, val createdAt: String, val updatedAt: String,
)
@Serializable data class CreateLaunchpadCustomerParams(
    val name: String, val email: String, val phone: String? = null,
)
@Serializable data class Plan(
    val id: String, val name: String, val description: String? = null,
    val price: Double, val currency: String, val interval: String,
    val intervalCount: Int, val trialDays: Int, val features: List<String>, val isActive: Boolean,
)
@Serializable data class LaunchpadSubscription(
    val id: String, val customerId: String, val planId: String,
    val planName: String, val status: String,
    val currentPeriodStart: String, val currentPeriodEnd: String,
    val trialEnd: String? = null, val cancelAtPeriodEnd: Boolean, val createdAt: String,
)
@Serializable data class CreateSubscriptionParams(
    val customerId: String, val planId: String,
    val trialDays: Int? = null, val couponCode: String? = null,
)
@Serializable data class LaunchpadInvoice(
    val id: String, val customerId: String, val subscriptionId: String? = null,
    val amount: Double, val currency: String, val status: String,
    val pdfUrl: String? = null, val createdAt: String, val paidAt: String? = null,
)
@Serializable data class Coupon(
    val id: String, val code: String, val discountType: String,
    val discountValue: Double, val maxRedemptions: Int? = null,
    val timesRedeemed: Int, val expiresAt: String? = null,
    val isActive: Boolean, val createdAt: String,
)
@Serializable data class CreateCouponParams(
    val code: String, val discountType: String, val discountValue: Double,
    val maxRedemptions: Int? = null, val expiresAt: String? = null,
)
@Serializable data class LaunchpadOverview(
    val mrr: Double, val arr: Double, val activeSubscriptions: Int,
    val churnRate: Double, val newSubscriptionsThisMonth: Int,
    val cancelledThisMonth: Int, val mrrGrowthPercent: Double,
)

class LaunchpadClient internal constructor(
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

    suspend fun listCustomers(limit: Int? = null, offset: Int? = null, search: String? = null) =
        call<ListResponse<LaunchpadCustomer>>(HttpMethod.Get, "/v1/launchpad/customers",
            mapOf("limit" to limit?.toString(), "offset" to offset?.toString(), "search" to search))

    suspend fun createCustomer(params: CreateLaunchpadCustomerParams) =
        call<LaunchpadCustomer>(HttpMethod.Post, "/v1/launchpad/customers", body = params)

    suspend fun getCustomer(id: String) =
        call<LaunchpadCustomer>(HttpMethod.Get, "/v1/launchpad/customers/$id")

    suspend fun listSubscriptions(customerId: String? = null, status: String? = null) =
        call<ListResponse<LaunchpadSubscription>>(HttpMethod.Get, "/v1/launchpad/subscriptions",
            mapOf("customer_id" to customerId, "status" to status))

    suspend fun createSubscription(params: CreateSubscriptionParams) =
        call<LaunchpadSubscription>(HttpMethod.Post, "/v1/launchpad/subscriptions", body = params)

    suspend fun cancelSubscription(id: String) =
        call<Unit>(HttpMethod.Delete, "/v1/launchpad/subscriptions/$id")

    suspend fun listPlans() =
        call<ListResponse<Plan>>(HttpMethod.Get, "/v1/launchpad/plans")

    suspend fun listInvoices(customerId: String? = null, limit: Int? = null) =
        call<ListResponse<LaunchpadInvoice>>(HttpMethod.Get, "/v1/launchpad/invoices",
            mapOf("customer_id" to customerId, "limit" to limit?.toString()))

    suspend fun listCoupons() =
        call<ListResponse<Coupon>>(HttpMethod.Get, "/v1/launchpad/coupons")

    suspend fun createCoupon(params: CreateCouponParams) =
        call<Coupon>(HttpMethod.Post, "/v1/launchpad/coupons", body = params)

    suspend fun getOverview() =
        call<LaunchpadOverview>(HttpMethod.Get, "/v1/launchpad/analytics/overview")
}
