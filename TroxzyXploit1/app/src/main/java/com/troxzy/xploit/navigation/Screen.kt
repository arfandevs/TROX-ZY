package com.troxzy.xploit.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Dashboard : Screen("dashboard")
    data object AiChat : Screen("ai_chat")
    data object AiChatSession : Screen("ai_chat/{sessionId}") {
        fun createRoute(sessionId: String) = "ai_chat/$sessionId"
    }
    data object NetworkScanner : Screen("network_scanner")
    data object PortScanner : Screen("port_scanner")
    data object VulnChecker : Screen("vuln_checker")
    data object WifiAnalyzer : Screen("wifi_analyzer")
    data object MitmDashboard : Screen("mitm_dashboard")
    data object PacketSniffer : Screen("packet_sniffer")
    data object DnsLookup : Screen("dns_lookup")
    data object WhoisLookup : Screen("whois_lookup")
    data object IpGeolocation : Screen("ip_geolocation")
    data object ReverseIp : Screen("reverse_ip")
    data object SubdomainFinder : Screen("subdomain_finder")
    data object PasswordGenerator : Screen("password_generator")
    data object HashTools : Screen("hash_tools")
    data object EncoderDecoder : Screen("encoder_decoder")
    data object EncryptionTools : Screen("encryption_tools")
    data object OsintUsername : Screen("osint_username")
    data object OsintEmail : Screen("osint_email")
    data object OsintImage : Screen("osint_image")
    data object WebScraper : Screen("web_scraper")
    data object Terminal : Screen("terminal")
    data object CryptoTracker : Screen("crypto_tracker")
    data object DeviceInfo : Screen("device_info")
    data object Settings : Screen("settings")
}
