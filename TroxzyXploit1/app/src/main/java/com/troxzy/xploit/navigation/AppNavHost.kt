package com.troxzy.xploit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.troxzy.xploit.ui.screens.splash.SplashScreen
import com.troxzy.xploit.ui.screens.dashboard.DashboardScreen
import com.troxzy.xploit.ui.screens.aichat.AiChatScreen
import com.troxzy.xploit.ui.screens.aichat.AiChatSessionScreen
import com.troxzy.xploit.ui.screens.networkscanner.NetworkScannerScreen
import com.troxzy.xploit.ui.screens.portscanner.PortScannerScreen
import com.troxzy.xploit.ui.screens.vulnchecker.VulnCheckerScreen
import com.troxzy.xploit.ui.screens.wifianalyzer.WifiAnalyzerScreen
import com.troxzy.xploit.ui.screens.mitm.MitmDashboardScreen
import com.troxzy.xploit.ui.screens.packetsniffer.PacketSnifferScreen
import com.troxzy.xploit.ui.screens.dnslookup.DnsLookupScreen
import com.troxzy.xploit.ui.screens.whois.WhoisLookupScreen
import com.troxzy.xploit.ui.screens.ipgeo.IpGeoScreen
import com.troxzy.xploit.ui.screens.reverseip.ReverseIpScreen
import com.troxzy.xploit.ui.screens.subdomain.SubdomainFinderScreen
import com.troxzy.xploit.ui.screens.passwordgen.PasswordGeneratorScreen
import com.troxzy.xploit.ui.screens.hashtools.HashToolsScreen
import com.troxzy.xploit.ui.screens.encoder.EncoderDecoderScreen
import com.troxzy.xploit.ui.screens.encryption.EncryptionToolsScreen
import com.troxzy.xploit.ui.screens.osint.UsernameSearchScreen
import com.troxzy.xploit.ui.screens.osint.EmailLookupScreen
import com.troxzy.xploit.ui.screens.osint.ImageReverseSearchScreen
import com.troxzy.xploit.ui.screens.webscraper.WebScraperScreen
import com.troxzy.xploit.ui.screens.terminal.TerminalScreen
import com.troxzy.xploit.ui.screens.crypto.CryptoTrackerScreen
import com.troxzy.xploit.ui.screens.deviceinfo.DeviceInfoScreen
import com.troxzy.xploit.ui.screens.settings.SettingsScreen

@Composable
fun AppNavHost(
    onSplashFinished: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val navigateTo: (String) -> Unit = { route -> navController.navigate(route) }
    val goBack: () -> Unit = { navController.popBackStack() }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onFinished = {
                    onSplashFinished()
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigate = { screen -> navController.navigate(screen.route) }
            )
        }
        composable(Screen.AiChat.route) {
            AiChatScreen(
                onOpenSession = { sessionId ->
                    navController.navigate("ai_chat/${sessionId}")
                }
            )
        }
        composable(
            route = "ai_chat/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionIdStr = backStackEntry.arguments?.getString("sessionId") ?: "0"
            val sessionId = sessionIdStr.toLongOrNull() ?: 0L
            AiChatSessionScreen(
                sessionId = sessionId,
                onBack = goBack
            )
        }
        composable(Screen.NetworkScanner.route) {
            NetworkScannerScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.PortScanner.route) {
            PortScannerScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.VulnChecker.route) {
            VulnCheckerScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.WifiAnalyzer.route) {
            WifiAnalyzerScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.MitmDashboard.route) {
            MitmDashboardScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.PacketSniffer.route) {
            PacketSnifferScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.DnsLookup.route) {
            DnsLookupScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.WhoisLookup.route) {
            WhoisLookupScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.IpGeolocation.route) {
            IpGeoScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.ReverseIp.route) {
            ReverseIpScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.SubdomainFinder.route) {
            SubdomainFinderScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.PasswordGenerator.route) {
            PasswordGeneratorScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.HashTools.route) {
            HashToolsScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.EncoderDecoder.route) {
            EncoderDecoderScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.EncryptionTools.route) {
            EncryptionToolsScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.OsintUsername.route) {
            UsernameSearchScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.OsintEmail.route) {
            EmailLookupScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.OsintImage.route) {
            ImageReverseSearchScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.WebScraper.route) {
            WebScraperScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.Terminal.route) {
            TerminalScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.CryptoTracker.route) {
            CryptoTrackerScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.DeviceInfo.route) {
            DeviceInfoScreen(onNavigate = navigateTo, onBack = goBack)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onNavigate = navigateTo, onBack = goBack)
        }
    }
}
