package com.xeboki.sdk

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

data class RateLimitInfo(
    val limit: Int,
    val remaining: Int,
    val reset: Long,
    val requestId: String,
)

internal data class HttpResponse<T>(
    val data: T,
    val rateLimit: RateLimitInfo,
)

internal class XebokiHttpClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.xeboki.com",
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        queryParams: Map<String, String?> = emptyMap(),
        body: Any? = null,
    ): HttpResponse<T> {
        val response = client.request("$baseUrl$path") {
            this.method = method
            header("Authorization", "Bearer $apiKey")
            header("Accept", "application/json")
            queryParams.forEach { (k, v) -> if (v != null) parameter(k, v) }
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(body as Any))
            }
        }

        val requestId = response.headers["X-Request-Id"]
        val rateLimit = RateLimitInfo(
            limit     = response.headers["X-RateLimit-Limit"]?.toIntOrNull() ?: 0,
            remaining = response.headers["X-RateLimit-Remaining"]?.toIntOrNull() ?: 0,
            reset     = response.headers["X-RateLimit-Reset"]?.toLongOrNull() ?: 0L,
            requestId = requestId ?: "",
        )

        if (!response.status.isSuccess()) {
            val bodyText = response.bodyAsText()
            val detail = runCatching {
                json.parseToJsonElement(bodyText).jsonObject["detail"]?.jsonPrimitive?.content
            }.getOrNull() ?: "HTTP ${response.status.value}"

            val retryAfter = if (response.status.value == 429)
                response.headers["Retry-After"]?.toIntOrNull()
            else null

            throw XebokiError(
                status     = response.status.value,
                message    = detail,
                requestId  = requestId,
                retryAfter = retryAfter,
            )
        }

        if (response.status == HttpStatusCode.NoContent) {
            @Suppress("UNCHECKED_CAST")
            return HttpResponse(Unit as T, rateLimit)
        }

        val data = json.decodeFromString<T>(response.bodyAsText())
        return HttpResponse(data, rateLimit)
    }

    fun close() = client.close()
}
