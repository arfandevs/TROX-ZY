package com.troxzy.xploit.ui.screens.subdomain

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.components.CommonScaffold
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.NeonCard
import com.troxzy.xploit.ui.theme.AMOLEDBlack
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.DarkSurface
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

private data class SubdomainResult(
    val domainName: String,
    val ipAddress: String = "",
    val httpStatus: Int = 0,
    val hasHttps: Boolean = false
)

private enum class SearchSource { BRUTE_FORCE, CERT_TRANSPARENCY, DNS_RECORDS }
private enum class WordlistType { COMMON, EXTENDED, SHORT }

private val COMMON_WORDLIST = listOf(
    "www", "mail", "ftp", "localhost", "webmail", "smtp", "pop", "ns1", "ns2", "ns3",
    "dns", "dns1", "dns2", "mx", "mx1", "mx2", "api", "dev", "staging", "test",
    "admin", "portal", "blog", "shop", "store", "app", "mobile", "cdn", "static",
    "media", "images", "img", "assets", "docs", "wiki", "forum", "support",
    "help", "web", "remote", "server", "cloud", "vpn", "gateway", "proxy",
    "login", "auth", "sso", "oauth", "beta", "alpha", "demo", "sandbox",
    "internal", "intranet", "extranet", "backup", "db", "database", "redis",
    "elastic", "search", "monitor", "status", "health", "metrics", "logs",
    "grafana", "kibana", "jenkins", "ci", "git", "gitlab", "github", "svn"
)

private val SHORT_WORDLIST = listOf(
    "www", "mail", "ftp", "api", "admin", "blog", "shop", "cdn", "dev", "test",
    "staging", "docs", "support", "portal", "app", "mobile", "ns1", "ns2",
    "mx", "mx1", "dns", "dns1", "webmail", "smtp", "login", "auth", "cloud"
)

private val EXTENDED_WORDLIST = COMMON_WORDLIST + listOf(
    "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n",
    "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
    "1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
    "stg", "prd", "uat", "qa", "old", "new", "v1", "v2", "v3",
    "web1", "web2", "web3", "srv1", "srv2", "srv3",
    "app1", "app2", "app3", "db1", "db2", "db3",
    "analytics", "tracking", "pixel", "ads", "adserver",
    "email", "newsletter", "subscribe", "unsubscribe",
    "payments", "billing", "checkout", "cart",
    "crm", "erp", "hr", "finance", "accounting",
    "vpn1", "vpn2", "vpn3", "gw", "router", "switch",
    "nas", "san", "backup1", "backup2", "archive",
    "monitor1", "monitor2", "zabbix", "nagios", "prometheus",
    "kafka", "rabbitmq", "mq", "queue", "worker",
    "s3", "storage", "minio", "ceph", "gluster",
    "webinar", "events", "calendar", "schedule",
    "careers", "jobs", "hr", "recruitment", "talent",
    "press", "news", "updates", "changelog", "release",
    "stats", "dashboard", "report", "reporting",
    "social", "facebook", "twitter", "linkedin", "instagram",
    "google", "bing", "yahoo", "baidu", "yandex"
)

private suspend fun searchCertTransparency(domain: String): List<SubdomainResult> = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://crt.sh/?q=%25.$domain&output=json")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("User-Agent", "TroxzyXploit/1.0")

        if (conn.responseCode == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            val jsonElements = Json.parseToJsonElement(response).jsonArray
            val subdomains = mutableSetOf<String>()

            for (element in jsonElements) {
                val obj = element.jsonObject
                val nameValue = obj["name_value"]?.jsonPrimitive?.content ?: ""
                // crt.sh can return multiple names separated by \n
                nameValue.split("\n").forEach { name ->
                    val cleaned = name.trim().removePrefix("*.")
                    if (cleaned.endsWith(domain) && cleaned != domain) {
                        subdomains.add(cleaned)
                    }
                }
            }

            subdomains.map { sub ->
                val ip = try {
                    InetAddress.getByName(sub).hostAddress ?: ""
                } catch (_: Exception) {
                    ""
                }
                val httpStatus = checkHttpStatus(sub)
                val hasHttps = checkHttps(sub)
                SubdomainResult(sub, ip, httpStatus, hasHttps)
            }
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun searchDnsRecords(domain: String): List<SubdomainResult> = withContext(Dispatchers.IO) {
    val commonSubdomains = listOf("www", "mail", "ftp", "api", "admin", "blog", "shop", "cdn", "ns1", "ns2", "mx", "dns")
    val results = mutableListOf<SubdomainResult>()

    for (sub in commonSubdomains) {
        try {
            val fullDomain = "$sub.$domain"
            val addr = InetAddress.getByName(fullDomain)
            results.add(SubdomainResult(fullDomain, addr.hostAddress ?: ""))
        } catch (_: Exception) {
            // Subdomain doesn't resolve, skip
        }
    }

    // Also try common DNS record types for the domain itself
    try {
        val names = InetAddress.getAllByName(domain)
        results.add(SubdomainResult(domain, names.firstOrNull()?.hostAddress ?: ""))
    } catch (_: Exception) {}

    results
}

private suspend fun bruteForceSubdomains(domain: String, wordlist: List<String>): List<SubdomainResult> = withContext(Dispatchers.IO) {
    val results = mutableListOf<SubdomainResult>()
    val found = mutableSetOf<String>()

    for (word in wordlist) {
        try {
            val fullDomain = "$word.$domain"
            if (found.contains(fullDomain)) continue
            val addr = InetAddress.getAllByName(fullDomain)
            if (addr.isNotEmpty()) {
                found.add(fullDomain)
                val ip = addr.first().hostAddress ?: ""
                val httpStatus = checkHttpStatus(fullDomain)
                val hasHttps = checkHttps(fullDomain)
                results.add(SubdomainResult(fullDomain, ip, httpStatus, hasHttps))
            }
        } catch (_: Exception) {
            // Subdomain doesn't exist
        }
    }
    results
}

private fun checkHttpStatus(subdomain: String): Int {
    try {
        val url = URL("http://$subdomain")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        conn.setRequestProperty("User-Agent", "TroxzyXploit/1.0")
        val code = conn.responseCode
        conn.disconnect()
        return code
    } catch (_: Exception) {
        return 0
    }
}

private fun checkHttps(subdomain: String): Boolean {
    try {
        val url = URL("https://$subdomain")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "HEAD"
        conn.connectTimeout = 3000
        conn.readTimeout = 3000
        conn.setRequestProperty("User-Agent", "TroxzyXploit/1.0")
        val code = conn.responseCode
        conn.disconnect()
        return code in 200..399
    } catch (_: Exception) {
        return false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubdomainFinderScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var domainInput by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(SearchSource.CERT_TRANSPARENCY) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var selectedWordlist by remember { mutableStateOf(WordlistType.COMMON) }
    var wordlistExpanded by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<SubdomainResult>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeFilter by remember { mutableIntStateOf(0) } // 0=All, 1=Live only, 2=Has HTTP, 3=Has HTTPS
    var searchProgress by remember { mutableStateOf("") }
    val history = remember { mutableStateListOf<String>() }

    val clipboardManager = LocalClipboardManager.current

    val filteredResults = when (activeFilter) {
        1 -> results.filter { it.ipAddress.isNotEmpty() }
        2 -> results.filter { it.httpStatus > 0 }
        3 -> results.filter { it.hasHttps }
        else -> results
    }

    val activeWordlist = when (selectedWordlist) {
        WordlistType.COMMON -> COMMON_WORDLIST
        WordlistType.EXTENDED -> EXTENDED_WORDLIST
        WordlistType.SHORT -> SHORT_WORDLIST
    }

    CommonScaffold(
        title = "Subdomain Finder",
        currentRoute = "subdomain_finder",
        onNavigate = onNavigate,
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AMOLEDBlack)
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Domain input
            OutlinedTextField(
                value = domainInput,
                onValueChange = { domainInput = it },
                label = { Text("Domain Name", fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkSurface,
                    focusedLabelColor = NeonCyan,
                    cursorColor = NeonCyan
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Source selector and wordlist selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = when (selectedSource) {
                            SearchSource.BRUTE_FORCE -> "Brute Force"
                            SearchSource.CERT_TRANSPARENCY -> "Cert Transparency"
                            SearchSource.DNS_RECORDS -> "DNS Records"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Source", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonCyan
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurface,
                            focusedLabelColor = NeonCyan
                        )
                    )
                    ExposedDropdownMenu(
                        containerColor = DarkCard,
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Certificate Transparency", color = NeonCyan, fontFamily = FontFamily.Monospace) },
                            onClick = { selectedSource = SearchSource.CERT_TRANSPARENCY; sourceExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Brute Force", color = NeonPurple, fontFamily = FontFamily.Monospace) },
                            onClick = { selectedSource = SearchSource.BRUTE_FORCE; sourceExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("DNS Records", color = NeonGreen, fontFamily = FontFamily.Monospace) },
                            onClick = { selectedSource = SearchSource.DNS_RECORDS; sourceExpanded = false }
                        )
                    }
                }

                if (selectedSource == SearchSource.BRUTE_FORCE) {
                    ExposedDropdownMenuBox(
                        expanded = wordlistExpanded,
                        onExpandedChange = { wordlistExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = when (selectedWordlist) {
                                WordlistType.COMMON -> "Common (${COMMON_WORDLIST.size})"
                                WordlistType.EXTENDED -> "Extended (${EXTENDED_WORDLIST.size})"
                                WordlistType.SHORT -> "Short (${SHORT_WORDLIST.size})"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Wordlist", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = wordlistExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = NeonPurple
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = DarkSurface,
                                focusedLabelColor = NeonPurple
                            )
                        )
                        ExposedDropdownMenu(
                            containerColor = DarkCard,
                            expanded = wordlistExpanded,
                            onDismissRequest = { wordlistExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Common (${COMMON_WORDLIST.size})", color = NeonCyan, fontFamily = FontFamily.Monospace) },
                                onClick = { selectedWordlist = WordlistType.COMMON; wordlistExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Extended (${EXTENDED_WORDLIST.size})", color = NeonGreen, fontFamily = FontFamily.Monospace) },
                                onClick = { selectedWordlist = WordlistType.EXTENDED; wordlistExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Short (${SHORT_WORDLIST.size})", color = NeonPurple, fontFamily = FontFamily.Monospace) },
                                onClick = { selectedWordlist = WordlistType.SHORT; wordlistExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search button
            Button(
                onClick = {
                    if (domainInput.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        results.clear()
                        searchProgress = "Starting search..."
                        kotlinx.coroutines.MainScope().launch {
                            try {
                                val found = when (selectedSource) {
                                    SearchSource.CERT_TRANSPARENCY -> {
                                        searchProgress = "Querying crt.sh..."
                                        searchCertTransparency(domainInput)
                                    }
                                    SearchSource.BRUTE_FORCE -> {
                                        searchProgress = "Brute forcing ${activeWordlist.size} subdomains..."
                                        bruteForceSubdomains(domainInput, activeWordlist)
                                    }
                                    SearchSource.DNS_RECORDS -> {
                                        searchProgress = "Checking DNS records..."
                                        searchDnsRecords(domainInput)
                                    }
                                }
                                results.clear()
                                results.addAll(found)
                                if (found.isEmpty()) errorMessage = "No subdomains found for $domainInput"
                                searchProgress = ""
                            } catch (e: Exception) {
                                errorMessage = e.message
                                searchProgress = ""
                            }
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.15f),
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading && domainInput.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("FIND SUBDOMAINS", fontFamily = FontFamily.Monospace)
            }

            // Progress indicator
            if (isLoading && searchProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(searchProgress, color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error display
            errorMessage?.let { err ->
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(err, color = Color(0xFFFF4444), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = listOf("All (${results.size})", "Live (${results.filter { it.ipAddress.isNotEmpty() }.size})", "HTTP (${results.filter { it.httpStatus > 0 }.size})", "HTTPS (${results.filter { it.hasHttps }.size})")
                filters.forEachIndexed { index, label ->
                    FilterChip(
                        selected = activeFilter == index,
                        onClick = { activeFilter = index },
                        label = {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (activeFilter == index) NeonCyan else Color.Gray
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.15f),
                            containerColor = DarkSurface,
                            selectedLabelColor = NeonCyan,
                            labelColor = Color.Gray
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = DarkSurface,
                            selectedBorderColor = NeonCyan,
                            enabled = true,
                            selected = activeFilter == index
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Results or empty state
            if (filteredResults.isEmpty() && errorMessage == null && !isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GlitchText(text = "SUBDOMAIN FINDER", color = NeonPurple)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Enter a domain to discover subdomains", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else if (filteredResults.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxSize().background(AMOLEDBlack)) {
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Found ${filteredResults.size} subdomains",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Row {
                            IconButton(onClick = {
                                history.add(domainInput)
                                // Save to history - in production would persist to database
                            }) {
                                Icon(Icons.Default.History, contentDescription = "Save", tint = NeonPurple, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = {
                                val text = filteredResults.joinToString("\n") { it.domainName }
                                clipboardManager.setText(AnnotatedString(text))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { /* Export */ }) {
                                Icon(Icons.Default.Download, contentDescription = "Export", tint = NeonGreen, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    // Results header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("DOMAIN", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.5f))
                        Text("IP", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text("HTTP", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(38.dp))
                        Text("TLS", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp))
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredResults) { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkCard)
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    sub.domainName,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    sub.ipAddress.ifEmpty { "—" },
                                    color = if (sub.ipAddress.isNotEmpty()) NeonGreen else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (sub.httpStatus > 0) "${sub.httpStatus}" else "—",
                                    color = when {
                                        sub.httpStatus in 200..299 -> NeonGreen
                                        sub.httpStatus in 300..399 -> Color(0xFFFFD700)
                                        sub.httpStatus in 400..599 -> Color(0xFFFF4444)
                                        else -> Color.Gray
                                    },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(38.dp)
                                )
                                Text(
                                    if (sub.hasHttps) "✓" else "✗",
                                    color = if (sub.hasHttps) NeonGreen else Color(0xFFFF4444),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.width(30.dp)
                                )
                            }
                            HorizontalDivider(color = DarkSurface, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Loading state overlay
            if (isLoading && filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(searchProgress, color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
