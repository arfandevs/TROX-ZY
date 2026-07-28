package com.troxzy.xploit.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.theme.AmoledBlack
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.DarkElevated
import com.troxzy.xploit.ui.theme.DarkSurface
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple
import com.troxzy.xploit.ui.theme.TextPrimary
import com.troxzy.xploit.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * Represents a tool module item in the side drawer.
 */
private data class DrawerToolItem(
    val name: String,
    val route: String,
    val icon: ImageVector = Icons.Filled.Security
)

/**
 * All 24 tool modules displayed in the side drawer.
 */
private val toolModules = listOf(
    DrawerToolItem("Network Scanner", "network_scanner"),
    DrawerToolItem("Port Scanner", "port_scanner"),
    DrawerToolItem("Vuln Checker", "vuln_checker"),
    DrawerToolItem("WiFi Analyzer", "wifi_analyzer"),
    DrawerToolItem("MITM Dashboard", "mitm_dashboard"),
    DrawerToolItem("Packet Sniffer", "packet_sniffer"),
    DrawerToolItem("DNS Lookup", "dns_lookup"),
    DrawerToolItem("WHOIS Lookup", "whois_lookup"),
    DrawerToolItem("IP Geolocation", "ip_geolocation"),
    DrawerToolItem("Reverse IP", "reverse_ip"),
    DrawerToolItem("Subdomain Finder", "subdomain_finder"),
    DrawerToolItem("Password Generator", "password_generator"),
    DrawerToolItem("Hash Tools", "hash_tools"),
    DrawerToolItem("Encoder/Decoder", "encoder_decoder"),
    DrawerToolItem("Encryption Tools", "encryption_tools"),
    DrawerToolItem("OSINT Username", "osint_username"),
    DrawerToolItem("OSINT Email", "osint_email"),
    DrawerToolItem("OSINT Image", "osint_image"),
    DrawerToolItem("Web Scraper", "web_scraper"),
    DrawerToolItem("Terminal", "terminal"),
    DrawerToolItem("Crypto Tracker", "crypto_tracker"),
    DrawerToolItem("Device Info", "device_info"),
    DrawerToolItem("Settings", "settings")
)

/**
 * A composable that provides a slide-in side drawer.
 *
 * The drawer contains:
 * - A header with "TROXZYXPLOIT" glitch text
 * - Owner info (version, developer info)
 * - A scrollable list of all 24 tool modules as clickable items
 *
 * @param drawerState The state controlling the drawer open/close.
 * @param onNavigate Callback invoked with the route string when a tool item is tapped.
 * @param content The main content composable displayed behind the drawer.
 */
@Composable
fun SideDrawer(
    drawerState: DrawerState,
    onNavigate: (String) -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AmoledBlack,
                drawerContentColor = TextPrimary,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Drawer Header ──────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        // Glitch text title
                        GlitchText(
                            text = "TROXZYXPLOIT",
                            isAnimating = true,
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            defaultColor = NeonCyan
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Version info
                        Text(
                            text = "v2.0.0 | BETA",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Owner info
                        Text(
                            text = "Developed by Troxzy",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "github.com/troxzy",
                            color = NeonPurple,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    HorizontalDivider(
                        color = NeonCyan.copy(alpha = 0.2f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // ── Section: Tool Modules ──────────────────────────
                    Text(
                        text = "MODULES",
                        color = NeonCyan.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // List of all tool modules
                    toolModules.forEachIndexed { index, item ->
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = item.name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.name,
                                    tint = NeonCyan.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            selected = false,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                }
                                onNavigate(item.route)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedContainerColor = Color.Transparent,
                                selectedContainerColor = DarkCard
                            )
                        )

                        // Add subtle dividers between groups
                        if (index == 5 || index == 10 || index == 14 || index == 18) {
                            HorizontalDivider(
                                color = NeonCyan.copy(alpha = 0.08f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ── Footer ────────────────────────────────────────
                    HorizontalDivider(
                        color = NeonCyan.copy(alpha = 0.15f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "// USE AT YOUR OWN RISK",
                        color = NeonPurple.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Text(
                        text = "// FOR EDUCATIONAL PURPOSES ONLY",
                        color = NeonPurple.copy(alpha = 0.4f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        content = content
    )
}
