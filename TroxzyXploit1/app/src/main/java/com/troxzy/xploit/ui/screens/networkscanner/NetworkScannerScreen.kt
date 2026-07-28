package com.troxzy.xploit.ui.screens.networkscanner

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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.net.InetAddress
import java.net.NetworkInterface

data class DeviceInfo(
    val ip: String,
    val mac: String,
    val vendor: String,
    val hostname: String,
    val openPorts: List<Int>,
    val isOnline: Boolean
)

object NetworkUtils {
    fun getLocalSubnet(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addresses = intf.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is InetAddress && addr.hostAddress?.contains('.') == true) {
                        val parts = addr.hostAddress!!.split(".")
                        return "${parts[0]}.${parts[1]}.${parts[2]}.0/24"
                    }
                }
            }
            "192.168.1.0/24"
        } catch (e: Exception) {
            "192.168.1.0/24"
        }
    }

    val commonPorts = listOf(21, 22, 23, 25, 53, 80, 110, 143, 443, 445, 993, 995, 1433, 3306, 3389, 5432, 5900, 8080, 8443, 8888)

    fun getMacVendor(mac: String): String {
        val prefix = mac.uppercase().take(8)
        val vendorMap = mapOf(
            "00:1A:2B" to "Vendor-A",
            "00:50:56" to "VMware",
            "00:0C:29" to "VMware",
            "00:1C:42" to "Parallels",
            "AA:BB:CC" to "Generic",
            "DC:A6:32" to "Raspberry Pi",
            "B8:27:EB" to "Raspberry Pi",
            "00:15:5D" to "Microsoft"
        )
        return vendorMap.entries.find { prefix.startsWith(it.key.replace(":", "").take(6)) }?.value ?: "Unknown"
    }
}

enum class ScanFilter { ALL, ONLINE, HAS_OPEN_PORTS }
enum class ScanSort { IP, HOSTNAME }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NetworkScannerScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var ipRange by remember { mutableStateOf(NetworkUtils.getLocalSubnet()) }
    var threadCount by remember { mutableStateOf(10f) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var devices by remember { mutableStateOf<List<DeviceInfo>>(emptyList()) }
    var foundCount by remember { mutableStateOf(0) }
    var currentFilter by remember { mutableStateOf(ScanFilter.ALL) }
    var currentSort by remember { mutableStateOf(ScanSort.IP) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var scanHistory by remember { mutableStateOf<List<List<DeviceInfo>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var totalHosts by remember { mutableStateOf(0) }

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

    fun parseIpRange(range: String): Pair<String, Int>? {
        val parts = range.split("/")
        if (parts.size != 2) return null
        val ipParts = parts[0].split(".")
        if (ipParts.size != 4) return null
        val cidr = parts[1].toIntOrNull() ?: return null
        if (cidr < 16 || cidr > 32) return null
        return Pair(parts[0], cidr)
    }

    fun getIpList(baseIp: String, cidr: Int): List<String> {
        val parts = baseIp.split(".").map { it.toInt() }
        val hostBits = 32 - cidr
        val baseAddr = (parts[0] shl 24) or (parts[1] shl 16) or (parts[2] shl 8) or parts[3]
        val mask = (0xFFFFFFFF shl hostBits).toInt()
        val networkAddr = baseAddr and mask
        val numHosts = (1 shl hostBits) - 2
        return (1..numHosts).map { offset ->
            val addr = networkAddr + offset
            "${(addr shr 24) and 0xFF}.${(addr shr 16) and 0xFF}.${(addr shr 8) and 0xFF}.${addr and 0xFF}"
        }
    }

    suspend fun scanNetwork() {
        val parsed = parseIpRange(ipRange)
        if (parsed == null) {
            errorMessage = "Invalid IP range format. Use CIDR notation (e.g., 192.168.1.0/24)"
            return
        }
        errorMessage = null
        isScanning = true
        scanProgress = 0f
        foundCount = 0
        devices = emptyList()

        val (baseIp, cidr) = parsed
        val ipList = getIpList(baseIp, cidr)
        totalHosts = ipList.size
        val concurrency = threadCount.toInt().coerceIn(1, 100)
        val dispatcher = Dispatchers.IO.limitedParallelism(concurrency)
        val results = mutableListOf<DeviceInfo>()

        try {
            withContext(dispatcher) {
                val chunkSize = concurrency
                val chunks = ipList.chunked(chunkSize)
                var processed = 0

                for (chunk in chunks) {
                    coroutineScope {
                        chunk.map { ip ->
                            async(dispatcher) {
                                try {
                                    val addr = InetAddress.getByName(ip)
                                    val reachable = addr.isReachable(2000)
                                    if (reachable) {
                                        val hostname = try { addr.hostName } catch (_: Exception) { ip }
                                        val mac = try {
                                            val interfaces = NetworkInterface.getByInetAddress(addr)
                                            interfaces?.hardwareAddress?.joinToString(":") {
                                                String.format("%02X", it)
                                            } ?: "N/A"
                                        } catch (_: Exception) { "N/A" }
                                        val vendor = if (mac != "N/A") NetworkUtils.getMacVendor(mac) else "Unknown"
                                        val openPorts = mutableListOf<Int>()
                                        for (port in NetworkUtils.commonPorts) {
                                            try {
                                                val socket = java.net.Socket()
                                                socket.connect(java.net.InetSocketAddress(ip, port), 1000)
                                                socket.close()
                                                openPorts.add(port)
                                            } catch (_: Exception) { }
                                        }
                                        DeviceInfo(ip, mac, vendor, hostname, openPorts, true)
                                    } else {
                                        DeviceInfo(ip, "N/A", "Unknown", ip, emptyList(), false)
                                    }
                                } catch (_: Exception) {
                                    DeviceInfo(ip, "N/A", "Unknown", ip, emptyList(), false)
                                }
                            }
                        }.awaitAll().forEach { device ->
                            synchronized(results) {
                                results.add(device)
                                if (device.isOnline) foundCount++
                            }
                        }
                    }
                    processed += chunk.size
                    scanProgress = processed.toFloat() / ipList.size
                }
            }
        } catch (e: Exception) {
            errorMessage = "Scan error: ${e.message}"
        }

        devices = results.toList()
        if (results.isNotEmpty()) {
            scanHistory = scanHistory + listOf(results)
        }
        isScanning = false
        scanProgress = 1f
    }

    fun exportAsJson(): String {
        val sb = StringBuilder()
        sb.append("[\n")
        devices.forEachIndexed { index, device ->
            sb.append("  {\n")
            sb.append("    \"ip\": \"${device.ip}\",\n")
            sb.append("    \"mac\": \"${device.mac}\",\n")
            sb.append("    \"vendor\": \"${device.vendor}\",\n")
            sb.append("    \"hostname\": \"${device.hostname}\",\n")
            sb.append("    \"openPorts\": [${device.openPorts.joinToString(",")}],\n")
            sb.append("    \"isOnline\": ${device.isOnline}\n")
            sb.append("  }${if (index < devices.size - 1) "," else ""}\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun exportAsTxt(): String {
        val sb = StringBuilder()
        sb.append("Network Scan Results\n")
        sb.append("Range: $ipRange\n")
        sb.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\n")
        sb.append("=" .repeat(60)).append("\n")
        devices.filter { it.isOnline }.forEach { device ->
            sb.append("IP: ${device.ip}\n")
            sb.append("MAC: ${device.mac}\n")
            sb.append("Vendor: ${device.vendor}\n")
            sb.append("Hostname: ${device.hostname}\n")
            sb.append("Open Ports: ${device.openPorts.joinToString(", ")}\n")
            sb.append("-".repeat(40)).append("\n")
        }
        return sb.toString()
    }

    val filteredDevices = devices
        .let { list ->
            when (currentFilter) {
                ScanFilter.ALL -> list
                ScanFilter.ONLINE -> list.filter { it.isOnline }
                ScanFilter.HAS_OPEN_PORTS -> list.filter { it.openPorts.isNotEmpty() }
            }
        }
        .sortedWith(
            when (currentSort) {
                ScanSort.IP -> compareBy { it.ip }
                ScanSort.HOSTNAME -> compareBy { it.hostname }
            }
        )

    CommonScaffold(
        title = "Network Scanner",
        currentRoute = "network_scanner",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AMOLEDBlack)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlitchText(
                text = "NETWORK SCANNER",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
            )

            OutlinedTextField(
                value = ipRange,
                onValueChange = { ipRange = it },
                label = { Text("IP Range (CIDR)", color = Color.Gray) },
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
                            kotlinx.coroutines.launch { scanNetwork() }
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
                    Icon(Icons.Filled.Wifi, contentDescription = "Scan", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("START SCAN", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isScanning || scanProgress > 0f) {
                Column {
                    LinearProgressIndicator(
                        progress = { scanProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonPurple,
                        trackColor = DarkElevated,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${(scanProgress * 100).toInt()}%",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Found: $foundCount online / $totalHosts total",
                            color = NeonGreen,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScanFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { currentFilter = filter },
                            label = {
                                Text(
                                    when (filter) {
                                        ScanFilter.ALL -> "All"
                                        ScanFilter.ONLINE -> "Online"
                                        ScanFilter.HAS_OPEN_PORTS -> "Open Ports"
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

                Row {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort", tint = NeonCyan)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            ScanSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            when (sort) {
                                                ScanSort.IP -> "By IP"
                                                ScanSort.HOSTNAME -> "By Hostname"
                                            },
                                            color = if (currentSort == sort) NeonCyan else Color.White
                                        )
                                    },
                                    onClick = {
                                        currentSort = sort
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.Download, contentDescription = "Export", tint = NeonGreen)
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false },
                            modifier = Modifier.background(DarkCard)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export JSON", color = Color.White) },
                                onClick = {
                                    showExportMenu = false
                                    val json = exportAsJson()
                                    android.util.Log.d("NetworkScanner", "JSON Export: ${json.take(200)}")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export TXT", color = Color.White) },
                                onClick = {
                                    showExportMenu = false
                                    val txt = exportAsTxt()
                                    android.util.Log.d("NetworkScanner", "TXT Export: ${txt.take(200)}")
                                }
                            )
                        }
                    }

                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = "History",
                            tint = if (showHistory) NeonPurple else Color.Gray
                        )
                    }
                }
            }

            if (showHistory && scanHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        "Scan History (${scanHistory.size} scans)",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    scanHistory.forEachIndexed { index, scan ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    devices = scan
                                    foundCount = scan.count { it.isOnline }
                                    showHistory = false
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Scan #${index + 1}",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                "${scan.count { it.isOnline }} online",
                                color = NeonGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            if (filteredDevices.isEmpty() && !isScanning && scanProgress == 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.WifiOff,
                            contentDescription = "No devices",
                            modifier = Modifier.size(48.dp),
                            tint = DarkElevated
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No devices found", color = Color.Gray, fontSize = 14.sp)
                        Text("Start a scan to discover devices", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredDevices, key = { it.ip }) { device ->
                        NeonCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Canvas(modifier = Modifier.size(10.dp)) {
                                            drawCircle(
                                                color = if (device.isOnline) NeonGreen else Color.Red,
                                                radius = 5f
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            device.ip,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        device.hostname,
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(0.4f, fill = false)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "MAC: ${device.mac}",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "Vendor: ${device.vendor}",
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                                if (device.openPorts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        device.openPorts.forEach { port ->
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        NeonPurple.copy(alpha = 0.2f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        NeonPurple.copy(alpha = 0.5f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    port.toString(),
                                                    color = NeonPurple,
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
    }
}
