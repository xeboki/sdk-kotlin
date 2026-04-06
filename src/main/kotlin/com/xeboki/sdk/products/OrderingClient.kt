package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Models ───────────────────────────────────────────────────────────────────

@Serializable
data class OrderingCategory(
    val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class ModifierOption(
    val id: String,
    val name: String,
    @SerialName("price_adjustment") val priceAdjustment: Double = 0.0,
    @SerialName("is_available") val isAvailable: Boolean = true,
)

@Serializable
data class ModifierGroup(
    val id: String,
    val name: String,
    val required: Boolean = false,
    @SerialName("min_selections") val minSelections: Int? = null,
    @SerialName("max_selections") val maxSelections: Int? = null,
    val options: List<ModifierOption> = emptyList(),
)

/** One selectable variant combination (e.g. "Red / L"). [price] null = inherit product price. */
@Serializable
data class ProductVariant(
    val id: String,
    val label: String,
    val attributes: Map<String, String> = emptyMap(),
    val sku: String? = null,
    val barcode: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val price: Double? = null,
    @SerialName("compare_at_price") val compareAtPrice: Double? = null,
    val stock: Int = 0,
    @SerialName("stock_by_location") val stockByLocation: Map<String, Int> = emptyMap(),
    @SerialName("sort_order") val sortOrder: Int = 0,
)

/** Defines one variant axis (e.g. Size with values S / M / L). */
@Serializable
data class VariantOption(
    val name: String,
    val values: List<String> = emptyList(),
)

@Serializable
data class OrderingProduct(
    val id: String,
    val name: String,
    val price: Double,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("track_inventory") val trackInventory: Boolean = false,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("stock_quantity") val stockQuantity: Int? = null,
    val stock: Int? = null,
    @SerialName("has_variants") val hasVariants: Boolean = false,
    @SerialName("variant_options") val variantOptions: List<VariantOption> = emptyList(),
    val variants: List<ProductVariant> = emptyList(), // populated on getProduct(), empty on listProducts()
    @SerialName("modifier_groups") val modifierGroups: List<ModifierGroup> = emptyList(),
    val tags: List<String> = emptyList(),
) {
    val inStock: Boolean get() = !trackInventory || ((stockQuantity ?: stock ?: 1) > 0)
}

@Serializable
data class OrderingCustomer(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    @SerialName("store_credit") val storeCredit: Double = 0.0,
    @SerialName("loyalty_points") val loyaltyPoints: Int = 0,
)

@Serializable
data class CustomerAuth(
    val customer: OrderingCustomer,
    val token: String,
)

@Serializable
data class OrderingLineItem(
    @SerialName("product_id") val productId: String,
    @SerialName("variant_id") val variantId: String? = null,
    @SerialName("variant_label") val variantLabel: String? = null,
    @SerialName("variant_attributes") val variantAttributes: Map<String, String>? = null,
    @SerialName("product_name") val productName: String,
    val quantity: Int,
    @SerialName("unit_price") val unitPrice: Double,
    @SerialName("total_price") val totalPrice: Double,
    @SerialName("modifier_names") val modifierNames: List<String> = emptyList(),
    val notes: String? = null,
)

@Serializable
data class OrderingOrder(
    val id: String,
    @SerialName("order_number") val orderNumber: String,
    val status: String,
    @SerialName("order_type") val orderType: String,
    val subtotal: Double,
    val tax: Double,
    val discount: Double,
    val total: Double,
    @SerialName("paid_total") val paidTotal: Double = 0.0,
    val items: List<OrderingLineItem> = emptyList(),
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("table_id") val tableId: String? = null,
    val notes: String? = null,
    val reference: String? = null,
    @SerialName("delivery_address") val deliveryAddress: String? = null,
    @SerialName("scheduled_at") val scheduledAt: String? = null,
    @SerialName("created_at") val createdAt: String,
) {
    val isActive: Boolean get() = status in listOf("pending", "confirmed", "processing", "ready")
    val isCompleted: Boolean get() = status == "completed"
    val isCancelled: Boolean get() = status == "cancelled"
}

@Serializable
data class DiscountValidation(
    val valid: Boolean,
    val type: String? = null,
    val reason: String? = null,
    val value: Double? = null,
    @SerialName("discount_amount") val discountAmount: Double? = null,
)

@Serializable
data class OrderingAppointment(
    val id: String,
    val status: String,
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    @SerialName("customer_id") val customerId: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("staff_id") val staffId: String? = null,
    @SerialName("staff_name") val staffName: String? = null,
    val notes: String? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("duration_minutes") val durationMinutes: Int = 60,
)

@Serializable
data class OrderingTable(
    val id: String,
    val name: String,
    val status: String,
    val capacity: Int? = null,
    val section: String? = null,
) {
    val isAvailable: Boolean get() = status == "available"
}

@Serializable
data class OrderingStaff(
    val id: String,
    val name: String,
    val role: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

data class OrderingListResponse<T>(
    val data: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
)

@Serializable
data class KeyValidationResult(
    val valid: Boolean,
    @SerialName("subscriber_id") val subscriberId: String,
    val subscription: KeyValidationSubscription? = null,
) {
    val subStatus: String get() = subscription?.status ?: ""
    val subPlan: String get() = subscription?.plan ?: ""
}

@Serializable
data class KeyValidationSubscription(val status: String = "", val plan: String = "")

@Serializable
data class OrderingFirebaseConfig(
    @SerialName("api_key") val apiKey: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("app_id") val appId: String,
    @SerialName("auth_domain") val authDomain: String = "",
    @SerialName("storage_bucket") val storageBucket: String = "",
    @SerialName("messaging_sender_id") val messagingSenderId: String = "",
    @SerialName("custom_token") val customToken: String? = null,
)

/** A line item passed to [OrderingClient.createOrder]. */
data class OrderItem(
    val productId: String,
    /** Required when the product has variants ([OrderingProduct.hasVariants] == true). */
    val variantId: String? = null,
    val quantity: Int,
    val modifiers: List<Map<String, Any?>> = emptyList(),
    val notes: String? = null,
) {
    fun toMap(): Map<String, Any?> = buildMap {
        put("product_id", productId)
        if (variantId != null) put("variant_id", variantId)
        put("quantity", quantity)
        if (modifiers.isNotEmpty()) put("modifiers", modifiers)
        if (notes != null) put("notes", notes)
    }
}

@Serializable
data class StoreConfig(
    @SerialName("business_type") val businessType: String = "",
    @SerialName("business_name") val businessName: String = "",
    @SerialName("currency_code") val currencyCode: String = "USD",
    @SerialName("currency_symbol") val currencySymbol: String = "$",
    val timezone: String = "UTC",
    @SerialName("tax_label") val taxLabel: String = "Tax",
    @SerialName("tax_rate") val taxRate: Double = 0.0,
    @SerialName("support_email") val supportEmail: String = "",
    @SerialName("support_phone") val supportPhone: String = "",
    val website: String = "",
)

@Serializable
data class StorefrontConfig(
    @SerialName("storefront_slug") val storefrontSlug: String? = null,
    @SerialName("is_published") val isPublished: Boolean = false,
    val theme: String = "minimal",
    @SerialName("primary_color") val primaryColor: String = "#000000",
    @SerialName("secondary_color") val secondaryColor: String = "#ffffff",
    val font: String = "inter",
    @SerialName("logo_url") val logoUrl: String? = null,
    @SerialName("favicon_url") val faviconUrl: String? = null,
    @SerialName("hero_image_url") val heroImageUrl: String? = null,
    @SerialName("hero_title") val heroTitle: String = "",
    @SerialName("hero_subtitle") val heroSubtitle: String = "",
    @SerialName("featured_category_ids") val featuredCategoryIds: List<String> = emptyList(),
    @SerialName("featured_product_ids") val featuredProductIds: List<String> = emptyList(),
    @SerialName("announcement_bar") val announcementBar: String? = null,
    @SerialName("seo_title") val seoTitle: String = "",
    @SerialName("seo_description") val seoDescription: String = "",
    @SerialName("social_links") val socialLinks: Map<String, String> = emptyMap(),
    @SerialName("custom_domain") val customDomain: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class StripePaymentIntent(
    @SerialName("client_secret") val clientSecret: String,
    @SerialName("publishable_key") val publishableKey: String,
    @SerialName("payment_intent_id") val paymentIntentId: String,
    val amount: Double,
    val currency: String,
    @SerialName("connected_account_id") val connectedAccountId: String? = null,
)

@Serializable
data class GiftCard(
    val id: String,
    val code: String,
    val balance: Double,
    @SerialName("initial_value") val initialValue: Double = 0.0,
    val currency: String = "USD",
    val status: String = "active",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("issued_at") val issuedAt: String? = null,
)

@Serializable
data class CustomerAddress(
    val id: String = "",
    val label: String? = null,
    val line1: String = "",
    val line2: String? = null,
    val city: String = "",
    val state: String? = null,
    val postcode: String = "",
    val country: String = "US",
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

// Private response wrappers for list endpoints

@Serializable
private data class FirebaseConfigResponse(
    @SerialName("firebase_config") val firebaseConfig: OrderingFirebaseConfig? = null,
    @SerialName("custom_token") val customToken: String? = null,
)

@Serializable
private data class CategoriesResponse(
    val categories: List<OrderingCategory>? = null,
    val data: List<OrderingCategory>? = null,
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
)

@Serializable
private data class ProductsResponse(
    val products: List<OrderingProduct>? = null,
    val data: List<OrderingProduct>? = null,
    val total: Int = 0,
    val limit: Int = 40,
    val offset: Int = 0,
)

@Serializable
private data class ProductResponse(val product: OrderingProduct? = null)

@Serializable
private data class OrdersResponse(
    val orders: List<OrderingOrder>? = null,
    val data: List<OrderingOrder>? = null,
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
)

@Serializable
private data class OrderResponse(val order: OrderingOrder? = null)

@Serializable
private data class TablesResponse(
    val tables: List<OrderingTable>? = null,
    val data: List<OrderingTable>? = null,
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
)

@Serializable
private data class AppointmentsResponse(
    val appointments: List<OrderingAppointment>? = null,
    val data: List<OrderingAppointment>? = null,
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
)

@Serializable
private data class AppointmentResponse(val appointment: OrderingAppointment? = null)

@Serializable
private data class StaffResponse(
    val staff: List<OrderingStaff>? = null,
    val data: List<OrderingStaff>? = null,
    val total: Int = 0,
    val limit: Int = 50,
    val offset: Int = 0,
)

@Serializable
private data class CustomerResponse(val customer: OrderingCustomer? = null)

// ─── Client ───────────────────────────────────────────────────────────────────

/**
 * Customer-facing ordering API client.
 *
 * ## Architecture note — Firestore-direct
 *
 * The official Xeboki Ordering App reads catalog, orders, and customers
 * **directly from the subscriber's Firestore** rather than going through the
 * REST API. This halves latency and removes API load at scale.
 *
 * To adopt the same pattern:
 * 1. Call [getFirebaseConfig] on startup to receive the Firebase project config
 *    + a short-lived custom auth token.
 * 2. Initialise a secondary `FirebaseApp` with the returned config.
 * 3. Sign in with `signInWithCustomToken(customToken)`.
 * 4. Read Firestore directly: `categories`, `products`, `orders`, `customers`.
 *
 * All REST methods below remain available for simpler integrations or
 * environments where Firestore is not an option.
 *
 * ```kotlin
 * val xeboki = XebokiClient("xbk_live_...")
 *
 * // Option A — Firestore-direct (recommended for real-time apps)
 * val fbConfig = xeboki.ordering.getFirebaseConfig()
 * // … initialise secondary Firebase app with fbConfig …
 *
 * // Option B — REST API
 * val products = xeboki.ordering.listProducts(limit = 20)
 * val auth = xeboki.ordering.loginCustomer(email = "...", password = "...")
 * ```
 */
class OrderingClient(
    private val http: XebokiHttpClient,
    private val onRateLimit: (RateLimitInfo) -> Unit,
) {
    // ── Startup validation ───────────────────────────────────────────────────

    /** Validates the API key and POS subscription on app startup. */
    suspend fun validateApiKey(): KeyValidationResult {
        val res = http.request<KeyValidationResult>(HttpMethod.Get, "/v1/pos/validate")
        onRateLimit(res.rateLimit)
        return res.data
    }

    // ── Firebase config ──────────────────────────────────────────────────────

    /**
     * Returns the subscriber's Firebase project config + a short-lived custom
     * auth token, enabling direct Firestore access for reads.
     *
     * Cache the result. The custom token expires in 1 hour.
     */
    suspend fun getFirebaseConfig(): OrderingFirebaseConfig {
        val res = http.request<FirebaseConfigResponse>(HttpMethod.Get, "/v1/pos/firebase-config")
        onRateLimit(res.rateLimit)
        val body = res.data
        // Merge custom_token into the config object
        val cfg = body.firebaseConfig ?: error("Missing firebase_config in response")
        return if (cfg.customToken == null && body.customToken != null)
            cfg.copy(customToken = body.customToken)
        else cfg
    }

    // ── FCM token ────────────────────────────────────────────────────────────

    /**
     * Registers a customer's FCM token for order-status push notifications.
     * Idempotent — safe to call every app launch.
     */
    suspend fun registerCustomerFcmToken(
        customerId: String,
        fcmToken: String,
        platform: String? = null,
        deviceId: String? = null,
    ) {
        val body = buildMap<String, Any?> {
            put("customer_id", customerId)
            put("fcm_token", fcmToken)
            if (platform != null) put("platform", platform)
            if (deviceId != null) put("device_id", deviceId)
        }
        val res = http.request<Unit>(HttpMethod.Post, "/v1/pos/customers/fcm-token", body = body)
        onRateLimit(res.rateLimit)
    }

    // ── Catalog ──────────────────────────────────────────────────────────────

    suspend fun listCategories(locationId: String? = null): OrderingListResponse<OrderingCategory> {
        val res = http.request<CategoriesResponse>(
            HttpMethod.Get, "/v1/pos/catalog/categories",
            queryParams = mapOf("location_id" to locationId),
        )
        onRateLimit(res.rateLimit)
        val list = res.data.categories ?: res.data.data ?: emptyList()
        return OrderingListResponse(list, res.data.total.takeIf { it > 0 } ?: list.size, res.data.limit, res.data.offset)
    }

    suspend fun listProducts(
        categoryId: String? = null,
        search: String? = null,
        locationId: String? = null,
        limit: Int = 40,
        offset: Int = 0,
    ): OrderingListResponse<OrderingProduct> {
        val res = http.request<ProductsResponse>(
            HttpMethod.Get, "/v1/pos/catalog/products",
            queryParams = mapOf(
                "category_id" to categoryId,
                "search" to search,
                "location_id" to locationId,
                "limit" to limit.toString(),
                "offset" to offset.toString(),
            ),
        )
        onRateLimit(res.rateLimit)
        val list = res.data.products ?: res.data.data ?: emptyList()
        return OrderingListResponse(list, res.data.total.takeIf { it > 0 } ?: list.size, limit, offset)
    }

    suspend fun getProduct(id: String): OrderingProduct {
        val res = http.request<ProductResponse>(HttpMethod.Get, "/v1/pos/catalog/products/$id")
        onRateLimit(res.rateLimit)
        return res.data.product ?: error("Missing product in response")
    }

    // ── Customer auth ────────────────────────────────────────────────────────

    suspend fun registerCustomer(
        email: String,
        password: String,
        fullName: String? = null,
        phone: String? = null,
    ): CustomerAuth {
        val body = buildMap<String, Any?> {
            put("email", email)
            put("password", password)
            if (fullName != null) put("full_name", fullName)
            if (phone != null) put("phone", phone)
        }
        val res = http.request<CustomerAuth>(HttpMethod.Post, "/v1/pos/customers/register", body = body)
        onRateLimit(res.rateLimit)
        return res.data
    }

    suspend fun loginCustomer(email: String, password: String): CustomerAuth {
        val res = http.request<CustomerAuth>(
            HttpMethod.Post, "/v1/pos/customers/login",
            body = mapOf("email" to email, "password" to password),
        )
        onRateLimit(res.rateLimit)
        return res.data
    }

    suspend fun getCustomer(id: String): OrderingCustomer? {
        return try {
            val res = http.request<CustomerResponse>(HttpMethod.Get, "/v1/pos/customers/$id")
            onRateLimit(res.rateLimit)
            res.data.customer
        } catch (_: Exception) {
            null
        }
    }

    // ── Discounts ────────────────────────────────────────────────────────────

    suspend fun validateDiscount(
        code: String,
        orderTotal: Double? = null,
        locationId: String? = null,
    ): DiscountValidation {
        val body = buildMap<String, Any?> {
            put("code", code)
            if (orderTotal != null) put("order_total", orderTotal)
            if (locationId != null) put("location_id", locationId)
        }
        val res = http.request<DiscountValidation>(HttpMethod.Post, "/v1/pos/discounts/validate", body = body)
        onRateLimit(res.rateLimit)
        return res.data
    }

    // ── Orders ───────────────────────────────────────────────────────────────

    suspend fun listOrders(
        customerId: String? = null,
        status: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): OrderingListResponse<OrderingOrder> {
        val res = http.request<OrdersResponse>(
            HttpMethod.Get, "/v1/pos/orders",
            queryParams = mapOf(
                "customer_id" to customerId,
                "status" to status,
                "limit" to limit.toString(),
                "offset" to offset.toString(),
            ),
        )
        onRateLimit(res.rateLimit)
        val list = res.data.orders ?: res.data.data ?: emptyList()
        return OrderingListResponse(list, res.data.total.takeIf { it > 0 } ?: list.size, limit, offset)
    }

    suspend fun getOrder(id: String): OrderingOrder {
        val res = http.request<OrderResponse>(HttpMethod.Get, "/v1/pos/orders/$id")
        onRateLimit(res.rateLimit)
        return res.data.order ?: error("Missing order in response")
    }

    suspend fun createOrder(
        orderType: String,
        items: List<OrderItem>,
        customerId: String? = null,
        notes: String? = null,
        tableId: String? = null,
        scheduledAt: String? = null,
        deliveryAddress: String? = null,
        idempotencyKey: String? = null,
        loyaltyPointsRedeemed: Int? = null,
    ): OrderingOrder {
        val body = buildMap<String, Any?> {
            put("order_type", orderType)
            put("items", items.map { it.toMap() })
            if (customerId != null) put("customer_id", customerId)
            if (!notes.isNullOrEmpty()) put("notes", notes)
            if (tableId != null) put("table_id", tableId)
            if (scheduledAt != null) put("scheduled_at", scheduledAt)
            if (deliveryAddress != null) put("delivery_address", deliveryAddress)
            if (idempotencyKey != null) put("idempotency_key", idempotencyKey)
            if (loyaltyPointsRedeemed != null && loyaltyPointsRedeemed > 0)
                put("loyalty_points_redeemed", loyaltyPointsRedeemed)
        }
        val res = http.request<OrderResponse>(HttpMethod.Post, "/v1/pos/orders", body = body)
        onRateLimit(res.rateLimit)
        return res.data.order ?: error("Missing order in response")
    }

    suspend fun payOrder(
        id: String,
        method: String,
        amount: Double,
        reference: String? = null,
    ): OrderingOrder {
        val body = buildMap<String, Any?> {
            put("method", method)
            put("amount", amount)
            if (reference != null) put("reference", reference)
        }
        val res = http.request<OrderResponse>(HttpMethod.Post, "/v1/pos/orders/$id/pay", body = body)
        onRateLimit(res.rateLimit)
        return res.data.order ?: error("Missing order in response")
    }

    // ── Tables ───────────────────────────────────────────────────────────────

    suspend fun listTables(
        locationId: String? = null,
        status: String? = null,
    ): OrderingListResponse<OrderingTable> {
        val res = http.request<TablesResponse>(
            HttpMethod.Get, "/v1/pos/tables",
            queryParams = mapOf("location_id" to locationId, "status" to status),
        )
        onRateLimit(res.rateLimit)
        val list = res.data.tables ?: res.data.data ?: emptyList()
        return OrderingListResponse(list, res.data.total.takeIf { it > 0 } ?: list.size, 50, 0)
    }

    // ── Appointments ─────────────────────────────────────────────────────────

    suspend fun listAppointments(
        customerId: String? = null,
        status: String? = null,
        date: String? = null,
        staffId: String? = null,
    ): OrderingListResponse<OrderingAppointment> {
        val res = http.request<AppointmentsResponse>(
            HttpMethod.Get, "/v1/pos/appointments",
            queryParams = mapOf(
                "customer_id" to customerId,
                "status" to status,
                "date" to date,
                "staff_id" to staffId,
            ),
        )
        onRateLimit(res.rateLimit)
        val list = res.data.appointments ?: res.data.data ?: emptyList()
        return OrderingListResponse(list, res.data.total.takeIf { it > 0 } ?: list.size, 50, 0)
    }

    suspend fun createAppointment(
        customerId: String,
        serviceId: String,
        startTime: String,
        staffId: String? = null,
        durationMinutes: Int = 60,
        notes: String? = null,
    ): OrderingAppointment {
        val body = buildMap<String, Any?> {
            put("customer_id", customerId)
            put("service_id", serviceId)
            put("start_time", startTime)
            put("duration_minutes", durationMinutes)
            if (staffId != null) put("staff_id", staffId)
            if (notes != null) put("notes", notes)
        }
        val res = http.request<AppointmentResponse>(HttpMethod.Post, "/v1/pos/appointments", body = body)
        onRateLimit(res.rateLimit)
        return res.data.appointment ?: error("Missing appointment in response")
    }

    suspend fun updateAppointmentStatus(id: String, status: String): OrderingAppointment {
        val res = http.request<AppointmentResponse>(
            HttpMethod.Patch, "/v1/pos/appointments/$id/status",
            body = mapOf("status" to status),
        )
        onRateLimit(res.rateLimit)
        return res.data.appointment ?: error("Missing appointment in response")
    }

    // ── Staff ────────────────────────────────────────────────────────────────

    suspend fun listStaff(
        locationId: String? = null,
        isActive: Boolean? = null,
    ): OrderingListResponse<OrderingStaff> {
        val res = http.request<StaffResponse>(
            HttpMethod.Get, "/v1/pos/staff",
            queryParams = mapOf("location_id" to locationId, "is_active" to isActive?.toString()),
        )
        onRateLimit(res.rateLimit)
        val list = res.data.staff ?: res.data.data ?: emptyList()
        return OrderingListResponse(list, res.data.total.takeIf { it > 0 } ?: list.size, 50, 0)
    }

    // ── Store & storefront config ────────────────────────────────────────────

    suspend fun getStoreConfig(): StoreConfig {
        val res = http.request<StoreConfig>(HttpMethod.Get, "/v1/pos/store-config")
        onRateLimit(res.rateLimit)
        return res.data
    }

    suspend fun getStorefrontConfig(): StorefrontConfig {
        val res = http.request<StorefrontConfig>(HttpMethod.Get, "/v1/pos/storefront-config")
        onRateLimit(res.rateLimit)
        return res.data
    }

    suspend fun updateStorefrontConfig(params: Map<String, Any?>): StorefrontConfig {
        val res = http.request<StorefrontConfig>(HttpMethod.Put, "/v1/pos/storefront-config", body = params)
        onRateLimit(res.rateLimit)
        return res.data
    }

    // ── Stripe ───────────────────────────────────────────────────────────────

    suspend fun createStripePaymentIntent(orderId: String): StripePaymentIntent {
        val res = http.request<StripePaymentIntent>(HttpMethod.Post, "/v1/pos/orders/$orderId/stripe/intent")
        onRateLimit(res.rateLimit)
        return res.data
    }

    suspend fun confirmStripePayment(orderId: String, paymentIntentId: String): Map<String, String> {
        @Serializable data class ConfirmResponse(
            @SerialName("order_id") val orderId: String = "",
            val status: String = "",
            @SerialName("paid_at") val paidAt: String = "",
        )
        val res = http.request<ConfirmResponse>(
            HttpMethod.Post, "/v1/pos/orders/$orderId/stripe/confirm",
            body = mapOf("payment_intent_id" to paymentIntentId),
        )
        onRateLimit(res.rateLimit)
        return mapOf("order_id" to res.data.orderId, "status" to res.data.status, "paid_at" to res.data.paidAt)
    }

    // ── Gift cards ────────────────────────────────────────────────────────────

    suspend fun getGiftCard(code: String): GiftCard? {
        return try {
            val res = http.request<GiftCard>(
                HttpMethod.Get, "/v1/pos/gift-cards/${code.uppercase()}"
            )
            onRateLimit(res.rateLimit)
            res.data
        } catch (_: Exception) { null }
    }

    // ── Product slug lookup ───────────────────────────────────────────────────

    suspend fun getProductBySlug(slug: String): OrderingProduct? {
        return try {
            val res = http.request<ProductResponse>(HttpMethod.Get, "/v1/pos/catalog/slug/$slug")
            onRateLimit(res.rateLimit)
            res.data.product
        } catch (_: Exception) { null }
    }

    // ── Customer profile update ───────────────────────────────────────────────

    suspend fun updateCustomer(customerId: String, updates: Map<String, Any?>): OrderingCustomer {
        val res = http.request<CustomerResponse>(HttpMethod.Patch, "/v1/pos/customers/$customerId", body = updates)
        onRateLimit(res.rateLimit)
        return res.data.customer ?: error("Missing customer in response")
    }

    // ── Customer address book ─────────────────────────────────────────────────

    @Serializable private data class AddressesResponse(val addresses: List<CustomerAddress> = emptyList())

    suspend fun listCustomerAddresses(customerId: String): List<CustomerAddress> {
        val res = http.request<AddressesResponse>(HttpMethod.Get, "/v1/pos/customers/$customerId/addresses")
        onRateLimit(res.rateLimit)
        return res.data.addresses
    }

    suspend fun addCustomerAddress(customerId: String, address: Map<String, Any?>): CustomerAddress {
        val res = http.request<CustomerAddress>(
            HttpMethod.Post, "/v1/pos/customers/$customerId/addresses", body = address
        )
        onRateLimit(res.rateLimit)
        return res.data
    }

    suspend fun updateCustomerAddress(
        customerId: String,
        addressId: String,
        address: Map<String, Any?>,
    ): CustomerAddress {
        val res = http.request<CustomerAddress>(
            HttpMethod.Put, "/v1/pos/customers/$customerId/addresses/$addressId", body = address
        )
        onRateLimit(res.rateLimit)
        return res.data
    }

    suspend fun deleteCustomerAddress(customerId: String, addressId: String) {
        val res = http.request<Unit>(HttpMethod.Delete, "/v1/pos/customers/$customerId/addresses/$addressId")
        onRateLimit(res.rateLimit)
    }
}
