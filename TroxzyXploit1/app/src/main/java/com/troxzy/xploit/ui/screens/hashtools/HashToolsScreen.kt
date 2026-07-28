package com.troxzy.xploit.ui.screens.hashtools

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.troxzy.xploit.ui.components.CommonScaffold
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.DarkSurface
import com.troxzy.xploit.util.CryptoUtils

private val AmoledBlack = Color(0xFF0A0A0A)

private val hashAlgorithms = listOf("MD5", "SHA1", "SHA256", "SHA512", "SHA3-256", "bcrypt")

private val commonPasswords = listOf(
    "password", "123456", "12345678", "qwerty", "abc123", "monkey", "1234567",
    "letmein", "trustno1", "dragon", "baseball", "iloveyou", "master", "sunshine",
    "ashley", "bailey", "shadow", "123123", "654321", "superman", "qazwsx",
    "michael", "football", "password1", "password123", "123456789", "1234567890",
    "12345678910", "000000", "1234", "12345", "123456789a", "admin", "admin123",
    "root", "toor", "pass", "test", "guest", "master", "welcome", "login",
    "changeme", "letmein", "welcome1", "password2", "123456a", "1234567890",
    "qwerty123", "1q2w3e4r", "1qaz2wsx", "zaq1xsw2", "!@#$%^&*", "qweasdzxc",
    "passw0rd", "p@ssw0rd", "p@ssword", "pass123", "1234qwer", "qwer1234",
    "1q2w3e", "1q2w3e4r5t", "asdfgh", "asdfghjkl", "zxcvbn", "zxcvbnm",
    "qazwsx", "qazwsxedc", "mustang", "access", "joshua", "jesus", "ninja",
    "solo", "hunter", "hunter2", "starwars", "samsung", "princess", "george",
    "andrew", "charlie", "thomas", "robert", "daniel", "matthew", "jordan",
    "david", "william", "richard", "james", "nicole", "daniel", "computer",
    "internet", "service", "golden", "diamond", "secret", "summer", "winter",
    "spring", "autumn", "soccer", "hockey", "ranger", "hammer", "yankees",
    "angel", "hannah", "amanda", "lovely", "nicole", "jessica", "pepper",
    "buster", "ginger", "tigger", "felix", "cookie", "fluffy", "samantha"
)

private data class HashTypeInfo(
    val name: String,
    val description: String,
    val length: Int,
    val example: String
)

private val hashTypes = listOf(
    HashTypeInfo("MD5", "32 hex characters", 32, "d41d8cd98f00b204e9800998ecf8427e"),
    HashTypeInfo("SHA-1", "40 hex characters", 40, "da39a3ee5e6b4b0d3255bfef95601890afd80709"),
    HashTypeInfo("SHA-256", "64 hex characters", 64, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
    HashTypeInfo("SHA-512", "128 hex characters", 128, "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce..."),
    HashTypeInfo("SHA3-256", "64 hex characters", 64, "a7ffc6f8bf1ed76651c14756a061d662f580ff4de43b49fa82d80a4b80f8434a"),
    HashTypeInfo("bcrypt", "60 characters, starts with $2", 60, "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"),
    HashTypeInfo("NTLM", "32 hex characters (Windows)", 32, "31d6cfe0d16ae931b73c59d7e0c089c0"),
    HashTypeInfo("CRC32", "8 hex characters", 8, "00000000"),
    HashTypeInfo("MySQL 4.x", "16 hex characters", 16, "606727496645bcba"),
    HashTypeInfo("MySQL 5.x", "41 characters, starts with *", 41, "*6C8989366EAF6BCBBBA1932C0516FD5A5ED7392B")
)

@Composable
fun HashToolsScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Generate", "Crack", "Compare", "Identify")

    CommonScaffold(
        title = "Hash Tools",
        currentRoute = "hash_tools",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack)
                .padding(paddingValues)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkCard,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 3.dp,
                        color = NeonPurple
                    )
                },
                divider = { Divider(color = Color(0xFF2A2A2A)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) NeonPurple else Color.Gray,
                                fontSize = 13.sp
                            )
                        },
                        selectedContentColor = NeonPurple,
                        unselectedContentColor = Color.Gray
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Spacer(modifier = Modifier.height(12.dp)) }

                when (selectedTab) {
                    0 -> item { HashGenerateTab() }
                    1 -> item { HashCrackTab() }
                    2 -> item { HashCompareTab() }
                    3 -> item { HashIdentifyTab() }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HashGenerateTab() {
    var inputText by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf("SHA256") }
    var hashResult by remember { mutableStateOf("") }
    var batchMode by remember { mutableStateOf(false) }
    var batchResults by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Input",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter text to hash", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = NeonCyan
                ),
                minLines = 2,
                maxLines = 4,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* File picker */ }) {
                    Icon(
                        Icons.Default.InsertDriveFile,
                        contentDescription = "Pick file",
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text("Hash file", color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (batchMode) NeonPurple.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            1.dp,
                            if (batchMode) NeonPurple else Color(0xFF333333),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { batchMode = !batchMode }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Batch",
                        color = if (batchMode) NeonPurple else Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Algorithm",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Algorithm selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                hashAlgorithms.forEach { algo ->
                    val isSelected = selectedAlgorithm == algo
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) NeonPurple.copy(alpha = 0.2f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) NeonPurple else Color(0xFF333333),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { selectedAlgorithm = algo }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = algo,
                            color = if (isSelected) NeonPurple else Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPurple)
                    .clickable {
                        if (inputText.isNotEmpty()) {
                            if (batchMode) {
                                batchResults = hashAlgorithms.map { algo ->
                                    algo to CryptoUtils.hash(inputText, algo)
                                }
                            } else {
                                hashResult = CryptoUtils.hash(inputText, selectedAlgorithm)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GENERATE HASH",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Single result
            if (hashResult.isNotEmpty() && !batchMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Result ($selectedAlgorithm)",
                    color = NeonCyan,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = hashResult,
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { /* Copy */ }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Batch results
            if (batchResults.isNotEmpty() && batchMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Batch Results",
                    color = NeonCyan,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                batchResults.forEach { (algo, result) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = algo,
                            color = NeonPurple,
                            fontSize = 11.sp,
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            text = result,
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { /* Copy */ },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun HashCrackTab() {
    var hashInput by remember { mutableStateOf("") }
    var crackResult by remember { mutableStateOf("") }
    var isCracking by remember { mutableStateOf(false) }
    var attempts by remember { mutableStateOf(0) }
    var totalAttempts by remember { mutableStateOf(commonPasswords.size) }
    var cracked by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Hash Crack (Dictionary Attack)",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = hashInput,
                onValueChange = { hashInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter hash to crack", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = NeonCyan
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.InsertDriveFile,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Wordlist: Built-in (${commonPasswords.size} entries)",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCracking) Color(0xFF555555) else NeonPurple)
                    .clickable {
                        if (hashInput.isNotEmpty() && !isCracking) {
                            isCracking = true
                            attempts = 0
                            cracked = false
                            crackResult = ""
                            for ((index, word) in commonPasswords.withIndex()) {
                                attempts = index + 1
                                val wordHash = CryptoUtils.hash(word, "MD5")
                                if (wordHash.equals(hashInput, ignoreCase = true)) {
                                    crackResult = word
                                    cracked = true
                                    break
                                }
                            }
                            if (!cracked) {
                                crackResult = "Not found in wordlist"
                            }
                            isCracking = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isCracking) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "CRACK HASH",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }

            // Progress
            if (attempts > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Attempts: $attempts / $totalAttempts",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (attempts > 0) "${(attempts * 100 / totalAttempts)}%" else "0%",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { attempts.toFloat() / totalAttempts.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (cracked) NeonGreen else NeonPurple,
                    trackColor = DarkSurface
                )
            }

            // Result
            if (crackResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (cracked) Color(0xFF0A2A0A) else Color(0xFF2A0A0A),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (cracked) NeonGreen else Color(0xFFFF4444),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (cracked) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (cracked) NeonGreen else Color(0xFFFF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (cracked) "Cracked: $crackResult" else crackResult,
                        color = if (cracked) NeonGreen else Color(0xFFFF4444),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun HashCompareTab() {
    var hash1 by remember { mutableStateOf("") }
    var hash2 by remember { mutableStateOf("") }
    var compareResult by remember { mutableStateOf<Boolean?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Compare Hashes",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = hash1,
                onValueChange = { hash1 = it; compareResult = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter first hash", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = NeonCyan
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = hash2,
                onValueChange = { hash2 = it; compareResult = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter second hash", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = NeonCyan
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPurple)
                    .clickable {
                        if (hash1.isNotEmpty() && hash2.isNotEmpty()) {
                            compareResult = hash1.equals(hash2, ignoreCase = true)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "COMPARE",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Result
            if (compareResult != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (compareResult!!) Color(0xFF0A2A0A) else Color(0xFF2A0A0A),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (compareResult!!) NeonGreen else Color(0xFFFF4444),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (compareResult!!) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (compareResult!!) NeonGreen else Color(0xFFFF4444),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (compareResult!!) "MATCH - Hashes are identical" else "NO MATCH - Hashes are different",
                        color = if (compareResult!!) NeonGreen else Color(0xFFFF4444),
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HashIdentifyTab() {
    var hashInput by remember { mutableStateOf("") }
    var identifiedTypes by remember { mutableStateOf(listOf<HashTypeInfo>()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Identify Hash Type",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = hashInput,
                onValueChange = { hashInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter hash to identify", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = NeonCyan
                ),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPurple)
                    .clickable {
                        if (hashInput.isNotEmpty()) {
                            val clean = hashInput.trim().lowercase().replace("-", "")
                            identifiedTypes = hashTypes.filter { type ->
                                when {
                                    type.name == "bcrypt" && clean.startsWith("\$2") -> true
                                    type.name == "MySQL 5.x" && hashInput.trim().startsWith("*") -> true
                                    type.name == "CRC32" && clean.length == 8 -> true
                                    type.name == "MySQL 4.x" && clean.length == 16 -> true
                                    type.name == "NTLM" && clean.length == 32 -> true
                                    type.name == "MD5" && clean.length == 32 -> true
                                    type.name == "SHA-1" && clean.length == 40 -> true
                                    type.name == "SHA-256" && clean.length == 64 -> true
                                    type.name == "SHA3-256" && clean.length == 64 -> true
                                    type.name == "SHA-512" && clean.length == 128 -> true
                                    else -> false
                                }
                            }
                            if (identifiedTypes.isEmpty()) {
                                identifiedTypes = listOf(
                                    HashTypeInfo(
                                        "Unknown",
                                        "Could not identify hash type (length: ${clean.length})",
                                        clean.length,
                                        ""
                                    )
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IDENTIFY",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Results
            if (identifiedTypes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Possible Hash Types (${identifiedTypes.size})",
                    color = NeonCyan,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                identifiedTypes.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.name,
                                color = NeonPurple,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = type.description,
                                color = Color(0xFFBBBBBB),
                                fontSize = 12.sp
                            )
                            if (type.example.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Example: ${type.example.take(40)}...",
                                    color = Color(0xFF777777),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}
