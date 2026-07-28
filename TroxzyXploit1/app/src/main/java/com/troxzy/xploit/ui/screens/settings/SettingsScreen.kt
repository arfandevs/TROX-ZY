package com.troxzy.xploit.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.troxzy.xploit.ui.components.CommonScaffold
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.NeonCard
import com.troxzy.xploit.ui.theme.AMOLEDBlack
import com.troxzy.xploit.ui.theme.DarkCard
import com.troxzy.xploit.ui.theme.DarkSurface
import com.troxzy.xploit.ui.theme.NeonCyan
import com.troxzy.xploit.ui.theme.NeonGreen
import com.troxzy.xploit.ui.theme.NeonPurple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "troxzy_settings")

private object SettingsKeys {
    val ACCENT_COLOR = stringPreferencesKey("accent_color")
    val AMOLED_ENABLED = booleanPreferencesKey("amoled_enabled")
    val AI_MODEL = stringPreferencesKey("ai_model")
    val AI_TEMPERATURE = doublePreferencesKey("ai_temperature")
    val AI_MAX_TOKENS = intPreferencesKey("ai_max_tokens")
    val AI_SYSTEM_PROMPT = stringPreferencesKey("ai_system_prompt")
    val AI_API_KEY = stringPreferencesKey("ai_api_key")
    val SCAN_TIMEOUT = intPreferencesKey("scan_timeout")
    val SCAN_THREADS = intPreferencesKey("scan_threads")
    val SCAN_PORT_RANGE = stringPreferencesKey("scan_port_range")
    val LOCK_PIN = stringPreferencesKey("lock_pin")
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val STEALTH_MODE = booleanPreferencesKey("stealth_mode")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
}

private enum class AccentColorOption(val label: String, val color: Color) {
    PURPLE("Purple", NeonPurple),
    CYAN("Cyan", NeonCyan),
    GREEN("Green", NeonGreen),
    CUSTOM("Custom", Color(0xFFFF6600))
}

private enum class AIModelOption(val label: String, val id: String) {
    GPT4("GPT-4", "gpt-4"),
    GPT4_MINI("GPT-4 Mini", "gpt-4-mini"),
    CLAUDE("Claude 3.5", "claude-3.5"),
    LOCAL("Local Model", "local")
}

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Settings state
    var accentColor by remember { mutableStateOf(AccentColorOption.PURPLE) }
    var amoledEnabled by remember { mutableStateOf(true) }
    var aiModel by remember { mutableStateOf(AIModelOption.GPT4) }
    var aiTemperature by remember { mutableFloatStateOf(0.7f) }
    var aiMaxTokens by remember { mutableStateOf("2048") }
    var aiSystemPrompt by remember { mutableStateOf("You are Troxzy AI, a cybersecurity assistant.") }
    var aiApiKey by remember { mutableStateOf("") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var scanTimeout by remember { mutableStateOf("30") }
    var scanThreads by remember { mutableStateOf("10") }
    var scanPortRange by remember { mutableStateOf("1-65535") }
    var lockPin by remember { mutableStateOf("") }
    var biometricEnabled by remember { mutableStateOf(false) }
    var stealthMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    // PIN dialog
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    // Update check
    var updateMessage by remember { mutableStateOf<String?>(null) }

    // Expanded sections
    var expandedSections by remember { mutableStateOf(setOf<String>("theme")) }

    // Load settings from DataStore
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val prefs = context.dataStore.data.first()
            accentColor = AccentColorOption.entries.find {
                it.label == prefs[SettingsKeys.ACCENT_COLOR]
            } ?: AccentColorOption.PURPLE
            amoledEnabled = prefs[SettingsKeys.AMOLED_ENABLED] ?: true
            aiModel = AIModelOption.entries.find {
                it.id == prefs[SettingsKeys.AI_MODEL]
            } ?: AIModelOption.GPT4
            aiTemperature = (prefs[SettingsKeys.AI_TEMPERATURE] ?: 0.7).toFloat()
            aiMaxTokens = (prefs[SettingsKeys.AI_MAX_TOKENS] ?: 2048).toString()
            aiSystemPrompt = prefs[SettingsKeys.AI_SYSTEM_PROMPT] ?: "You are Troxzy AI, a cybersecurity assistant."
            aiApiKey = prefs[SettingsKeys.AI_API_KEY] ?: ""
            scanTimeout = (prefs[SettingsKeys.SCAN_TIMEOUT] ?: 30).toString()
            scanThreads = (prefs[SettingsKeys.SCAN_THREADS] ?: 10).toString()
            scanPortRange = prefs[SettingsKeys.SCAN_PORT_RANGE] ?: "1-65535"
            lockPin = prefs[SettingsKeys.LOCK_PIN] ?: ""
            biometricEnabled = prefs[SettingsKeys.BIOMETRIC_ENABLED] ?: false
            stealthMode = prefs[SettingsKeys.STEALTH_MODE] ?: false
            notificationsEnabled = prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true
        }
    }

    fun saveSetting(key: Preferences.Key<*>, value: Any) {
        scope.launch(Dispatchers.IO) {
            context.dataStore.edit { prefs ->
                when (key) {
                    SettingsKeys.ACCENT_COLOR -> prefs[SettingsKeys.ACCENT_COLOR] = value as String
                    SettingsKeys.AMOLED_ENABLED -> prefs[SettingsKeys.AMOLED_ENABLED] = value as Boolean
                    SettingsKeys.AI_MODEL -> prefs[SettingsKeys.AI_MODEL] = value as String
                    SettingsKeys.AI_TEMPERATURE -> prefs[SettingsKeys.AI_TEMPERATURE] = value as Double
                    SettingsKeys.AI_MAX_TOKENS -> prefs[SettingsKeys.AI_MAX_TOKENS] = value as Int
                    SettingsKeys.AI_SYSTEM_PROMPT -> prefs[SettingsKeys.AI_SYSTEM_PROMPT] = value as String
                    SettingsKeys.AI_API_KEY -> prefs[SettingsKeys.AI_API_KEY] = value as String
                    SettingsKeys.SCAN_TIMEOUT -> prefs[SettingsKeys.SCAN_TIMEOUT] = value as Int
                    SettingsKeys.SCAN_THREADS -> prefs[SettingsKeys.SCAN_THREADS] = value as Int
                    SettingsKeys.SCAN_PORT_RANGE -> prefs[SettingsKeys.SCAN_PORT_RANGE] = value as String
                    SettingsKeys.LOCK_PIN -> prefs[SettingsKeys.LOCK_PIN] = value as String
                    SettingsKeys.BIOMETRIC_ENABLED -> prefs[SettingsKeys.BIOMETRIC_ENABLED] = value as Boolean
                    SettingsKeys.STEALTH_MODE -> prefs[SettingsKeys.STEALTH_MODE] = value as Boolean
                    SettingsKeys.NOTIFICATIONS_ENABLED -> prefs[SettingsKeys.NOTIFICATIONS_ENABLED] = value as Boolean
                }
            }
        }
    }

    val currentAccentColor = accentColor.color

    CommonScaffold(
        title = "Settings",
        currentRoute = "settings",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AMOLEDBlack)
                .padding(paddingValues),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Theme Customization
            item {
                ExpandableSection(
                    title = "Theme Customization",
                    icon = Icons.Default.Palette,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("theme"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("theme"))
                            expandedSections - "theme" else expandedSections + "theme"
                    }
                ) {
                    // Accent color picker
                    Text("Accent Color", color = Color.Gray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccentColorOption.entries.forEach { option ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        accentColor = option
                                        saveSetting(SettingsKeys.ACCENT_COLOR, option.label)
                                    },
                                color = if (accentColor == option) option.color.copy(alpha = 0.2f) else DarkCard,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(option.color)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        option.label,
                                        color = if (accentColor == option) option.color else Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AMOLED toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Android, null, tint = currentAccentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AMOLED Black Mode", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = amoledEnabled,
                            onCheckedChange = {
                                amoledEnabled = it
                                saveSetting(SettingsKeys.AMOLED_ENABLED, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = currentAccentColor,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
            }

            // AI Configuration
            item {
                ExpandableSection(
                    title = "AI Configuration",
                    icon = Icons.Default.Psychology,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("ai"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("ai"))
                            expandedSections - "ai" else expandedSections + "ai"
                    }
                ) {
                    // Model dropdown
                    Text("Model", color = Color.Gray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AIModelOption.entries.forEach { model ->
                            OutlinedButton(
                                onClick = {
                                    aiModel = model
                                    saveSetting(SettingsKeys.AI_MODEL, model.id)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (aiModel == model) currentAccentColor.copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (aiModel == model) currentAccentColor else Color.Gray
                                )
                            ) {
                                Text(model.label, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Temperature slider
                    Text("Temperature: ${String.format("%.1f", aiTemperature)}", color = Color.Gray, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("0", color = Color.Gray, fontSize = 11.sp)
                        androidx.compose.material3.Slider(
                            value = aiTemperature,
                            onValueChange = { aiTemperature = it },
                            valueRange = 0f..2f,
                            steps = 19,
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.SliderDefaults.colors(
                                thumbColor = currentAccentColor,
                                activeTrackColor = currentAccentColor
                            ),
                            onValueChangeFinished = {
                                saveSetting(SettingsKeys.AI_TEMPERATURE, aiTemperature.toDouble())
                            }
                        )
                        Text("2", color = Color.Gray, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Max tokens
                    OutlinedTextField(
                        value = aiMaxTokens,
                        onValueChange = {
                            if (it.toIntOrNull() != null && it.toInt() >= 1 && it.toInt() <= 8192) {
                                aiMaxTokens = it
                                saveSetting(SettingsKeys.AI_MAX_TOKENS, it.toInt())
                            }
                        },
                        label = { Text("Max Tokens (1-8192)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // System prompt
                    OutlinedTextField(
                        value = aiSystemPrompt,
                        onValueChange = {
                            aiSystemPrompt = it
                            saveSetting(SettingsKeys.AI_SYSTEM_PROMPT, it)
                        },
                        label = { Text("System Prompt", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // API Key
                    OutlinedTextField(
                        value = aiApiKey,
                        onValueChange = {
                            aiApiKey = it
                            saveSetting(SettingsKeys.AI_API_KEY, it)
                        },
                        label = { Text("API Key", color = Color.Gray) },
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    if (apiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    "Toggle visibility",
                                    tint = currentAccentColor
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )
                }
            }

            // Scan Settings
            item {
                ExpandableSection(
                    title = "Scan Settings",
                    icon = Icons.Default.NetworkCheck,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("scan"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("scan"))
                            expandedSections - "scan" else expandedSections + "scan"
                    }
                ) {
                    OutlinedTextField(
                        value = scanTimeout,
                        onValueChange = {
                            if (it.toIntOrNull() != null && it.toInt() > 0) {
                                scanTimeout = it
                                saveSetting(SettingsKeys.SCAN_TIMEOUT, it.toInt())
                            }
                        },
                        label = { Text("Default Timeout (seconds)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = scanThreads,
                        onValueChange = {
                            if (it.toIntOrNull() != null && it.toInt() > 0 && it.toInt() <= 100) {
                                scanThreads = it
                                saveSetting(SettingsKeys.SCAN_THREADS, it.toInt())
                            }
                        },
                        label = { Text("Thread Count (1-100)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = scanPortRange,
                        onValueChange = {
                            scanPortRange = it
                            saveSetting(SettingsKeys.SCAN_PORT_RANGE, it)
                        },
                        label = { Text("Default Port Range (e.g. 1-65535)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )
                }
            }

            // Data Management
            item {
                ExpandableSection(
                    title = "Data Management",
                    icon = Icons.Default.Storage,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("data"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("data"))
                            expandedSections - "data" else expandedSections + "data"
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Export all data to clipboard
                                scope.launch {
                                    val report = buildExportData(context)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Troxzy Data Export", report))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", color = AMOLEDBlack, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                // Import - placeholder: opens file dialog concept
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        context.dataStore.edit { it.clear() }
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = currentAccentColor)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val cacheDir = context.cacheDir
                                        cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                                    } catch (e: Exception) {}
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Cache", color = Color.Red, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        context.dataStore.edit { prefs ->
                                            prefs.remove(SettingsKeys.AI_SYSTEM_PROMPT)
                                            prefs.remove(SettingsKeys.AI_API_KEY)
                                        }
                                    }
                                    aiSystemPrompt = "You are Troxzy AI, a cybersecurity assistant."
                                    aiApiKey = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear History", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }

            // App Lock
            item {
                ExpandableSection(
                    title = "App Lock",
                    icon = Icons.Default.Lock,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("lock"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("lock"))
                            expandedSections - "lock" else expandedSections + "lock"
                    }
                ) {
                    // PIN setup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPinDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Keyboard, null, tint = currentAccentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("PIN Lock", color = Color.White, fontSize = 14.sp)
                                Text(
                                    if (lockPin.isNotEmpty()) "PIN set: ****" else "No PIN set",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Biometric toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Fingerprint, null, tint = currentAccentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Biometric Unlock", color = Color.White, fontSize = 14.sp)
                                val biometricStatus = remember {
                                    val bm = BiometricManager.from(context)
                                    when (bm.canAuthenticate(BiometricManager.AUTHENTICATORS_FINGERPRINT)) {
                                        BiometricManager.BIOMETRIC_SUCCESS -> "Available"
                                        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "No hardware"
                                        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Unavailable"
                                        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No fingerprints enrolled"
                                        else -> "Not available"
                                    }
                                }
                                Text(biometricStatus, color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = {
                                biometricEnabled = it
                                saveSetting(SettingsKeys.BIOMETRIC_ENABLED, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = currentAccentColor,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
            }

            // Stealth Mode
            item {
                ExpandableSection(
                    title = "Stealth Mode",
                    icon = Icons.Default.Shield,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("stealth"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("stealth"))
                            expandedSections - "stealth" else expandedSections + "stealth"
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hide App Icon", color = Color.White, fontSize = 14.sp)
                            Text("When enabled, the app icon will be hidden from the launcher. Dial *#*#789789#*#* to open.", color = Color.Gray, fontSize = 11.sp)
                        }
                        Switch(
                            checked = stealthMode,
                            onCheckedChange = {
                                stealthMode = it
                                saveSetting(SettingsKeys.STEALTH_MODE, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = currentAccentColor,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
            }

            // Notification Settings
            item {
                ExpandableSection(
                    title = "Notifications",
                    icon = Icons.Default.Notifications,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("notifications"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("notifications"))
                            expandedSections - "notifications" else expandedSections + "notifications"
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null, tint = currentAccentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enable Notifications", color = Color.White, fontSize = 14.sp)
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = {
                                notificationsEnabled = it
                                saveSetting(SettingsKeys.NOTIFICATIONS_ENABLED, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = currentAccentColor,
                                checkedThumbColor = Color.White
                            )
                        )
                    }
                }
            }

            // About
            item {
                ExpandableSection(
                    title = "About",
                    icon = Icons.Default.Info,
                    accentColor = currentAccentColor,
                    isExpanded = expandedSections.contains("about"),
                    onToggle = {
                        expandedSections = if (expandedSections.contains("about"))
                            expandedSections - "about" else expandedSections + "about"
                    }
                ) {
                    InfoRow("App Name", "TroxzyXploit")
                    InfoRow("Version", "1.0.0")
                    InfoRow("Owner", "by Troxzy")
                    InfoRow("Telegram", "t.me/SoloBanNoTrash")
                    InfoRow("License", "Proprietary")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Credits", color = currentAccentColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("• Troxzy - Lead Developer & Designer", color = Color.Gray, fontSize = 12.sp)
                    Text("• SoloBanNoTrash Community - Testing & Support", color = Color.Gray, fontSize = 12.sp)
                    Text("• Jetpack Compose - UI Framework", color = Color.Gray, fontSize = 12.sp)
                    Text("• Kotlin - Programming Language", color = Color.Gray, fontSize = 12.sp)
                }
            }

            // Update Checker
            item {
                Button(
                    onClick = {
                        scope.launch {
                            updateMessage = "Checking for updates..."
                            delay(2000)
                            updateMessage = "No updates available. You are on the latest version (v1.0.0)."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = currentAccentColor)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check for Updates", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (updateMessage != null) {
                    Text(
                        updateMessage!!,
                        color = when {
                            updateMessage!!.startsWith("No updates") -> NeonGreen
                            updateMessage!!.startsWith("Checking") -> currentAccentColor
                            else -> Color.White
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Feedback Button
            item {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/SoloBanNoTrash"))
                            context.startActivity(intent)
                        } catch (e: Exception) {}
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Feedback / Telegram", color = AMOLEDBlack, fontWeight = FontWeight.Bold)
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    // PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set PIN Lock", color = Color.White) },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to lock the app:", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                pinInput = it
                            }
                        },
                        placeholder = { Text("4-digit PIN", color = Color.Gray) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = currentAccentColor,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInput.length == 4) {
                            lockPin = pinInput
                            saveSetting(SettingsKeys.LOCK_PIN, pinInput)
                            showPinDialog = false
                            pinInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentAccentColor)
                ) {
                    Text("Set PIN", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        pinInput = ""
                    }
                ) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        color = DarkCard,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = accentColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = DarkSurface)
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private suspend fun buildExportData(context: Context): String {
    val prefs = context.dataStore.data.first()
    val sb = StringBuilder()
    sb.appendLine("=== TROXZY XPLOIT DATA EXPORT ===")
    sb.appendLine("Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
    sb.appendLine()
    sb.appendLine("Theme:")
    sb.appendLine("  Accent Color: ${prefs[SettingsKeys.ACCENT_COLOR] ?: "Purple"}")
    sb.appendLine("  AMOLED: ${prefs[SettingsKeys.AMOLED_ENABLED] ?: true}")
    sb.appendLine()
    sb.appendLine("AI Config:")
    sb.appendLine("  Model: ${prefs[SettingsKeys.AI_MODEL] ?: "gpt-4"}")
    sb.appendLine("  Temperature: ${prefs[SettingsKeys.AI_TEMPERATURE] ?: 0.7}")
    sb.appendLine("  Max Tokens: ${prefs[SettingsKeys.AI_MAX_TOKENS] ?: 2048}")
    sb.appendLine("  System Prompt: ${prefs[SettingsKeys.AI_SYSTEM_PROMPT] ?: ""}")
    sb.appendLine()
    sb.appendLine("Scan Config:")
    sb.appendLine("  Timeout: ${prefs[SettingsKeys.SCAN_TIMEOUT] ?: 30}s")
    sb.appendLine("  Threads: ${prefs[SettingsKeys.SCAN_THREADS] ?: 10}")
    sb.appendLine("  Port Range: ${prefs[SettingsKeys.SCAN_PORT_RANGE] ?: "1-65535"}")
    sb.appendLine()
    sb.appendLine("Security:")
    sb.appendLine("  PIN: ${if (prefs[SettingsKeys.LOCK_PIN]?.isNotEmpty() == true) "Set" else "Not Set"}")
    sb.appendLine("  Biometric: ${prefs[SettingsKeys.BIOMETRIC_ENABLED] ?: false}")
    sb.appendLine("  Stealth: ${prefs[SettingsKeys.STEALTH_MODE] ?: false}")
    sb.appendLine("  Notifications: ${prefs[SettingsKeys.NOTIFICATIONS_ENABLED] ?: true}")
    sb.appendLine()
    sb.appendLine("=== END EXPORT ===")
    return sb.toString()
}
