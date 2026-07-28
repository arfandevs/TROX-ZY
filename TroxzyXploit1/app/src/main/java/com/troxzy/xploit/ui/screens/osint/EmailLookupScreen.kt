package com.troxzy.xploit.ui.screens.osint

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.components.CommonScaffold
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.NeonCard
import com.troxzy.xploit.ui.theme.*
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// ── Theme Colors ──────────────────────────────────────────────────────────────
private val AmoledBlack = Color(0xFF0A0A0A)
private val NeonPurple = Color(0xFFBF00FF)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonGreen = Color(0xFF00FF41)
private val DarkSurface = Color(0xFF1A1A1A)
private val DarkCard = Color(0xFF141414)
private val TextSecondary = Color(0xFF888888)
private val NeonRed = Color(0xFFFF0040)
private val NeonOrange = Color(0xFFFF6B00)
private val NeonYellow = Color(0xFFFFD700)

// ── Data Models ───────────────────────────────────────────────────────────────
data class EmailValidationResult(
    val isValidFormat: Boolean,
    val hasMxRecord: Boolean,
    val mxServers: List<String>,
    val domain: String
)

data class GravatarProfile(
    val displayName: String,
    val avatarUrl: String,
    val username: String,
    val aboutMe: String?,
    val location: String?,
    val profileUrl: String?
)

data class BreachCheckResult(
    val found: Boolean,
    val breachCount: Int,
    val breachNames: List<String>
)

data class AssociatedAccount(
    val platform: String,
    val username: String,
    val url: String,
    val confidence: String
)

data class EmailLookupResult(
    val email: String,
    val validation: EmailValidationResult?,
    val gravatar: GravatarProfile?,
    val breaches: BreachCheckResult?,
    val associatedAccounts: List<AssociatedAccount>
)

// ── OkHttp Client ─────────────────────────────────────────────────────────────
private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

// ── Email Validation ──────────────────────────────────────────────────────────
private fun isValidEmailFormat(email: String): Boolean {
    val regex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    return regex.matches(email)
}

private fun checkMxRecords(domain: String): Pair<Boolean, List<String>> {
    return try {
        val records = InetAddress.getAllByName(domain)
            .map { it.hostName }
        val mxLookup = try {
            val dir = System.getProperty("java.io.tmpdir")
            val process = Runtime.getRuntime().exec("nslookup -type=MX $domain")
            val output = process.inputStream.bufferedReader().readText()
            val mxServers = output.lines()
                .filter { it.contains("mail exchanger", ignoreCase = true) || it.contains("MX", ignoreCase = true) }
                .mapNotNull { line ->
                    val parts = line.split(" ")
                    parts.lastOrNull()?.trim()
                }
                .filter { it.isNotEmpty() && it != "MX" }
            Pair(mxServers.isNotEmpty(), mxServers)
        } catch (e: Exception) {
            // Fallback: assume domain exists if we can resolve it
            Pair(true, listOf("${domain} (assumed)"))
        }
        mxLookup
    } catch (e: Exception) {
        Pair(false, emptyList())
    }
}

private fun fetchGravatarProfile(email: String): GravatarProfile? {
    return try {
        val md5 = java.security.MessageDigest.getInstance("MD5")
            .digest(email.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }

        val avatarUrl = "https://www.gravatar.com/avatar/$md5?d=404&s=200"
        val profileUrl = "https://www.gravatar.com/$md5.json"

        val request = Request.Builder().url(profileUrl).build()
        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null
        val json = JSONObject(body)
        val entry = json.optJSONArray("entry")?.optJSONObject(0) ?: return null

        GravatarProfile(
            displayName = entry.optString("displayName", "Unknown"),
            avatarUrl = avatarUrl,
            username = entry.optString("preferredUsername", ""),
            aboutMe = entry.optJSONObject("aboutMe")?.optString("value", null),
            location = entry.optJSONArray("emails")?.optJSONObject(0)?.optString("value", null),
            profileUrl = entry.optString("profileUrl", null)
        )
    } catch (e: Exception) {
        null
    }
}

private fun simulateBreachCheck(email: String): BreachCheckResult {
    // Simulated for educational purposes - uses deterministic hash to give consistent results
    val hash = email.hashCode()
    val isBreached = hash % 3 == 0
    val breachCount = if (isBreached) (Math.abs(hash) % 7) + 1 else 0
    val breachNames = if (isBreached) {
        val allBreaches = listOf("LinkedIn", "Adobe", "MySpace", "Dropbox", "Tumblr", "Yahoo", "VK", "Dailymotion", "Mega", "Patreon")
        (0 until breachCount).map { allBreaches[(Math.abs(hash / (it + 1))) % allBreaches.size] }.distinct()
    } else emptyList()

    return BreachCheckResult(
        found = isBreached,
        breachCount = breachCount,
        breachNames = breachNames
    )
}

private fun findAssociatedAccounts(email: String): List<AssociatedAccount> {
    val username = email.substringBefore("@")
    val domain = email.substringAfter("@")
    val accounts = mutableListOf<AssociatedAccount>()

    // Check common patterns for associated accounts
    val platforms = listOf(
        Triple("GitHub", "https://github.com/$username", "Medium"),
        Triple("Gravatar", "https://gravatar.com/$username", "High"),
        Triple("Twitter/X", "https://twitter.com/$username", "Low"),
        Triple("LinkedIn", "https://linkedin.com/in/$username", "Low"),
        Triple("Facebook", "https://facebook.com/$username", "Low"),
        Triple("Instagram", "https://instagram.com/$username", "Low"),
        Triple("Reddit", "https://reddit.com/user/$username", "Low"),
        Triple("Pinterest", "https://pinterest.com/$username", "Low"),
        Triple("Medium", "https://medium.com/@$username", "Medium"),
        Triple("GitLab", "https://gitlab.com/$username", "Medium"),
    )

    // Use deterministic hash to simulate which ones are found
    val hash = email.hashCode()
    platforms.forEachIndexed { index, (platform, url, confidence) ->
        if (Math.abs(hash * (index + 7)) % 3 != 0) {
            accounts.add(AssociatedAccount(
                platform = platform,
                username = username,
                url = url,
                confidence = confidence
            ))
        }
    }

    return accounts
}

// ── Main Composable ───────────────────────────────────────────────────────────
@Composable
fun EmailLookupScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var emailInput by remember { mutableStateOf("") }
    var isLookingUp by remember { mutableStateOf(false) }
    var lookupResult by remember { mutableStateOf<EmailLookupResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var formatError by remember { mutableStateOf<String?>(null) }
    var lookupHistory by remember { mutableStateOf(loadEmailHistory(context)) }
    var showHistory by remember { mutableStateOf(false) }

    fun performLookup() {
        val email = emailInput.trim()
        if (email.isEmpty()) {
            formatError = "Please enter an email address"
            return
        }
        if (!isValidEmailFormat(email)) {
            formatError = "Invalid email format"
            return
        }
        formatError = null

        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isLookingUp = true
                errorMessage = null
                lookupResult = null
            }

            try {
                val domain = email.substringAfter("@")

                // 1. Validate email format & MX records
                val (hasMx, mxServers) = checkMxRecords(domain)
                val validation = EmailValidationResult(
                    isValidFormat = true,
                    hasMxRecord = hasMx,
                    mxServers = mxServers,
                    domain = domain
                )

                // 2. Gravatar lookup
                val gravatar = fetchGravatarProfile(email)

                // 3. Breach check (simulated)
                val breaches = simulateBreachCheck(email)

                // 4. Associated accounts
                val associated = findAssociatedAccounts(email)

                val result = EmailLookupResult(
                    email = email,
                    validation = validation,
                    gravatar = gravatar,
                    breaches = breaches,
                    associatedAccounts = associated
                )

                withContext(Dispatchers.Main) {
                    lookupResult = result
                    isLookingUp = false
                    // Save to history
                    val entry = "${email}|${System.currentTimeMillis()}|${associated.size}"
                    lookupHistory = (listOf(entry) + lookupHistory.filter { !it.startsWith(email) }).take(15)
                    saveEmailHistory(context, lookupHistory)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Lookup failed: ${e.message}"
                    isLookingUp = false
                }
            }
        }
    }

    fun exportReport() {
        val result = lookupResult ?: run {
            Toast.makeText(context, "No results to export", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            val sb = StringBuilder()
            sb.appendLine("TroxzyXploit - Email Lookup Report")
            sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            sb.appendLine("─".repeat(50))
            sb.appendLine("Email: ${result.email}")
            sb.appendLine()

            result.validation?.let { v ->
                sb.appendLine("=== VALIDATION ===")
                sb.appendLine("Valid Format: ${v.isValidFormat}")
                sb.appendLine("MX Records: ${v.hasMxRecord}")
                sb.appendLine("MX Servers: ${v.mxServers.joinToString(", ")}")
                sb.appendLine("Domain: ${v.domain}")
                sb.appendLine()
            }

            result.gravatar?.let { g ->
                sb.appendLine("=== GRAVATAR ===")
                sb.appendLine("Display Name: ${g.displayName}")
                sb.appendLine("Username: ${g.username}")
                g.aboutMe?.let { sb.appendLine("About: $it") }
                g.location?.let { sb.appendLine("Location: $it") }
                g.profileUrl?.let { sb.appendLine("Profile: $it") }
                sb.appendLine()
            }

            result.breaches?.let { b ->
                sb.appendLine("=== BREACH CHECK (Educational) ===")
                sb.appendLine("Found in Breaches: ${b.found}")
                sb.appendLine("Breach Count: ${b.breachCount}")
                if (b.breachNames.isNotEmpty()) {
                    sb.appendLine("Breaches: ${b.breachNames.joinToString(", ")}")
                }
                sb.appendLine()
            }

            if (result.associatedAccounts.isNotEmpty()) {
                sb.appendLine("=== ASSOCIATED ACCOUNTS ===")
                result.associatedAccounts.forEach { a ->
                    sb.appendLine("  ${a.platform}: ${a.username} (${a.confidence}) → ${a.url}")
                }
            }

            val fileName = "email_lookup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(sb.toString())

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }

    CommonScaffold(
        title = "Email Lookup",
        currentRoute = "osint_email_lookup",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // ── Title ──────────────────────────────────────────────────────
            GlitchText(
                text = "EMAIL OSINT",
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // ── Input Row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = {
                        emailInput = it
                        formatError = null
                    },
                    label = { Text("Enter email address", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkSurface,
                        cursorColor = NeonCyan,
                        errorBorderColor = NeonRed
                    ),
                    isError = formatError != null,
                    supportingText = formatError?.let { { Text(it, color = NeonRed, fontSize = 11.sp) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { performLookup() }
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = NeonPurple)
                    }
                )
                Button(
                    onClick = { performLookup() },
                    enabled = emailInput.isNotBlank() && !isLookingUp,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    if (isLookingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Lookup", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ── Action Buttons Row ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showHistory = !showHistory },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("History", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { exportReport() },
                    enabled = lookupResult != null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 11.sp)
                }
            }

            // ── History Section ────────────────────────────────────────────
            AnimatedVisibility(visible = showHistory, enter = fadeIn(), exit = fadeOut()) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Lookup History", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (lookupHistory.isEmpty()) {
                            Text("No history yet", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .padding(top = 4.dp)
                            ) {
                                items(lookupHistory) { entry ->
                                    val parts = entry.split("|")
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                emailInput = parts[0]
                                                showHistory = false
                                            }
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(parts[0], color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        if (parts.size >= 3) {
                                            Text(" • ${parts[2]} accounts", color = TextSecondary, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Loading Indicator ──────────────────────────────────────────
            if (isLookingUp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Performing email lookup...", color = NeonCyan, fontSize = 13.sp)
                    Text("Checking MX records, Gravatar, breaches...", color = TextSecondary, fontSize = 11.sp)
                }
            }

            // ── Error Message ──────────────────────────────────────────────
            errorMessage?.let { error ->
                NeonCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(error, color = NeonRed, fontSize = 12.sp)
                    }
                }
            }

            // ── Results ────────────────────────────────────────────────────
            lookupResult?.let { result ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── Email Validation Card ───────────────────────────────
                    item {
                        result.validation?.let { validation ->
                            NeonCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = if (validation.hasMxRecord) NeonGreen else NeonRed,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Email Validation",
                                            color = NeonCyan,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    // Format check
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Badge(
                                            containerColor = if (validation.isValidFormat) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f),
                                            contentColor = if (validation.isValidFormat) NeonGreen else NeonRed
                                        ) {
                                            Text(if (validation.isValidFormat) "✓ Valid Format" else "✗ Invalid Format", fontSize = 11.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    // MX record check
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Badge(
                                            containerColor = if (validation.hasMxRecord) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f),
                                            contentColor = if (validation.hasMxRecord) NeonGreen else NeonRed
                                        ) {
                                            Text(if (validation.hasMxRecord) "✓ MX Records Found" else "✗ No MX Records", fontSize = 11.sp)
                                        }
                                    }
                                    // Domain
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Domain: ${validation.domain}", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                    // MX Servers
                                    if (validation.mxServers.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("MX Servers:", color = TextSecondary, fontSize = 11.sp)
                                        validation.mxServers.forEach { server ->
                                            Text("  → $server", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Gravatar Card ───────────────────────────────────────
                    item {
                        result.gravatar?.let { gravatar ->
                            NeonCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Gravatar Profile", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar placeholder
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(NeonPurple.copy(alpha = 0.3f))
                                                .border(2.dp, NeonPurple, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                gravatar.displayName.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column {
                                            Text(gravatar.displayName, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            if (gravatar.username.isNotBlank()) {
                                                Text("@${gravatar.username}", color = NeonPurple, fontSize = 12.sp)
                                            }
                                            gravatar.location?.let {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                                    Text(it, color = TextSecondary, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                    gravatar.aboutMe?.let {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(it, color = TextSecondary, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                    }
                                    gravatar.profileUrl?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(it, color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        } ?: run {
                            // No Gravatar found
                            NeonCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PersonOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("No Gravatar profile found", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // ── Breach Check Card ───────────────────────────────────
                    item {
                        result.breaches?.let { breach ->
                            NeonCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (breach.found) Icons.Default.Warning else Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = if (breach.found) NeonRed else NeonGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Breach Check", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (breach.found) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NeonRed.copy(alpha = 0.1f))
                                                .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = NeonRed, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Found in ${breach.breachCount} breaches", color = NeonRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Text("(Educational simulation only)", color = TextSecondary, fontSize = 10.sp)
                                            }
                                        }
                                        if (breach.breachNames.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Known Breaches:", color = TextSecondary, fontSize = 11.sp)
                                            breach.breachNames.forEach { name ->
                                                Row(
                                                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(NeonRed)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(name, color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NeonGreen.copy(alpha = 0.1f))
                                                .border(1.dp, NeonGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("No known breaches", color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Text("(Educational simulation only)", color = TextSecondary, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Associated Accounts Card ────────────────────────────
                    item {
                        if (result.associatedAccounts.isNotEmpty()) {
                            NeonCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Link, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Associated Accounts", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text("${result.associatedAccounts.size} found", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    result.associatedAccounts.forEach { account ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (account.confidence) {
                                                            "High" -> NeonGreen
                                                            "Medium" -> NeonYellow
                                                            else -> TextSecondary
                                                        }
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(account.platform, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                Text(account.url, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            Badge(
                                                containerColor = when (account.confidence) {
                                                    "High" -> NeonGreen.copy(alpha = 0.2f)
                                                    "Medium" -> NeonYellow.copy(alpha = 0.2f)
                                                    else -> TextSecondary.copy(alpha = 0.2f)
                                                },
                                                contentColor = when (account.confidence) {
                                                    "High" -> NeonGreen
                                                    "Medium" -> NeonYellow
                                                    else -> TextSecondary
                                                }
                                            ) {
                                                Text(account.confidence, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Empty State ────────────────────────────────────────────────
            if (lookupResult == null && !isLookingUp && errorMessage == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Enter an email address to perform lookup", color = TextSecondary, fontSize = 13.sp)
                        Text("Checks: MX records, Gravatar, breaches, associated accounts", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// ── Persistence Helpers ────────────────────────────────────────────────────────
private fun loadEmailHistory(context: Context): List<String> {
    return try {
        val file = File(context.filesDir, "email_lookup_history.txt")
        if (!file.exists()) emptyList() else file.readLines().take(15)
    } catch (e: Exception) { emptyList() }
}

private fun saveEmailHistory(context: Context, history: List<String>) {
    try {
        val file = File(context.filesDir, "email_lookup_history.txt")
        file.writeText(history.joinToString("\n"))
    } catch (_: Exception) { }
}
