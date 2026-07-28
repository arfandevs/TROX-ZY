package com.troxzy.xploit.ui.screens.dashboard

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.navigation.Screen
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.MatrixRain
import com.troxzy.xploit.ui.components.NeonCard
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple
import com.troxzy.xploit.ui.theme.NeonRed
import com.troxzy.xploit.utils.NetworkUtils
import kotlinx.coroutines.launch

// ============================================================ data model

/**
 * Represents a single tool / module shown in the dashboard grid.
 */
data class ToolItem(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val screen: Screen
)

// ============================================================ tool list

private val tools = listOf(
    ToolItem("Network Scanner", "Scan local & remote networks", Icons.Default.Wifi, Screen.NetworkScanner),
    ToolItem("Port Scanner", "Discover open ports on hosts", Icons.Default.Storage, Screen.PortScanner),
    ToolItem("Vuln Checker", "Check for known vulnerabilities", Icons.Default.Security, Screen.VulnChecker),
    ToolItem("WiFi Analyzer", "Analyze WiFi signals & security", Icons.Default.WifiTethering, Screen.WifiAnalyzer),
    ToolItem("MITM Dashboard", "Man-in-the-middle attack hub", Icons.Default.SwapHoriz, Screen.MitmDashboard),
    ToolItem("Packet Sniffer", "Capture & inspect packets", Icons.Default.DataObject, Screen.PacketSniffer),
    ToolItem("DNS Lookup", "Query DNS records", Icons.Default.Dns, Screen.DnsLookup),
    ToolItem("WHOIS Lookup", "Retrieve WHOIS domain info", Icons.Default.Info, Screen.WhoisLookup),
    ToolItem("IP Geolocation", "Locate IP addresses on map", Icons.Default.LocationOn, Screen.IpGeolocation),
    ToolItem("Reverse IP", "Find domains on same IP", Icons.Default.SwapVert, Screen.ReverseIp),
    ToolItem("Subdomain Finder", "Discover subdomains", Icons.Default.Explore, Screen.SubdomainFinder),
    ToolItem("Password Generator", "Generate secure passwords", Icons.Default.Password, Screen.PasswordGenerator),
    ToolItem("Hash Tools", "Calculate & compare hashes", Icons.Default.Fingerprint, Screen.HashTools),
    ToolItem("Encoder/Decoder", "Encode & decode data", Icons.Default.Code, Screen.EncoderDecoder),
    ToolItem("Encryption Tools", "Encrypt & decrypt files/text", Icons.Default.Lock, Screen.EncryptionTools),
    ToolItem("OSINT Username", "Search usernames across sites", Icons.Default.PersonSearch, Screen.OsintUsername),
    ToolItem("OSINT Email", "Investigate email addresses", Icons.Default.Email, Screen.OsintEmail),
    ToolItem("OSINT Image", "Reverse image search", Icons.Default.ImageSearch, Screen.OsintImage),
    ToolItem("Web Scraper", "Scrape & extract web data", Icons.Default.Language, Screen.WebScraper),
    ToolItem("Terminal", "Interactive shell terminal", Icons.Default.Terminal, Screen.Terminal),
    ToolItem("Crypto Tracker", "Track cryptocurrency prices", Icons.Default.CurrencyBitcoin, Screen.CryptoTracker),
    ToolItem("Device Info", "View device specifications", Icons.Default.PhoneAndroid, Screen.DeviceInfo),
    ToolItem("Settings", "App configuration & prefs", Icons.Default.Settings, Screen.Settings)
)

// ============================================================ screen

/**
 * Main dashboard screen. Shows a searchable list of all 24 tools,
 * quick‑action cards, a status bar, and a side drawer.
 *
 * @param onNavigate callback invoked with the target [Screen] when
 *   the user taps a tool card or quick action.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (Screen) -> Unit
) {
    // -------------------------------------------------------------- state
    var searchQuery by remember { mutableStateOf("") }
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val filteredTools = remember(searchQuery) {
        if (searchQuery.isBlank()) tools
        else tools.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    // ------------------------------------------------------------ drawer
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF0A0A0A),
                drawerContentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.Start
                ) {
                    GlitchText(
                        text = "TROXZYXPLOIT",
                        isAnimating = true,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "by Troxzy | t.me/SoloBanNoTrash",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(32.dp))

                    // Drawer menu items
                    tools.take(10).forEach { tool ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.name,
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = tool.name,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    ) {
        // ---------------------------------------------------------- scaffold
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        GlitchText(
                            text = "TROXZYXPLOIT",
                            isAnimating = true,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = NeonCyan
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0A0A0A),
                        titleContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0A0A0A),
                    contentColor = NeonCyan
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* already on dashboard */ },
                        icon = {
                            Icon(Icons.Default.Wifi, contentDescription = "Dashboard", tint = NeonCyan)
                        },
                        label = { Text("Dashboard", color = NeonCyan, fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onNavigate(Screen.Terminal) },
                        icon = {
                            Icon(Icons.Default.Terminal, contentDescription = "Terminal", tint = NeonPurple)
                        },
                        label = { Text("Terminal", color = NeonPurple, fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onNavigate(Screen.NetworkScanner) },
                        icon = {
                            Icon(Icons.Default.Search, contentDescription = "Scanner", tint = NeonGreen)
                        },
                        label = { Text("Scanner", color = NeonGreen, fontSize = 11.sp) }
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = { onNavigate(Screen.Settings) },
                        icon = {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
                        },
                        label = { Text("Settings", color = Color.Gray, fontSize = 11.sp) }
                    )
                }
            },
            containerColor = Color(0xFF0A0A0A)
        ) { innerPadding ->
            // --------------------------------------------------- content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background layer – matrix rain
                MatrixRain(modifier = Modifier.fillMaxSize())

                // Foreground scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // ------------------------------------------------ search
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        placeholder = {
                            Text("Search tools...", color = Color(0x99FFFFFF))
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = NeonCyan
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = NeonCyan.copy(alpha = 0.5f),
                            cursorColor = NeonCyan,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ---------------------------------------------- status bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Device IP
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Wifi,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = NetworkUtils.getLocalIpAddress(),
                                color = NeonGreen,
                                fontSize = 12.sp
                            )
                        }

                        // Network status
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WifiTethering,
                                contentDescription = "Network",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Connected",
                                color = NeonCyan,
                                fontSize = 12.sp
                            )
                        }

                        // AI connection status dot
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(NeonGreen, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AI Online",
                                color = NeonGreen,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ---------------------------------------- quick action cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // AI Chat
                        NeonCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.AiChat) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.PersonSearch,
                                    contentDescription = "AI Chat",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("AI Chat", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Terminal
                        NeonCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.Terminal) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = "Terminal",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Terminal", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Network Scanner
                        NeonCard(
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(Screen.NetworkScanner) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Wifi,
                                    contentDescription = "Network Scanner",
                                    tint = NeonGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Network Scanner", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ------------------------------------------- tool grid header
                    Text(
                        text = "ALL MODULES",
                        color = NeonPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // ------------------------------------------- tool grid
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTools, key = { it.name }) { tool ->
                            NeonCard(
                                onClick = { onNavigate(tool.screen) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Icon(
                                        imageVector = tool.icon,
                                        contentDescription = tool.name,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = tool.name,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = tool.description,
                                        color = Color(0xAAFFFFFF),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
