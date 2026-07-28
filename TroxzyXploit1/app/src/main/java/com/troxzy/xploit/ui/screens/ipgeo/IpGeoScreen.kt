package com.troxzy.xploit.ui.screens.ipgeo

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class IpGeoData(
    val query: String = "",
    val status: String = "",
    val country: String = "",
    val countryCode: String = "",
    val region: String = "",
    val regionName: String = "",
    val city: String = "",
    val zip: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timezone: String = "",
    val isp: String = "",
    val org: String = "",
    val `as`: String = ""
)

private data class BatchGeoResult(
    val ip: String,
    val geo: IpGeoData?,
    val error: String? = null
)

// Simplified world map coastline coordinates (normalized 0-1 for lat/lon mapping)
// These are approximate continent outlines for visual representation
private val CONTINENT_PATHS = listOf(
    // North America (simplified)
    listOf(0.12f to 0.18f, 0.22f to 0.15f, 0.28f to 0.18f, 0.30f to 0.25f, 0.28f to 0.32f, 0.25f to 0.38f, 0.22f to 0.42f, 0.18f to 0.40f, 0.15f to 0.35f, 0.10f to 0.28f, 0.08f to 0.22f, 0.12f to 0.18f),
    // South America
    listOf(0.25f to 0.52f, 0.28f to 0.48f, 0.32f to 0.50f, 0.34f to 0.55f, 0.33f to 0.62f, 0.30f to 0.70f, 0.27f to 0.78f, 0.24f to 0.75f, 0.22f to 0.68f, 0.23f to 0.60f, 0.25f to 0.52f),
    // Europe
    listOf(0.46f to 0.15f, 0.52f to 0.14f, 0.56f to 0.16f, 0.54f to 0.22f, 0.52f to 0.28f, 0.48f to 0.30f, 0.45f to 0.28f, 0.44f to 0.22f, 0.46f to 0.15f),
    // Africa
    listOf(0.46f to 0.35f, 0.52f to 0.33f, 0.56f to 0.36f, 0.58f to 0.42f, 0.56f to 0.52f, 0.54f to 0.62f, 0.50f to 0.68f, 0.47f to 0.65f, 0.44f to 0.55f, 0.43f to 0.45f, 0.46f to 0.35f),
    // Asia
    listOf(0.56f to 0.14f, 0.65f to 0.12f, 0.75f to 0.15f, 0.82f to 0.18f, 0.85f to 0.25f, 0.82f to 0.32f, 0.78f to 0.38f, 0.72f to 0.42f, 0.65f to 0.40f, 0.60f to 0.35f, 0.56f to 0.28f, 0.56f to 0.14f),
    // Australia
    listOf(0.78f to 0.58f, 0.84f to 0.56f, 0.88f to 0.60f, 0.87f to 0.68f, 0.83f to 0.72f, 0.79f to 0.68f, 0.78f to 0.62f, 0.78f to 0.58f)
)

private suspend fun fetchIpGeo(ip: String): IpGeoData? = withContext(Dispatchers.IO) {
    try {
        val url = URL("http://ip-api.com/json/$ip")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val response = conn.inputStream.bufferedReader().readText()
            Json { ignoreUnknownKeys = true }.decodeFromString<IpGeoData>(response)
        } else null
    } catch (e: Exception) {
        null
    }
}

private suspend fun fetchMyIp(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.ipify.org")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.inputStream.bufferedReader().readText()
    } catch (e: Exception) {
        null
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@Composable
fun IpGeoScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var ipInput by remember { mutableStateOf("") }
    var geoData by remember { mutableStateOf<IpGeoData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var myIp by remember { mutableStateOf<String?>(null) }
    var myGeoData by remember { mutableStateOf<IpGeoData?>(null) }
    var distanceKm by remember { mutableStateOf<Double?>(null) }

    // Batch
    var batchInput by remember { mutableStateOf("") }
    val batchResults = remember { mutableStateListOf<BatchGeoResult>() }
    var batchLoading by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    // Auto-detect user's IP
    LaunchedEffect(Unit) {
        val ip = fetchMyIp()
        if (ip != null) {
            myIp = ip
            ipInput = ip
            myGeoData = fetchIpGeo(ip)
        }
    }

    CommonScaffold(
        title = "IP Geolocation",
        currentRoute = "ip_geo",
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text("IP Address", fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.weight(1f),
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
                IconButton(
                    onClick = {
                        if (myIp != null) {
                            ipInput = myIp!!
                        } else {
                            kotlinx.coroutines.MainScope().launch {
                                val ip = fetchMyIp()
                                if (ip != null) {
                                    myIp = ip
                                    ipInput = ip
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My IP", tint = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lookup button
            Button(
                onClick = {
                    if (ipInput.isNotBlank()) {
                        isLoading = true
                        errorMessage = null
                        geoData = null
                        distanceKm = null
                        kotlinx.coroutines.MainScope().launch {
                            val result = fetchIpGeo(ipInput)
                            if (result != null && result.status == "success") {
                                geoData = result
                                if (myGeoData != null && result.lat != 0.0 && result.lon != 0.0) {
                                    distanceKm = calculateDistance(
                                        myGeoData!!.lat, myGeoData!!.lon,
                                        result.lat, result.lon
                                    )
                                }
                            } else {
                                errorMessage = "Failed to get geolocation data for $ipInput"
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
                enabled = !isLoading && ipInput.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Lookup", modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text("LOOKUP", fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tabs
            val tabs = listOf("Details", "Map", "Batch")
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
                    // Details tab
                    errorMessage?.let { err ->
                        NeonCard(modifier = Modifier.fillMaxWidth()) {
                            Text(err, color = Color(0xFFFF4444), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                        }
                    }
                    if (geoData == null && errorMessage == null) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                GlitchText(text = "IP GEOLOCATION", color = NeonPurple)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Enter an IP address to lookup", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                    geoData?.let { geo ->
                        Column(modifier = Modifier.fillMaxSize().background(AMOLEDBlack)) {
                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    val text = buildString {
                                        append("IP: ${geo.query}\n")
                                        append("Country: ${geo.country}\n")
                                        append("Region: ${geo.regionName}\n")
                                        append("City: ${geo.city}\n")
                                        append("Lat/Lon: ${geo.lat}, ${geo.lon}\n")
                                        append("Timezone: ${geo.timezone}\n")
                                        append("ISP: ${geo.isp}\n")
                                        append("ASN: ${geo.`as`}\n")
                                        append("Org: ${geo.org}")
                                    }
                                    clipboardManager.setText(AnnotatedString(text))
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                }
                                IconButton(onClick = { /* Export */ }) {
                                    Icon(Icons.Default.Download, contentDescription = "Export", tint = NeonGreen, modifier = Modifier.size(16.dp))
                                }
                            }

                            NeonCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    GeoInfoRow("IP Address", geo.query, NeonCyan)
                                    GeoInfoRow("Country", "${geo.country} (${geo.countryCode})", NeonGreen)
                                    GeoInfoRow("Region", geo.regionName, Color.White)
                                    GeoInfoRow("City", geo.city, Color.White)
                                    GeoInfoRow("ZIP Code", geo.zip, Color.White)
                                    GeoInfoRow("Latitude", geo.lat.toString(), NeonCyan)
                                    GeoInfoRow("Longitude", geo.lon.toString(), NeonCyan)
                                    GeoInfoRow("Timezone", geo.timezone, Color.White)
                                    HorizontalDivider(color = DarkSurface, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    GeoInfoRow("ISP", geo.isp, NeonPurple)
                                    GeoInfoRow("ASN", geo.`as`, NeonPurple)
                                    GeoInfoRow("Organization", geo.org, NeonPurple)
                                }
                            }

                            distanceKm?.let { dist ->
                                Spacer(modifier = Modifier.height(8.dp))
                                NeonCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("DISTANCE FROM YOU", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            Text(
                                                String.format("%.1f km", dist),
                                                color = NeonGreen,
                                                fontSize = 16.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Map tab
                    geoData?.let { geo ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AMOLEDBlack)
                                .padding(8.dp)
                        ) {
                            Text(
                                "LOCATION MAP",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Background
                                drawRect(color = Color(0xFF0D1117))

                                // Grid lines
                                for (i in 0..8) {
                                    val y = canvasHeight * i / 8f
                                    drawLine(
                                        color = Color(0xFF1A2332),
                                        start = Offset(0f, y),
                                        end = Offset(canvasWidth, y),
                                        strokeWidth = 0.5f
                                    )
                                }
                                for (i in 0..16) {
                                    val x = canvasWidth * i / 16f
                                    drawLine(
                                        color = Color(0xFF1A2332),
                                        start = Offset(x, 0f),
                                        end = Offset(x, canvasHeight),
                                        strokeWidth = 0.5f
                                    )
                                }

                                // Draw continents
                                CONTINENT_PATHS.forEach { pathPoints ->
                                    val path = Path()
                                    pathPoints.forEachIndexed { index, (xRatio, yRatio) ->
                                        val x = xRatio * canvasWidth
                                        val y = yRatio * canvasHeight
                                        if (index == 0) path.moveTo(x, y)
                                        else path.lineTo(x, y)
                                    }
                                    path.close()
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF1A2A3A),
                                        style = Fill
                                    )
                                    drawPath(
                                        path = path,
                                        color = Color(0xFF2A3A4A),
                                        style = Stroke(width = 1f)
                                    )
                                }

                                // Draw pin for location
                                // Convert lat/lon to map coordinates
                                // lon: -180 to 180 -> 0 to canvasWidth
                                // lat: 90 to -90 -> 0 to canvasHeight
                                val pinX = ((geo.lon + 180f) / 360f) * canvasWidth
                                val pinY = ((90f - geo.lat) / 180f) * canvasHeight

                                // Pin glow
                                drawCircle(
                                    color = NeonCyan.copy(alpha = 0.15f),
                                    radius = 20f,
                                    center = Offset(pinX, pinY)
                                )
                                drawCircle(
                                    color = NeonCyan.copy(alpha = 0.3f),
                                    radius = 12f,
                                    center = Offset(pinX, pinY)
                                )
                                // Pin dot
                                drawCircle(
                                    color = NeonCyan,
                                    radius = 5f,
                                    center = Offset(pinX, pinY)
                                )
                                // Pin crosshair
                                drawLine(
                                    color = NeonCyan.copy(alpha = 0.5f),
                                    start = Offset(pinX - 15f, pinY),
                                    end = Offset(pinX + 15f, pinY),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = NeonCyan.copy(alpha = 0.5f),
                                    start = Offset(pinX, pinY - 15f),
                                    end = Offset(pinX, pinY + 15f),
                                    strokeWidth = 1f
                                )

                                // Label
                                drawContext.canvas.nativeCanvas.drawText(
                                    "${geo.city}, ${geo.countryCode}",
                                    pinX + 10f,
                                    pinY - 10f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.CYAN
                                        textSize = 22f
                                        typeface = android.graphics.Typeface.MONOSPACE
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${geo.lat}, ${geo.lon}", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(geo.timezone, color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize().background(AMOLEDBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Perform a lookup first to see the map", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
                2 -> {
                    // Batch lookup
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AMOLEDBlack)
                            .padding(8.dp)
                    ) {
                        Text("BATCH IP LOOKUP", color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = batchInput,
                            onValueChange = { batchInput = it },
                            label = { Text("IPs (comma or newline separated)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkSurface,
                                focusedLabelColor = NeonCyan,
                                cursorColor = NeonCyan
                            ),
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val ips = batchInput.split(",", "\n").map { it.trim() }.filter { it.isNotBlank() }
                                if (ips.isNotEmpty()) {
                                    batchLoading = true
                                    batchResults.clear()
                                    kotlinx.coroutines.MainScope().launch {
                                        val results = ips.map { ip ->
                                            val geo = fetchIpGeo(ip)
                                            if (geo != null && geo.status == "success") {
                                                BatchGeoResult(ip, geo)
                                            } else {
                                                BatchGeoResult(ip, null, "Failed to lookup")
                                            }
                                        }
                                        batchResults.clear()
                                        batchResults.addAll(results)
                                        batchLoading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPurple.copy(alpha = 0.15f),
                                contentColor = NeonPurple
                            ),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !batchLoading
                        ) {
                            if (batchLoading) CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LOOKUP ALL", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(batchResults) { result ->
                                NeonCard(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(result.ip, color = NeonCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                        result.error?.let { err ->
                                            Text(err, color = Color(0xFFFF4444), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                        result.geo?.let { geo ->
                                            Text("${geo.city}, ${geo.regionName}, ${geo.country}", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                            Text("ISP: ${geo.isp}", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            Text("Coords: ${geo.lat}, ${geo.lon}", color = NeonGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
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

@Composable
private fun GeoInfoRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            "$label: ",
            color = Color.Gray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            value,
            color = valueColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
