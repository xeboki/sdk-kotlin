package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable data class OrderItemModifier(val name: String, val price: Double)
@Serializable data class OrderItem(
    val productId: String, val name: String, val quantity: Int,
    val unitPrice: Double, val subtotal: Double,
    val modifiers: List<OrderItemModifier>? = null,
)
@Serializable data class Order(
    val id: String, val orderNumber: String, val status: String,
    val items: List<OrderItem>, val subtotal: Double, val tax: Double,
    val discount: Double, val total: Double, val customerId: String? = null,
    val locationId: String, val employeeId: String, val paymentMethod: String,
    val notes: String? = null, val createdAt: String, val updatedAt: String,
)
@Serializable data class CreateOrderModifierParams(val modifierId: String)
@Serializable data class CreateOrderItemParams(
    val productId: String, val quantity: Int,
    val modifiers: List<CreateOrderModifierParams>? = null,
)
@Serializable data class CreateOrderParams(
    val items: List<CreateOrderItemParams>, val customerId: String? = null,
    val locationId: String, val paymentMethod: String,
    val discount: Double? = null, val notes: String? = null,
)
@Serializable data class Product(
    val id: String, val name: String, val description: String? = null,
    val sku: String? = null, val barcode: String? = null,
    val categoryId: String? = null, val price: Double, val cost: Double? = null,
    val taxRate: Double, val imageUrl: String? = null, val isActive: Boolean,
    val trackInventory: Boolean, val locationId: String,
    val modifierGroupIds: List<String>? = null,
    val createdAt: String, val updatedAt: String,
)
@Serializable data class CreateProductParams(
    val name: String, val description: String? = null, val sku: String? = null,
    val barcode: String? = null, val categoryId: String? = null,
    val price: Double, val cost: Double? = null, val taxRate: Double? = null,
    val imageUrl: String? = null, val isActive: Boolean? = null,
    val trackInventory: Boolean? = null, val locationId: String,
    val modifierGroupIds: List<String>? = null,
)
@Serializable data class UpdateProductParams(
    val name: String? = null, val description: String? = null,
    val price: Double? = null, val cost: Double? = null,
    val taxRate: Double? = null, val isActive: Boolean? = null,
    val categoryId: String? = null, val imageUrl: String? = null,
)
@Serializable data class InventoryItem(
    val id: String, val productId: String, val productName: String,
    val locationId: String, val quantity: Int,
    val lowStockThreshold: Int? = null, val unit: String, val lastUpdated: String,
)
@Serializable data class UpdateInventoryParams(
    val quantity: Int, val reason: String? = null, val notes: String? = null,
)
@Serializable data class PosCustomer(
    val id: String, val name: String, val email: String? = null,
    val phone: String? = null, val loyaltyPoints: Int? = null,
    val totalSpend: Double? = null, val visitCount: Int? = null,
    val notes: String? = null, val createdAt: String, val updatedAt: String,
)
@Serializable data class CreatePosCustomerParams(
    val name: String, val email: String? = null,
    val phone: String? = null, val notes: String? = null,
)
@Serializable data class SalesReport(
    val locationId: String, val startDate: String, val endDate: String,
    val totalOrders: Int, val totalRevenue: Double, val totalTax: Double,
    val totalDiscount: Double, val netRevenue: Double, val averageOrderValue: Double,
)
@Serializable data class PosSession(
    val id: String, val locationId: String, val employeeId: String,
    val employeeName: String, val openedAt: String, val closedAt: String? = null,
    val status: String, val openingCash: Double, val closingCash: Double? = null,
    val totalSales: Double, val totalOrders: Int,
)
@Serializable data class ListResponse<T>(
    val data: List<T>, val total: Int, val limit: Int, val offset: Int,
)

class PosClient internal constructor(
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

    suspend fun listOrders(limit: Int? = null, offset: Int? = null, status: String? = null,
                           locationId: String? = null, customerId: String? = null,
                           startDate: String? = null, endDate: String? = null) =
        call<ListResponse<Order>>(HttpMethod.Get, "/v1/pos/orders", mapOf(
            "limit" to limit?.toString(), "offset" to offset?.toString(),
            "status" to status, "location_id" to locationId,
            "customer_id" to customerId, "start_date" to startDate, "end_date" to endDate,
        ))

    suspend fun createOrder(params: CreateOrderParams) =
        call<Order>(HttpMethod.Post, "/v1/pos/orders", body = params)

    suspend fun getOrder(id: String) =
        call<Order>(HttpMethod.Get, "/v1/pos/orders/$id")

    suspend fun listProducts(limit: Int? = null, offset: Int? = null,
                             categoryId: String? = null, locationId: String? = null,
                             isActive: Boolean? = null, search: String? = null) =
        call<ListResponse<Product>>(HttpMethod.Get, "/v1/pos/products", mapOf(
            "limit" to limit?.toString(), "offset" to offset?.toString(),
            "category_id" to categoryId, "location_id" to locationId,
            "is_active" to isActive?.toString(), "search" to search,
        ))

    suspend fun createProduct(params: CreateProductParams) =
        call<Product>(HttpMethod.Post, "/v1/pos/products", body = params)

    suspend fun updateProduct(id: String, params: UpdateProductParams) =
        call<Product>(HttpMethod.Put, "/v1/pos/products/$id", body = params)

    suspend fun listInventory(locationId: String? = null, lowStockOnly: Boolean? = null) =
        call<ListResponse<InventoryItem>>(HttpMethod.Get, "/v1/pos/inventory", mapOf(
            "location_id" to locationId, "low_stock_only" to lowStockOnly?.toString(),
        ))

    suspend fun updateInventory(id: String, params: UpdateInventoryParams) =
        call<InventoryItem>(HttpMethod.Put, "/v1/pos/inventory/$id", body = params)

    suspend fun listCustomers(search: String? = null, limit: Int? = null, offset: Int? = null) =
        call<ListResponse<PosCustomer>>(HttpMethod.Get, "/v1/pos/customers", mapOf(
            "search" to search, "limit" to limit?.toString(), "offset" to offset?.toString(),
        ))

    suspend fun createCustomer(params: CreatePosCustomerParams) =
        call<PosCustomer>(HttpMethod.Post, "/v1/pos/customers", body = params)

    suspend fun getSalesReport(startDate: String? = null, endDate: String? = null, locationId: String? = null) =
        call<SalesReport>(HttpMethod.Get, "/v1/pos/reports/sales", mapOf(
            "start_date" to startDate, "end_date" to endDate, "location_id" to locationId,
        ))

    suspend fun listSessions(locationId: String? = null, status: String? = null,
                             limit: Int? = null, offset: Int? = null) =
        call<ListResponse<PosSession>>(HttpMethod.Get, "/v1/pos/sessions", mapOf(
            "location_id" to locationId, "status" to status,
            "limit" to limit?.toString(), "offset" to offset?.toString(),
        ))
}
