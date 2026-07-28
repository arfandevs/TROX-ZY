package com.troxzy.xploit.ui.screens.encoder

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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

private val algorithms = listOf(
    "Base64", "URL", "HTML", "Hex", "Binary", "JWT", "ROT13", "Morse", "Auto-Detect"
)

private val morseMap = mapOf(
    'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
    'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
    'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
    'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
    'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
    'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---",
    '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....",
    '7' to "--...", '8' to "---..", '9' to "----.", ' ' to "/",
    '.' to ".-.-.-", ',' to "--..--", '?' to "..--..", '!' to "-.-.--"
)

private val reverseMorseMap = morseMap.entries.associate { (k, v) -> v to k }

private data class AutoDetectResult(
    val algorithm: String,
    val result: String,
    val success: Boolean
)

private fun encodeText(input: String, algorithm: String): String {
    if (input.isEmpty()) return ""
    return try {
        when (algorithm) {
            "Base64" -> CryptoUtils.encodeBase64(input)
            "URL" -> java.net.URLEncoder.encode(input, "UTF-8")
            "HTML" -> input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
            "Hex" -> input.toByteArray(Charsets.UTF_8).joinToString("") { "%02x".format(it) }
            "Binary" -> input.toByteArray(Charsets.UTF_8).joinToString(" ") {
                String.format("%8s", Integer.toBinaryString(it.toInt() and 0xFF)).replace(' ', '0')
            }
            "JWT" -> input // JWT is decoded only
            "ROT13" -> input.map { c ->
                when {
                    c in 'A'..'Z' -> 'A' + (c - 'A' + 13) % 26
                    c in 'a'..'z' -> 'a' + (c - 'a' + 13) % 26
                    else -> c
                }
            }.joinToString("")
            "Morse" -> input.uppercase().map { c ->
                morseMap[c] ?: c.toString()
            }.joinToString(" ")
            else -> input
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

private fun decodeText(input: String, algorithm: String): String {
    if (input.isEmpty()) return ""
    return try {
        when (algorithm) {
            "Base64" -> CryptoUtils.decodeBase64(input)
            "URL" -> java.net.URLDecoder.decode(input, "UTF-8")
            "HTML" -> input
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
            "Hex" -> input.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                .toString(Charsets.UTF_8)
            "Binary" -> input.trim().split(" ").map { it.toInt(2).toByte() }.toByteArray()
                .toString(Charsets.UTF_8)
            "JWT" -> decodeJwt(input)
            "ROT13" -> input.map { c ->
                when {
                    c in 'A'..'Z' -> 'A' + (c - 'A' + 13) % 26
                    c in 'a'..'z' -> 'a' + (c - 'a' + 13) % 26
                    else -> c
                }
            }.joinToString("")
            "Morse" -> input.split(" ").map { code ->
                reverseMorseMap[code]?.toString() ?: code
            }.joinToString("")
            else -> input
        }
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}

private fun decodeJwt(token: String): String {
    val parts = token.split(".")
    if (parts.size < 2) return "Invalid JWT format"
    val sb = StringBuilder()
    try {
        val header = CryptoUtils.decodeBase64(parts[0].replace("-", "+").replace("_", "/"))
        sb.appendLine("=== HEADER ===")
        sb.appendLine(formatJson(header))
    } catch (e: Exception) {
        sb.appendLine("=== HEADER ===")
        sb.appendLine("(Could not decode header)")
    }
    try {
        val payload = CryptoUtils.decodeBase64(parts[1].replace("-", "+").replace("_", "/"))
        sb.appendLine("=== PAYLOAD ===")
        sb.appendLine(formatJson(payload))
    } catch (e: Exception) {
        sb.appendLine("=== PAYLOAD ===")
        sb.appendLine("(Could not decode payload)")
    }
    if (parts.size >= 3) {
        sb.appendLine("=== SIGNATURE ===")
        sb.appendLine(parts[2])
    }
    return sb.toString().trimEnd()
}

private fun formatJson(json: String): String {
    val sb = StringBuilder()
    var indent = 0
    var inString = false
    var escape = false
    for (c in json) {
        when {
            escape -> {
                sb.append(c)
                escape = false
            }
            c == '\\' && inString -> {
                sb.append(c)
                escape = true
            }
            c == '"' -> {
                sb.append(c)
                inString = !inString
            }
            !inString && (c == '{' || c == '[') -> {
                sb.append(c)
                sb.appendLine()
                indent++
                sb.append("  ".repeat(indent))
            }
            !inString && (c == '}' || c == ']') -> {
                sb.appendLine()
                indent--
                sb.append("  ".repeat(indent))
                sb.append(c)
            }
            !inString && c == ',' -> {
                sb.append(c)
                sb.appendLine()
                sb.append("  ".repeat(indent))
            }
            !inString && c == ':' -> {
                sb.append(c)
                sb.append(" ")
            }
            !inString && c == ' ' -> { /* skip whitespace outside strings */ }
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

private fun autoDetectDecode(input: String): List<AutoDetectResult> {
    val results = mutableListOf<AutoDetectResult>()
    for (algo in listOf("Base64", "URL", "Hex", "Binary", "ROT13", "Morse")) {
        try {
            val decoded = decodeText(input, algo)
            if (decoded.isNotEmpty() && !decoded.startsWith("Error:") && decoded != input) {
                results.add(AutoDetectResult(algo, decoded.take(200), true))
            }
        } catch (_: Exception) {
            // Skip failed decodings
        }
    }
    // Try JWT
    if (input.count { it == '.' } >= 2) {
        try {
            val decoded = decodeText(input, "JWT")
            if (decoded.isNotEmpty() && !decoded.startsWith("Error:")) {
                results.add(AutoDetectResult("JWT", decoded.take(200), true))
            }
        } catch (_: Exception) {
            // Skip
        }
    }
    if (results.isEmpty()) {
        results.add(AutoDetectResult("None", "Could not auto-detect encoding", false))
    }
    return results
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncoderDecoderScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    var selectedMode by remember { mutableStateOf(0) }
    val modes = listOf("Encode", "Decode")
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf("Base64") }
    var algorithmExpanded by remember { mutableStateOf(false) }
    var autoDetectResults by remember { mutableStateOf(listOf<AutoDetectResult>()) }

    CommonScaffold(
        title = "Encoder / Decoder",
        currentRoute = "encoder_decoder",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AmoledBlack)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Mode selector tabs
            item {
                TabRow(
                    selectedTabIndex = selectedMode,
                    containerColor = DarkCard,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedMode]),
                            height = 3.dp,
                            color = NeonCyan
                        )
                    }
                ) {
                    modes.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedMode == index,
                            onClick = { selectedMode = index },
                            text = {
                                Text(
                                    title,
                                    color = if (selectedMode == index) NeonCyan else Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
            }

            // Algorithm selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Algorithm",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = algorithmExpanded,
                            onExpandedChange = { algorithmExpanded = !algorithmExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedAlgorithm,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = algorithmExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = NeonCyan,
                                    unfocusedTextColor = NeonCyan,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color(0xFF333333),
                                    cursorColor = NeonCyan
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = algorithmExpanded,
                                onDismissRequest = { algorithmExpanded = false },
                                containerColor = DarkSurface
                            ) {
                                algorithms.forEach { algo ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                algo,
                                                color = if (algo == selectedAlgorithm) NeonCyan else Color.White,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            selectedAlgorithm = algo
                                            algorithmExpanded = false
                                        },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 16.dp,
                                            vertical = 8.dp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input field
            item {
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
                            placeholder = {
                                Text(
                                    "Enter text to ${modes[selectedMode].lowercase()}",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color(0xFF333333),
                                cursorColor = NeonCyan
                            ),
                            minLines = 4,
                            maxLines = 8,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }

            // Action buttons row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Convert button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan)
                            .clickable {
                                if (inputText.isNotEmpty()) {
                                    if (selectedAlgorithm == "Auto-Detect") {
                                        autoDetectResults = autoDetectDecode(inputText)
                                        outputText = ""
                                    } else {
                                        outputText = if (selectedMode == 0) {
                                            encodeText(inputText, selectedAlgorithm)
                                        } else {
                                            decodeText(inputText, selectedAlgorithm)
                                        }
                                        autoDetectResults = emptyList()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = modes[selectedMode].uppercase(),
                            color = AmoledBlack,
                            fontSize = 14.sp
                        )
                    }

                    // Swap button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                            .clickable {
                                val temp = inputText
                                inputText = outputText
                                outputText = temp
                                selectedMode = if (selectedMode == 0) 1 else 0
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SwapHoriz,
                            contentDescription = "Swap",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Clear button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A2A2A))
                            .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                            .clickable {
                                inputText = ""
                                outputText = ""
                                autoDetectResults = emptyList()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Output field
            item {
                AnimatedVisibility(visible = outputText.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Output",
                                    color = Color.White,
                                    fontSize = 14.sp
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSurface, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = outputText,
                                    color = NeonGreen,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Auto-Detect results
            item {
                AnimatedVisibility(visible = autoDetectResults.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Auto-Detect Results",
                                color = NeonCyan,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            autoDetectResults.forEach { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkSurface, RoundedCornerShape(8.dp))
                                        .border(
                                            1.dp,
                                            if (result.success) Color(0xFF333333) else Color(0xFFFF4444),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = result.algorithm,
                                            color = NeonPurple,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = result.result,
                                            color = if (result.success) NeonGreen else Color(0xFFFF4444),
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { /* Copy */ }) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }

            // JWT detailed view
            item {
                AnimatedVisibility(
                    visible = outputText.isNotEmpty() && selectedAlgorithm == "JWT" && selectedMode == 1
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "JWT Decoded Details",
                                color = NeonCyan,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val sections = outputText.split("=== ")
                            sections.filter { it.isNotEmpty() }.forEach { section ->
                                val sectionLines = section.trim().split("\n", limit = 2)
                                if (sectionLines.isNotEmpty()) {
                                    val title = sectionLines[0].replace(" ===", "").replace("===", "").trim()
                                    if (title.isNotEmpty()) {
                                        Text(
                                            text = title,
                                            color = NeonPurple,
                                            fontSize = 13.sp
                                        )
                                    }
                                    if (sectionLines.size > 1) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkSurface, RoundedCornerShape(6.dp))
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = sectionLines[1].trim(),
                                                color = NeonGreen,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
