package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable data class ProcessedImage(
    val id: String, val resultUrl: String, val format: String,
    val width: Int, val height: Int, val creditsUsed: Int, val createdAt: String,
)
@Serializable data class ProcessImageParams(
    val imageUrl: String? = null, val imageBase64: String? = null,
    val outputFormat: String? = null, val bgColor: String? = null,
)
@Serializable data class QuotaInfo(
    val plan: String, val creditsTotal: Int, val creditsUsed: Int,
    val creditsRemaining: Int, val resetsAt: String,
)
@Serializable data class BatchImageInput(
    val imageUrl: String? = null, val imageBase64: String? = null,
)
@Serializable data class SubmitBatchParams(
    val images: List<BatchImageInput>, val outputFormat: String? = null,
)
@Serializable data class BatchResult(
    val index: Int, val status: String,
    val resultUrl: String? = null, val error: String? = null,
)
@Serializable data class BatchJob(
    val id: String, val status: String, val totalImages: Int,
    val processedImages: Int, val failedImages: Int,
    val results: List<BatchResult>? = null,
    val createdAt: String, val completedAt: String? = null,
)

class RemoveBGClient internal constructor(
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

    suspend fun process(params: ProcessImageParams) =
        call<ProcessedImage>(HttpMethod.Post, "/v1/removebg/process", body = params)

    suspend fun getQuota() =
        call<QuotaInfo>(HttpMethod.Get, "/v1/removebg/quota")

    suspend fun submitBatch(params: SubmitBatchParams) =
        call<BatchJob>(HttpMethod.Post, "/v1/removebg/batch", body = params)

    suspend fun getBatch(id: String) =
        call<BatchJob>(HttpMethod.Get, "/v1/removebg/batch/$id")
}
