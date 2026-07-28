package com.troxzy.xploit.ui.screens.passwordgen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun PasswordGeneratorScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    CommonScaffold(
        title = "Password Generator",
        currentRoute = "password_generator",
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

            // Password Length Slider
            item {
                PasswordLengthSection()
            }

            // Charset Toggles
            item {
                CharsetTogglesSection()
            }

            // Custom Character Set
            item {
                CustomCharsetSection()
            }

            // Generate Button
            item {
                GenerateButtonSection()
            }

            // Generated Password Display
            item {
                GeneratedPasswordSection()
            }

            // Password Strength Meter
            item {
                PasswordStrengthSection()
            }

            // Batch Generation
            item {
                BatchGenerationSection()
            }

            // Password History
            item {
                PasswordHistorySection()
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun PasswordLengthSection() {
    var length by remember { mutableIntStateOf(16) }

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
                    text = "Password Length",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "$length",
                    color = NeonCyan,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = length.toFloat(),
                onValueChange = { length = it.toInt() },
                valueRange = 8f..128f,
                colors = SliderDefaults.colors(
                    thumbColor = NeonPurple,
                    activeTrackColor = NeonPurple,
                    inactiveTrackColor = DarkSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("8", color = Color.Gray, fontSize = 11.sp)
                Text("128", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun CharsetTogglesSection() {
    var uppercase by remember { mutableStateOf(true) }
    var lowercase by remember { mutableStateOf(true) }
    var numbers by remember { mutableStateOf(true) }
    var symbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Character Sets",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            CharsetToggle("Uppercase (A-Z)", uppercase) { uppercase = it }
            CharsetToggle("Lowercase (a-z)", lowercase) { lowercase = it }
            CharsetToggle("Numbers (0-9)", numbers) { numbers = it }
            CharsetToggle("Symbols (!@#$...)", symbols) { symbols = it }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFF2A2A2A))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Exclude Ambiguous Characters",
                    color = Color(0xFFBBBBBB),
                    fontSize = 13.sp
                )
                Switch(
                    checked = excludeAmbiguous,
                    onCheckedChange = { excludeAmbiguous = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = NeonPurple,
                        checkedThumbColor = Color.White,
                        uncheckedTrackColor = DarkSurface,
                        uncheckedThumbColor = Color.Gray
                    )
                )
            }
        }
    }
}

@Composable
private fun CharsetToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFFBBBBBB),
            fontSize = 13.sp
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = NeonPurple,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = DarkSurface,
                uncheckedThumbColor = Color.Gray
            )
        )
    }
}

@Composable
private fun CustomCharsetSection() {
    var customCharset by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Custom Character Set",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customCharset,
                onValueChange = { customCharset = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Enter custom characters (optional)",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
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
        }
    }
}

@Composable
private fun GenerateButtonSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NeonPurple)
            .clickable { /* Generation triggered via parent state */ },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Generate",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "GENERATE PASSWORD",
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun GeneratedPasswordSection() {
    var generatedPassword by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Generated Password",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = generatedPassword.ifEmpty { "Click generate to create a password" },
                    color = if (generatedPassword.isEmpty()) Color.Gray else NeonGreen,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (generatedPassword.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            copied = true
                            countdown = 30
                        }
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = copied && countdown > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Copied! Auto-clear in ",
                        color = NeonGreen,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${countdown}s",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordStrengthSection() {
    var strengthScore by remember { mutableIntStateOf(0) }
    var crackTime by remember { mutableStateOf("N/A") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Password Strength",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Strength meter - 10 segments
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val segmentColors = listOf(
                    Color(0xFFFF0000), Color(0xFFFF3300), Color(0xFFFF6600),
                    Color(0xFFFF9900), Color(0xFFFFCC00), Color(0xFFCCFF00),
                    Color(0xFF99FF00), Color(0xFF66FF00), Color(0xFF33FF00),
                    Color(0xFF00FF00)
                )
                for (i in 0 until 10) {
                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (i < strengthScore) 1f else 0.15f,
                        animationSpec = tween(300),
                        label = "strength_$i"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .background(segmentColors[i].copy(alpha = animatedAlpha))
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val strengthLabel = when {
                    strengthScore <= 2 -> "Very Weak"
                    strengthScore <= 4 -> "Weak"
                    strengthScore <= 6 -> "Medium"
                    strengthScore <= 8 -> "Strong"
                    else -> "Very Strong"
                }
                val strengthColor = when {
                    strengthScore <= 2 -> Color(0xFFFF0000)
                    strengthScore <= 4 -> Color(0xFFFF9900)
                    strengthScore <= 6 -> Color(0xFFFFCC00)
                    strengthScore <= 8 -> Color(0xFF66FF00)
                    else -> Color(0xFF00FF00)
                }
                Text(
                    text = strengthLabel,
                    color = strengthColor,
                    fontSize = 13.sp
                )
                Text(
                    text = "Score: $strengthScore/10",
                    color = Color(0xFFBBBBBB),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Estimated crack time: $crackTime",
                color = Color(0xFF999999),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun BatchGenerationSection() {
    var batchCount by remember { mutableStateOf("5") }
    var batchPasswords by remember { mutableStateOf(listOf<String>()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Batch Generation",
                color = Color.White,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = batchCount,
                    onValueChange = {
                        val num = it.toIntOrNull()
                        if (it.isEmpty() || (num != null && num in 1..50)) {
                            batchCount = it
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    label = { Text("Count", color = Color.Gray, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color(0xFF333333),
                        cursorColor = NeonCyan
                    ),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2A1A3A))
                        .border(1.dp, NeonPurple, RoundedCornerShape(8.dp))
                        .clickable {
                            val count = batchCount.toIntOrNull()?.coerceIn(1, 50) ?: 5
                            batchPasswords = (1..count).mapIndexed { index, _ ->
                                CryptoUtils.generatePassword(16, true, true, true, true, false, "")
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GENERATE BATCH",
                        color = NeonPurple,
                        fontSize = 13.sp
                    )
                }
            }

            if (batchPasswords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((batchPasswords.size * 40).coerceAtMost(300).dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(batchPasswords) { password ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = password,
                                color = NeonGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { /* Copy to clipboard */ },
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
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordHistorySection() {
    val passwordHistory = remember { mutableStateListOf<String>() }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Password History",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
                if (passwordHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { passwordHistory.clear() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = Color(0xFFFF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (passwordHistory.isEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No passwords generated yet",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((passwordHistory.size * 40).coerceAtMost(300).dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(passwordHistory) { password ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurface, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = password,
                                color = Color(0xFFCCCCCC),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { /* Copy to clipboard */ },
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
                    }
                }
            }
        }
    }
}
