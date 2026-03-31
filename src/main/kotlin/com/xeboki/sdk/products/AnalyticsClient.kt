package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable data class AnalyticsReport(
    val id: String, val name: String, val description: String? = null,
    val product: String, val metrics: List<String>,
    val availableGranularities: List<String>, val createdAt: String,
)
@Serializable data class ExportResult(
    val exportId: String, val downloadUrl: String,
    val format: String, val expiresAt: String,
)
@Serializable data class ExportReportParams(
    val reportId: String, val format: String,
    val startDate: String? = null, val endDate: String? = null,
)

class AnalyticsClient internal constructor(
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

    suspend fun listReports(product: String? = null) =
        call<ListResponse<AnalyticsReport>>(HttpMethod.Get, "/v1/analytics/reports",
            mapOf("product" to product))

    suspend fun getReport(id: String, startDate: String? = null, endDate: String? = null,
                          granularity: String? = null) =
        call<Map<String, kotlinx.serialization.json.JsonElement>>(
            HttpMethod.Get, "/v1/analytics/reports/$id",
            mapOf("start_date" to startDate, "end_date" to endDate, "granularity" to granularity))

    suspend fun exportReport(params: ExportReportParams) =
        call<ExportResult>(HttpMethod.Post, "/v1/analytics/reports/export", body = params)
}
