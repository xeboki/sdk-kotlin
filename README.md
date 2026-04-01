# xeboki-sdk for Kotlin / Android

Official Kotlin SDK for the [Xeboki developer API](https://developers.xeboki.com). Built with Ktor and Kotlin coroutines — idiomatic, suspend-based, and ready for Android and JVM.

[![Maven Central](https://img.shields.io/maven-central/v/com.xeboki/xeboki-sdk)](https://central.sonatype.com/artifact/com.xeboki/xeboki-sdk)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Requirements

- Kotlin 1.9+
- Android API 26+ (Android 8.0) or any JVM 11+
- Coroutines — all SDK methods are `suspend` functions

## Installation

### Gradle (Kotlin DSL)

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.xeboki:xeboki-sdk:1.0.0")
}
```

### Gradle (Groovy)

```groovy
// build.gradle
dependencies {
    implementation 'com.xeboki:xeboki-sdk:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>com.xeboki</groupId>
    <artifactId>xeboki-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```kotlin
import com.xeboki.sdk.XebokiClient

val xeboki = XebokiClient(apiKey = "xbk_live_...")

// List recent orders (suspend function — call from a coroutine or viewModelScope)
val response = xeboki.pos.listOrders(limit = 20, status = "completed")
println(response.data)

// Check rate limit after any call
xeboki.lastRateLimit?.let { rl ->
    println("${rl.remaining}/${rl.limit} requests remaining")
}

// Always close the client when done (releases Ktor resources)
xeboki.close()
```

### With ViewModelScope (Android)

```kotlin
class OrdersViewModel : ViewModel() {
    private val xeboki = XebokiClient(apiKey = BuildConfig.XEBOKI_API_KEY)

    val orders = MutableStateFlow<List<Order>>(emptyList())

    fun loadOrders() {
        viewModelScope.launch {
            try {
                val result = xeboki.pos.listOrders(limit = 50)
                orders.value = result.data
            } catch (e: XebokiError) {
                // handle error
            }
        }
    }

    override fun onCleared() {
        xeboki.close()
    }
}
```

## Authentication

API keys are available in your [Xeboki account dashboard](https://account.xeboki.com).

| Key prefix     | Environment |
|----------------|-------------|
| `xbk_live_...` | Production  |
| `xbk_test_...` | Sandbox     |

**Do not embed production API keys in APKs.** Read from `BuildConfig`, `local.properties`, or a remote configuration service. Use `xbk_test_` keys during development.

## Client Configuration

```kotlin
// Simple — API key only
val xeboki = XebokiClient(apiKey = "xbk_live_...")

// Custom base URL (e.g. self-hosted gateway)
val xeboki = XebokiClient(
    apiKey = "xbk_live_...",
    baseUrl = "https://api.yourcompany.com"
)
```

---

## Products

### `pos` — Point of Sale

Manage orders, products, inventory, customers, and sales reports.

#### Orders

```kotlin
// List orders with filters
val result = xeboki.pos.listOrders(
    limit      = 50,
    offset     = 0,
    status     = "completed",
    locationId = "loc_abc",
    startDate  = "2026-01-01",
    endDate    = "2026-03-31"
)
// result.data: List<Order>
// result.total: Int

// Get a single order
val order = xeboki.pos.getOrder(id = "ord_abc123")
println("${order.orderNumber}: ${order.total}")

// Create an order
val newOrder = xeboki.pos.createOrder(
    locationId    = "loc_abc",
    paymentMethod = "cash",
    items = listOf(
        OrderItemRequest(productId = "prod_1", quantity = 2),
        OrderItemRequest(
            productId = "prod_2",
            quantity  = 1,
            modifiers = listOf(ModifierRequest(modifierId = "mod_oat"))
        )
    ),
    customerId = "cust_xyz",
    discount   = 5.00,
    notes      = "No ice please"
)
```

**`Order` fields**

| Field           | Type     | Description                                             |
|-----------------|----------|---------------------------------------------------------|
| `id`            | `String` | Unique order ID                                         |
| `orderNumber`   | `String` | Human-readable order number                             |
| `status`        | `String` | `pending`, `processing`, `completed`, `cancelled`, `refunded` |
| `items`         | `List<OrderItem>` | Line items                                     |
| `subtotal`      | `Double` | Pre-tax, pre-discount total                             |
| `tax`           | `Double` | Tax amount                                              |
| `discount`      | `Double` | Discount amount                                         |
| `total`         | `Double` | Final charged amount                                    |
| `locationId`    | `String` | Location that processed the order                       |
| `employeeId`    | `String` | Employee who processed the order                        |
| `paymentMethod` | `String` | Payment method used                                     |
| `createdAt`     | `String` | ISO 8601 timestamp                                      |

#### Products

```kotlin
// List products
val products = xeboki.pos.listProducts(
    locationId = "loc_abc",
    isActive   = true,
    search     = "espresso"
)

// Create a product
val product = xeboki.pos.createProduct(
    name           = "Flat White",
    price          = 4.50,
    locationId     = "loc_abc",
    taxRate        = 0.10,
    trackInventory = true,
    categoryId     = "cat_coffee"
)

// Update a product
val updated = xeboki.pos.updateProduct(
    id    = "prod_abc",
    price = 4.75
)
```

#### Inventory

```kotlin
// List inventory (optional: filter to low-stock items only)
val inventory = xeboki.pos.listInventory(
    locationId   = "loc_abc",
    lowStockOnly = true
)

// Adjust a stock level
val item = xeboki.pos.updateInventory(
    id       = "inv_abc",
    quantity = 50,
    reason   = "restock",
    notes    = "Weekly delivery"
)
```

#### Customers

```kotlin
val customers = xeboki.pos.listCustomers(search = "jane", limit = 20)

val customer = xeboki.pos.createCustomer(
    name  = "Jane Doe",
    email = "jane@example.com",
    phone = "+1-555-0100"
)
```

#### Sales Report

```kotlin
val report = xeboki.pos.getSalesReport(
    startDate  = "2026-03-01",
    endDate    = "2026-03-31",
    locationId = "loc_abc"
)
println("Revenue: ${report.totalRevenue}")
println("Avg order: ${report.averageOrderValue}")
println("Top products: ${report.topProducts}")
```

---

### `chat` — Customer Support

```kotlin
// List open conversations
val conversations = xeboki.chat.listConversations(
    status  = "open",
    inboxId = "inbox_web"
)

// Send a message
val message = xeboki.chat.sendMessage(
    conversationId = "conv_abc",
    content        = "How can I help you today?"
)

// Resolve a conversation
xeboki.chat.updateConversation(id = "conv_abc", status = "resolved")

// Create a contact
val contact = xeboki.chat.createContact(
    name    = "Alex Smith",
    email   = "alex@example.com",
    company = "Acme Corp"
)

// List available agents
val agents = xeboki.chat.listAgents(isAvailable = true)

// List inboxes
val inboxes = xeboki.chat.listInboxes()
```

**Supported channels:** `web` · `email` · `sms` · `whatsapp` · `instagram` · `twitter`

---

### `link` — URL Shortener

```kotlin
// Create a short link
val link = xeboki.link.createLink(
    destinationUrl = "https://yoursite.com/campaign",
    title          = "Summer Sale",
    customCode     = "summer26",
    tags           = listOf("marketing", "q2")
)
println(link.shortUrl)   // https://xbk.io/summer26

// List links
val links = xeboki.link.listLinks(isActive = true, tag = "marketing")

// Get analytics for a link
val analytics = xeboki.link.getAnalytics(
    id        = "lnk_abc",
    startDate = "2026-03-01",
    endDate   = "2026-03-31"
)
println("Clicks: ${analytics.totalClicks}")

// Update or delete
xeboki.link.updateLink(id = "lnk_abc", isActive = false)
xeboki.link.deleteLink(id = "lnk_abc")
```

---

### `removebg` — Background Removal

```kotlin
// Submit a job
val job = xeboki.removebg.removeBackground(
    imageUrl     = "https://example.com/photo.jpg",
    outputFormat = "png"
)

// Poll until complete
val result = xeboki.removebg.getJob(jobId = job.jobId)
if (result.status == "completed") {
    println(result.resultUrl)
}
```

---

### `analytics` — Cross-Product Analytics

```kotlin
// List reports
val reports = xeboki.analytics.listReports(product = "pos")

// Run a report
val data = xeboki.analytics.getReport(
    id        = "rep_revenue_daily",
    startDate = "2026-01-01",
    endDate   = "2026-03-31",
    groupBy   = "month"
)
println(data.summary)

// Export to CSV
val export = xeboki.analytics.exportReport(
    reportId  = "rep_revenue_daily",
    format    = "csv",
    startDate = "2026-01-01",
    endDate   = "2026-03-31"
)
```

---

### `account` — Account Management

```kotlin
val account = xeboki.account.getAccount()
val usage   = xeboki.account.getUsage()
println("${usage.pos.used} / ${usage.pos.limit}")

// Create a webhook
val webhook = xeboki.account.createWebhook(
    url    = "https://yourserver.com/webhooks",
    events = listOf("order.completed", "conversation.created")
)

// Manage API keys
val newKey = xeboki.account.createApiKey(
    name   = "Android Production",
    scopes = listOf("pos:read", "pos:write")
)
println(newKey.key)   // shown only once
```

---

### `launchpad` — App Distribution

```kotlin
val apps = xeboki.launchpad.listApps()

val release = xeboki.launchpad.createRelease(
    appId        = "app_abc",
    version      = "2.4.0",
    releaseNotes = "Bug fixes.",
    downloadUrl  = "https://cdn.example.com/app-2.4.0.apk",
    platform     = "android"
)
```

---

## Error Handling

All SDK methods throw `XebokiError` on non-2xx responses.

```kotlin
import com.xeboki.sdk.XebokiError

try {
    val order = xeboki.pos.createOrder(...)
} catch (e: XebokiError) {
    println("Status:     ${e.status}")
    println("Message:    ${e.message}")
    println("Request ID: ${e.requestId}")   // include in support tickets

    if (e.status == 429) {
        val retryAfter = e.retryAfter ?: 60
        println("Rate limited — retry after ${retryAfter}s")
    }
}
```

**`XebokiError` properties**

| Property     | Type      | Description                                              |
|--------------|-----------|----------------------------------------------------------|
| `status`     | `Int`     | HTTP status code                                         |
| `message`    | `String`  | Human-readable error description                         |
| `requestId`  | `String?` | Unique request ID — include in support tickets           |
| `retryAfter` | `Int?`    | Seconds to wait before retrying (only present on 429)    |

**Common status codes**

| Status | Meaning                                           |
|--------|---------------------------------------------------|
| `400`  | Bad request — check your parameters               |
| `401`  | Invalid or missing API key                        |
| `403`  | Insufficient scope / permissions                  |
| `404`  | Resource not found                                |
| `422`  | Validation error                                  |
| `429`  | Rate limit exceeded — check `retryAfter`          |
| `500`  | Server error — retry with exponential back-off    |

---

## Rate Limiting

Each product has its own daily request quota. The SDK surfaces live counters via `lastRateLimit` after every call.

```kotlin
val orders = xeboki.pos.listOrders()

xeboki.lastRateLimit?.let { rl ->
    println("${rl.remaining} / ${rl.limit} requests remaining")
    println("Resets at ${Instant.ofEpochSecond(rl.reset.toLong())}")
}
```

**`RateLimitInfo` properties**

| Property    | Type     | Description                                    |
|-------------|----------|------------------------------------------------|
| `limit`     | `Int`    | Daily request quota for this product           |
| `remaining` | `Int`    | Requests remaining today                       |
| `reset`     | `Int`    | Unix timestamp (UTC) when the counter resets   |
| `requestId` | `String` | ID of the most recent request                  |

---

## Resource Management

`XebokiClient` holds a Ktor `HttpClient` and a coroutine engine. Call `close()` when you no longer need the client — typically in `ViewModel.onCleared()` or `Application.onTerminate()`.

```kotlin
// In ViewModel
override fun onCleared() {
    super.onCleared()
    xeboki.close()
}
```

---

## ProGuard / R8

If you use ProGuard or R8, add these rules to `proguard-rules.pro`:

```proguard
-keep class com.xeboki.sdk.** { *; }
-keepattributes *Annotation*
```

---

## Support

- **Documentation:** [developers.xeboki.com](https://developers.xeboki.com)
- **Issues:** [github.com/xeboki/sdk-kotlin/issues](https://github.com/xeboki/sdk-kotlin/issues)
- **Email:** developers@xeboki.com

Include the `requestId` from `XebokiError` in all support requests.

## License

MIT
