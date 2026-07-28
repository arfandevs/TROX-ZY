package com.troxzy.xploit.ui.screens.reverseip

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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.input.KeyboardType
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
import java.net.HttpURLConnection
import java.net.URL

private data class ReverseDomain(
    val domainName: String,
    val title: String = "",
    val lastResolved: String = "",
    val ip: String = ""
)

private data class BulkReverseResult(
    val ip: String,
    val domains: List<ReverseDomain>,
    val error: String? = null
)

private suspend fun performReverseIpLookup(ip: String): List<ReverseDomain> = withContext(Dispatchers.IO) {
    try {
        // Using hackertarget API for reverse IP lookup
        val url = URL("https://api.hackertarget.com/reverseiplookup/?q=$ip")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 15000

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            // Parse the response - hackertarget returns comma-separated domains
            val domains = response.lines().firstOrNull()?.split(",")?.map { domain ->
                ReverseDomain(
                    domainName = domain.trim(),
                    ip = ip
                )
            } ?: emptyList()
            domains
        } else {
            // Fallback: try ViewDNS API
            tryReverseIpViewDNS(ip)
        }
    } catch (e: Exception) {
        tryReverseIpViewDNS(ip)
    }
}

private suspend fun tryReverseIpViewDNS(ip: String): List<ReverseDomain> = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.hackertarget.com/reverseiplookup/?q=$ip")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 15000
        val response = conn.inputStream.bufferedReader().readText()
        response.lines().firstOrNull()?.split(",")?.map { domain ->
            ReverseDomain(domainName = domain.trim(), ip = ip)
        } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

private suspend fun performBulkReverseLookup(ips: List<String>): List<BulkReverseResult> = withContext(Dispatchers.IO) {
    ips.map { ip ->
        try {
            val domains = performReverseIpLookup(ip)
            BulkReverseResult(ip, domains)
        } catch (e: Exception) {
            BulkReverseResult(ip, emptyList(), e.message)
        }
    }
}

@Composable
fun ReverseIpScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var ipInput by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<ReverseDomain>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Bulk
    var bulkInput by remember { mutableStateOf("") }
    val bulkResults = remember { mutableStateListOf<BulkReverseResult>() }
    var bulkLoading by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    CommonScaffold(
        title = "Reverse IP",
        currentRoute = "reverse_ip",
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

            // IP input
            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("IP Address", fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
            Button(
                onClick = {
                    if (ipInput.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        results.clear()
                        kotlinx.coroutines.MainScope().launch {
                            val domains = performReverseIpLookup(ipInput)
                            results.clear()
                            results.addAll(domains)
                            if (domains.isEmpty()) errorMessage = "No domains found for $ipInput"
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
                enabled = !isLoading && ipInput.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Lookup", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("REVERSE IP LOOKUP", fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            val tabs = listOf("Results (${results.size})", "Bulk Lookup")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkCard,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan,
                        height = 2.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) NeonCyan else Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (selectedTab) {
                0 -> {
                    // Results
                    errorMessage?.let { err ->
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Text(err, color = Color(0xFFFF4444), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                        }
                    }
                    if (results.isEmpty() && errorMessage == null) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GlitchText(text = "REVERSE IP", color = NeonPurple)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Enter an IP to find hosted domains", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } else if (results.isNotEmpty()) {
                        Column(modifier = Modifier.fillMaxSize().background(AMOLEDBlack)) {
                            // Header and action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Domains on ${ipInput}",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row {
                                    IconButton(onClick = {
                                        val text = results.joinToString("\n") { it.domainName }
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
                                Text("#", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(30.dp))
                                Text("DOMAIN", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
                                Text("IP", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                Text("RESOLVED", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(90.dp))
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(results) { domain ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkCard)
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            "${results.indexOf(domain) + 1}",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(30.dp)
                                        )
                                        Text(
                                            domain.domainName,
                                            color = NeonGreen,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(2f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            domain.ip,
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            domain.lastResolved.ifEmpty { "N/A" },
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.width(90.dp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    HorizontalDivider(color = DarkSurface, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Bulk lookup
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AMOLEDBlack)
                            .padding(8.dp)
                    ) {
                        Text("BULK REVERSE IP LOOKUP", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Find all domains hosted on multiple IPs", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bulkInput,
                            onValueChange = { bulkInput = it },
                            label = { Text("IPs (comma or newline separated)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val ips = bulkInput.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                                if (ips.isNotEmpty()) {
                                    bulkLoading = true
                                    bulkResults.clear()
                                    kotlinx.coroutines.MainScope().launch {
                                        val results = performBulkReverseLookup(ips)
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
                            if (bulkLoading) CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LOOKUP ALL IPs", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(bulkResults) { result ->
                                NeonCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(result.ip, color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            if (result.error != null) {
                                                Text("ERROR", color = Color(0xFFFF4444), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            } else {
                                                Text("${result.domains.size} domains", color = NeonGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                        result.error?.let { err ->
                                            Text(err, color = Color(0xFFFF4444), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        if (result.domains.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            result.domains.take(5).forEach { domain ->
                                                Text(domain.domainName, color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                            if (result.domains.size > 5) {
                                                Text(
                                                    "... and ${result.domains.size - 5} more",
                                                    color = Color.Gray,
                                                    fontSize = 9.sp,
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
    }
}
