package com.troxzy.xploit.ui.screens.portscanner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class PortResult(
    val port: Int,
    val protocol: String,
    val state: PortState,
    val service: String,
    val version: String
)

enum class PortState { OPEN, CLOSED, FILTERED }

object PortServiceMap {
    private val serviceMap = mapOf(
        21 to "FTP" to "File Transfer Protocol",
        22 to "SSH" to "Secure Shell",
        23 to "Telnet" to "Telnet Protocol",
        25 to "SMTP" to "Simple Mail Transfer",
        53 to "DNS" to "Domain Name System",
        80 to "HTTP" to "Hypertext Transfer Protocol",
        110 to "POP3" to "Post Office Protocol",
        143 to "IMAP" to "Internet Message Access",
        443 to "HTTPS" to "HTTP over TLS",
        445 to "SMB" to "Server Message Block",
        993 to "IMAPS" to "IMAP over TLS",
        995 to "POP3S" to "POP3 over TLS",
        1433 to "MSSQL" to "Microsoft SQL Server",
        3306 to "MySQL" to "MySQL Database",
        3389 to "RDP" to "Remote Desktop Protocol",
        5432 to "PostgreSQL" to "PostgreSQL Database",
        5900 to "VNC" to "Virtual Network Computing",
        6379 to "Redis" to "Redis Key-Value Store",
        8080 to "HTTP-Alt" to "HTTP Alternate",
        8443 to "HTTPS-Alt" to "HTTPS Alternate",
        8888 to "HTTP-Proxy" to "HTTP Proxy"
    )

    private val portToName = mapOf(
        21 to "FTP", 22 to "SSH", 23 to "Telnet", 25 to "SMTP", 53 to "DNS",
        80 to "HTTP", 110 to "POP3", 111 to "RPCBind", 135 to "MSRPC",
        139 to "NetBIOS", 143 to "IMAP", 443 to "HTTPS", 445 to "SMB",
        993 to "IMAPS", 995 to "POP3S", 1433 to "MSSQL", 1521 to "Oracle",
        3306 to "MySQL", 3389 to "RDP", 5432 to "PostgreSQL", 5900 to "VNC",
        6379 to "Redis", 8080 to "HTTP-Alt", 8443 to "HTTPS-Alt", 8888 to "HTTP-Proxy",
        27017 to "MongoDB"
    )

    fun getServiceName(port: Int): String = portToName[port] ?: "Unknown"

    val commonPorts = listOf(21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 993, 995, 1433, 3306, 3389, 5432, 5900, 6379, 8080, 8443)
    val webPorts = listOf(80, 443, 8080, 8443, 8888, 3000, 5000, 8000, 9000)
}

enum class PortPreset { CUSTOM, COMMON, WEB, ALL }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PortScannerScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var targetHost by remember { mutableStateOf("") }
    var portStart by remember { mutableStateOf("1") }
    var portEnd by remember { mutableStateOf("1024") }
    var threadCount by remember { mutableStateOf(20f) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var results by remember { mutableStateOf<List<PortResult>>(emptyList()) }
    var currentPreset by remember { mutableStateOf(PortPreset.COMMON) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalPorts by remember { mutableStateOf(0) }
    var scannedPorts by remember { mutableStateOf(0) }
    var scanHistory by remember { mutableStateOf<List<List<PortResult>>>(emptyList()) }

    val infiniteTransition = rememberInfiniteTransition(label = "scanPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    fun getPortList(): List<Int> {
        return when (currentPreset) {
            PortPreset.COMMON -> PortServiceMap.commonPorts
            PortPreset.WEB -> PortServiceMap.webPorts
            PortPreset.ALL -> (portStart.toIntOrNull() ?: 1)..(portEnd.toIntOrNull() ?: 1024) step 1
            PortPreset.CUSTOM -> {
                val start = portStart.toIntOrNull() ?: 1
                val end = portEnd.toIntOrNull() ?: 1024
                if (start > end) emptyList() else (start..end).toList()
            }
        }
    }

    suspend fun scanPorts() {
        val host = targetHost.trim()
        if (host.isEmpty()) {
            errorMessage = "Please enter a target host or IP"
            return
        }
        errorMessage = null
        isScanning = true
        scanProgress = 0f
        scannedPorts = 0
        results = emptyList()

        val portList = getPortList()
        if (portList.isEmpty()) {
            errorMessage = "No ports to scan. Check your port range."
            isScanning = false
            return
        }
        totalPorts = portList.size

        val concurrency = threadCount.toInt().coerceIn(1, 100)
        val dispatcher = Dispatchers.IO.limitedParallelism(concurrency)
        val scanResults = mutableListOf<PortResult>()

        try {
            withContext(dispatcher) {
                val chunkSize = concurrency
                val chunks = portList.chunked(chunkSize)
                var processed = 0

                for (chunk in chunks) {
                    coroutineScope {
                        chunk.map { port ->
                            async(dispatcher) {
                                try {
                                    val socket = Socket()
                                    socket.connect(InetSocketAddress(host, port), 2000)
                                    socket.close()
                                    PortResult(
                                        port = port,
                                        protocol = "TCP",
                                        state = PortState.OPEN,
                                        service = PortServiceMap.getServiceName(port),
                                        version = ""
                                    )
                                } catch (e: java.net.ConnectException) {
                                    PortResult(
                                        port = port,
                                        protocol = "TCP",
                                        state = PortState.CLOSED,
                                        service = PortServiceMap.getServiceName(port),
                                        version = ""
                                    )
                                } catch (e: java.net.SocketTimeoutException) {
                                    PortResult(
                                        port = port,
                                        protocol = "TCP",
                                        state = PortState.FILTERED,
                                        service = PortServiceMap.getServiceName(port),
                                        version = ""
                                    )
                                } catch (e: Exception) {
                                    PortResult(
                                        port = port,
                                        protocol = "TCP",
                                        state = PortState.CLOSED,
                                        service = PortServiceMap.getServiceName(port),
                                        version = ""
                                    )
                                }
                            }
                        }.awaitAll().forEach { result ->
                            synchronized(scanResults) {
                                scanResults.add(result)
                            }
                        }
                    }
                    processed += chunk.size
                    scannedPorts = processed
                    scanProgress = processed.toFloat() / portList.size
                }
            }
        } catch (e: Exception) {
            errorMessage = "Scan error: ${e.message}"
        }

        results = scanResults.sortedBy { it.port }
        if (scanResults.isNotEmpty()) {
            scanHistory = scanHistory + listOf(scanResults)
        }
        isScanning = false
        scanProgress = 1f
    }

    fun exportResults(): String {
        val sb = StringBuilder()
        sb.append("Port Scan Results\n")
        sb.append("Target: $targetHost\n")
        sb.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
        sb.append("=".repeat(50)).append("\n")
        results.forEach { r ->
            sb.append("${r.port}/${r.protocol.lowercase()}\t${r.state.name.lowercase()}\t${r.service}\t${r.version}\n")
        }
        sb.append("\nOpen Ports: ${results.count { it.state == PortState.OPEN }}\n")
        sb.append("Closed Ports: ${results.count { it.state == PortState.CLOSED }}\n")
        sb.append("Filtered Ports: ${results.count { it.state == PortState.FILTERED }}\n")
        return sb.toString()
    }

    val openCount = results.count { it.state == PortState.OPEN }
    val closedCount = results.count { it.state == PortState.CLOSED }
    val filteredCount = results.count { it.state == PortState.FILTERED }

    CommonScaffold(
        title = "Port Scanner",
        currentRoute = "port_scanner",
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
                text = "PORT SCANNER",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
            )

            OutlinedTextField(
                value = targetHost,
                onValueChange = { targetHost = it },
                label = { Text("Target IP / Hostname", color = Color.Gray) },
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

            Text("Preset", color = Color.Gray, fontSize = 12.sp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PortPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = currentPreset == preset,
                        onClick = { currentPreset = preset },
                        label = {
                            Text(
                                when (preset) {
                                    PortPreset.CUSTOM -> "Custom"
                                    PortPreset.COMMON -> "Common"
                                    PortPreset.WEB -> "Web"
                                    PortPreset.ALL -> "All 1-1024"
                                },
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple.copy(alpha = 0.3f),
                            selectedLabelColor = NeonPurple,
                            containerColor = DarkElevated,
                            labelColor = Color.Gray
                        )
                    )
                }
            }

            if (currentPreset == PortPreset.CUSTOM || currentPreset == PortPreset.ALL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = portStart,
                        onValueChange = { portStart = it },
                        label = { Text("Start Port", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NeonCyan,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = DarkElevated,
                            cursorColor = NeonCyan
                        ),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = portEnd,
                        onValueChange = { portEnd = it },
                        label = { Text("End Port", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NeonCyan,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = DarkElevated,
                            cursorColor = NeonCyan
                        ),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Threads: ${threadCount.toInt()}",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(0.3f)
                )
                Slider(
                    value = threadCount,
                    onValueChange = { threadCount = it },
                    valueRange = 1f..100f,
                    modifier = Modifier.weight(0.7f),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPurple,
                        activeTrackColor = NeonPurple,
                        inactiveTrackColor = DarkElevated
                    )
                )
            }

            Button(
                onClick = {
                    if (!isScanning) {
                        kotlinx.coroutines.MainScope().apply {
                            kotlinx.coroutines.launch { scanPorts() }
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
                    Text("Scanning $scannedPorts/$totalPorts...", color = Color.White, fontSize = 14.sp)
                } else {
                    Icon(Icons.Filled.Security, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START SCAN", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isScanning || scanProgress > 0f) {
                LinearProgressIndicator(
                    progress = { scanProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonPurple,
                    trackColor = DarkElevated,
                    strokeCap = StrokeCap.Round
                )
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

            if (results.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Open: $openCount", color = NeonGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("Closed: $closedCount", color = Color.Red, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("Filtered: $filteredCount", color = Color.Yellow, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row {
                        IconButton(onClick = {
                            val export = exportResults()
                            android.util.Log.d("PortScanner", "Export: ${export.take(200)}")
                        }) {
                            Icon(Icons.Filled.Download, contentDescription = "Export", tint = NeonGreen)
                        }
                        IconButton(onClick = {
                            scanHistory = scanHistory + listOf(results)
                        }) {
                            Icon(Icons.Filled.Save, contentDescription = "Save", tint = NeonCyan)
                        }
                    }
                }
            }

            if (results.isEmpty() && !isScanning && scanProgress == 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Security,
                            contentDescription = "No results",
                            modifier = Modifier.size(48.dp),
                            tint = DarkElevated
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No scan results", color = Color.Gray, fontSize = 14.sp)
                        Text("Enter a target and start scanning", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(results, key = { it.port }) { result ->
                        NeonCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        result.port.toString(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "/${result.protocol.lowercase()}",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        result.service,
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (result.state) {
                                                    PortState.OPEN -> NeonGreen.copy(alpha = 0.2f)
                                                    PortState.CLOSED -> Color.Red.copy(alpha = 0.2f)
                                                    PortState.FILTERED -> Color.Yellow.copy(alpha = 0.2f)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                when (result.state) {
                                                    PortState.OPEN -> NeonGreen.copy(alpha = 0.5f)
                                                    PortState.CLOSED -> Color.Red.copy(alpha = 0.5f)
                                                    PortState.FILTERED -> Color.Yellow.copy(alpha = 0.5f)
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            result.state.name.lowercase().replaceFirstChar { it.uppercase() },
                                            color = when (result.state) {
                                                PortState.OPEN -> NeonGreen
                                                PortState.CLOSED -> Color.Red
                                                PortState.FILTERED -> Color.Yellow
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            if (result.version.isNotEmpty()) {
                                Text(
                                    "Version: ${result.version}",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
