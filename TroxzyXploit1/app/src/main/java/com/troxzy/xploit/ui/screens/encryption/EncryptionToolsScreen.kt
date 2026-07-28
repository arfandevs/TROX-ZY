package com.troxzy.xploit.ui.screens.encryption

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

private val encryptionAlgorithms = listOf(
    "AES-128 CBC", "AES-256 CBC", "AES-128 GCM", "AES-256 GCM", "DES", "ChaCha20"
)

private val keySizes = listOf("128-bit", "192-bit", "256-bit")

private fun generateHexKey(sizeBits: Int): String {
    val bytes = sizeBits / 8
    val byteArray = ByteArray(bytes)
    val random = java.security.SecureRandom()
    random.nextBytes(byteArray)
    return byteArray.joinToString("") { "%02x".format(it) }
}

private fun generateRsaKeyPair(): Pair<String, String> {
    val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048)
    val keyPair = keyPairGenerator.generateKeyPair()
    val publicKey = android.util.Base64.encodeToString(
        keyPair.public.encoded,
        android.util.Base64.NO_WRAP
    )
    val privateKey = android.util.Base64.encodeToString(
        keyPair.private.encoded,
        android.util.Base64.NO_WRAP
    )
    return publicKey to privateKey
}

@Composable
fun EncryptionToolsScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Encrypt", "Decrypt", "Key Generator")

    CommonScaffold(
        title = "Encryption Tools",
        currentRoute = "encryption_tools",
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
                    0 -> item { EncryptTab() }
                    1 -> item { DecryptTab() }
                    2 -> item { KeyGeneratorTab() }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun EncryptTab() {
    var plaintext by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf("AES-256 CBC") }
    var encryptedResult by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Encrypt",
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Plaintext input
            Text(
                text = "Plaintext",
                color = Color(0xFFBBBBBB),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = plaintext,
                onValueChange = { plaintext = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter plaintext to encrypt", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = NeonCyan
                ),
                minLines = 3,
                maxLines = 6,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Key input
            Text(
                text = "Encryption Key",
                color = Color(0xFFBBBBBB),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter encryption key", color = Color.Gray, fontSize = 13.sp) },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (showKey) "Hide key" else "Show key",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Algorithm selector
            Text(
                text = "Algorithm",
                color = Color(0xFFBBBBBB),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                encryptionAlgorithms.forEach { algo ->
                    val isSelected = selectedAlgorithm == algo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) NeonPurple else Color(0xFF333333),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedAlgorithm = algo }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isSelected) NeonPurple else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = algo,
                            color = if (isSelected) NeonPurple else Color(0xFFBBBBBB),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Note for DES and ChaCha20
            if (selectedAlgorithm == "DES" || selectedAlgorithm == "ChaCha20") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A1A0A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF8800), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF8800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedAlgorithm == "DES") {
                            "DES is considered insecure. Use only for legacy compatibility."
                        } else {
                            "ChaCha20 requires a specific implementation with nonce management."
                        },
                        color = Color(0xFFFF8800),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Encrypt button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPurple)
                    .clickable {
                        if (plaintext.isNotEmpty() && key.isNotEmpty()) {
                            try {
                                encryptedResult = when {
                                    selectedAlgorithm.startsWith("AES") -> {
                                        val mode = if (selectedAlgorithm.contains("CBC")) "CBC" else "GCM"
                                        CryptoUtils.aesEncrypt(plaintext, key, mode)
                                    }
                                    else -> {
                                        "Encryption for $selectedAlgorithm requires specific implementation"
                                    }
                                }
                                errorMessage = ""
                            } catch (e: Exception) {
                                errorMessage = "Encryption failed: ${e.message}"
                                encryptedResult = ""
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENCRYPT",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }

            // Error message
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF4444),
                    fontSize = 12.sp
                )
            }

            // Result
            AnimatedVisibility(visible = encryptedResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Encrypted Result",
                    color = NeonCyan,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = encryptedResult,
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { /* Copy to clipboard */ }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DecryptTab() {
    var ciphertext by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf("AES-256 CBC") }
    var decryptedResult by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Decrypt",
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Ciphertext input
            Text(
                text = "Ciphertext",
                color = Color(0xFFBBBBBB),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = ciphertext,
                onValueChange = { ciphertext = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter ciphertext to decrypt", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color(0xFF333333),
                    cursorColor = NeonCyan
                ),
                minLines = 3,
                maxLines = 6,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Key input
            Text(
                text = "Decryption Key",
                color = Color(0xFFBBBBBB),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it; errorMessage = "" },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter decryption key", color = Color.Gray, fontSize = 13.sp) },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            if (showKey) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = if (showKey) "Hide key" else "Show key",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
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

            Spacer(modifier = Modifier.height(12.dp))

            // Algorithm selector
            Text(
                text = "Algorithm",
                color = Color(0xFFBBBBBB),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                encryptionAlgorithms.forEach { algo ->
                    val isSelected = selectedAlgorithm == algo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) NeonPurple else Color(0xFF333333),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedAlgorithm = algo }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isSelected) NeonPurple else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = algo,
                            color = if (isSelected) NeonPurple else Color(0xFFBBBBBB),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Note for DES and ChaCha20
            if (selectedAlgorithm == "DES" || selectedAlgorithm == "ChaCha20") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2A1A0A), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFFF8800), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF8800),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedAlgorithm == "DES") {
                            "DES is considered insecure. Use only for legacy compatibility."
                        } else {
                            "ChaCha20 requires a specific implementation with nonce management."
                        },
                        color = Color(0xFFFF8800),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Decrypt button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonCyan)
                    .clickable {
                        if (ciphertext.isNotEmpty() && key.isNotEmpty()) {
                            try {
                                decryptedResult = when {
                                    selectedAlgorithm.startsWith("AES") -> {
                                        val mode = if (selectedAlgorithm.contains("CBC")) "CBC" else "GCM"
                                        CryptoUtils.aesDecrypt(ciphertext, key, mode)
                                    }
                                    else -> {
                                        "Decryption for $selectedAlgorithm requires specific implementation"
                                    }
                                }
                                errorMessage = ""
                            } catch (e: Exception) {
                                errorMessage = "Decryption failed: ${e.message}"
                                decryptedResult = ""
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = AmoledBlack,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DECRYPT",
                        color = AmoledBlack,
                        fontSize = 15.sp
                    )
                }
            }

            // Error message
            AnimatedVisibility(visible = errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF4444),
                    fontSize = 12.sp
                )
            }

            // Result
            AnimatedVisibility(visible = decryptedResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Decrypted Result",
                    color = NeonCyan,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = decryptedResult,
                        color = NeonGreen,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { /* Copy to clipboard */ }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyGeneratorTab() {
    var selectedKeySize by remember { mutableStateOf("256-bit") }
    var generatedKey by remember { mutableStateOf("") }
    var rsaPublicKey by remember { mutableStateOf("") }
    var rsaPrivateKey by remember { mutableStateOf("") }
    var showRsaKeys by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Key Generator",
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Key size selector
            Text(
                text = "Symmetric Key Size",
                color = Color(0xFFBBBBBB),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                keySizes.forEach { size ->
                    val isSelected = selectedKeySize == size
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else DarkSurface)
                            .border(
                                1.dp,
                                if (isSelected) NeonPurple else Color(0xFF333333),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedKeySize = size }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = size,
                            color = if (isSelected) NeonPurple else Color(0xFFBBBBBB),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Generate symmetric key button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPurple)
                    .clickable {
                        val bits = selectedKeySize.replace("-bit", "").toIntOrNull() ?: 256
                        generatedKey = generateHexKey(bits)
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERATE KEY",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }

            // Generated key display
            AnimatedVisibility(visible = generatedKey.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Generated Key (Hex)",
                    color = NeonCyan,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = generatedKey,
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { /* Copy to clipboard */ }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Key length: ${generatedKey.length} hex chars (${generatedKey.length * 4} bits)",
                    color = Color(0xFF888888),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF2A2A2A))
            Spacer(modifier = Modifier.height(16.dp))

            // RSA Key Pair Generation
            Text(
                text = "RSA Key Pair",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Generate 2048-bit RSA public/private key pair",
                color = Color(0xFF888888),
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A0A2A))
                    .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                    .clickable {
                        try {
                            val (pub, priv) = generateRsaKeyPair()
                            rsaPublicKey = pub
                            rsaPrivateKey = priv
                            showRsaKeys = true
                        } catch (e: Exception) {
                            rsaPublicKey = "Error: ${e.message}"
                            rsaPrivateKey = ""
                            showRsaKeys = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GENERATE RSA KEY PAIR",
                        color = NeonPurple,
                        fontSize = 13.sp
                    )
                }
            }

            // RSA Key display
            AnimatedVisibility(visible = showRsaKeys && rsaPublicKey.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                // Public Key
                Text(
                    text = "Public Key",
                    color = NeonGreen,
                    fontSize = 13.sp
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
                        text = rsaPublicKey,
                        color = NeonGreen,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { /* Copy */ }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Private Key
                if (rsaPrivateKey.isNotEmpty()) {
                    Text(
                        text = "Private Key",
                        color = Color(0xFFFF6688),
                        fontSize = 13.sp
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
                            text = rsaPrivateKey,
                            color = Color(0xFFFF6688),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = { /* Copy */ }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF2A1A0A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFF8800), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFF8800),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keep your private key secure. Never share it.",
                            color = Color(0xFFFF8800),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
