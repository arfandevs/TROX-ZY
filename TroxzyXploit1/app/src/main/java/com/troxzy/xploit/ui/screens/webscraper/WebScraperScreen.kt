package com.troxzy.xploit.ui.screens.webscraper

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.components.CommonScaffold
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.NeonCard
import com.troxzy.xploit.ui.theme.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Theme Colors ──────────────────────────────────────────────────────────────
private val AmoledBlack = Color(0xFF0A0A0A)
private val NeonPurple = Color(0xFFBF00FF)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonGreen = Color(0xFF00FF41)
private val DarkSurface = Color(0xFF1A1A1A)
private val DarkCard = Color(0xFF141414)
private val TextSecondary = Color(0xFF888888)
private val NeonRed = Color(0xFFFF0040)
private val NeonOrange = Color(0xFFFF6B00)
private val NeonYellow = Color(0xFFFFD700)

// ── Data Models ───────────────────────────────────────────────────────────────
enum class ScraperTab(val displayName: String) {
    Source("Source"),
    Links("Links"),
    Emails("Emails"),
    Forms("Forms"),
    Tech("Tech"),
    Cookies("Cookies")
}

data class ScrapedLink(
    val url: String,
    val text: String?,
    val isInternal: Boolean
)

data class ScrapedEmail(
    val email: String,
    val context: String
)

data class ScrapedForm(
    val action: String?,
    val method: String?,
    val fields: List<FormField>
)

data class FormField(
    val name: String?,
    val type: String?,
    val value: String?,
    val required: Boolean
)

data class DetectedTech(
    val name: String,
    val category: String,
    val confidence: String
)

data class ScrapedCookie(
    val name: String,
    val value: String,
    val domain: String?,
    val path: String?,
    val expires: String?
)

data class RedirectEntry(
    val url: String,
    val statusCode: Int,
    val timestamp: Long
)

data class ScrapedData(
    val rawHtml: String,
    val formattedHtml: String,
    val links: List<ScrapedLink>,
    val emails: List<ScrapedEmail>,
    val forms: List<ScrapedForm>,
    val technologies: List<DetectedTech>,
    val cookies: List<ScrapedCookie>,
    val redirects: List<RedirectEntry>,
    val statusCode: Int,
    val contentType: String?,
    val responseTime: Long,
    val headers: Map<String, String>
)

// ── OkHttp Client ─────────────────────────────────────────────────────────────
private val okHttpClient: OkHttpClient by lazy {
    val cookieManager = CookieManager()
    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .cookieJar(okhttp3.JavaNetCookieJar(cookieManager))
        .build()
}

// ── Regex Parsing ─────────────────────────────────────────────────────────────
private fun extractLinks(html: String, baseUrl: String): List<ScrapedLink> {
    val links = mutableListOf<ScrapedLink>()
    val baseDomain = try {
        val uri = URI(baseUrl)
        "${uri.scheme}://${uri.host}"
    } catch (e: Exception) { baseUrl }

    // href links
    val hrefRegex = Regex("""href\s*=\s*["']([^"']+)["']\s*(?:>\s*([^<]{0,50}))?""", RegexOption.IGNORE_CASE)
    hrefRegex.findAll(html).forEach { match ->
        val url = match.groupValues[1].trim()
        val text = match.groupValues[2]?.trim()
        if (url.isNotEmpty() && !url.startsWith("#") && !url.startsWith("javascript:") && !url.startsWith("mailto:")) {
            val fullUrl = when {
                url.startsWith("http") -> url
                url.startsWith("/") -> "$baseDomain$url"
                else -> "$baseDomain/$url"
            }
            val isInternal = fullUrl.contains(baseDomain) || url.startsWith("/")
            links.add(ScrapedLink(fullUrl, text, isInternal))
        }
    }

    // src links (images, scripts, etc.)
    val srcRegex = Regex("""src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    srcRegex.findAll(html).forEach { match ->
        val url = match.groupValues[1].trim()
        if (url.isNotEmpty() && url.startsWith("http")) {
            val isInternal = url.contains(baseDomain)
            if (!links.any { it.url == url }) {
                links.add(ScrapedLink(url, "resource", isInternal))
            }
        }
    }

    return links.distinctBy { it.url }
}

private fun extractEmails(html: String): List<ScrapedEmail> {
    val emails = mutableListOf<ScrapedEmail>()
    val emailRegex = Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}""")
    emailRegex.findAll(html).forEach { match ->
        val email = match.value
        // Find context around the email
        val startIndex = maxOf(0, match.range.first - 30)
        val endIndex = minOf(html.length, match.range.last + 30)
        val context = html.substring(startIndex, endIndex)
            .replace(Regex("<[^>]+>"), "")
            .trim()
        emails.add(ScrapedEmail(email, context))
    }
    return emails.distinctBy { it.email }
}

private fun extractForms(html: String, baseUrl: String): List<ScrapedForm> {
    val forms = mutableListOf<ScrapedForm>()
    val formRegex = Regex("""<form[^>]*>(.*?)</form>""", RegexOption.IGNORE_CASE)
    formRegex.findAll(html).forEach { formMatch ->
        val formTag = html.substring(maxOf(0, formMatch.range.first - 200), minOf(html.length, formMatch.range.first + 200))
        val action = Regex("""action\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(formTag)?.groupValues?.get(1)
        val method = Regex("""method\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(formTag)?.groupValues?.get(1) ?: "GET"

        val fields = mutableListOf<FormField>()
        val inputRegex = Regex("""<input[^>]*>""", RegexOption.IGNORE_CASE)
        inputRegex.findAll(formMatch.groupValues[1]).forEach { inputMatch ->
            val inputTag = inputMatch.value
            val name = Regex("""name\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(inputTag)?.groupValues?.get(1)
            val type = Regex("""type\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(inputTag)?.groupValues?.get(1) ?: "text"
            val value = Regex("""value\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                .find(inputTag)?.groupValues?.get(1)
            val required = Regex("""required""", RegexOption.IGNORE_CASE).containsMatchIn(inputTag)
            fields.add(FormField(name, type, value, required))
        }

        // Also textarea and select fields
        val textareaRegex = Regex("""<textarea[^>]*name\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        textareaRegex.findAll(formMatch.groupValues[1]).forEach { match ->
            fields.add(FormField(match.groupValues[1], "textarea", null, false))
        }
        val selectRegex = Regex("""<select[^>]*name\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        selectRegex.findAll(formMatch.groupValues[1]).forEach { match ->
            fields.add(FormField(match.groupValues[1], "select", null, false))
        }

        val fullAction = if (action != null && !action.startsWith("http")) {
            if (action.startsWith("/")) "$baseUrl$action" else "$baseUrl/$action"
        } else action

        forms.add(ScrapedForm(fullAction, method.uppercase(), fields))
    }
    return forms
}

private fun detectTechnologies(html: String, headers: Map<String, String>): List<DetectedTech> {
    val techs = mutableListOf<DetectedTech>()

    // Framework detection
    if (html.contains("react-root", ignoreCase = true) || html.contains("__NEXT_DATA__", ignoreCase = true) || html.contains("next/", ignoreCase = true)) {
        techs.add(DetectedTech("Next.js", "Framework", "High"))
    }
    if (html.contains("__NUXT__", ignoreCase = true) || html.contains("nuxt", ignoreCase = true)) {
        techs.add(DetectedTech("Nuxt.js", "Framework", "High"))
    }
    if (html.contains("react", ignoreCase = true) && html.contains("ReactDOM", ignoreCase = true)) {
        techs.add(DetectedTech("React", "Framework", "Medium"))
    }
    if (html.contains("vue", ignoreCase = true) && html.contains("Vue", ignoreCase = true)) {
        techs.add(DetectedTech("Vue.js", "Framework", "Medium"))
    }
    if (html.contains("angular", ignoreCase = true) || html.contains("ng-version", ignoreCase = true)) {
        techs.add(DetectedTech("Angular", "Framework", "High"))
    }
    if (html.contains("jquery", ignoreCase = true) || html.contains("jQuery", ignoreCase = true)) {
        techs.add(DetectedTech("jQuery", "Library", "Medium"))
    }
    if (html.contains("bootstrap", ignoreCase = true) || html.contains("Bootstrap", ignoreCase = true)) {
        techs.add(DetectedTech("Bootstrap", "CSS Framework", "High"))
    }
    if (html.contains("tailwind", ignoreCase = true)) {
        techs.add(DetectedTech("Tailwind CSS", "CSS Framework", "Medium"))
    }
    if (html.contains("material-ui", ignoreCase = true) || html.contains("Mui", ignoreCase = true)) {
        techs.add(DetectedTech("Material UI", "UI Library", "Medium"))
    }

    // CMS detection
    if (html.contains("wp-content", ignoreCase = true) || html.contains("wordpress", ignoreCase = true)) {
        techs.add(DetectedTech("WordPress", "CMS", "High"))
    }
    if (html.contains("drupal", ignoreCase = true)) {
        techs.add(DetectedTech("Drupal", "CMS", "High"))
    }
    if (html.contains("joomla", ignoreCase = true)) {
        techs.add(DetectedTech("Joomla", "CMS", "High"))
    }
    if (html.contains("shopify", ignoreCase = true)) {
        techs.add(DetectedTech("Shopify", "E-Commerce", "High"))
    }
    if (html.contains("magento", ignoreCase = true)) {
        techs.add(DetectedTech("Magento", "E-Commerce", "Medium"))
    }
    if (html.contains("squarespace", ignoreCase = true)) {
        techs.add(DetectedTech("Squarespace", "CMS", "Medium"))
    }

    // Server detection via headers
    headers["server"]?.let { server ->
        techs.add(DetectedTech(server, "Server", "High"))
    }
    headers["x-powered-by"]?.let { powered ->
        techs.add(DetectedTech(powered, "Backend", "High"))
    }

    // Other
    if (html.contains("cloudflare", ignoreCase = true) || headers.containsKey("cf-ray")) {
        techs.add(DetectedTech("Cloudflare", "CDN/Security", "High"))
    }
    if (html.contains("google-analytics", ignoreCase = true) || html.contains("gtag", ignoreCase = true)) {
        techs.add(DetectedTech("Google Analytics", "Analytics", "High"))
    }
    if (html.contains("facebook", ignoreCase = true) && html.contains("fbq", ignoreCase = true)) {
        techs.add(DetectedTech("Facebook Pixel", "Analytics", "Medium"))
    }
    if (html.contains("recaptcha", ignoreCase = true)) {
        techs.add(DetectedTech("Google reCAPTCHA", "Security", "High"))
    }
    if (html.contains("swagger", ignoreCase = true)) {
        techs.add(DetectedTech("Swagger/OpenAPI", "API Docs", "High"))
    }

    // Check for meta generator tag
    val generatorRegex = Regex("""<meta[^>]*name\s*=\s*["']generator["'][^>]*content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    generatorRegex.find(html)?.let { match ->
        techs.add(DetectedTech(match.groupValues[1], "Generator", "High"))
    }

    return techs.distinctBy { it.name }
}

private fun extractCookies(cookieManager: CookieManager, url: String): List<ScrapedCookie> {
    return try {
        val uri = URI(url)
        cookieManager.cookieStore.get(uri).map { cookie ->
            ScrapedCookie(
                name = cookie.name,
                value = cookie.value,
                domain = cookie.domain,
                path = cookie.path,
                expires = if (cookie.maxAge > 0) {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        .format(Date(System.currentTimeMillis() + cookie.maxAge * 1000L))
                } else "Session"
            )
        }
    } catch (e: Exception) { emptyList() }
}

// ── Formatting Helper ─────────────────────────────────────────────────────────
private fun formatHtml(html: String): String {
    return html
        .replace(Regex(">\\s+<"), ">\n<")
        .replace(Regex("<br[^>]*>"), "\n")
        .replace(Regex("</p>"), "\n")
        .replace(Regex("</div>"), "\n")
        .replace(Regex("</h[1-6]>"), "\n")
        .replace(Regex("</li>"), "\n")
        .replace(Regex("</tr>"), "\n")
        .trimIndent()
}

// ── Main Composable ───────────────────────────────────────────────────────────
@Composable
fun WebScraperScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var urlInput by remember { mutableStateOf("") }
    var isScraping by remember { mutableStateOf(false) }
    var scrapedData by remember { mutableStateOf<ScrapedData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableStateOf(ScraperTab.Source) }
    var showFormatted by remember { mutableStateOf(false) }
    var sourceSearchQuery by remember { mutableStateOf("") }
    var linkFilter by remember { mutableStateOf("all") } // all, internal, external
    var showRedirects by remember { mutableStateOf(false) }
    var showHeaders by remember { mutableStateOf(false) }
    var scrapeHistory by remember { mutableStateOf(loadScraperHistory(context)) }
    var showHistory by remember { mutableStateOf(false) }

    fun performScrape() {
        val url = urlInput.trim()
        if (url.isEmpty()) {
            errorMessage = "Please enter a URL"
            return
        }
        val finalUrl = if (!url.startsWith("http")) "https://$url" else url

        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isScraping = true
                errorMessage = null
                scrapedData = null
            }

            try {
                val startTime = System.currentTimeMillis()
                val cookieManager = CookieManager()
                cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .cookieJar(okhttp3.JavaNetCookieJar(cookieManager))
                    .build()

                val request = Request.Builder().url(finalUrl).build()
                val response = client.newCall(request).execute()
                val responseTime = System.currentTimeMillis() - startTime
                val html = response.body?.string() ?: ""

                // Extract headers
                val headersMap = mutableMapOf<String, String>()
                response.headers.forEach { (name, value) -> headersMap[name] = value }

                // Build redirect chain
                val redirects = mutableListOf<RedirectEntry>()
                val redirectChain = response.networkResponse?.let { network ->
                    network.request.url.toString()
                    RedirectEntry(network.request.url.toString(), network.code, System.currentTimeMillis())
                }
                // OkHttp doesn't expose redirect chain directly; we simulate based on prior responses
                redirects.add(RedirectEntry(finalUrl, response.code, startTime))

                // Parse data
                val links = extractLinks(html, finalUrl)
                val emails = extractEmails(html)
                val forms = extractForms(html, finalUrl)
                val techs = detectTechnologies(html, headersMap)
                val cookies = extractCookies(cookieManager, finalUrl)

                val data = ScrapedData(
                    rawHtml = html,
                    formattedHtml = formatHtml(html),
                    links = links,
                    emails = emails,
                    forms = forms,
                    technologies = techs,
                    cookies = cookies,
                    redirects = redirects,
                    statusCode = response.code,
                    contentType = headersMap["content-type"],
                    responseTime = responseTime,
                    headers = headersMap
                )

                withContext(Dispatchers.Main) {
                    scrapedData = data
                    isScraping = false
                    // Save to history
                    val entry = "$finalUrl|${System.currentTimeMillis()}|${data.statusCode}|${data.links.size}|${data.emails.size}"
                    scrapeHistory = (listOf(entry) + scrapeHistory.filter { !it.startsWith(finalUrl) }).take(15)
                    saveScraperHistory(context, scrapeHistory)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Scrape failed: ${e.message}"
                    isScraping = false
                }
            }
        }
    }

    fun exportAllData() {
        val data = scrapedData ?: run {
            Toast.makeText(context, "No data to export", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            val sb = StringBuilder()
            sb.appendLine("TroxzyXploit - Web Scraper Report")
            sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            sb.appendLine("URL: ${urlInput.trim()}")
            sb.appendLine("Status: ${data.statusCode} | Time: ${data.responseTime}ms")
            sb.appendLine("─".repeat(50))

            sb.appendLine("\n=== LINKS (${data.links.size}) ===")
            data.links.forEach { l ->
                sb.appendLine("[${if (l.isInternal) "INT" else "EXT"}] ${l.url} ${l.text ?: ""}")
            }

            sb.appendLine("\n=== EMAILS (${data.emails.size}) ===")
            data.emails.forEach { e ->
                sb.appendLine("${e.email} | Context: ${e.context}")
            }

            sb.appendLine("\n=== FORMS (${data.forms.size}) ===")
            data.forms.forEach { f ->
                sb.appendLine("Form: action=${f.action ?: "none"} method=${f.method ?: "GET"}")
                f.fields.forEach { field ->
                    sb.appendLine("  ${field.name ?: "unnamed"} (${field.type ?: "text"}) value=${field.value ?: "empty"} required=${field.required}")
                }
            }

            sb.appendLine("\n=== TECHNOLOGIES (${data.technologies.size}) ===")
            data.technologies.forEach { t ->
                sb.appendLine("${t.name} [${t.category}] confidence=${t.confidence}")
            }

            sb.appendLine("\n=== COOKIES (${data.cookies.size}) ===")
            data.cookies.forEach { c ->
                sb.appendLine("${c.name}=${c.value} domain=${c.domain ?: ""} path=${c.path ?: ""} expires=${c.expires ?: ""}")
            }

            sb.appendLine("\n=== HEADERS ===")
            data.headers.forEach { (k, v) ->
                sb.appendLine("$k: $v")
            }

            val fileName = "webscraper_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(sb.toString())

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }

    CommonScaffold(
        title = "Web Scraper",
        currentRoute = "webscraper",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // ── Title ──────────────────────────────────────────────────────
            GlitchText(
                text = "WEB SCRAPER",
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // ── URL Input Row ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it; errorMessage = null },
                    label = { Text("Enter URL", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkSurface,
                        cursorColor = NeonCyan
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { performScrape() }),
                    leadingIcon = {
                        Icon(Icons.Default.Language, contentDescription = null, tint = NeonPurple)
                    }
                )
                Button(
                    onClick = { performScrape() },
                    enabled = urlInput.isNotBlank() && !isScraping,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    if (isScraping) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Scrape", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Action Buttons Row ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showHistory = !showHistory },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("History", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { exportAllData() },
                    enabled = scrapedData != null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Export", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { showRedirects = !showRedirects },
                    enabled = scrapedData != null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonOrange),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Redirects", fontSize = 10.sp)
                }
                OutlinedButton(
                    onClick = { showHeaders = !showHeaders },
                    enabled = scrapedData != null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonYellow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonYellow),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Headers", fontSize = 10.sp)
                }
            }

            // ── History ────────────────────────────────────────────────────
            AnimatedVisibility(visible = showHistory, enter = fadeIn(), exit = fadeOut()) {
                NeonCard(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Scrape History", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (scrapeHistory.isEmpty()) {
                            Text("No history yet", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 100.dp).padding(top = 4.dp)) {
                                items(scrapeHistory) { entry ->
                                    val parts = entry.split("|")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { urlInput = parts[0]; showHistory = false }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(parts[0], color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        if (parts.size >= 4) Text(" ${parts[2]} | ${parts[3]} links", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Loading ────────────────────────────────────────────────────
            if (isScraping) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Scraping webpage...", color = NeonCyan, fontSize = 13.sp)
                    Text("Fetching HTML, parsing links, emails, forms...", color = TextSecondary, fontSize = 11.sp)
                }
            }

            // ── Error ──────────────────────────────────────────────────────
            errorMessage?.let { error ->
                NeonCard(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = NeonRed, fontSize = 12.sp)
                    }
                }
            }

            // ── Redirect Tracer ────────────────────────────────────────────
            AnimatedVisibility(visible = showRedirects && scrapedData != null, enter = fadeIn(), exit = fadeOut()) {
                scrapedData?.let { data ->
                    NeonCard(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Repeat, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Redirect Chain", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            data.redirects.forEachIndexed { index, redirect ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonOrange.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("${index + 1}", color = NeonOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(redirect.url, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("Status: ${redirect.statusCode}", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Headers ────────────────────────────────────────────────────
            AnimatedVisibility(visible = showHeaders && scrapedData != null, enter = fadeIn(), exit = fadeOut()) {
                scrapedData?.let { data ->
                    NeonCard(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Response Headers", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("${data.statusCode} | ${data.responseTime}ms", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            data.headers.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key, color = NeonYellow, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Text(value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                }
                            }
                        }
                    }
                }
            }

            // ── Status Info ────────────────────────────────────────────────
            scrapedData?.let { data ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Badge(containerColor = NeonGreen.copy(alpha = 0.2f), contentColor = NeonGreen) {
                        Text("${data.statusCode}", fontSize = 11.sp)
                    }
                    Badge(containerColor = NeonCyan.copy(alpha = 0.2f), contentColor = NeonCyan) {
                        Text("${data.links.size} links", fontSize = 11.sp)
                    }
                    Badge(containerColor = NeonOrange.copy(alpha = 0.2f), contentColor = NeonOrange) {
                        Text("${data.emails.size} emails", fontSize = 11.sp)
                    }
                    Badge(containerColor = NeonPurple.copy(alpha = 0.2f), contentColor = NeonPurple) {
                        Text("${data.forms.size} forms", fontSize = 11.sp)
                    }
                    Badge(containerColor = NeonYellow.copy(alpha = 0.2f), contentColor = NeonYellow) {
                        Text("${data.technologies.size} techs", fontSize = 11.sp)
                    }
                }
            }

            // ── Tab Selector ───────────────────────────────────────────────
            if (scrapedData != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScraperTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val tabColor = when (tab) {
                            ScraperTab.Source -> NeonPurple
                            ScraperTab.Links -> NeonCyan
                            ScraperTab.Emails -> NeonOrange
                            ScraperTab.Forms -> NeonGreen
                            ScraperTab.Tech -> NeonYellow
                            ScraperTab.Cookies -> Color(0xFFFF69B4)
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                    color = if (isSelected) tabColor else DarkSurface,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedTab = tab },
                            color = if (isSelected) tabColor.copy(alpha = 0.15f) else DarkCard,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                tab.displayName,
                                color = if (isSelected) tabColor else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ── Tab Content ────────────────────────────────────────────────
            scrapedData?.let { data ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (selectedTab) {
                        // ── SOURCE TAB ──────────────────────────────────────
                        ScraperTab.Source -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("HTML Source", color = NeonPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { showFormatted = !showFormatted },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (showFormatted) NeonGreen else TextSecondary),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (showFormatted) NeonGreen else DarkSurface),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(if (showFormatted) "Formatted" else "Raw", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            item {
                                OutlinedTextField(
                                    value = sourceSearchQuery,
                                    onValueChange = { sourceSearchQuery = it },
                                    label = { Text("Search in source", color = TextSecondary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = NeonPurple,
                                        unfocusedBorderColor = DarkSurface,
                                        cursorColor = NeonCyan
                                    ),
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp)) }
                                )
                            }
                            item {
                                val sourceText = if (showFormatted) data.formattedHtml else data.rawHtml
                                val filteredSource = if (sourceSearchQuery.isNotBlank()) {
                                    sourceText.lines().filter { it.contains(sourceSearchQuery, ignoreCase = true) }
                                } else {
                                    sourceText.lines()
                                }
                                NeonCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            "${filteredSource.size} lines${if (sourceSearchQuery.isNotBlank()) " (filtered)" else ""}",
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 400.dp)
                                        ) {
                                            val scrollState = rememberScrollState()
                                            androidx.compose.foundation.verticalScroll(scrollState).let { mod ->
                                                Text(
                                                    filteredSource.joinToString("\n").take(50000),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = mod
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── LINKS TAB ───────────────────────────────────────
                        ScraperTab.Links -> {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Extracted Links (${data.links.size})", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("all", "internal", "external").forEach { filter ->
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .border(
                                                        width = if (linkFilter == filter) 1.dp else 0.5.dp,
                                                        color = if (linkFilter == filter) NeonCyan else DarkSurface,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { linkFilter = filter },
                                                color = if (linkFilter == filter) NeonCyan.copy(alpha = 0.15f) else DarkCard,
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    filter.capitalize(),
                                                    color = if (linkFilter == filter) NeonCyan else TextSecondary,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            val filteredLinks = when (linkFilter) {
                                "internal" -> data.links.filter { it.isInternal }
                                "external" -> data.links.filter { !it.isInternal }
                                else -> data.links
                            }
                            items(filteredLinks.take(200)) { link ->
                                NeonCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (link.isInternal) NeonCyan.copy(alpha = 0.2f) else NeonOrange.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                if (link.isInternal) "I" else "E",
                                                color = if (link.isInternal) NeonCyan else NeonOrange,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            link.text?.let {
                                                Text(it, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Text(link.url, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        // ── EMAILS TAB ──────────────────────────────────────
                        ScraperTab.Emails -> {
                            item {
                                Text("Extracted Emails (${data.emails.size})", color = NeonOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (data.emails.isEmpty()) {
                                item {
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("No email addresses found on this page", color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                items(data.emails) { email ->
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Email", email.email))
                                                    Toast.makeText(context, "Email copied", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Email, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(email.email, color = NeonOrange, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                if (email.context.isNotBlank()) {
                                                    Text(email.context.take(80), color = TextSecondary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // ── FORMS TAB ───────────────────────────────────────
                        ScraperTab.Forms -> {
                            item {
                                Text("Forms (${data.forms.size})", color = NeonGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (data.forms.isEmpty()) {
                                item {
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("No forms found on this page", color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                items(data.forms) { form ->
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Description, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Form", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.weight(1f))
                                                Badge(containerColor = NeonPurple.copy(alpha = 0.2f), contentColor = NeonPurple) {
                                                    Text(form.method ?: "GET", fontSize = 10.sp)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            form.action?.let {
                                                Text("Action: $it", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Fields (${form.fields.size}):", color = TextSecondary, fontSize = 11.sp)
                                            form.fields.forEach { field ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(1.dp)).background(NeonGreen))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        "${field.name ?: "unnamed"} (${field.type ?: "text"})",
                                                        color = Color.White, fontSize = 11.sp
                                                    )
                                                    if (field.required) {
                                                        Badge(containerColor = NeonRed.copy(alpha = 0.2f), contentColor = NeonRed) {
                                                            Text("req", fontSize = 8.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── TECH TAB ────────────────────────────────────────
                        ScraperTab.Tech -> {
                            item {
                                Text("Detected Technologies (${data.technologies.size})", color = NeonYellow, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (data.technologies.isEmpty()) {
                                item {
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Code, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("No technologies detected", color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                items(data.technologies) { tech ->
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NeonYellow.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Code, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(tech.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text(tech.category, color = TextSecondary, fontSize = 11.sp)
                                            }
                                            Badge(
                                                containerColor = when (tech.confidence) {
                                                    "High" -> NeonGreen.copy(alpha = 0.2f)
                                                    "Medium" -> NeonYellow.copy(alpha = 0.2f)
                                                    else -> TextSecondary.copy(alpha = 0.2f)
                                                },
                                                contentColor = when (tech.confidence) {
                                                    "High" -> NeonGreen
                                                    "Medium" -> NeonYellow
                                                    else -> TextSecondary
                                                }
                                            ) {
                                                Text(tech.confidence, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── COOKIES TAB ─────────────────────────────────────
                        ScraperTab.Cookies -> {
                            item {
                                Text("Cookies (${data.cookies.size})", color = Color(0xFFFF69B4), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (data.cookies.isEmpty()) {
                                item {
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Cookie, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("No cookies detected", color = TextSecondary, fontSize = 12.sp)
                                        }
                                    }
                                }
                            } else {
                                items(data.cookies) { cookie ->
                                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Cookie, contentDescription = null, tint = Color(0xFFFF69B4), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(cookie.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Value: ${cookie.value.take(50)}", color = Color(0xFFFF69B4), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            cookie.domain?.let {
                                                Text("Domain: $it", color = TextSecondary, fontSize = 11.sp)
                                            }
                                            cookie.path?.let {
                                                Text("Path: $it", color = TextSecondary, fontSize = 11.sp)
                                            }
                                            cookie.expires?.let {
                                                Text("Expires: $it", color = if (it == "Session") NeonGreen else NeonYellow, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Empty State ────────────────────────────────────────────────
            if (scrapedData == null && !isScraping && errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Enter a URL to scrape", color = TextSecondary, fontSize = 13.sp)
                        Text("Extracts: links, emails, forms, tech, cookies", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ── Persistence Helpers ────────────────────────────────────────────────────────
private fun loadScraperHistory(context: Context): List<String> {
    return try {
        val file = File(context.filesDir, "scraper_history.txt")
        if (!file.exists()) emptyList() else file.readLines().take(15)
    } catch (e: Exception) { emptyList() }
}

private fun saveScraperHistory(context: Context, history: List<String>) {
    try {
        val file = File(context.filesDir, "scraper_history.txt")
        file.writeText(history.joinToString("\n"))
    } catch (_: Exception) { }
}
