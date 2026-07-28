package com.troxzy.xploit.ui.screens.dnslookup

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Inet4Address
import java.net.Inet6Address
import java.nio.ByteBuffer

private data class DnsRecord(
    val name: String,
    val type: String,
    val value: String,
    val ttl: Long
)

private data class PropagationResult(
    val server: String,
    val ip: String,
    val records: List<DnsRecord>,
    val latencyMs: Long,
    val error: String? = null
)

private val RECORD_TYPES = listOf("A", "AAAA", "MX", "NS", "TXT", "CNAME", "SOA", "PTR", "SRV", "CAA")
private val DNS_SERVERS = mapOf(
    "Google" to "8.8.8.8",
    "Cloudflare" to "1.1.1.1",
    "OpenDNS" to "208.67.222.222",
    "Quad9" to "9.9.9.9"
)

private fun buildDnsQuery(domain: String, recordType: String): ByteArray {
    val buffer = ByteBuffer.allocate(512)
    // Transaction ID
    buffer.putShort(0x1234)
    // Flags: standard query
    buffer.putShort(0x0100)
    // Questions: 1
    buffer.putShort(1)
    // Answer/Authority/Additional RRs
    buffer.putShort(0)
    buffer.putShort(0)
    buffer.putShort(0)
    // QNAME
    for (label in domain.split(".")) {
        buffer.put(label.length.toByte())
        buffer.put(label.toByteArray())
    }
    buffer.put(0)
    // QTYPE
    val typeValue = when (recordType) {
        "A" -> 1; "NS" -> 2; "CNAME" -> 5; "SOA" -> 6; "PTR" -> 12
        "MX" -> 15; "TXT" -> 16; "AAAA" -> 28; "SRV" -> 33; "CAA" -> 257
        else -> 1
    }
    buffer.putShort(typeValue.toShort())
    // QCLASS: IN
    buffer.putShort(1)
    return buffer.array().copyOfRange(0, buffer.position())
}

private fun parseDnsResponse(data: ByteArray, domain: String, recordType: String): List<DnsRecord> {
    if (data.size < 12) return emptyList()
    val buffer = ByteBuffer.wrap(data)
    buffer.short // transaction ID
    val flags = buffer.short.toInt() and 0xFFFF
    val rcode = flags and 0xF
    if (rcode != 0) return emptyList()
    val qdCount = buffer.short.toInt() and 0xFFFF
    val anCount = buffer.short.toInt() and 0xFFFF
    buffer.short // nsCount
    buffer.short // arCount

    // Skip questions
    repeat(qdCount) {
        while (true) {
            val len = buffer.get().toInt() and 0xFF
            if (len == 0) break
            buffer.position(buffer.position() + len)
        }
        buffer.short // QTYPE
        buffer.short // QCLASS
    }

    val records = mutableListOf<DnsRecord>()
    repeat(anCount) {
        if (buffer.remaining() < 12) break
        // Parse name (handle compression)
        val name = parseName(buffer, data)
        val rType = buffer.short.toInt() and 0xFFFF
        buffer.short // class
        val ttl = buffer.int.toLong() and 0xFFFFFFFFL
        val rdLength = buffer.short.toInt() and 0xFFFF
        if (buffer.remaining() < rdLength) break
        val rdStart = buffer.position()

        val value = when (rType) {
            1 -> { // A
                if (rdLength >= 4) {
                    "${buffer.get().toInt() and 0xFF}.${buffer.get().toInt() and 0xFF}.${buffer.get().toInt() and 0xFF}.${buffer.get().toInt() and 0xFF}"
                } else ""
            }
            28 -> { // AAAA
                if (rdLength >= 16) {
                    val bytes = ByteArray(16) { buffer.get() }
                    bytes.toList().chunked(2).joinToString(":") {
                        String.format("%02x%02x", it[0], it[1])
                    }
                } else ""
            }
            15 -> { // MX
                buffer.short // preference
                parseName(buffer, data)
            }
            2, 5, 12 -> { // NS, CNAME, PTR
                parseName(buffer, data)
            }
            6 -> { // SOA
                val mname = parseName(buffer, data)
                val rname = parseName(buffer, data)
                if (buffer.remaining() >= 20) {
                    buffer.int; buffer.int; buffer.int; buffer.int; buffer.int
                }
                "$mname $rname"
            }
            16 -> { // TXT
                val txtLen = buffer.get().toInt() and 0xFF
                val txtBytes = ByteArray(minOf(txtLen, buffer.remaining())) { buffer.get() }
                String(txtBytes, Charsets.UTF_8)
            }
            33 -> { // SRV
                buffer.short // priority
                buffer.short // weight
                val port = buffer.short.toInt() and 0xFFFF
                val target = parseName(buffer, data)
                "$target:$port"
            }
            257 -> { // CAA
                buffer.get() // flags
                val tagLen = buffer.get().toInt() and 0xFF
                val tag = ByteArray(tagLen) { buffer.get() }.toString(Charsets.UTF_8)
                val valLen = rdLength - 2 - tagLen
                val valueBytes = ByteArray(minOf(valLen, buffer.remaining())) { buffer.get() }
                "$tag ${String(valueBytes, Charsets.UTF_8)}"
            }
            else -> {
                buffer.position(rdStart + rdLength)
                "Unknown (type $rType)"
            }
        }

        buffer.position(rdStart + rdLength)
        val typeStr = when (rType) {
            1 -> "A"; 2 -> "NS"; 5 -> "CNAME"; 6 -> "SOA"; 12 -> "PTR"
            15 -> "MX"; 16 -> "TXT"; 28 -> "AAAA"; 33 -> "SRV"; 257 -> "CAA"
            else -> "TYPE$rType"
        }
        records.add(DnsRecord(name, typeStr, value, ttl))
    }
    return records
}

private fun parseName(buffer: ByteBuffer, data: ByteArray): String {
    val labels = mutableListOf<String>()
    var jumped = false
    var jumpPos = -1
    while (true) {
        val len = buffer.get().toInt() and 0xFF
        if (len == 0) break
        if ((len and 0xC0) == 0xC0) {
            if (!jumped) jumpPos = buffer.position()
            val offset = ((len and 0x3F) shl 8) or (buffer.get().toInt() and 0xFF)
            buffer.position(offset)
            jumped = true
            continue
        }
        val labelBytes = ByteArray(len) { buffer.get() }
        labels.add(String(labelBytes, Charsets.UTF_8))
    }
    if (jumped) buffer.position(jumpPos + 1)
    return labels.joinToString(".")
}

private suspend fun performDnsLookup(domain: String, recordType: String, dnsServer: String): List<DnsRecord> =
    withContext(Dispatchers.IO) {
        try {
            val query = buildDnsQuery(domain, recordType)
            val socket = DatagramSocket()
            socket.soTimeout = 5000
            val serverAddress = InetAddress.getByName(dnsServer)
            val sendPacket = DatagramPacket(query, query.size, serverAddress, 53)
            socket.send(sendPacket)
            val receiveData = ByteArray(1024)
            val receivePacket = DatagramPacket(receiveData, receiveData.size)
            socket.receive(receivePacket)
            socket.close()
            parseDnsResponse(receiveData.copyOf(receivePacket.length), domain, recordType)
        } catch (e: Exception) {
            emptyList()
        }
    }

private suspend fun performReverseLookup(ip: String): String = withContext(Dispatchers.IO) {
    try {
        val addr = InetAddress.getByName(ip)
        val host = addr.hostName
        if (host != ip) host else "No PTR record found"
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

private suspend fun performPropagationCheck(domain: String, recordType: String): List<PropagationResult> =
    withContext(Dispatchers.IO) {
        DNS_SERVERS.map { (name, ip) ->
            val start = System.currentTimeMillis()
            try {
                val records = performDnsLookup(domain, recordType, ip)
                val latency = System.currentTimeMillis() - start
                PropagationResult(name, ip, records, latency)
            } catch (e: Exception) {
                PropagationResult(name, ip, emptyList(), System.currentTimeMillis() - start, e.message)
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsLookupScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var domainInput by remember { mutableStateOf("") }
    var selectedRecordType by remember { mutableStateOf("A") }
    var recordTypeExpanded by remember { mutableStateOf(false) }
    var selectedDnsServer by remember { mutableStateOf("Google") }
    var dnsServerExpanded by remember { mutableStateOf(false) }
    var customDnsServer by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<DnsRecord>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Reverse DNS
    var reverseIpInput by remember { mutableStateOf("") }
    var reverseResult by remember { mutableStateOf<String?>(null) }
    var reverseLoading by remember { mutableStateOf(false) }

    // Propagation
    var propagationResults by remember { mutableStateOf<List<PropagationResult>>(emptyList()) }
    var propagationLoading by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    val activeDnsServer = if (selectedDnsServer == "Custom") customDnsServer else DNS_SERVERS[selectedDnsServer]

    CommonScaffold(
        title = "DNS Lookup",
        currentRoute = "dns_lookup",
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
                label = { Text("Domain", fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
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

            // Record type and DNS server row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = recordTypeExpanded,
                    onExpandedChange = { recordTypeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedRecordType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Record", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = recordTypeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonPurple
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurface,
                            focusedLabelColor = NeonCyan
                        )
                    )
                    ExposedDropdownMenu(
                        containerColor = DarkCard,
                        expanded = recordTypeExpanded,
                        onDismissRequest = { recordTypeExpanded = false }
                    ) {
                        RECORD_TYPES.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = NeonCyan, fontFamily = FontFamily.Monospace) },
                                onClick = { selectedRecordType = type; recordTypeExpanded = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = dnsServerExpanded,
                    onExpandedChange = { dnsServerExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDnsServer,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("DNS Server", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dnsServerExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreen
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurface,
                            focusedLabelColor = NeonCyan
                        )
                    )
                    ExposedDropdownMenu(
                        containerColor = DarkCard,
                        expanded = dnsServerExpanded,
                        onDismissRequest = { dnsServerExpanded = false }
                    ) {
                        DNS_SERVERS.forEach { (name, ip) ->
                            DropdownMenuItem(
                                text = { Text("$name ($ip)", color = NeonCyan, fontFamily = FontFamily.Monospace) },
                                onClick = { selectedDnsServer = name; dnsServerExpanded = false }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Custom", color = NeonPurple, fontFamily = FontFamily.Monospace) },
                            onClick = { selectedDnsServer = "Custom"; dnsServerExpanded = false }
                        )
                    }
                }
            }

            if (selectedDnsServer == "Custom") {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = customDnsServer,
                    onValueChange = { customDnsServer = it },
                    label = { Text("Custom DNS IP", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        color = androidx.compose.ui.graphics.Color.White
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
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lookup button
            Button(
                onClick = {
                    if (domainInput.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        results.clear()
                        val server = activeDnsServer ?: "8.8.8.8"
                        kotlinx.coroutines.MainScope().launch {
                            val res = performDnsLookup(domainInput, selectedRecordType, server)
                            results.clear()
                            results.addAll(res)
                            if (res.isEmpty()) errorMessage = "No records found for $selectedRecordType"
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
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NeonCyan,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Lookup", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("LOOKUP", fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            val tabs = listOf("Results (${results.size})", "Reverse DNS", "Propagation")
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
                                color = if (selectedTab == index) NeonCyan else androidx.compose.ui.graphics.Color.Gray,
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
                        NeonCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(err, color = androidx.compose.ui.graphics.Color(0xFFFF4444), fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                        }
                    }
                    if (results.isEmpty() && errorMessage == null) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GlitchText(text = "DNS LOOKUP", color = NeonPurple)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Enter a domain and hit Lookup", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().background(AMOLEDBlack)) {
                            // Export / Copy buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    val text = results.joinToString("\n") { "${it.name}\t${it.type}\t${it.value}\t${it.ttl}" }
                                    clipboardManager.setText(AnnotatedString(text))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { /* Export to file */ }) {
                                    Icon(Icons.Default.Download, contentDescription = "Export", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                }
                            }
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth().background(DarkSurface).padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("NAME", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f))
                                Text("TYPE", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(45.dp))
                                Text("VALUE", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f))
                                Text("TTL", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(55.dp))
                            }
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                items(results) { record ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(record.name, color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(record.type, color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(45.dp))
                                        Text(record.value, color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(2f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("${record.ttl}s", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(55.dp))
                                    }
                                    HorizontalDivider(color = DarkSurface, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Reverse DNS
                    Column(modifier = Modifier.fillMaxSize().background(AMOLEDBlack).padding(8.dp)) {
                        Text("REVERSE DNS LOOKUP", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reverseIpInput,
                            onValueChange = { reverseIpInput = it },
                            label = { Text("IP Address", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = androidx.compose.ui.graphics.Color.White),
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
                        Button(
                            onClick = {
                                if (reverseIpInput.isNotBlank()) {
                                    reverseLoading = true
                                    reverseResult = null
                                    kotlinx.coroutines.MainScope().launch {
                                        reverseResult = performReverseLookup(reverseIpInput)
                                        reverseLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f), contentColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !reverseLoading && reverseIpInput.isNotBlank()
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Reverse", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REVERSE LOOKUP", fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        reverseResult?.let { result ->
                            NeonCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("RESULT", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(result, color = NeonGreen, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        if (reverseLoading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
                2 -> {
                    // Propagation checker
                    Column(modifier = Modifier.fillMaxSize().background(AMOLEDBlack).padding(8.dp)) {
                        Text("DNS PROPAGATION CHECKER", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Compare results across multiple DNS servers", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (domainInput.isNotBlank()) {
                                    propagationLoading = true
                                    propagationResults = emptyList()
                                    kotlinx.coroutines.MainScope().launch {
                                        propagationResults = performPropagationCheck(domainInput, selectedRecordType)
                                        propagationLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !propagationLoading && domainInput.isNotBlank()
                        ) {
                            if (propagationLoading) {
                                CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Check", modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("CHECK PROPAGATION", fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(propagationResults) { result ->
                                NeonCard(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(result.server, color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            Text("${result.latencyMs}ms", color = if (result.latencyMs < 100) NeonGreen else androidx.compose.ui.graphics.Color(0xFFFF6600), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        Text(result.ip, color = androidx.compose.ui.graphics.Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        result.error?.let { err ->
                                            Text("Error: $err", color = androidx.compose.ui.graphics.Color(0xFFFF4444), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        if (result.records.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            result.records.forEach { rec ->
                                                Text("${rec.type}: ${rec.value}", color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        } else if (result.error == null) {
                                            Text("No records found", color = androidx.compose.ui.graphics.Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
