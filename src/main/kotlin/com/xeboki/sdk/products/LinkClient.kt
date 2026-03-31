package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable data class ShortLink(
    val id: String, val shortCode: String, val shortUrl: String,
    val destinationUrl: String, val title: String? = null,
    val isActive: Boolean, val clickCount: Int,
    val createdAt: String, val updatedAt: String,
)
@Serializable data class CreateLinkParams(
    val destinationUrl: String, val title: String? = null, val customCode: String? = null,
)
@Serializable data class UpdateLinkParams(
    val destinationUrl: String? = null, val title: String? = null, val isActive: Boolean? = null,
)
@Serializable data class ClicksByDay(val date: String, val clicks: Int)
@Serializable data class TopReferrer(val referrer: String, val clicks: Int)
@Serializable data class TopCountry(val country: String, val clicks: Int)
@Serializable data class LinkAnalytics(
    val linkId: String, val totalClicks: Int, val uniqueClicks: Int,
    val clicksByDay: List<ClicksByDay>, val topReferrers: List<TopReferrer>,
    val topCountries: List<TopCountry>,
)

class LinkClient internal constructor(
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

    suspend fun listLinks(limit: Int? = null, offset: Int? = null) =
        call<ListResponse<ShortLink>>(HttpMethod.Get, "/v1/link/links",
            mapOf("limit" to limit?.toString(), "offset" to offset?.toString()))

    suspend fun createLink(params: CreateLinkParams) =
        call<ShortLink>(HttpMethod.Post, "/v1/link/links", body = params)

    suspend fun getLink(id: String) =
        call<ShortLink>(HttpMethod.Get, "/v1/link/links/$id")

    suspend fun updateLink(id: String, params: UpdateLinkParams) =
        call<ShortLink>(HttpMethod.Patch, "/v1/link/links/$id", body = params)

    suspend fun deleteLink(id: String) =
        call<Unit>(HttpMethod.Delete, "/v1/link/links/$id")

    suspend fun getAnalytics(id: String, startDate: String? = null, endDate: String? = null) =
        call<LinkAnalytics>(HttpMethod.Get, "/v1/link/analytics/$id",
            mapOf("start_date" to startDate, "end_date" to endDate))
}
