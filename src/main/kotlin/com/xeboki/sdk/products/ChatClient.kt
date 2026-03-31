package com.xeboki.sdk.products

import com.xeboki.sdk.RateLimitInfo
import com.xeboki.sdk.XebokiHttpClient
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable data class Conversation(
    val id: String, val status: String, val inboxId: String,
    val contactId: String, val assigneeId: String? = null,
    val subject: String? = null, val createdAt: String, val updatedAt: String,
)
@Serializable data class CreateConversationParams(
    val inboxId: String, val contactId: String,
    val subject: String? = null, val assigneeId: String? = null,
)
@Serializable data class UpdateConversationParams(
    val status: String? = null, val assigneeId: String? = null,
)
@Serializable data class Message(
    val id: String, val conversationId: String, val content: String,
    val type: String, val authorId: String? = null,
    val authorType: String, val createdAt: String,
)
@Serializable data class SendMessageParams(
    val content: String, val type: String = "outgoing",
)
@Serializable data class Agent(
    val id: String, val name: String, val email: String,
    val role: String, val isActive: Boolean,
    val createdAt: String, val updatedAt: String,
)
@Serializable data class CreateAgentParams(
    val name: String, val email: String, val role: String = "agent",
)
@Serializable data class Contact(
    val id: String, val name: String, val email: String? = null,
    val phone: String? = null, val createdAt: String, val updatedAt: String,
)
@Serializable data class CreateContactParams(
    val name: String, val email: String? = null, val phone: String? = null,
)
@Serializable data class Inbox(
    val id: String, val name: String, val channelType: String,
    val isEnabled: Boolean, val createdAt: String,
)

class ChatClient internal constructor(
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

    suspend fun listConversations(status: String? = null, inboxId: String? = null,
                                  assigneeId: String? = null, limit: Int? = null) =
        call<ListResponse<Conversation>>(HttpMethod.Get, "/v1/chat/conversations", mapOf(
            "status" to status, "inbox_id" to inboxId,
            "assignee_id" to assigneeId, "limit" to limit?.toString(),
        ))

    suspend fun createConversation(params: CreateConversationParams) =
        call<Conversation>(HttpMethod.Post, "/v1/chat/conversations", body = params)

    suspend fun getConversation(id: String) =
        call<Conversation>(HttpMethod.Get, "/v1/chat/conversations/$id")

    suspend fun updateConversation(id: String, params: UpdateConversationParams) =
        call<Conversation>(HttpMethod.Patch, "/v1/chat/conversations/$id", body = params)

    suspend fun listMessages(conversationId: String, limit: Int? = null) =
        call<ListResponse<Message>>(HttpMethod.Get,
            "/v1/chat/conversations/$conversationId/messages",
            mapOf("limit" to limit?.toString()))

    suspend fun sendMessage(conversationId: String, params: SendMessageParams) =
        call<Message>(HttpMethod.Post,
            "/v1/chat/conversations/$conversationId/messages", body = params)

    suspend fun listAgents(limit: Int? = null) =
        call<ListResponse<Agent>>(HttpMethod.Get, "/v1/chat/agents",
            mapOf("limit" to limit?.toString()))

    suspend fun createAgent(params: CreateAgentParams) =
        call<Agent>(HttpMethod.Post, "/v1/chat/agents", body = params)

    suspend fun listContacts(search: String? = null, limit: Int? = null) =
        call<ListResponse<Contact>>(HttpMethod.Get, "/v1/chat/contacts",
            mapOf("search" to search, "limit" to limit?.toString()))

    suspend fun createContact(params: CreateContactParams) =
        call<Contact>(HttpMethod.Post, "/v1/chat/contacts", body = params)

    suspend fun getContact(id: String) =
        call<Contact>(HttpMethod.Get, "/v1/chat/contacts/$id")

    suspend fun listInboxes() =
        call<ListResponse<Inbox>>(HttpMethod.Get, "/v1/chat/inboxes")
}
