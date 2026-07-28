package com.troxzy.xploit.ui.screens.mitm

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

data class PacketInfo(
    val id: Int,
    val sourceIp: String,
    val destIp: String,
    val protocol: String,
    val size: Int,
    val info: String,
    val timestamp: Long,
    val hexDump: String,
    val asciiDump: String
)

enum class PacketFilter { ALL, HTTP, DNS, TCP, UDP }

object SimulatedPacketGenerator {
    private var counter = 0
    private val protocols = listOf("TCP", "UDP", "HTTP", "DNS", "ICMP", "ARP")
    private val ips = listOf(
        "192.168.1.1", "192.168.1.100", "192.168.1.105",
        "192.168.1.110", "10.0.0.1", "10.0.0.5",
        "172.16.0.1", "8.8.8.8", "8.8.4.4", "1.1.1.1"
    )
    private val httpInfos = listOf(
        "GET /index.html HTTP/1.1",
        "POST /api/login HTTP/1.1",
        "GET /style.css HTTP/1.1",
        "200 OK Content-Type: text/html",
        "GET /api/data HTTP/1.1",
        "301 Moved Permanently",
        "404 Not Found",
        "GET /images/logo.png HTTP/1.1"
    )
    private val dnsInfos = listOf(
        "Query: example.com A",
        "Query: google.com A",
        "Response: example.com -> 93.184.216.34",
        "Query: api.service.com AAAA",
        "Response: google.com -> 142.250.80.46",
        "Query: cdn.example.com CNAME"
    )
    private val tcpInfos = listOf(
        "SYN -> Port 80",
        "SYN-ACK -> Port 80",
        "ACK Established",
        "FIN Connection Close",
        "RST Connection Reset",
        "SYN -> Port 443",
        "ACK -> Port 22",
        "PSH+ACK Data Transfer"
    )
    private val udpInfos = listOf(
        "Port 53 -> 53 DNS Query",
        "Port 123 -> 123 NTP Sync",
        "Port 5353 -> 5353 mDNS",
        "Port 1900 -> 1900 SSDP",
        "Port 5060 SIP Register",
        "Port 3478 STUN Binding"
    )

    fun generatePacket(): PacketInfo {
        counter++
        val protocol = protocols.random()
        val srcIp = ips.random()
        val destIp = ips.random()
        val size = (64..1500).random()
        val info = when (protocol) {
            "HTTP" -> httpInfos.random()
            "DNS" -> dnsInfos.random()
            "TCP" -> tcpInfos.random()
            "UDP" -> udpInfos.random()
            else -> "ICMP Echo Request" + if (protocol == "ARP") "ARP Who has $destIp? Tell $srcIp" else ""
        }
        val hexLines = (0..7).map { lineNum ->
            val hexBytes = (0..15).map { String.format("%02X", (lineNum * 16 + it) % 256) }
            hexBytes.joinToString(" ")
        }
        val hexDump = hexLines.joinToString("\n")
        val asciiLines = (0..7).map { lineNum ->
            val asciiChars = (0..15).map {
                val c = ((lineNum * 16 + it) % 95 + 32).toChar()
                if (c.isLetterOrDigit() || c in ' '..'~') c else '.'
            }
            asciiChars.joinToString("")
        }
        val asciiDump = asciiLines.joinToString("\n")
        return PacketInfo(
            id = counter,
            sourceIp = srcIp,
            destIp = destIp,
            protocol = protocol,
            size = size,
            info = info,
            timestamp = System.currentTimeMillis(),
            hexDump = hexDump,
            asciiDump = asciiDump
        )
    }
}

object PcapExporter {
    fun exportPcap(packets: List<PacketInfo>): ByteArray {
        val globalHeader = ByteArray(24)
        globalHeader[0] = 0xA1.toByte()
        globalHeader[1] = 0xB2.toByte()
        globalHeader[2] = 0xC3.toByte()
        globalHeader[3] = 0xD4.toByte()
        val versionMajor = 2
        globalHeader[4] = (versionMajor shr 8).toByte()
        globalHeader[5] = versionMajor.toByte()
        val versionMinor = 4
        globalHeader[6] = (versionMinor shr 8).toByte()
        globalHeader[7] = versionMinor.toByte()
        globalHeader[8] = 0
        globalHeader[9] = 0
        globalHeader[10] = 0
        globalHeader[11] = 0
        globalHeader[12] = 0
        globalHeader[13] = 0
        globalHeader[14] = 0
        globalHeader[15] = 0
        globalHeader[16] = 228.toByte()
        globalHeader[17] = 0
        globalHeader[18] = 0
        globalHeader[19] = 0
        globalHeader[20] = 1.toByte()
        globalHeader[21] = 0
        globalHeader[22] = 0
        globalHeader[23] = 0
        return globalHeader + packets.map { packet ->
            val packetHeader = ByteArray(16)
            val tsSec = (packet.timestamp / 1000).toInt()
            packetHeader[0] = (tsSec shr 24).toByte()
            packetHeader[1] = (tsSec shr 16).toByte()
            packetHeader[2] = (tsSec shr 8).toByte()
            packetHeader[3] = tsSec.toByte()
            val tsUsec = ((packet.timestamp % 1000) * 1000).toInt()
            packetHeader[4] = (tsUsec shr 24).toByte()
            packetHeader[5] = (tsUsec shr 16).toByte()
            packetHeader[6] = (tsUsec shr 8).toByte()
            packetHeader[7] = tsUsec.toByte()
            val inclLen = packet.size.coerceAtMost(1500)
            packetHeader[8] = (inclLen shr 24).toByte()
            packetHeader[9] = (inclLen shr 16).toByte()
            packetHeader[10] = (inclLen shr 8).toByte()
            packetHeader[11] = inclLen.toByte()
            packetHeader[12] = (inclLen shr 24).toByte()
            packetHeader[13] = (inclLen shr 16).toByte()
            packetHeader[14] = (inclLen shr 8).toByte()
            packetHeader[15] = inclLen.toByte()
            val packetData = ByteArray(inclLen) { (it % 256).toByte() }
            packetHeader + packetData
        }.reduce { acc, bytes -> acc + bytes }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MitmDashboardScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var targetIp by remember { mutableStateOf("") }
    var gatewayIp by remember { mutableStateOf("") }
    var isArpSpoofing by remember { mutableStateOf(false) }
    var showArpWarning by remember { mutableStateOf(false) }
    var packets by remember { mutableStateOf<List<PacketInfo>>(emptyList()) }
    var currentFilter by remember { mutableStateOf(PacketFilter.ALL) }
    var selectedPacket by remember { mutableStateOf<PacketInfo?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "mitmPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mitmPulse"
    )

    LaunchedEffect(isCapturing, isArpSpoofing) {
        while (isCapturing && isArpSpoofing) {
            kotlinx.coroutines.delay(800)
            val newPacket = SimulatedPacketGenerator.generatePacket()
            packets = packets + newPacket
        }
    }

    val filteredPackets = packets.filter { packet ->
        when (currentFilter) {
            PacketFilter.ALL -> true
            PacketFilter.HTTP -> packet.protocol == "HTTP"
            PacketFilter.DNS -> packet.protocol == "DNS"
            PacketFilter.TCP -> packet.protocol == "TCP"
            PacketFilter.UDP -> packet.protocol == "UDP"
        }
    }

    val protocolBreakdown = packets.groupBy { it.protocol }.mapValues { it.value.size }
    val topTalkers = packets.groupBy { it.sourceIp }
        .mapValues { it.value.size }
        .entries
        .sortedByDescending { it.value }
        .take(5)

    CommonScaffold(
        title = "MITM Dashboard",
        currentRoute = "mitm_dashboard",
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
                text = "MITM DASHBOARD",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
            )

            NeonCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Warning",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            "DISCLAIMER: This tool is for educational and authorized testing only.",
                            color = Color.Yellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Unauthorized interception of network traffic is illegal in most jurisdictions. Use only on networks you own or have explicit permission to test.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = targetIp,
                    onValueChange = { targetIp = it },
                    label = { Text("Target IP", color = Color.Gray) },
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
                    value = gatewayIp,
                    onValueChange = { gatewayIp = it },
                    label = { Text("Gateway IP", color = Color.Gray) },
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

            Button(
                onClick = { showArpWarning = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isArpSpoofing) Color.Red.copy(alpha = 0.7f) else DarkElevated,
                    contentColor = if (isArpSpoofing) Color.White else Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Filled.NetworkCheck,
                    contentDescription = "ARP Spoofing",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isArpSpoofing) "ARP SPOOFING ACTIVE" else "ENABLE ARP SPOOFING",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isArpSpoofing) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(
                            color = Color.Red.copy(alpha = pulseAlpha),
                            radius = 5f
                        )
                    }
                }
            }

            if (showArpWarning) {
                AlertDialog(
                    onDismissRequest = { showArpWarning = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = Color.Red, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ARP Spoofing Warning", color = Color.Red)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                "ARP spoofing intercepts network traffic between the target and gateway. This action:",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• Requires root access on the device", color = Color.Yellow, fontSize = 12.sp)
                            Text("• May disrupt network connectivity", color = Color.Yellow, fontSize = 12.sp)
                            Text("• Is ILLEGAL without authorization", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "This is a SIMULATED demonstration only. No actual ARP spoofing will be performed. The tool generates simulated packet data for educational purposes.",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                isArpSpoofing = !isArpSpoofing
                                isCapturing = isArpSpoofing
                                showArpWarning = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isArpSpoofing) Color.Red else NeonGreen
                            )
                        ) {
                            Text(
                                if (isArpSpoofing) "DISABLE" else "ENABLE (Simulated)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showArpWarning = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                        ) {
                            Text("Cancel")
                        }
                    },
                    containerColor = DarkCard
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PacketFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = currentFilter == filter,
                            onClick = { currentFilter = filter },
                            label = {
                                Text(
                                    when (filter) {
                                        PacketFilter.ALL -> "All"
                                        PacketFilter.HTTP -> "HTTP"
                                        PacketFilter.DNS -> "DNS"
                                        PacketFilter.TCP -> "TCP"
                                        PacketFilter.UDP -> "UDP"
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
                    IconButton(onClick = {
                        val pcapData = PcapExporter.exportPcap(packets)
                        android.util.Log.d("MitmDashboard", "PCAP Export: ${pcapData.size} bytes, ${packets.size} packets")
                    }) {
                        Icon(Icons.Filled.Download, contentDescription = "Export PCAP", tint = NeonGreen)
                    }
                    IconButton(onClick = {
                        packets = emptyList()
                        selectedPacket = null
                        SimulatedPacketGenerator.counter = 0
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = Color.Red)
                    }
                }
            }

            if (packets.isNotEmpty()) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Statistics", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total: ${packets.size}", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Captured", color = NeonGreen, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (protocolBreakdown.isNotEmpty()) {
                            Text("Protocol Breakdown", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val maxCount = protocolBreakdown.values.maxOrNull() ?: 1
                            val totalPackets = packets.size.toFloat()
                            val protocolColors = mapOf(
                                "TCP" to NeonCyan, "UDP" to Color(0xFFFF9100),
                                "HTTP" to NeonGreen, "DNS" to NeonPurple,
                                "ICMP" to Color.Yellow, "ARP" to Color.Red
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkElevated)
                            ) {
                                protocolBreakdown.entries.sortedByDescending { it.value }.forEach { (proto, count) ->
                                    val proportion = count.toFloat() / totalPackets
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .weight(proportion.coerceAtLeast(0.02f))
                                            .background(protocolColors[proto] ?: Color.Gray)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                protocolBreakdown.entries.sortedByDescending { it.value }.forEach { (proto, count) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(protocolColors[proto] ?: Color.Gray, RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("$proto: $count", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        if (topTalkers.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Top Talkers", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(3.dp))
                            topTalkers.forEach { (ip, count) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(ip, color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("$count packets", color = Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            selectedPacket?.let { packet ->
                AlertDialog(
                    onDismissRequest = { selectedPacket = null },
                    title = {
                        Text("Packet #${packet.id} Detail", color = NeonCyan, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Source:", color = Color.Gray, fontSize = 12.sp)
                                Text(packet.sourceIp, color = NeonGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dest:", color = Color.Gray, fontSize = 12.sp)
                                Text(packet.destIp, color = NeonGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Protocol:", color = Color.Gray, fontSize = 12.sp)
                                Text(packet.protocol, color = NeonCyan, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Size:", color = Color.Gray, fontSize = 12.sp)
                                Text("${packet.size} bytes", color = Color.White, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Info:", color = Color.Gray, fontSize = 12.sp)
                                Text(packet.info, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Hex Dump", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(DarkElevated, RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    packet.hexDump,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 10.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ASCII View", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .background(DarkElevated, RoundedCornerShape(4.dp))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    packet.asciiDump,
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { selectedPacket = null },
                            colors = ButtonDefaults.textButtonColors(contentColor = NeonCyan)
                        ) {
                            Text("Close")
                        }
                    },
                    containerColor = DarkCard
                )
            }

            if (filteredPackets.isEmpty() && !isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.NetworkCheck,
                            contentDescription = "No packets",
                            modifier = Modifier.size(48.dp),
                            tint = DarkElevated
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No packets captured", color = Color.Gray, fontSize = 14.sp)
                        Text("Enable ARP spoofing to begin capture", color = Color.DarkGray, fontSize = 12.sp)
                        Text("(Simulated data for educational purposes)", color = Color.DarkGray, fontSize = 11.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredPackets, key = { it.id }) { packet ->
                        NeonCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPacket = packet }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(0.45f, fill = false)
                                ) {
                                    Text(
                                        "#${packet.id}",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        packet.sourceIp,
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "->",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        packet.destIp,
                                        color = NeonGreen,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                when (packet.protocol) {
                                                    "HTTP" -> NeonGreen.copy(alpha = 0.15f)
                                                    "DNS" -> NeonPurple.copy(alpha = 0.15f)
                                                    "TCP" -> NeonCyan.copy(alpha = 0.15f)
                                                    "UDP" -> Color(0xFFFF9100).copy(alpha = 0.15f)
                                                    "ICMP" -> Color.Yellow.copy(alpha = 0.15f)
                                                    "ARP" -> Color.Red.copy(alpha = 0.15f)
                                                    else -> DarkElevated
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .border(
                                                1.dp,
                                                when (packet.protocol) {
                                                    "HTTP" -> NeonGreen.copy(alpha = 0.5f)
                                                    "DNS" -> NeonPurple.copy(alpha = 0.5f)
                                                    "TCP" -> NeonCyan.copy(alpha = 0.5f)
                                                    "UDP" -> Color(0xFFFF9100).copy(alpha = 0.5f)
                                                    "ICMP" -> Color.Yellow.copy(alpha = 0.5f)
                                                    "ARP" -> Color.Red.copy(alpha = 0.5f)
                                                    else -> Color.Gray
                                                },
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            packet.protocol,
                                            color = when (packet.protocol) {
                                                "HTTP" -> NeonGreen
                                                "DNS" -> NeonPurple
                                                "TCP" -> NeonCyan
                                                "UDP" -> Color(0xFFFF9100)
                                                "ICMP" -> Color.Yellow
                                                "ARP" -> Color.Red
                                                else -> Color.Gray
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        "${packet.size}B",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                packet.info,
                                color = Color.Gray,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
