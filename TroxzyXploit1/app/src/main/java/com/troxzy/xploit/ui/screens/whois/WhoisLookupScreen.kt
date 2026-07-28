package com.troxzy.xploit.ui.screens.whois

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

private data class WhoisResult(
    val domain: String,
    val rawOutput: String,
    val sections: Map<String, String>,
    val error: String? = null
)

private data class WhoisSection(
    val title: String,
    val color: androidx.compose.ui.graphics.Color
)

private val WHOIS_SECTIONS = listOf(
    WhoisSection("Registrar", NeonCyan),
    WhoisSection("Creation Date", NeonGreen),
    WhoisSection("Expiry Date", androidx.compose.ui.graphics.Color(0xFFFF6600)),
    WhoisSection("Name Server", NeonPurple),
    WhoisSection("Registrant", NeonCyan),
    WhoisSection("DNSSEC", NeonGreen),
    WhoisSection("Updated Date", androidx.compose.ui.graphics.Color(0xFFFFD700)),
    WhoisSection("Status", androidx.compose.ui.graphics.Color(0xFFFF4444))
)

private suspend fun performWhoisLookup(domain: String): WhoisResult = withContext(Dispatchers.IO) {
    try {
        val tld = domain.substringAfterLast(".")
        val referralServer = getReferralServer(tld)
        val server = referralServer ?: "whois.iana.org"
        val rawOutput = queryWhoisServer(domain, server, referralServer != null)
        val sections = parseWhoisSections(rawOutput)
        WhoisResult(domain, rawOutput, sections)
    } catch (e: Exception) {
        WhoisResult(domain, "", emptyMap(), e.message)
    }
}

private fun getReferralServer(tld: String): String? {
    return when (tld.lowercase()) {
        "com" -> "whois.verisign-grs.com"
        "net" -> "whois.verisign-grs.com"
        "org" -> "whois.pir.org"
        "info" -> "whois.afilias.net"
        "io" -> "whois.nic.io"
        "dev" -> "whois.nic.google"
        "app" -> "whois.nic.google"
        "me" -> "whois.nic.me"
        "co" -> "whois.nic.co"
        "xyz" -> "whois.nic.xyz"
        "ru" -> "whois.tcinet.ru"
        "uk" -> "whois.nic.uk"
        "de" -> "whois.denic.de"
        "fr" -> "whois.nic.fr"
        else -> null
    }
}

private fun queryWhoisServer(domain: String, server: String, isDirect: Boolean): String {
    val socket = Socket()
    try {
        socket.connect(java.net.InetSocketAddress(server, 43), 10000)
        socket.soTimeout = 15000
        val output = socket.getOutputStream()
        val input = BufferedReader(InputStreamReader(socket.getInputStream()))

        output.write("$domain\r\n".toByteArray())
        output.flush()

        val response = StringBuilder()
        var line: String?
        while (input.readLine().also { line = it } != null) {
            response.append(line).append("\n")
        }

        // If not direct, try to follow referral
        if (!isDirect) {
            val referralMatch = Regex("refer:\\s*(\\S+)", RegexOption.IGNORE_CASE).find(response.toString())
            if (referralMatch != null) {
                val refServer = referralMatch.groupValues[1]
                return queryWhoisServer(domain, refServer, true)
            }
        }

        // Check for referral in response
        val whoisServerMatch = Regex("Registrar WHOIS Server:\\s*(\\S+)", RegexOption.IGNORE_CASE).find(response.toString())
        if (whoisServerMatch != null && whoisServerMatch.groupValues[1] != server) {
            return queryWhoisServer(domain, whoisServerMatch.groupValues[1], true)
        }

        return response.toString()
    } finally {
        try { socket.close() } catch (_: Exception) {}
    }
}

private fun parseWhoisSections(raw: String): Map<String, String> {
    val sections = mutableMapOf<String, String>()
    val lines = raw.lines()

    var currentSection = ""
    var currentContent = mutableListOf<String>()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            if (currentSection.isNotEmpty() && currentContent.isNotEmpty()) {
                sections[currentSection] = currentContent.joinToString("\n")
                currentContent = mutableListOf()
            }
            currentSection = ""
            continue
        }

        // Check if line is a key-value pair
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex > 0 && colonIndex < trimmed.length - 1) {
            val key = trimmed.substring(0, colonIndex).trim()
            val value = trimmed.substring(colonIndex + 1).trim()

            // Categorize into sections
            val section = when {
                key.contains("registrar", ignoreCase = true) -> "Registrar"
                key.contains("creation", ignoreCase = true) || key.contains("created", ignoreCase = true) -> "Creation Date"
                key.contains("expir", ignoreCase = true) || key.contains("registry expiry", ignoreCase = true) -> "Expiry Date"
                key.contains("name server", ignoreCase = true) || key.contains("nserver", ignoreCase = true) -> "Name Server"
                key.contains("registrant", ignoreCase = true) -> "Registrant"
                key.contains("dnssec", ignoreCase = true) -> "DNSSEC"
                key.contains("updated", ignoreCase = true) -> "Updated Date"
                key.contains("status", ignoreCase = true) -> "Status"
                else -> ""
            }

            if (section.isNotEmpty()) {
                if (section != currentSection && currentSection.isNotEmpty() && currentContent.isNotEmpty()) {
                    sections[currentSection] = currentContent.joinToString("\n")
                    currentContent = mutableListOf()
                }
                currentSection = section
                currentContent.add("$key: $value")
            } else if (currentSection.isNotEmpty()) {
                currentContent.add(trimmed)
            }
        } else if (currentSection.isNotEmpty()) {
            currentContent.add(trimmed)
        }
    }

    if (currentSection.isNotEmpty() && currentContent.isNotEmpty()) {
        sections[currentSection] = currentContent.joinToString("\n")
    }

    return sections
}

private suspend fun performBulkWhois(domains: List<String>): List<WhoisResult> = withContext(Dispatchers.IO) {
    domains.map { domain ->
        performWhoisLookup(domain.trim())
    }
}

@Composable
fun WhoisLookupScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var domainInput by remember { mutableStateOf("") }
    var whoisResult by remember { mutableStateOf<WhoisResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRawOutput by remember { mutableStateOf(false) }
    var showBulkSection by remember { mutableStateOf(false) }
    var bulkInput by remember { mutableStateOf("") }
    val bulkResults = remember { mutableStateListOf<WhoisResult>() }
    var bulkLoading by remember { mutableStateOf(false) }
    var expandedBulkIndex by remember { mutableStateOf(-1) }

    val clipboardManager = LocalClipboardManager.current

    CommonScaffold(
        title = "WHOIS Lookup",
        currentRoute = "whois_lookup",
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
                    color = androidx.compose.ui.graphics.Color.White
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

            // Lookup button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (domainInput.isNotBlank()) {
                            isLoading = true
                            errorMessage = null
                            whoisResult = null
                            kotlinx.coroutines.MainScope().launch {
                                val result = performWhoisLookup(domainInput)
                                whoisResult = result
                                if (result.error != null) errorMessage = result.error
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
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
                        Icon(Icons.Default.Search, contentDescription = "Lookup", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WHOIS LOOKUP", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }

                Button(
                    onClick = { showBulkSection = !showBulkSection },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showBulkSection) NeonPurple.copy(alpha = 0.3f) else DarkSurface,
                        contentColor = NeonPurple
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("BULK", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }

            // Bulk WHOIS section
            if (showBulkSection) {
                Spacer(modifier = Modifier.height(8.dp))
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("BULK WHOIS LOOKUP", color = NeonPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = bulkInput,
                            onValueChange = { bulkInput = it },
                            label = { Text("Domains (comma separated)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = androidx.compose.ui.graphics.Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = DarkSurface,
                                focusedLabelColor = NeonPurple,
                                cursorColor = NeonPurple
                            ),
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val domains = bulkInput.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                if (domains.isNotEmpty()) {
                                    bulkLoading = true
                                    bulkResults.clear()
                                    kotlinx.coroutines.MainScope().launch {
                                        val results = performBulkWhois(domains)
                                        bulkResults.clear()
                                        bulkResults.addAll(results)
                                        bulkLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple.copy(alpha = 0.15f),
                                contentColor = NeonPurple
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !bulkLoading
                        ) {
                            if (bulkLoading) {
                                CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LOOKUP ALL", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.height(200.dp)) {
                            items(bulkResults) { result ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard)
                                        .clip(RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            result.domain,
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (result.error != null) {
                                            Text("ERROR", color = androidx.compose.ui.graphics.Color(0xFFFF4444), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        } else {
                                            Text("OK", color = NeonGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    if (result.sections.isNotEmpty()) {
                                        result.sections.entries.take(3).forEach { (key, value) ->
                                            Text(
                                                "$key: ${value.lines().firstOrNull()?.take(60) ?: ""}",
                                                color = androidx.compose.ui.graphics.Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error display
            errorMessage?.let { err ->
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "ERROR: $err",
                        color = androidx.compose.ui.graphics.Color(0xFFFF4444),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Results display
            whoisResult?.let { result ->
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        result.domain.uppercase(),
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Row {
                        Button(
                            onClick = { showRawOutput = !showRawOutput },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showRawOutput) NeonCyan.copy(alpha = 0.2f) else DarkSurface,
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(if (showRawOutput) "FORMATTED" else "RAW", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(result.rawOutput))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { /* Export as TXT */ }) {
                            Icon(Icons.Default.Download, contentDescription = "Export", tint = NeonGreen, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (showRawOutput) {
                    // Raw output
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkCard)
                            .clip(RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        Text(
                            result.rawOutput,
                            color = NeonGreen,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    // Formatted sections
                    if (result.sections.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No structured data found. Try RAW view.", color = androidx.compose.ui.graphics.Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AMOLEDBlack)
                        ) {
                            result.sections.forEach { (sectionTitle, content) ->
                                val sectionColor = WHOIS_SECTIONS.find { it.title == sectionTitle }?.color ?: NeonCyan
                                item {
                                    NeonCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                sectionTitle.uppercase(),
                                                color = sectionColor,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            HorizontalDivider(color = DarkSurface, thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            content.lines().forEach { line ->
                                                val colonIdx = line.indexOf(':')
                                                if (colonIdx > 0) {
                                                    Row(modifier = Modifier.padding(vertical = 1.dp)) {
                                                        Text(
                                                            line.substring(0, colonIdx + 1),
                                                            color = androidx.compose.ui.graphics.Color.Gray,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                        Text(
                                                            line.substring(colonIdx + 1),
                                                            color = androidx.compose.ui.graphics.Color.White,
                                                            fontSize = 10.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                } else {
                                                    Text(
                                                        line,
                                                        color = androidx.compose.ui.graphics.Color.White,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (whoisResult == null && errorMessage == null) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GlitchText(text = "WHOIS LOOKUP", color = NeonPurple)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Enter a domain to query WHOIS data", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
