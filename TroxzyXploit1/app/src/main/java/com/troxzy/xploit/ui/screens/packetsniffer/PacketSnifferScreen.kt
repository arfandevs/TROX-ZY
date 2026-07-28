package com.troxzy.xploit.ui.screens.packetsniffer

import android.content.Intent
import android.net.VpnService
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
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
import kotlinx.coroutines.delay
import kotlin.random.Random

private data class PacketInfo(
    val id: Long,
    val timestamp: String,
    val sourceIp: String,
    val destIp: String,
    val protocol: String,
    val length: Int,
    val info: String,
    val rawData: ByteArray = ByteArray(0)
)

private data class ConnectionTrack(
    val sourceIp: String,
    val destIp: String,
    val packets: Int,
    val bytes: Long
)

private val PROTOCOLS = listOf("TCP", "UDP", "HTTP", "DNS", "TLS", "ICMP")
private val SIMULATED_IPS = listOf(
    "192.168.1.1", "10.0.0.1", "172.16.0.1", "8.8.8.8", "1.1.1.1",
    "142.250.80.46", "151.101.1.140", "13.107.42.14", "31.13.65.36",
    "52.85.132.99", "104.244.42.1", "185.199.108.153", "140.82.121.4"
)

private fun generateSimulatedPacket(id: Long): PacketInfo {
    val protocol = PROTOCOLS[Random.nextInt(PROTOCOLS.size)]
    val srcIp = SIMULATED_IPS[Random.nextInt(SIMULATED_IPS.size)]
    val destIp = SIMULATED_IPS[Random.nextInt(SIMULATED_IPS.size)]
    val length = when (protocol) {
        "DNS" -> Random.nextInt(28, 512)
        "HTTP" -> Random.nextInt(64, 4096)
        "TLS" -> Random.nextInt(64, 2048)
        "ICMP" -> Random.nextInt(28, 1500)
        else -> Random.nextInt(40, 1500)
    }
    val now = System.currentTimeMillis()
    val ts = String.format(
        "%02d:%02d:%02d.%03d",
        (now / 3600000) % 24,
        (now / 60000) % 60,
        (now / 1000) % 60,
        now % 1000
    )
    val info = when (protocol) {
        "TCP" -> "${srcIp}:${Random.nextInt(1024, 65535)} → ${destIp}:${Random.nextInt(1, 1024)} [${if (Random.nextBoolean()) "SYN" else "ACK"}]"
        "UDP" -> "${srcIp}:${Random.nextInt(1024, 65535)} → ${destIp}:${Random.nextInt(1, 1024)} Len=$length"
        "HTTP" -> "${if (Random.nextBoolean()) "GET" else "POST"} /${listOf("index.html", "api/v1/data", "assets/img.png", "login").random()} HTTP/1.1"
        "DNS" -> "Standard query A ${listOf("google.com", "example.com", "github.com", "reddit.com").random()}"
        "TLS" -> "Client Hello, Version: TLS 1.3, SNI: ${listOf("google.com", "example.com").random()}"
        "ICMP" -> "Echo (ping) request, id=0x${Random.nextInt(0, 65535).toString(16)}, seq=${Random.nextInt(0, 256)}"
        else -> ""
    }
    val rawData = ByteArray(minOf(length, 256)) { Random.nextInt(0, 256).toByte() }
    return PacketInfo(id, ts, srcIp, destIp, protocol, length, info, rawData)
}

private fun formatHexDump(data: ByteArray): String {
    val sb = StringBuilder()
    var offset = 0
    while (offset < data.size) {
        sb.append(String.format("%08X  ", offset))
        val hexPart = StringBuilder()
        val asciiPart = StringBuilder()
        for (i in 0 until 16) {
            if (offset + i < data.size) {
                val b = data[offset + i].toInt() and 0xFF
                hexPart.append(String.format("%02X ", b))
                asciiPart.append(if (b in 32..126) b.toChar() else '.')
            } else {
                hexPart.append("   ")
                asciiPart.append(' ')
            }
            if (i == 7) hexPart.append(" ")
        }
        sb.append(hexPart).append(" |").append(asciiPart).append("|\n")
        offset += 16
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketSnifferScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var isCapturing by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val packets = remember { mutableStateListOf<PacketInfo>() }
    val connections = remember { mutableStateListOf<ConnectionTrack>() }
    var selectedPacket by remember { mutableStateOf<PacketInfo?>(null) }
    var filterIp by remember { mutableStateOf("") }
    var filterPort by remember { mutableStateOf("") }
    var filterProtocol by remember { mutableStateOf("") }
    var filterKeyword by remember { mutableStateOf("") }
    var protocolExpanded by remember { mutableStateOf(false) }
    var bandwidthHistory by remember { mutableStateOf(listOf<Float>()) }
    var packetIdCounter by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isCapturing) {
        while (isCapturing) {
            delay(200)
            val newPacket = generateSimulatedPacket(packetIdCounter++)
            packets.add(0, newPacket)
            if (packets.size > 500) packets.removeRange(0, packets.size - 500)

            val existing = connections.find { it.sourceIp == newPacket.sourceIp && it.destIp == newPacket.destIp }
            if (existing != null) {
                val idx = connections.indexOf(existing)
                connections[idx] = existing.copy(
                    packets = existing.packets + 1,
                    bytes = existing.bytes + newPacket.length
                )
            } else {
                connections.add(ConnectionTrack(newPacket.sourceIp, newPacket.destIp, 1, newPacket.length.toLong()))
            }

            val throughput = packets.take(10).sumOf { it.length }.toFloat() / 10f
            bandwidthHistory = (bandwidthHistory + throughput).takeLast(60)
        }
    }

    val filteredPackets = packets.filter { pkt ->
        (filterIp.isEmpty() || pkt.sourceIp.contains(filterIp) || pkt.destIp.contains(filterIp)) &&
        (filterPort.isEmpty() || pkt.info.contains(filterPort)) &&
        (filterProtocol.isEmpty() || pkt.protocol.equals(filterProtocol, ignoreCase = true)) &&
        (filterKeyword.isEmpty() || pkt.info.contains(filterKeyword, ignoreCase = true))
    }

    val protocolColor: (String) -> Color = { proto ->
        when (proto) {
            "TCP" -> NeonCyan
            "UDP" -> NeonPurple
            "HTTP" -> NeonGreen
            "DNS" -> Color(0xFFFF6600)
            "TLS" -> Color(0xFFFFD700)
            "ICMP" -> Color(0xFFFF4444)
            else -> Color.White
        }
    }

    CommonScaffold(
        title = "Packet Sniffer",
        currentRoute = "packet_sniffer",
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

            // Control buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!isCapturing) {
                            val intent = VpnService.prepare(context)
                            if (intent != null) {
                                context.startActivity(intent)
                            } else {
                                isCapturing = true
                            }
                        } else {
                            isCapturing = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCapturing) Color(0xFF8B0000) else NeonGreen.copy(alpha = 0.2f),
                        contentColor = if (isCapturing) Color.White else NeonGreen
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isCapturing) "Stop" else "Start",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isCapturing) "STOP" else "START", fontFamily = FontFamily.Monospace)
                }

                Button(
                    onClick = {
                        isCapturing = false
                        packets.clear()
                        connections.clear()
                        bandwidthHistory = emptyList()
                        packetIdCounter = 0L
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = {
                        // Save capture to file - in production would use ContentResolver
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurface,
                        contentColor = NeonCyan
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(18.dp))
                }
            }

            if (isCapturing) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Capturing... ${packets.size} packets",
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter row
            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("FILTERS", color = NeonPurple, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = filterIp,
                            onValueChange = { filterIp = it },
                            label = { Text("IP", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.width(110.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
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
                        OutlinedTextField(
                            value = filterPort,
                            onValueChange = { filterPort = it },
                            label = { Text("Port", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.width(80.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
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
                        ExposedDropdownMenuBox(
                            expanded = protocolExpanded,
                            onExpandedChange = { protocolExpanded = it },
                            modifier = Modifier.width(100.dp)
                        ) {
                            OutlinedTextField(
                                value = filterProtocol,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Proto", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = protocolExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkSurface,
                                    focusedLabelColor = NeonCyan
                                )
                            )
                            ExposedDropdownMenu(
                                containerColor = DarkCard,
                                expanded = protocolExpanded,
                                onDismissRequest = { protocolExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All", color = Color.White, fontFamily = FontFamily.Monospace) },
                                    onClick = { filterProtocol = ""; protocolExpanded = false }
                                )
                                PROTOCOLS.forEach { proto ->
                                    DropdownMenuItem(
                                        text = { Text(proto, color = protocolColor(proto), fontFamily = FontFamily.Monospace) },
                                        onClick = { filterProtocol = proto; protocolExpanded = false }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = filterKeyword,
                            onValueChange = { filterKeyword = it },
                            label = { Text("Search", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp), tint = NeonCyan) },
                            modifier = Modifier.width(120.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 11.sp,
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            val tabs = listOf("Packets (${filteredPackets.size})", "Bandwidth", "Connections (${connections.size})")
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
                    // Packet list
                    if (filteredPackets.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AMOLEDBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GlitchText(
                                    text = if (isCapturing) "WAITING FOR PACKETS..." else "NO CAPTURE DATA",
                                    color = NeonPurple
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    if (isCapturing) "Packets will appear here" else "Press START to begin capture",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AMOLEDBlack)
                        ) {
                            items(filteredPackets, key = { it.id }) { pkt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkCard)
                                        .clickable { selectedPacket = pkt }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        pkt.timestamp,
                                        color = Color.Gray,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(72.dp)
                                    )
                                    Text(
                                        pkt.protocol,
                                        color = protocolColor(pkt.protocol),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "${pkt.sourceIp} → ${pkt.destIp}",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            pkt.info,
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        "${pkt.length}B",
                                        color = NeonCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                HorizontalDivider(color = DarkSurface, thickness = 0.5.dp)
                            }
                        }
                    }
                }
                1 -> {
                    // Bandwidth graph
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AMOLEDBlack)
                            .padding(8.dp)
                    ) {
                        Text(
                            "REAL-TIME THROUGHPUT",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val data = bandwidthHistory
                            if (data.isEmpty()) return@Canvas

                            val maxVal = data.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                            val padding = 40f

                            // Draw grid
                            for (i in 0..4) {
                                val y = padding + (canvasHeight - 2 * padding) * i / 4f
                                drawLine(
                                    color = Color(0xFF2A2A2A),
                                    start = Offset(padding, y),
                                    end = Offset(canvasWidth - padding, y),
                                    strokeWidth = 1f
                                )
                                drawContext.canvas.nativeCanvas.drawText(
                                    String.format("%.0f", maxVal * (1f - i / 4f)),
                                    0f,
                                    y + 4f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 20f
                                        typeface = android.graphics.Typeface.MONOSPACE
                                    }
                                )
                            }

                            // Draw line
                            if (data.size > 1) {
                                val path = Path()
                                val fillPath = Path()
                                val xStep = (canvasWidth - 2 * padding) / (data.size - 1).coerceAtLeast(1)

                                data.forEachIndexed { index, value ->
                                    val x = padding + index * xStep
                                    val y = padding + (canvasHeight - 2 * padding) * (1f - value / maxVal)
                                    if (index == 0) {
                                        path.moveTo(x, y)
                                        fillPath.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }
                                }

                                fillPath.lineTo(padding + (data.size - 1) * xStep, canvasHeight - padding)
                                fillPath.lineTo(padding, canvasHeight - padding)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(NeonCyan.copy(alpha = 0.15f), Color.Transparent),
                                        startY = padding,
                                        endY = canvasHeight - padding
                                    )
                                )
                                drawPath(
                                    path = path,
                                    color = NeonCyan,
                                    style = Stroke(width = 2f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("60s ago", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            Text("Now", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            "Peak: ${bandwidthHistory.maxOrNull()?.let { String.format("%.1f B/s", it) } ?: "0 B/s"}",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                2 -> {
                    // Connection tracking table
                    if (connections.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("NO CONNECTIONS TRACKED", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AMOLEDBlack)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface)
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text("SOURCE", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                Text("DEST", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                Text("PKTS", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(45.dp))
                                Text("BYTES", color = NeonPurple, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
                            }
                            LazyColumn {
                                items(connections) { conn ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkCard)
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(conn.sourceIp, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(conn.destIp, color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${conn.packets}", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(45.dp))
                                        Text("${conn.bytes}", color = NeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(60.dp))
                                    }
                                    HorizontalDivider(color = DarkSurface, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }

            // Packet detail dialog
            selectedPacket?.let { pkt ->
                AlertDialog(
                    onDismissRequest = { selectedPacket = null },
                    containerColor = DarkCard,
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                pkt.protocol,
                                color = protocolColor(pkt.protocol),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Packet Detail", color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                        }
                    },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            InfoRow("Time", pkt.timestamp)
                            InfoRow("Source", pkt.sourceIp)
                            InfoRow("Destination", pkt.destIp)
                            InfoRow("Length", "${pkt.length} bytes")
                            InfoRow("Info", pkt.info)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("HEX DUMP", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .background(AMOLEDBlack)
                                    .clip(RoundedCornerShape(4.dp))
                                    .padding(4.dp)
                            ) {
                                androidx.compose.foundation.horizontalScroll(rememberScrollState()) {
                                    Text(
                                        formatHexDump(pkt.rawData),
                                        color = NeonGreen,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedPacket = null }) {
                            Text("CLOSE", color = NeonCyan, fontFamily = FontFamily.Monospace)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            "$label: ",
            color = NeonPurple,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            value,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
