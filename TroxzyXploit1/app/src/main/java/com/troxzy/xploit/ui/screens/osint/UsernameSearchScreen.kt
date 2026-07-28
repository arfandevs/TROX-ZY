package com.troxzy.xploit.ui.screens.osint

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.io.File
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

// ── Data Models ───────────────────────────────────────────────────────────────
enum class PlatformCategory { Social, Forum, Gaming, Development, Media, All }

data class Platform(
    val name: String,
    val urlPattern: String,
    val category: PlatformCategory
)

data class SearchResult(
    val platform: Platform,
    val status: ResultStatus,
    val url: String
)

enum class ResultStatus { FOUND, NOT_FOUND, ERROR, PENDING }

data class SearchHistoryEntry(
    val username: String,
    val timestamp: Long,
    val foundCount: Int,
    val totalChecked: Int
)

// ── Platform List (30+) ──────────────────────────────────────────────────────
private val PLATFORMS = listOf(
    Platform("GitHub", "https://github.com/{username}", PlatformCategory.Development),
    Platform("Twitter/X", "https://twitter.com/{username}", PlatformCategory.Social),
    Platform("Instagram", "https://www.instagram.com/{username}", PlatformCategory.Social),
    Platform("Reddit", "https://www.reddit.com/user/{username}", PlatformCategory.Forum),
    Platform("YouTube", "https://www.youtube.com/@{username}", PlatformCategory.Media),
    Platform("TikTok", "https://www.tiktok.com/@{username}", PlatformCategory.Media),
    Platform("LinkedIn", "https://www.linkedin.com/in/{username}", PlatformCategory.Social),
    Platform("Pinterest", "https://www.pinterest.com/{username}", PlatformCategory.Social),
    Platform("Twitch", "https://www.twitch.tv/{username}", PlatformCategory.Gaming),
    Platform("Steam", "https://steamcommunity.com/id/{username}", PlatformCategory.Gaming),
    Platform("Spotify", "https://open.spotify.com/user/{username}", PlatformCategory.Media),
    Platform("Medium", "https://medium.com/@{username}", PlatformCategory.Media),
    Platform("DevTo", "https://dev.to/{username}", PlatformCategory.Development),
    Platform("HackerNews", "https://news.ycombinator.com/user?id={username}", PlatformCategory.Development),
    Platform("GitLab", "https://gitlab.com/{username}", PlatformCategory.Development),
    Platform("Keybase", "https://keybase.io/{username}", PlatformCategory.Social),
    Platform("Telegram", "https://t.me/{username}", PlatformCategory.Social),
    Platform("Discord", "https://discord.com/users/{username}", PlatformCategory.Social),
    Platform("Facebook", "https://www.facebook.com/{username}", PlatformCategory.Social),
    Platform("Snapchat", "https://www.snapchat.com/add/{username}", PlatformCategory.Social),
    Platform("Quora", "https://www.quora.com/profile/{username}", PlatformCategory.Forum),
    Platform("Flickr", "https://www.flickr.com/people/{username}", PlatformCategory.Media),
    Platform("Vimeo", "https://vimeo.com/{username}", PlatformCategory.Media),
    Platform("SoundCloud", "https://soundcloud.com/{username}", PlatformCategory.Media),
    Platform("Bandcamp", "https://bandcamp.com/{username}", PlatformCategory.Media),
    Platform("Patreon", "https://www.patreon.com/{username}", PlatformCategory.Media),
    Platform("Kik", "https://kik.me/{username}", PlatformCategory.Social),
    Platform("About.me", "https://about.me/{username}", PlatformCategory.Social),
    Platform("MySpace", "https://myspace.com/{username}", PlatformCategory.Social),
    Platform("Dribbble", "https://dribbble.com/{username}", PlatformCategory.Development),
)

// ── OkHttp Client ─────────────────────────────────────────────────────────────
private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

// ── Main Composable ───────────────────────────────────────────────────────────
@Composable
fun UsernameSearchScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var usernameInput by remember { mutableStateOf("") }
    var batchInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchProgress by remember { mutableStateOf(0) }
    var totalPlatforms by remember { mutableStateOf(0) }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(PlatformCategory.All) }
    var showOnlyFound by remember { mutableStateOf(false) }
    var searchHistory by remember { mutableStateOf(loadSearchHistory(context)) }
    var showHistory by remember { mutableStateOf(false) }
    var showBatchSection by remember { mutableStateOf(false) }
    var currentUsername by remember { mutableStateOf("") }
    var searchedUsernames by remember { mutableStateOf<List<String>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun getFilteredResults(): List<SearchResult> {
        var filtered = results
        if (selectedCategory != PlatformCategory.All) {
            filtered = filtered.filter { it.platform.category == selectedCategory }
        }
        if (showOnlyFound) {
            filtered = filtered.filter { it.status == ResultStatus.FOUND }
        }
        return filtered
    }

    fun performSearch(usernames: List<String>) {
        if (usernames.isEmpty() || isSearching) return
        scope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                isSearching = true
                errorMessage = null
                results = PLATFORMS.map { SearchResult(it, ResultStatus.PENDING, "") }
                totalPlatforms = PLATFORMS.size
                searchProgress = 0
                searchedUsernames = usernames
            }

            val allResults = mutableListOf<SearchResult>()
            for (username in usernames) {
                withContext(Dispatchers.Main) {
                    currentUsername = username
                }
                val userResults = mutableListOf<SearchResult>()
                for ((index, platform) in PLATFORMS.withIndex()) {
                    val url = platform.urlPattern.replace("{username}", username)
                    val status = try {
                        val request = Request.Builder().url(url).head().build()
                        val response = okHttpClient.newCall(request).execute()
                        when {
                            response.isSuccessful -> ResultStatus.FOUND
                            response.code == 404 -> ResultStatus.NOT_FOUND
                            response.code in 301..399 -> ResultStatus.FOUND
                            else -> ResultStatus.NOT_FOUND
                        }
                    } catch (e: Exception) {
                        ResultStatus.ERROR
                    }
                    val result = SearchResult(platform, status, url)
                    userResults.add(result)
                    withContext(Dispatchers.Main) {
                        searchProgress = index + 1
                        results = (allResults + userResults).toList()
                    }
                }
                allResults.addAll(userResults)
            }

            withContext(Dispatchers.Main) {
                isSearching = false
                results = allResults.toList()
                // Save to history
                usernames.forEach { uname ->
                    val foundCount = allResults.count {
                        it.url.contains(uname) && it.status == ResultStatus.FOUND
                    }
                    val entry = SearchHistoryEntry(
                        username = uname,
                        timestamp = System.currentTimeMillis(),
                        foundCount = foundCount,
                        totalChecked = PLATFORMS.size
                    )
                    searchHistory = (listOf(entry) + searchHistory.filter { it.username != uname }).take(20)
                    saveSearchHistory(context, searchHistory)
                }
            }
        }
    }

    fun exportResults() {
        val filtered = getFilteredResults()
        if (filtered.isEmpty()) {
            Toast.makeText(context, "No results to export", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch(Dispatchers.IO) {
            val sb = StringBuilder()
            sb.appendLine("TroxzyXploit - Username Search Results")
            sb.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            sb.appendLine("Usernames: ${searchedUsernames.joinToString(", ")}")
            sb.appendLine("─".repeat(50))
            filtered.forEach { result ->
                val statusStr = when (result.status) {
                    ResultStatus.FOUND -> "[FOUND]"
                    ResultStatus.NOT_FOUND -> "[NOT FOUND]"
                    ResultStatus.ERROR -> "[ERROR]"
                    ResultStatus.PENDING -> "[PENDING]"
                }
                sb.appendLine("${result.platform.name} $statusStr → ${result.url}")
            }
            sb.appendLine()
            sb.appendLine("Total: ${filtered.size} | Found: ${filtered.count { it.status == ResultStatus.FOUND }} | Not Found: ${filtered.count { it.status == ResultStatus.NOT_FOUND }}")

            val fileName = "username_search_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            val file = File(context.getExternalFilesDir(null), fileName)
            file.writeText(sb.toString())

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        }
    }

    CommonScaffold(
        title = "Username Search",
        currentRoute = "osint_username_search",
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
                text = "USERNAME OSINT",
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // ── Input Row ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { Text("Enter username", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = DarkSurface,
                        cursorColor = NeonCyan
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = { performSearch(listOf(usernameInput.trim())) }
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = NeonPurple)
                    }
                )
                Button(
                    onClick = { performSearch(listOf(usernameInput.trim())) },
                    enabled = usernameInput.isNotBlank() && !isSearching,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Search", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // ── Action Buttons Row ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // History button
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
                // Batch search toggle
                OutlinedButton(
                    onClick = { showBatchSection = !showBatchSection },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Batch", fontSize = 11.sp)
                }
                // Export button
                OutlinedButton(
                    onClick = { exportResults() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPurple),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonPurple),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 11.sp)
                }
            }

            // ── Batch Search Section ───────────────────────────────────────
            AnimatedVisibility(visible = showBatchSection, enter = fadeIn(), exit = fadeOut()) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Batch Username Search",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = batchInput,
                            onValueChange = { batchInput = it },
                            label = { Text("Comma-separated usernames", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonGreen,
                                unfocusedBorderColor = DarkSurface,
                                cursorColor = NeonCyan
                            ),
                            minLines = 2,
                            maxLines = 4
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val usernames = batchInput.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .distinct()
                                if (usernames.isNotEmpty()) {
                                    performSearch(usernames)
                                } else {
                                    Toast.makeText(context, "Enter at least one username", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = batchInput.isNotBlank() && !isSearching,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Search All", color = AmoledBlack, fontWeight = FontWeight.Bold)
                        }
                    }
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
                        Text(
                            "Search History",
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (searchHistory.isEmpty()) {
                            Text(
                                "No search history yet",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp)
                                    .padding(top = 4.dp)
                            ) {
                                items(searchHistory) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                usernameInput = entry.username
                                                showHistory = false
                                            }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                entry.username,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "${entry.foundCount}/${entry.totalChecked} found • ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(entry.timestamp))}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Use",
                                            tint = TextSecondary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { usernameInput = entry.username; showHistory = false }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Category Filter Chips ──────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val categories = PlatformCategory.entries
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    val chipColor = when (category) {
                        PlatformCategory.Social -> NeonPurple
                        PlatformCategory.Forum -> NeonCyan
                        PlatformCategory.Gaming -> NeonGreen
                        PlatformCategory.Development -> Color(0xFFFF6B00)
                        PlatformCategory.Media -> Color(0xFFFFD700)
                        PlatformCategory.All -> Color.White
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) chipColor else DarkSurface,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = category },
                        color = if (isSelected) chipColor.copy(alpha = 0.15f) else DarkCard,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            category.name,
                            color = if (isSelected) chipColor else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── Filter Toggle: Show Only Found ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show only found", color = TextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = showOnlyFound,
                        onCheckedChange = { showOnlyFound = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = NeonGreen,
                            checkedThumbColor = Color.White,
                            uncheckedTrackColor = DarkSurface,
                            uncheckedThumbColor = TextSecondary
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
                if (results.isNotEmpty()) {
                    val foundCount = results.count { it.status == ResultStatus.FOUND }
                    Text(
                        "$foundCount found / ${results.size} checked",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Progress Indicator ─────────────────────────────────────────
            if (isSearching) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { if (totalPlatforms > 0) searchProgress.toFloat() / totalPlatforms else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonPurple,
                        trackColor = DarkSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Checking $searchProgress of ${totalPlatforms}+ platforms...${if (searchedUsernames.size > 1) " (User: $currentUsername)" else ""}",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            }

            // ── Error Message ──────────────────────────────────────────────
            errorMessage?.let { error ->
                Text(
                    error,
                    color = NeonRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // ── Results LazyColumn ─────────────────────────────────────────
            val filteredResults = getFilteredResults()
            if (results.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enter a username to search across 30+ platforms",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredResults) { result ->
                        PlatformResultCard(
                            result = result,
                            onOpenUrl = { url ->
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onCopyUrl = { url ->
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("URL", url))
                                Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Platform Result Card ──────────────────────────────────────────────────────
@Composable
private fun PlatformResultCard(
    result: SearchResult,
    onOpenUrl: (String) -> Unit,
    onCopyUrl: (String) -> Unit
) {
    val statusColor = when (result.status) {
        ResultStatus.FOUND -> NeonGreen
        ResultStatus.NOT_FOUND -> TextSecondary
        ResultStatus.ERROR -> NeonRed
        ResultStatus.PENDING -> Color(0xFF555555)
    }
    val statusText = when (result.status) {
        ResultStatus.FOUND -> "Found"
        ResultStatus.NOT_FOUND -> "Not Found"
        ResultStatus.ERROR -> "Error"
        ResultStatus.PENDING -> "Pending..."
    }
    val categoryIcon = when (result.platform.category) {
        PlatformCategory.Social -> "👥"
        PlatformCategory.Forum -> "💬"
        PlatformCategory.Gaming -> "🎮"
        PlatformCategory.Development -> "💻"
        PlatformCategory.Media -> "🎬"
        PlatformCategory.All -> "🔍"
    }

    NeonCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(categoryIcon, fontSize = 16.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        result.platform.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        result.url,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            // Status badge
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = if (result.status == ResultStatus.FOUND) 1.dp else 0.dp,
                        color = if (result.status == ResultStatus.FOUND) NeonGreen else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    ),
                color = if (result.status == ResultStatus.FOUND) NeonGreen.copy(alpha = 0.15f) else DarkSurface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            // Action buttons
            if (result.status == ResultStatus.FOUND) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.Link,
                    contentDescription = "Open",
                    tint = NeonCyan,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { onOpenUrl(result.url) }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy",
                    tint = TextSecondary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCopyUrl(result.url) }
                )
            }
        }
    }
}

// ── Persistence Helpers ────────────────────────────────────────────────────────
private fun loadSearchHistory(context: Context): List<SearchHistoryEntry> {
    return try {
        val file = File(context.filesDir, "username_search_history.txt")
        if (!file.exists()) return emptyList()
        file.readLines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 4) {
                SearchHistoryEntry(
                    username = parts[0],
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    foundCount = parts[2].toIntOrNull() ?: 0,
                    totalChecked = parts[3].toIntOrNull() ?: 0
                )
            } else null
        }
    } catch (e: Exception) { emptyList() }
}

private fun saveSearchHistory(context: Context, history: List<SearchHistoryEntry>) {
    try {
        val file = File(context.filesDir, "username_search_history.txt")
        file.writeText(history.joinToString("\n") { "${it.username}|${it.timestamp}|${it.foundCount}|${it.totalChecked}" })
    } catch (_: Exception) { }
}
