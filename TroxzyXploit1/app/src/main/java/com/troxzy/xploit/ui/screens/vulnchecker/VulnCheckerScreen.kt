package com.troxzy.xploit.ui.screens.vulnchecker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.components.CommonScaffold
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.NeonCard
import com.troxzy.xploit.ui.theme.AMOLEDBlack
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.DarkElevated
import com.troxzy.xploit.ui.theme.DarkSurface
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext

enum class Severity(val color: Color, val label: String) {
    CRITICAL(Color(0xFFFF1744), "Critical"),
    HIGH(Color(0xFFFF9100), "High"),
    MEDIUM(Color(0xFFFFEA00), "Medium"),
    LOW(Color(0xFF00E676), "Low"),
    INFO(Color(0xFF2979FF), "Info")
}

data class VulnFinding(
    val name: String,
    val severity: Severity,
    val description: String,
    val recommendation: String
)

data class CmsDetection(
    val cms: String,
    val confidence: Int,
    val version: String,
    val indicators: List<String>
)

data class SecurityHeaderResult(
    val header: String,
    val present: Boolean,
    val value: String,
    val recommendation: String
)

data class SslInfo(
    val subject: String,
    val issuer: String,
    val validFrom: String,
    val validTo: String,
    val protocol: String,
    val isExpired: Boolean
)

data class VulnScanResult(
    val findings: List<VulnFinding>,
    val cmsDetection: CmsDetection?,
    val securityHeaders: List<SecurityHeaderResult>,
    val sslInfo: SslInfo?
)

object CmsDetector {
    private val cmsSignatures = mapOf(
        "WordPress" to listOf(
            "/wp-content/", "/wp-includes/", "wp-json", "wp-login.php",
            "<meta name=\"generator\" content=\"WordPress"
        ),
        "Joomla" to listOf(
            "/media/jui/", "/components/com_", "Joomla!",
            "<meta name=\"generator\" content=\"Joomla"
        ),
        "Drupal" to listOf(
            "/misc/drupal.js", "/sites/default/", "Drupal.settings",
            "<meta name=\"Generator\" content=\"Drupal"
        ),
        "Magento" to listOf(
            "/skin/frontend/", "/js/mage/", "Mage.Cookies",
            "Magento Commerce"
        ),
        "Shopify" to listOf(
            "cdn.shopify.com", "Shopify.theme", "shopify.com/s/"
        ),
        "Laravel" to listOf(
            "laravel_session", "XSRF-TOKEN", "laravel"
        ),
        "Django" to listOf(
            "csrftoken", "django", "X-Frame-Options"
        ),
        "React" to listOf(
            "__NEXT_DATA__", "_next/", "react", "reactjs"
        ),
        "Angular" to listOf(
            "ng-version", "angular", "ng-app"
        ),
        "Vue.js" to listOf(
            "data-v-", "vue", "__vue__"
        )
    )

    fun detectCms(html: String, url: String): CmsDetection? {
        var bestMatch: Pair<String, Int>? = null
        var bestIndicators = listOf<String>()

        for ((cms, indicators) in cmsSignatures) {
            val matched = indicators.filter { indicator ->
                html.contains(indicator, ignoreCase = true)
            }
            if (matched.isNotEmpty()) {
                val confidence = (matched.size * 100 / indicators.size).coerceAtMost(100)
                if (bestMatch == null || confidence > bestMatch.second) {
                    bestMatch = cms to confidence
                    bestIndicators = matched
                }
            }
        }

        val version = extractVersion(html, bestMatch?.first)

        return bestMatch?.let { (cms, confidence) ->
            CmsDetection(cms, confidence, version, bestIndicators)
        }
    }

    private fun extractVersion(html: String, cms: String?): String {
        if (cms == null) return "Unknown"
        val versionRegex = when (cms) {
            "WordPress" -> Regex("""WordPress\s+([\d.]+)""", RegexOption.IGNORE_CASE)
            "Joomla" -> Regex("""Joomla!\s*([\d.]+)""", RegexOption.IGNORE_CASE)
            "Drupal" -> Regex("""Drupal\s+([\d.]+)""", RegexOption.IGNORE_CASE)
            else -> Regex("""version\s*[:=]\s*"?([\d.]+)"?""", RegexOption.IGNORE_CASE)
        }
        return versionRegex.find(html)?.groupValues?.getOrNull(1) ?: "Unknown"
    }
}

object SecurityHeaderChecker {
    private val importantHeaders = listOf(
        "Content-Security-Policy",
        "Strict-Transport-Security",
        "X-Frame-Options",
        "X-Content-Type-Options",
        "X-XSS-Protection",
        "Referrer-Policy",
        "Permissions-Policy",
        "Feature-Policy",
        "Cache-Control",
        "X-Permitted-Cross-Domain-Policies"
    )

    private val recommendations = mapOf(
        "Content-Security-Policy" to "Implement CSP to prevent XSS and data injection attacks. Define allowed sources for scripts, styles, and other resources.",
        "Strict-Transport-Security" to "Enable HSTS to force HTTPS connections. Set max-age to at least 31536000 seconds and include subdomains.",
        "X-Frame-Options" to "Set to DENY or SAMEORIGIN to prevent clickjacking attacks via iframe embedding.",
        "X-Content-Type-Options" to "Set to 'nosniff' to prevent MIME type sniffing which can lead to security bypasses.",
        "X-XSS-Protection" to "Enable XSS filtering. Set to '1; mode=block' to prevent rendering of detected XSS attacks.",
        "Referrer-Policy" to "Set to 'strict-origin-when-cross-origin' or 'no-referrer' to control referrer information leakage.",
        "Permissions-Policy" to "Define which browser features and APIs can be used. Restrict access to sensitive features like camera, microphone, geolocation.",
        "Feature-Policy" to "Deprecated in favor of Permissions-Policy. Migrate to Permissions-Policy header.",
        "Cache-Control" to "Set appropriate caching directives for sensitive pages. Use 'no-store' for authenticated content.",
        "X-Permitted-Cross-Domain-Policies" to "Set to 'none' to prevent cross-domain data loading from Flash/PDF."
    )

    fun checkHeaders(headers: Map<String, String>): List<SecurityHeaderResult> {
        return importantHeaders.map { header ->
            val value = headers[header.lowercase()] ?: headers[header] ?: ""
            SecurityHeaderResult(
                header = header,
                present = value.isNotEmpty(),
                value = value,
                recommendation = recommendations[header] ?: "Review this security header."
            )
        }
    }
}

object SslChecker {
    fun checkSsl(urlStr: String): SslInfo? {
        return try {
            val url = URL(urlStr)
            if (url.protocol != "https") return null

            val sslContext = SSLContext.getDefault()
            val socket = sslContext.socketFactory.createSocket(url.host, url.port.let { if (it == -1) 443 else it }) as javax.net.ssl.SSLSocket
            socket.startHandshake()
            val certs = socket.session.peerCertificates
            socket.close()

            if (certs.isNotEmpty()) {
                val cert = certs[0] as X509Certificate
                val now = System.currentTimeMillis()
                SslInfo(
                    subject = cert.subjectX500Principal.name,
                    issuer = cert.issuerX500Principal.name,
                    validFrom = cert.notBefore.toString(),
                    validTo = cert.notAfter.toString(),
                    protocol = socket.session.protocol,
                    isExpired = now > cert.notAfter.time || now < cert.notBefore.time
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VulnCheckerScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var targetUrl by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<VulnScanResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scanHistory by remember { mutableStateOf<List<VulnScanResult>>(emptyList()) }
    var expandedSection by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun performScan() {
        var url = targetUrl.trim()
        if (url.isEmpty()) {
            errorMessage = "Please enter a URL or IP address"
            return
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        errorMessage = null
        isScanning = true
        scanResult = null

        try {
            withContext(Dispatchers.IO) {
                val findings = mutableListOf<VulnFinding>()
                val headersMap = mutableMapOf<String, String>()

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val html = response.body?.string() ?: ""

                response.headers.forEach { (name, value) ->
                    headersMap[name.lowercase()] = value
                }

                val cmsDetection = CmsDetector.detectCms(html, url)
                val securityHeaders = SecurityHeaderChecker.checkHeaders(headersMap)
                val sslInfo = SslChecker.checkSsl(url)

                if (!headersMap.containsKey("content-security-policy")) {
                    findings.add(VulnFinding(
                        name = "Missing Content-Security-Policy",
                        severity = Severity.HIGH,
                        description = "The Content-Security-Policy header is not set. This leaves the application vulnerable to Cross-Site Scripting (XSS) and other injection attacks.",
                        recommendation = "Implement a strict CSP that defines allowed sources for scripts, styles, and other resources."
                    ))
                }

                if (!headersMap.containsKey("strict-transport-security")) {
                    findings.add(VulnFinding(
                        name = "Missing HSTS Header",
                        severity = Severity.HIGH,
                        description = "Strict-Transport-Security header is not present. Users may be vulnerable to downgrade attacks from HTTPS to HTTP.",
                        recommendation = "Enable HSTS with a max-age of at least 31536000 seconds and include subdomains."
                    ))
                }

                if (!headersMap.containsKey("x-frame-options")) {
                    findings.add(VulnFinding(
                        name = "Missing X-Frame-Options",
                        severity = Severity.MEDIUM,
                        description = "The X-Frame-Options header is not set, making the application vulnerable to clickjacking attacks.",
                        recommendation = "Set X-Frame-Options to DENY or SAMEORIGIN."
                    ))
                }

                if (!headersMap.containsKey("x-content-type-options")) {
                    findings.add(VulnFinding(
                        name = "Missing X-Content-Type-Options",
                        severity = Severity.MEDIUM,
                        description = "The X-Content-Type-Options header is not set. Browsers may MIME-sniff the content type, which can lead to security bypasses.",
                        recommendation = "Set X-Content-Type-Options to 'nosniff'."
                    ))
                }

                if (headersMap.containsKey("server")) {
                    val serverVal = headersMap["server"] ?: ""
                    if (serverVal.contains(Regex("""[\d.]+"""))) {
                        findings.add(VulnFinding(
                            name = "Server Version Disclosure",
                            severity = Severity.LOW,
                            description = "The Server header reveals version information: '$serverVal'. This helps attackers identify known vulnerabilities.",
                            recommendation = "Configure the server to hide version information in the Server header."
                        ))
                    }
                }

                if (headersMap.containsKey("x-powered-by")) {
                    val poweredBy = headersMap["x-powered-by"] ?: ""
                    findings.add(VulnFinding(
                        name = "Technology Disclosure via X-Powered-By",
                        severity = Severity.LOW,
                        description = "The X-Powered-By header reveals: '$poweredBy'. This information can help attackers target specific technologies.",
                        recommendation = "Remove or disable the X-Powered-By header."
                    ))
                }

                if (cmsDetection != null) {
                    findings.add(VulnFinding(
                        name = "CMS Detected: ${cmsDetection.cms}",
                        severity = Severity.INFO,
                        description = "Detected ${cmsDetection.cms} (version: ${cmsDetection.version}) with ${cmsDetection.confidence}% confidence. Indicators: ${cmsDetection.indicators.joinToString(", ")}",
                        recommendation = "Ensure the CMS and all plugins are kept up to date. Remove version identifiers from public pages."
                    ))
                }

                if (html.contains("debug", ignoreCase = true) || html.contains("stack trace", ignoreCase = true)) {
                    findings.add(VulnFinding(
                        name = "Debug Information Exposed",
                        severity = Severity.CRITICAL,
                        description = "The application appears to expose debug information or stack traces, which can reveal sensitive internal details.",
                        recommendation = "Disable debug mode in production. Ensure error pages do not reveal stack traces or internal paths."
                    ))
                }

                if (html.contains("../") || html.contains("..\\")) {
                    findings.add(VulnFinding(
                        name = "Potential Path Traversal Indicators",
                        severity = Severity.CRITICAL,
                        description = "Page content contains path traversal patterns. This may indicate the application is vulnerable to directory traversal attacks.",
                        recommendation = "Validate and sanitize all file path inputs. Use allowlists for permitted file access."
                    ))
                }

                if (sslInfo != null && sslInfo.isExpired) {
                    findings.add(VulnFinding(
                        name = "SSL Certificate Expired or Invalid",
                        severity = Severity.CRITICAL,
                        description = "The SSL certificate is expired or not yet valid. This breaks encrypted communication and enables man-in-the-middle attacks.",
                        recommendation = "Renew the SSL certificate immediately and ensure proper certificate chain configuration."
                    ))
                }

                if (headersMap.containsKey("set-cookie")) {
                    val cookieVal = headersMap["set-cookie"] ?: ""
                    if (!cookieVal.contains("secure", ignoreCase = true)) {
                        findings.add(VulnFinding(
                            name = "Cookie Without Secure Flag",
                            severity = Severity.MEDIUM,
                            description = "A cookie is set without the Secure flag, meaning it can be transmitted over unencrypted HTTP connections.",
                            recommendation = "Set the Secure flag on all cookies to ensure they are only sent over HTTPS."
                        ))
                    }
                    if (!cookieVal.contains("httponly", ignoreCase = true)) {
                        findings.add(VulnFinding(
                            name = "Cookie Without HttpOnly Flag",
                            severity = Severity.MEDIUM,
                            description = "A cookie is set without the HttpOnly flag, making it accessible to JavaScript and vulnerable to XSS-based cookie theft.",
                            recommendation = "Set the HttpOnly flag on all cookies that don't need JavaScript access."
                        ))
                    }
                }

                val missingHeaders = securityHeaders.count { !it.present }
                if (missingHeaders > 0) {
                    findings.add(VulnFinding(
                        name = "$missingHeaders Security Headers Missing",
                        severity = if (missingHeaders > 5) Severity.HIGH else Severity.MEDIUM,
                        description = "Out of ${securityHeaders.size} important security headers checked, $missingHeaders are missing. This reduces the application's defense-in-depth posture.",
                        recommendation = "Review and implement all missing security headers according to OWASP best practices."
                    ))
                }

                if (findings.isEmpty()) {
                    findings.add(VulnFinding(
                        name = "No Major Vulnerabilities Detected",
                        severity = Severity.INFO,
                        description = "Basic checks did not identify significant vulnerabilities. This does not guarantee the application is secure.",
                        recommendation = "Consider performing a comprehensive penetration test for deeper security analysis."
                    ))
                }

                scanResult = VulnScanResult(findings, cmsDetection, securityHeaders, sslInfo)
            }
        } catch (e: Exception) {
            errorMessage = "Scan error: ${e.message}"
        }
        isScanning = false
    }

    fun exportReport(): String {
        val result = scanResult ?: return "No scan results to export"
        val sb = StringBuilder()
        sb.append("Vulnerability Scan Report\n")
        sb.append("Target: $targetUrl\n")
        sb.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
        sb.append("=".repeat(60)).append("\n\n")

        result.cmsDetection?.let { cms ->
            sb.append("CMS Detection\n")
            sb.append("-".repeat(30)).append("\n")
            sb.append("CMS: ${cms.cms}\n")
            sb.append("Version: ${cms.version}\n")
            sb.append("Confidence: ${cms.confidence}%\n")
            sb.append("Indicators: ${cms.indicators.joinToString(", ")}\n\n")
        }

        sb.append("Security Headers\n")
        sb.append("-".repeat(30)).append("\n")
        result.securityHeaders.forEach { h ->
            sb.append("${h.header}: ${if (h.present) "Present" else "MISSING"}\n")
        }
        sb.append("\n")

        result.sslInfo?.let { ssl ->
            sb.append("SSL/TLS Information\n")
            sb.append("-".repeat(30)).append("\n")
            sb.append("Subject: ${ssl.subject}\n")
            sb.append("Issuer: ${ssl.issuer}\n")
            sb.append("Valid From: ${ssl.validFrom}\n")
            sb.append("Valid To: ${ssl.validTo}\n")
            sb.append("Protocol: ${ssl.protocol}\n")
            sb.append("Expired: ${ssl.isExpired}\n\n")
        }

        sb.append("Findings\n")
        sb.append("-".repeat(30)).append("\n")
        result.findings.forEach { f ->
            sb.append("[${f.severity.label}] ${f.name}\n")
            sb.append("  Description: ${f.description}\n")
            sb.append("  Recommendation: ${f.recommendation}\n\n")
        }
        return sb.toString()
    }

    CommonScaffold(
        title = "Vulnerability Checker",
        currentRoute = "vuln_checker",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AMOLEDBlack)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlitchText(
                text = "VULNERABILITY CHECKER",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
            )

            OutlinedTextField(
                value = targetUrl,
                onValueChange = { targetUrl = it },
                label = { Text("URL / IP Address", color = Color.Gray) },
                placeholder = { Text("https://example.com", color = Color.DarkGray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = NeonCyan,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = DarkElevated,
                    cursorColor = NeonCyan
                ),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                singleLine = true
            )

            Button(
                onClick = {
                    if (!isScanning) {
                        kotlinx.coroutines.MainScope().apply {
                            kotlinx.coroutines.launch { performScan() }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) DarkElevated else NeonPurple,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = NeonCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning...", color = Color.White, fontSize = 14.sp)
                } else {
                    Icon(Icons.Filled.BugReport, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START SCAN", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            errorMessage?.let { error ->
                Text(
                    error,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                )
            }

            scanResult?.let { result ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${result.findings.size} findings",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = {
                            val report = exportReport()
                            android.util.Log.d("VulnChecker", "Export: ${report.take(200)}")
                        }) {
                            Icon(Icons.Filled.Download, contentDescription = "Export", tint = NeonGreen)
                        }
                        IconButton(onClick = {
                            scanHistory = scanHistory + result
                        }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save", tint = NeonCyan)
                        }
                    }
                }

                result.cmsDetection?.let { cms ->
                    NeonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedSection = if (expandedSection == "cms") null else "cms"
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Shield,
                                        contentDescription = "CMS",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CMS Detection", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("${cms.confidence}%", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Detected: ${cms.cms}", color = Color.White, fontSize = 12.sp)
                            Text("Version: ${cms.version}", color = Color.Gray, fontSize = 11.sp)
                            if (expandedSection == "cms") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Indicators:", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                cms.indicators.forEach { indicator ->
                                    Text("  • $indicator", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                result.sslInfo?.let { ssl ->
                    NeonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedSection = if (expandedSection == "ssl") null else "ssl"
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SSL/TLS Info", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                if (ssl.isExpired) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.Red.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("EXPIRED", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .background(NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                            .border(1.dp, NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("VALID", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            if (expandedSection == "ssl") {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Subject: ${ssl.subject}", color = Color.Gray, fontSize = 11.sp)
                                Text("Issuer: ${ssl.issuer}", color = Color.Gray, fontSize = 11.sp)
                                Text("Valid From: ${ssl.validFrom}", color = Color.Gray, fontSize = 11.sp)
                                Text("Valid To: ${ssl.validTo}", color = Color.Gray, fontSize = 11.sp)
                                Text("Protocol: ${ssl.protocol}", color = NeonCyan, fontSize = 11.sp)
                            }
                        }
                    }
                }

                if (result.securityHeaders.isNotEmpty()) {
                    NeonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedSection = if (expandedSection == "headers") null else "headers"
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Security Headers", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                val presentCount = result.securityHeaders.count { it.present }
                                Text("$presentCount/${result.securityHeaders.size}", color = NeonCyan, fontSize = 12.sp)
                            }
                            if (expandedSection == "headers") {
                                Spacer(modifier = Modifier.height(6.dp))
                                result.securityHeaders.forEach { header ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            header.header,
                                            color = if (header.present) NeonGreen else Color.Red,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f, fill = false),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (header.present) NeonGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f),
                                                    RoundedCornerShape(3.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (header.present) "OK" else "MISSING",
                                                color = if (header.present) NeonGreen else Color.Red,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Findings", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(result.findings, key = { it.name }) { finding ->
                        NeonCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        finding.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(0.7f, fill = false)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                finding.severity.color.copy(alpha = 0.2f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                finding.severity.color.copy(alpha = 0.5f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            finding.severity.label,
                                            color = finding.severity.color,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    finding.description,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkElevated, RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                ) {
                                    Text("Fix: ", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        finding.recommendation,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } ?: run {
                if (!isScanning) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.BugReport,
                                contentDescription = "No results",
                                modifier = Modifier.size(48.dp),
                                tint = DarkElevated
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No scan results", color = Color.Gray, fontSize = 14.sp)
                            Text("Enter a URL and start scanning", color = Color.DarkGray, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
