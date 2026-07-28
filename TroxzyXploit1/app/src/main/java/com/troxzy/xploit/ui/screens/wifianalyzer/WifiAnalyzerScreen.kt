package com.troxzy.xploit.ui.screens.wifianalyzer

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val level: Int,
    val frequency: Int,
    val channel: Int,
    val securityType: String,
    val isHidden: Boolean,
    val capabilities: String
)

object WifiChannelMapper {
    private val freq2_4Ghz = mapOf(
        2412 to 1, 2417 to 2, 2422 to 3, 2427 to 4, 2432 to 5,
        2437 to 6, 2442 to 7, 2447 to 8, 2452 to 9, 2457 to 10,
        2462 to 11, 2467 to 12, 2472 to 13, 2484 to 14
    )
    private val freq5Ghz = mapOf(
        5170 to 34, 5180 to 36, 5190 to 38, 5200 to 40, 5210 to 42,
        5220 to 44, 5230 to 46, 5240 to 48, 5250 to 50, 5260 to 52,
        5270 to 54, 5280 to 56, 5290 to 58, 5300 to 60, 5310 to 62,
        5320 to 64, 5500 to 100, 5510 to 102, 5520 to 104, 5530 to 106,
        5540 to 108, 5550 to 110, 5560 to 112, 5570 to 114, 5580 to 116,
        5590 to 118, 5600 to 120, 5610 to 122, 5620 to 124, 5630 to 126,
        5640 to 128, 5660 to 132, 5670 to 134, 5680 to 136, 5690 to 138,
        5700 to 140, 5710 to 142, 5720 to 144, 5745 to 149, 5755 to 151,
        5765 to 153, 5775 to 155, 5785 to 157, 5795 to 159, 5805 to 161,
        5825 to 165
    )

    fun frequencyToChannel(freq: Int): Int {
        return freq2_4Ghz[freq] ?: freq5Ghz[freq] ?: ((freq - 2407) / 5)
    }

    fun getBand(freq: Int): String {
        return if (freq < 3000) "2.4 GHz" else if (freq < 6000) "5 GHz" else "6 GHz"
    }
}

object WifiSecurityParser {
    fun parseSecurityType(capabilities: String): String {
        val caps = capabilities.uppercase()
        return when {
            caps.contains("SAE") || caps.contains("OWE") -> "WPA3"
            caps.contains("WPA3") -> "WPA3"
            caps.contains("WPA2") -> "WPA2"
            caps.contains("WPA") -> "WPA"
            caps.contains("WEP") -> "WEP"
            else -> "Open"
        }
    }
}

fun getSignalColor(level: Int): Color {
    return when {
        level > -50 -> NeonGreen
        level > -60 -> Color(0xFF00CC33)
        level > -70 -> Color.Yellow
        level > -80 -> Color(0xFFFF9100)
        else -> Color.Red
    }
}

fun getSignalLabel(level: Int): String {
    return when {
        level > -50 -> "Excellent"
        level > -60 -> "Good"
        level > -70 -> "Fair"
        level > -80 -> "Weak"
        else -> "Very Weak"
    }
}

fun ScanResult.toWifiNetworkInfo(): WifiNetworkInfo {
    val channel = WifiChannelMapper.frequencyToChannel(frequency)
    val securityType = WifiSecurityParser.parseSecurityType(capabilities)
    val isHidden = ssid.isNullOrEmpty()
    return WifiNetworkInfo(
        ssid = if (isHidden) "<Hidden Network>" else ssid,
        bssid = bssid,
        level = level,
        frequency = frequency,
        channel = channel,
        securityType = securityType,
        isHidden = isHidden,
        capabilities = capabilities
    )
}

fun exportToCsv(networks: List<WifiNetworkInfo>): String {
    val sb = StringBuilder()
    sb.append("SSID,BSSID,Signal(dBm),Channel,Frequency,Security,Hidden\n")
    networks.forEach { n ->
        sb.append("\"${n.ssid}\",\"${n.bssid}\",${n.level},${n.channel},${n.frequency},${n.securityType},${n.isHidden}\n")
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAnalyzerScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val wifiManager = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }

    var networks by remember { mutableStateOf<List<WifiNetworkInfo>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasWifiPermission by remember { mutableStateOf(false) }
    var selectedNetwork by remember { mutableStateOf<WifiNetworkInfo?>(null) }
    var showChannelGraph by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "signalPulse")
    val signalAnim by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signalAnim"
    )

    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        hasWifiPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_WIFI_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun performScan() {
        if (!hasLocationPermission || !hasWifiPermission) {
            errorMessage = "WiFi and Location permissions are required. Please grant them in Settings."
            return
        }
        errorMessage = null
        isScanning = true
        try {
            wifiManager.startScan()
            val results = wifiManager.scanResults.map { it.toWifiNetworkInfo() }
            networks = results.sortedByDescending { it.level }
        } catch (e: SecurityException) {
            errorMessage = "Permission denied: ${e.message}"
        } catch (e: Exception) {
            errorMessage = "Scan error: ${e.message}"
        }
        isScanning = false
    }

    val hiddenNetworks = networks.filter { it.isHidden }
    val channelOccupancy = networks.groupBy { it.channel }
        .mapValues { it.value.size }

    fun getBestChannelRecommendation(): String {
        val channels2_4 = (1..13).toList()
        val nonOverlapping = listOf(1, 6, 11)
        val occupiedChannels = channelOccupancy.keys
        val bestChannel = nonOverlapping
            .filter { it !in occupiedChannels }
            .minByOrNull { channelOccupancy[it] ?: 0 } ?: nonOverlapping.minByOrNull { channelOccupancy[it] ?: 0 } ?: 1
        return "Channel $bestChannel (least congested among 1/6/11)"
    }

    CommonScaffold(
        title = "WiFi Analyzer",
        currentRoute = "wifi_analyzer",
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
                text = "WiFi ANALYZER",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
            )

            if (!hasLocationPermission || !hasWifiPermission) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Permission",
                            tint = Color.Yellow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "WiFi and Location permissions required for scanning. Please grant them in device Settings.",
                            color = Color.Yellow,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { performScan() },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isScanning
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeonCyan,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scanning...", fontSize = 13.sp)
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Scan", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SCAN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = { showChannelGraph = !showChannelGraph },
                    modifier = Modifier
                        .background(DarkElevated, RoundedCornerShape(8.dp))
                        .size(44.dp)
                ) {
                    Icon(
                        Icons.Filled.NetworkWifi,
                        contentDescription = "Channel Graph",
                        tint = if (showChannelGraph) NeonCyan else Color.Gray
                    )
                }

                IconButton(
                    onClick = {
                        val csv = exportToCsv(networks)
                        android.util.Log.d("WifiAnalyzer", "CSV Export: ${csv.take(200)}")
                    },
                    modifier = Modifier
                        .background(DarkElevated, RoundedCornerShape(8.dp))
                        .size(44.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Export CSV", tint = NeonGreen)
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

            if (networks.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${networks.size} networks found", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("Best: ${getBestChannelRecommendation()}", color = NeonGreen, fontSize = 11.sp)
                }
            }

            if (showChannelGraph && networks.isNotEmpty()) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Channel Occupancy", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        val maxCount = channelOccupancy.values.maxOrNull() ?: 1
                        val channels = channelOccupancy.keys.sorted()
                        val barWidth = 240f / channels.size.coerceAtLeast(1)
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        ) {
                            val canvasWidth = size.width
                            val canvasHeight = size.height
                            val padding = 30f
                            val drawWidth = canvasWidth - padding * 2
                            val drawHeight = canvasHeight - padding
                            val barSpacing = drawWidth / channels.size.coerceAtLeast(1)

                            drawLine(
                                color = Color.DarkGray,
                                start = Offset(padding, drawHeight),
                                end = Offset(canvasWidth - padding, drawHeight),
                                strokeWidth = 1f
                            )

                            channels.forEachIndexed { index, channel ->
                                val count = channelOccupancy[channel] ?: 0
                                val barHeight = (count.toFloat() / maxCount.toFloat()) * (drawHeight - 20f)
                                val x = padding + index * barSpacing + barSpacing * 0.15f

                                drawRoundRect(
                                    color = NeonPurple.copy(alpha = 0.8f),
                                    topLeft = Offset(x, drawHeight - barHeight),
                                    size = Size(barSpacing * 0.7f, barHeight),
                                    cornerRadius = CornerRadius(3f, 3f)
                                )

                                drawContext.canvas.nativeCanvas.apply {
                                    val paint = android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 9f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                    drawText("Ch$channel", x + barSpacing * 0.35f, drawHeight + 12f, paint)
                                    if (count > 0) {
                                        paint.color = android.graphics.Color.WHITE
                                        drawText(count.toString(), x + barSpacing * 0.35f, drawHeight - barHeight - 4f, paint)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (hiddenNetworks.isNotEmpty()) {
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Wifi,
                                contentDescription = "Hidden",
                                tint = Color.Yellow,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hidden Networks Detected: ${hiddenNetworks.size}", color = Color.Yellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        hiddenNetworks.forEach { network ->
                            Text(
                                "BSSID: ${network.bssid} | Ch: ${network.channel} | ${network.securityType}",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            selectedNetwork?.let { net ->
                NeonCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Signal Strength Meter", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${net.level} dBm",
                                color = getSignalColor(net.level),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                getSignalLabel(net.level),
                                color = getSignalColor(net.level),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(16.dp)
                                    .background(DarkElevated, RoundedCornerShape(4.dp))
                            ) {
                                val normalizedLevel = ((net.level + 100).coerceIn(0, 100)) / 100f
                                val animatedWidth = normalizedLevel * signalAnim
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedWidth)
                                        .background(
                                            getSignalColor(net.level),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SSID: ${net.ssid}", color = Color.Gray, fontSize = 11.sp)
                            Text("Ch: ${net.channel} | ${WifiChannelMapper.getBand(net.frequency)}", color = Color.Gray, fontSize = 11.sp)
                        }
                        Text("BSSID: ${net.bssid}", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Security: ${net.securityType}", color = NeonCyan, fontSize = 11.sp)
                    }
                }
            }

            if (networks.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Wifi,
                            contentDescription = "No networks",
                            modifier = Modifier.size(48.dp),
                            tint = DarkElevated
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No networks found", color = Color.Gray, fontSize = 14.sp)
                        Text("Tap SCAN to discover WiFi networks", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(networks, key = { it.bssid }) { network ->
                        NeonCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNetwork = network }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(0.65f, fill = false)
                                    ) {
                                        Canvas(modifier = Modifier.size(8.dp)) {
                                            drawCircle(
                                                color = getSignalColor(network.level),
                                                radius = 4f
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            network.ssid,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
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
                                                    when (network.securityType) {
                                                        "WPA3" -> NeonGreen.copy(alpha = 0.15f)
                                                        "WPA2" -> NeonCyan.copy(alpha = 0.15f)
                                                        "WPA" -> Color.Yellow.copy(alpha = 0.15f)
                                                        "WEP" -> Color(0xFFFF9100).copy(alpha = 0.15f)
                                                        else -> Color.Red.copy(alpha = 0.15f)
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    when (network.securityType) {
                                                        "WPA3" -> NeonGreen.copy(alpha = 0.5f)
                                                        "WPA2" -> NeonCyan.copy(alpha = 0.5f)
                                                        "WPA" -> Color.Yellow.copy(alpha = 0.5f)
                                                        "WEP" -> Color(0xFFFF9100).copy(alpha = 0.5f)
                                                        else -> Color.Red.copy(alpha = 0.5f)
                                                    },
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                network.securityType,
                                                color = when (network.securityType) {
                                                    "WPA3" -> NeonGreen
                                                    "WPA2" -> NeonCyan
                                                    "WPA" -> Color.Yellow
                                                    "WEP" -> Color(0xFFFF9100)
                                                    else -> Color.Red
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            "${network.level}dBm",
                                            color = getSignalColor(network.level),
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "BSSID: ${network.bssid}",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        "Ch ${network.channel} | ${WifiChannelMapper.getBand(network.frequency)} | ${network.frequency}MHz",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (network.isHidden) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Hidden Network",
                                        color = Color.Yellow,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
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
