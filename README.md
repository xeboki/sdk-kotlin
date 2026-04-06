<p align="center">
  <img src="assets/xe_logo.svg" alt="Xeboki" width="120" />
</p>

<h1 align="center">Xeboki SDK for Kotlin & Android</h1>

<p align="center">Official Kotlin SDK for the <a href="https://developers.xeboki.com">Xeboki developer API</a>. Built with Ktor and Kotlin coroutines — idiomatic, suspend-based, and ready for Android and JVM.</p>

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

Generate and manage API keys at [account.xeboki.com/developer](https://account.xeboki.com/developer).

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

Build custom ordering apps, mobile storefronts, kiosk interfaces, and integrations on top of any subscriber's POS data.

#### Catalog

```kotlin
// List active products
val products = xeboki.pos.listProducts(
    locationId = "loc_abc",
    categoryId = "cat_drinks",
    isActive   = true,
    search     = "espresso",
    limit      = 100
)

// Get a single product
val product = xeboki.pos.getProduct(id = "prod_abc")
println("${product.name}: \$${product.price}")

// List categories
val categories = xeboki.pos.listCategories(
    locationId = "loc_abc",
    isActive   = true
)
```

#### Orders

```kotlin
// List orders
val result = xeboki.pos.listOrders(
    limit      = 50,
    offset     = 0,
    status     = "confirmed",  // pending|confirmed|processing|ready|completed|cancelled
    locationId = "loc_abc",
    startDate  = "2026-01-01",
    endDate    = "2026-03-31"
)
// result.data: List<Order>, result.total: Int

// Get a single order
val order = xeboki.pos.getOrder(id = "ord_abc123")
println("${order.orderNumber}: \$${order.total}, paid: \$${order.paidTotal}")

// Create an order — inventory atomically reserved on create
val newOrder = xeboki.pos.createOrder(
    locationId     = "loc_abc",
    orderType      = "pickup",          // pickup|delivery|dine_in|takeaway
    items = listOf(
        OrderItemRequest(productId = "prod_1", quantity = 2),
        OrderItemRequest(
            productId = "prod_2",
            quantity  = 1,
            modifiers = listOf(ModifierRequest(modifierId = "mod_oat"))
        )
    ),
    customerId     = "cust_xyz",
    reference      = "web-order-991",   // your external order ID (idempotency)
    notes          = "No ice please",
    tableId        = "tbl_5",
    idempotencyKey = UUID.randomUUID().toString()  // prevents duplicate orders on retry
)

// Update order status (invalid transitions return 409)
xeboki.pos.updateOrderStatus(
    id     = "ord_abc123",
    status = "confirmed",
    note   = "Confirmed by kitchen"
)

// Cancel — inventory automatically restored
xeboki.pos.updateOrderStatus(id = "ord_abc123", status = "cancelled")
```

**Order status machine:** `pending → confirmed → processing → ready → completed` (any non-terminal → `cancelled`)

**`Order` fields**

| Field          | Type     | Description                                                           |
|----------------|----------|-----------------------------------------------------------------------|
| `id`           | `String` | Unique order ID                                                       |
| `orderNumber`  | `String` | Human-readable order number                                           |
| `status`       | `String` | `pending`, `confirmed`, `processing`, `ready`, `completed`, `cancelled` |
| `orderType`    | `String` | `pickup`, `delivery`, `dine_in`, `takeaway`                           |
| `items`        | `List<OrderItem>` | Line items                                                   |
| `subtotal`     | `Double` | Pre-tax, pre-discount total                                           |
| `tax`          | `Double` | Tax amount                                                            |
| `discount`     | `Double` | Discount amount                                                       |
| `total`        | `Double` | Final charged amount                                                  |
| `paidTotal`    | `Double` | Amount paid so far                                                    |
| `reference`    | `String?`| Your external order ID                                                |
| `locationId`   | `String` | Location that processed the order                                     |
| `createdAt`    | `String` | ISO 8601 timestamp                                                    |

#### Payments

The API records payments — it does not process card charges. Collect payment in your app, then record the result.

```kotlin
// Record a full payment
val payment = xeboki.pos.payOrder(
    id        = "ord_abc123",
    method    = "card",
    amount    = 42.50,
    reference = "pi_stripe_abc"  // optional — gateway transaction ID
)

// Split payment — add partial amounts one at a time
val first = xeboki.pos.addPayment(
    orderId = "ord_abc123",
    method  = "cash",
    amount  = 20.00
)
println("Remaining: \$${first.remainingAmount}")

val second = xeboki.pos.addPayment(
    orderId   = "ord_abc123",
    method    = "card",
    amount    = 22.50,
    reference = "pi_stripe_xyz"
)
println("Fully paid: ${second.isFullyPaid}")  // true → order auto-completes

// List all payments on an order
val payments = xeboki.pos.listPayments(orderId = "ord_abc123")
```

#### Customers

```kotlin
// Search customers
val customers = xeboki.pos.listCustomers(search = "jane", limit = 20)

// Get a single customer (includes loyalty points, store credit)
val customer = xeboki.pos.getCustomer(id = "cust_abc")

// Create a customer
val newCustomer = xeboki.pos.createCustomer(
    name  = "Jane Doe",
    email = "jane@example.com",
    phone = "+1-555-0100"
)
```

#### Appointments

For service-based businesses — salons, gyms, repair shops, spas.

```kotlin
// List appointments
val appts = xeboki.pos.listAppointments(
    locationId = "loc_abc",
    status     = "confirmed",  // pending|confirmed|in_progress|completed|cancelled|no_show
    date       = "2026-04-15",
    staffId    = "staff_xyz"
)

// Book an appointment
val newAppt = xeboki.pos.createAppointment(
    locationId       = "loc_abc",
    customerId       = "cust_xyz",
    serviceId        = "prod_haircut",
    staffId          = "staff_xyz",
    startTime        = "2026-04-15T14:00:00Z",
    durationMinutes  = 60,
    notes            = "Trim only"
)

// Update appointment status
// When status → "completed", a POS order is auto-created for sales reporting
xeboki.pos.updateAppointmentStatus(id = "appt_abc", status = "confirmed")
```

#### Staff

```kotlin
// List active staff
val staff = xeboki.pos.listStaff(locationId = "loc_abc", isActive = true)

// Get a staff member
val member = xeboki.pos.getStaffMember(id = "staff_abc")
```

#### Discounts

```kotlin
// List active discount rules
val discounts = xeboki.pos.listDiscounts(locationId = "loc_abc", isActive = true)

// Validate a discount code before applying
val result = xeboki.pos.validateDiscount(
    code       = "SUMMER20",
    orderTotal = 85.00,
    locationId = "loc_abc"
)
if (result.valid) {
    println("${result.type}: ${result.value}, saves: \$${result.discountAmount}")
} else {
    println(result.reason)  // expired | not_found | minimum_not_met
}
```

#### Tables

```kotlin
// List tables
val tables = xeboki.pos.listTables(
    locationId = "loc_abc",
    status     = "available"  // available|occupied|reserved|cleaning
)

// Update table status
xeboki.pos.updateTable(id = "tbl_5", status = "occupied")
```

#### Gift Cards

```kotlin
// Look up a gift card by code
val card = xeboki.pos.getGiftCard(code = "GC-XYZ-123")
println("Balance: \$${card.balance}, active: ${card.isActive}")
```

#### Inventory

```kotlin
// List inventory (optional: low-stock only)
val inventory = xeboki.pos.listInventory(
    locationId   = "loc_abc",
    lowStockOnly = true
)

// Adjust a stock level
xeboki.pos.updateInventory(
    id       = "inv_abc",
    quantity = 50,
    reason   = "restock",
    notes    = "Weekly delivery"
)
```

#### Webhooks

```kotlin
// Register a webhook
val webhook = xeboki.pos.createWebhook(
    url    = "https://yourserver.com/xeboki/events",
    events = listOf("order.created", "order.completed", "order.cancelled")
)
println(webhook.secret)  // whsec_... — shown ONCE, store securely

// List registered webhooks
val webhooks = xeboki.pos.listWebhooks()

// Delete a webhook
xeboki.pos.deleteWebhook(id = "wh_abc123")
```

**Verifying signatures in Kotlin**

```kotlin
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun verifyWebhook(secret: String, body: String, signature: String): Boolean {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    val expected = "sha256=" + mac.doFinal(body.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return expected == signature
}
```

**Available POS events:** `order.created` · `order.updated` · `order.completed` · `order.cancelled` · `order.payment_added` · `order.paid` · `appointment.created` · `appointment.updated` · `appointment.completed` · `appointment.cancelled` · `inventory.low_stock`

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

### `ordering` — Customer Ordering

Build custom ordering apps, kiosks, and mobile storefronts on top of any
subscriber's POS catalog. Includes customer auth, real-time order tracking,
appointments, and more.

```kotlin
// Validate API key on startup
val result = xeboki.ordering.validateApiKey()

// Browse catalog
val products = xeboki.ordering.listProducts(limit = 20)
val categories = xeboki.ordering.listCategories()

// Customer login / registration
val auth = xeboki.ordering.loginCustomer(email = "...", password = "...")
xeboki.ordering.registerCustomer(email = "...", password = "...", fullName = "Jane Doe")

// Place an order
val order = xeboki.ordering.createOrder(
    orderType = "pickup",
    items = listOf(mapOf("product_id" to "prod_abc", "quantity" to 2)),
    customerId = "cust_xyz",
)

// Pay an order
val paid = xeboki.ordering.payOrder(order.id, method = "card", amount = order.total)

// Real-time tracking via Firestore (see Firestore-direct section below)
val fbConfig = xeboki.ordering.getFirebaseConfig()
```

---

### `developer` — API Keys & Webhooks

Manage API keys and webhook endpoints programmatically.
Requires a POS JWT issued to an admin-role user.

```kotlin
// ── API Keys ─────────────────────────────────────────────────────────────────

// List all keys
val keys = xeboki.developer.listApiKeys()

// Create a key — full key shown ONCE, store it securely
val created = xeboki.developer.createApiKey(
    CreateApiKeyParams(name = "Mobile Storefront", scopes = listOf("pos:read", "orders:write"))
)
println("Save this key now: ${created.key}")

// Revoke a key
xeboki.developer.revokeApiKey(keyId)

// ── Webhooks ──────────────────────────────────────────────────────────────────

// Register an endpoint
val hook = xeboki.developer.registerWebhook(
    RegisterWebhookParams(
        url    = "https://yourserver.com/webhooks/xeboki",
        events = listOf("order.created", "order.status_changed")
    )
)

// Send a test event
xeboki.developer.testWebhook(hook.id, event = "order.created")

// Delete an endpoint
xeboki.developer.deleteWebhook(hook.id)
```

**Verifying webhook signatures in Kotlin**

```kotlin
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun verifyWebhook(secret: String, rawBody: String, header: String): Boolean {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
    val expected = "sha256=" + mac.doFinal(rawBody.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return expected == header
}
```

---

### Firestore-direct path (ordering apps)

The official Xeboki Ordering App reads directly from the subscriber's Firestore.

```kotlin
// Step 1 — fetch Firebase config + custom auth token
val fbConfig = xeboki.ordering.getFirebaseConfig()

// Step 2 — initialise a secondary Firebase app
val options = FirebaseOptions.Builder()
    .setApiKey(fbConfig.apiKey)
    .setApplicationId(fbConfig.appId)
    .setProjectId(fbConfig.projectId)
    .setStorageBucket(fbConfig.storageBucket)
    .build()
FirebaseApp.initializeApp(context, options, "ordering_pro")

// Step 3 — sign in with the custom token
FirebaseAuth.getInstance(FirebaseApp.getInstance("ordering_pro"))
    .signInWithCustomToken(fbConfig.customToken!!)
    .await()

// Step 4 — real-time order tracking
FirebaseFirestore.getInstance(FirebaseApp.getInstance("ordering_pro"))
    .collection("orders").document(orderId)
    .addSnapshotListener { snap, _ ->
        println("Status: ${snap?.getString("status")}")
    }
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
