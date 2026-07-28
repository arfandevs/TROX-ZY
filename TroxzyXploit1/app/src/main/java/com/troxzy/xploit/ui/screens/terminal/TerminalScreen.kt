package com.troxzy.xploit.ui.screens.terminal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val MonoFont = FontFamily.Monospace

private data class TerminalLine(
    val timestamp: String,
    val text: String,
    val type: LineType
)

private enum class LineType {
    OUTPUT, ERROR, COMMAND, WARNING, SYSTEM
}

private data class TerminalSession(
    val id: Int,
    val name: String,
    val lines: List<TerminalLine>,
    val history: List<String>,
    val workingDir: String
)

private enum class TerminalTheme(val label: String, val outputColor: Color, val promptColor: Color) {
    GREEN_ON_BLACK("Green on Black", NeonGreen, NeonGreen),
    AMBER_ON_BLACK("Amber on Black", Color(0xFFFFBF00), Color(0xFFFFBF00)),
    CYAN_ON_BLACK("Cyan on Black", NeonCyan, NeonCyan),
    CUSTOM("Custom", NeonPurple, NeonPurple)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var sessions by remember { mutableStateOf(listOf(
        TerminalSession(1, "Session 1", emptyList(), emptyList(), "/")
    )) }
    var activeSessionId by remember { mutableIntStateOf(1) }
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var historyIndex by remember { mutableIntStateOf(-1) }
    var terminalTheme by remember { mutableStateOf(TerminalTheme.GREEN_ON_BLACK) }
    var fontSize by remember { mutableFloatStateOf(14f) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showThemeSelector by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var copyConfirmation by remember { mutableStateOf<String?>(null) }

    val activeSession = sessions.first { it.id == activeSessionId }

    fun timestamp(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun addLine(sessionId: Int, text: String, type: LineType) {
        sessions = sessions.map { session ->
            if (session.id == sessionId) {
                session.copy(lines = session.lines + TerminalLine(timestamp(), text, type))
            } else session
        }
    }

    fun addHistory(sessionId: Int, cmd: String) {
        sessions = sessions.map { session ->
            if (session.id == sessionId) {
                session.copy(history = session.history + cmd)
            } else session
        }
    }

    fun executeCommand(command: String) {
        if (command.isBlank()) return
        val trimmed = command.trim()
        addLine(activeSessionId, "troxzy@xploit:~$ $trimmed", LineType.COMMAND)
        addHistory(activeSessionId, trimmed)
        historyIndex = -1

        scope.launch {
            when {
                trimmed == "clear" -> {
                    sessions = sessions.map { session ->
                        if (session.id == activeSessionId) session.copy(lines = emptyList())
                        else session
                    }
                }
                trimmed == "help" -> {
                    val helpText = """
                        ╔══════════════════════════════════════════╗
                        ║         TROXZY TERMINAL COMMANDS         ║
                        ╠══════════════════════════════════════════╣
                        ║  SYSTEM COMMANDS:                        ║
                        ║    ls        - List directory contents    ║
                        ║    cd <dir>  - Change directory           ║
                        ║    cat <f>   - Display file contents      ║
                        ║    grep <p>  - Search text pattern        ║
                        ║    find <p>  - Find files                 ║
                        ║    ping <h>  - Network ping               ║
                        ║    ps        - List processes             ║
                        ║    kill <pid>- Kill process               ║
                        ║    chmod <m> - Change file permissions    ║
                        ║    mkdir <d> - Create directory           ║
                        ║    rm <f>    - Remove file                ║
                        ║    cp <s> <d>- Copy file                  ║
                        ║    mv <s> <d>- Move file                  ║
                        ║    pwd       - Print working directory    ║
                        ║    echo <t>  - Print text                 ║
                        ║    whoami    - Current user               ║
                        ║    date      - Current date/time          ║
                        ║    uname     - System information         ║
                        ║    netstat   - Network connections        ║
                        ║    ifconfig  - Network interfaces         ║
                        ║    id        - User/group IDs             ║
                        ║    env       - Environment variables      ║
                        ║    df        - Disk free space            ║
                        ║    du        - Disk usage                 ║
                        ║    free      - Memory usage               ║
                        ║    top       - Running processes          ║
                        ║    clear     - Clear screen               ║
                        ║  TROXZY COMMANDS:                         ║
                        ║    troxzy-help   - Show troxzy commands   ║
                        ║    troxzy-scan   - Network scan           ║
                        ║    troxzy-osint  - OSINT info             ║
                        ║    troxzy-crack  - Hash cracking demo     ║
                        ║    troxzy-encrypt- Encryption demo        ║
                        ╚══════════════════════════════════════════╝
                    """.trimIndent()
                    helpText.lineSequence().forEach { addLine(activeSessionId, it, LineType.OUTPUT) }
                }
                trimmed == "troxzy-help" -> {
                    val troxzyHelp = """
                        [★] TROXZY CUSTOM COMMANDS:
                        [★] troxzy-scan    - Simulate network scan
                        [★] troxzy-osint   - Open Source Intelligence
                        [★] troxzy-crack   - Hash cracking demo
                        [★] troxzy-encrypt - Encryption demo
                    """.trimIndent()
                    troxzyHelp.lineSequence().forEach { addLine(activeSessionId, it, LineType.WARNING) }
                }
                trimmed == "troxzy-scan" -> {
                    addLine(activeSessionId, "[★] Starting network scan...", LineType.WARNING)
                    delay(500)
                    addLine(activeSessionId, "[★] Scanning subnet 192.168.1.0/24...", LineType.OUTPUT)
                    delay(400)
                    val hosts = listOf(
                        "192.168.1.1" to "Gateway (Router)",
                        "192.168.1.2" to "DNS Server",
                        "192.168.1.10" to "Android Device",
                        "192.168.1.15" to "Smart TV",
                        "192.168.1.20" to "NAS Storage",
                        "192.168.1.25" to "IoT Camera",
                        "192.168.1.30" to "Printer",
                        "192.168.1.100" to "Workstation",
                        "192.168.1.105" to "Laptop",
                        "192.168.1.110" to "Smartphone"
                    )
                    hosts.forEach { (ip, desc) ->
                        delay(200)
                        addLine(activeSessionId, "  [+] $ip - ${desc} - Port 80,443 OPEN", LineType.OUTPUT)
                    }
                    addLine(activeSessionId, "[★] Scan complete. ${hosts.size} hosts found.", LineType.WARNING)
                }
                trimmed == "troxzy-osint" -> {
                    addLine(activeSessionId, "[★] Gathering OSINT data...", LineType.WARNING)
                    delay(300)
                    addLine(activeSessionId, "  [*] Public IP: 103.xxx.xxx.42", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] ISP: Example Telecom", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Country: ID", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Region: Jakarta", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Coordinates: -6.2, 106.8", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] DNS: 8.8.8.8, 8.8.4.4", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Timezone: Asia/Jakarta", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] ASN: AS12345", LineType.OUTPUT)
                    addLine(activeSessionId, "[★] OSINT data collected.", LineType.WARNING)
                }
                trimmed == "troxzy-crack" -> {
                    addLine(activeSessionId, "[★] Hash Cracking Demo", LineType.WARNING)
                    delay(200)
                    addLine(activeSessionId, "  [*] Input: 5f4dcc3b5aa765d61d8327deb882cf99", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Type: MD5", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Starting dictionary attack...", LineType.OUTPUT)
                    delay(800)
                    addLine(activeSessionId, "  [*] Trying 1000 words...", LineType.OUTPUT)
                    delay(600)
                    addLine(activeSessionId, "  [*] Trying 5000 words...", LineType.OUTPUT)
                    delay(400)
                    addLine(activeSessionId, "  [+] CRACKED: 'password'", LineType.WARNING)
                    addLine(activeSessionId, "  [+] Time: 1.8s | Attempts: 5,234", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Try SHA-256: 5e884898da28...", LineType.OUTPUT)
                    delay(500)
                    addLine(activeSessionId, "  [+] CRACKED: 'secret'", LineType.WARNING)
                }
                trimmed == "troxzy-encrypt" -> {
                    addLine(activeSessionId, "[★] Encryption Demo", LineType.WARNING)
                    delay(200)
                    addLine(activeSessionId, "  [*] Plaintext: \"TroxzyXploit\"", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] AES-256-CBC:", LineType.OUTPUT)
                    addLine(activeSessionId, "    Encrypted: U2FsdGVkX1+xG3Q2Kz9mVpA7bN8cD4eF6hI0jKlMnOqRsTuVwXyZaB1CdEfGhIjK==", LineType.OUTPUT)
                    addLine(activeSessionId, "    Key: 256-bit | IV: 128-bit random", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] RSA-2048:", LineType.OUTPUT)
                    addLine(activeSessionId, "    Encrypted: [Base64 encoded block - 256 bytes]", LineType.OUTPUT)
                    addLine(activeSessionId, "  [*] Base64:", LineType.OUTPUT)
                    addLine(activeSessionId, "    Encrypted: VHJveHp5WHBsb2l0", LineType.OUTPUT)
                    addLine(activeSessionId, "  [+] Encryption demo complete.", LineType.WARNING)
                }
                trimmed == "whoami" -> {
                    addLine(activeSessionId, "u0_a${(100..120).random()}", LineType.OUTPUT)
                }
                trimmed == "pwd" -> {
                    val dir = activeSession.workingDir
                    addLine(activeSessionId, dir, LineType.OUTPUT)
                }
                trimmed == "date" -> {
                    addLine(activeSessionId, SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.getDefault()).format(Date()), LineType.OUTPUT)
                }
                trimmed == "uname" || trimmed == "uname -a" -> {
                    addLine(activeSessionId, "Linux localhost 5.10.0-android12-9-00001-g${(100000..999999).random()} #1 SMP PREEMPT", LineType.OUTPUT)
                }
                trimmed == "id" -> {
                    addLine(activeSessionId, "uid=10${(0..99).random()}(u0_a${(100..120).random()}) gid=10${(0..99).random()} groups=10${(0..99).random()},3003,9997", LineType.OUTPUT)
                }
                trimmed.startsWith("echo ") -> {
                    addLine(activeSessionId, trimmed.removePrefix("echo ").removeSurrounding("\""), LineType.OUTPUT)
                }
                trimmed == "ps" -> {
                    addLine(activeSessionId, "  PID  USER     STAT   RSS  %MEM  COMMAND", LineType.OUTPUT)
                    val procs = listOf(
                        "1    root     S      2048  0.1  init",
                        "245  system   S      4096  0.2  zygote",
                        "312  u0_a105  S      8192  0.4  com.troxzy.xploit",
                        "456  system   S      6144  0.3  system_server",
                        "501  u0_a108  S      12288 0.6  com.android.chrome",
                        "678  u0_a110  S      10240 0.5  com.google.android.gms",
                        "723  u0_a115  S      5120  0.2  com.android.systemui",
                        "890  root     S      3072  0.1  adbd",
                        "912  system   S      2048  0.1  vold",
                        "1024 u0_a120  S      15360 0.7  com.android.launcher3"
                    )
                    procs.forEach { addLine(activeSessionId, "  $it", LineType.OUTPUT) }
                }
                trimmed == "top" -> {
                    addLine(activeSessionId, "Tasks: ${85..120}.random() total, 2 running, ${80..110}.random() sleeping", LineType.OUTPUT)
                    addLine(activeSessionId, "Mem: 7680M total, 5120M used, 2560M free", LineType.OUTPUT)
                    addLine(activeSessionId, "CPU: ${15..45}.random()% user, ${5..15}.random()% system, ${40..75}.random()% idle", LineType.OUTPUT)
                    addLine(activeSessionId, "  PID  %CPU  %MEM  COMMAND", LineType.OUTPUT)
                    val topProcs = listOf(
                        "312  12.3  0.4  com.troxzy.xploit",
                        "501  8.7   0.6  com.android.chrome",
                        "456  5.2   0.3  system_server",
                        "678  3.1   0.5  com.google.android.gms",
                        "723  1.8   0.2  com.android.systemui"
                    )
                    topProcs.forEach { addLine(activeSessionId, "  $it", LineType.OUTPUT) }
                }
                trimmed == "free" -> {
                    addLine(activeSessionId, "              total     used     free    shared  buffers  cached", LineType.OUTPUT)
                    addLine(activeSessionId, "Mem:          7680     5120     2560      128      256     1024", LineType.OUTPUT)
                    addLine(activeSessionId, "Swap:         4096      512     3584", LineType.OUTPUT)
                }
                trimmed == "df" -> {
                    addLine(activeSessionId, "Filesystem      Size  Used  Avail Use% Mounted on", LineType.OUTPUT)
                    addLine(activeSessionId, "/dev/root       32G   18G   14G   56%  /", LineType.OUTPUT)
                    addLine(activeSessionId, "tmpfs           3.8G  256M  3.6G   7%  /tmp", LineType.OUTPUT)
                    addLine(activeSessionId, "/dev/block/8    52G   30G   22G   58%  /data", LineType.OUTPUT)
                }
                trimmed == "du" -> {
                    addLine(activeSessionId, "4.0K    /data/local/tmp", LineType.OUTPUT)
                    addLine(activeSessionId, "128M    /data/app/com.troxzy.xploit", LineType.OUTPUT)
                    addLine(activeSessionId, "2.4G    /data/data", LineType.OUTPUT)
                    addLine(activeSessionId, "5.1G    /data/media", LineType.OUTPUT)
                }
                trimmed == "netstat" -> {
                    addLine(activeSessionId, "Proto Recv-Q Send-Q Local Address        Foreign Address      State", LineType.OUTPUT)
                    addLine(activeSessionId, "tcp   0      0      0.0.0.0:443          0.0.0.0:*            LISTEN", LineType.OUTPUT)
                    addLine(activeSessionId, "tcp   0      0      192.168.1.10:52341   142.250.80.46:443    ESTABLISHED", LineType.OUTPUT)
                    addLine(activeSessionId, "tcp   0      0      192.168.1.10:48922   151.101.1.69:443     ESTABLISHED", LineType.OUTPUT)
                    addLine(activeSessionId, "udp   0      0      0.0.0.0:53           0.0.0.0:*            ", LineType.OUTPUT)
                }
                trimmed == "ifconfig" -> {
                    addLine(activeSessionId, "wlan0: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>", LineType.OUTPUT)
                    addLine(activeSessionId, "  inet 192.168.1.10 netmask 255.255.255.0 broadcast 192.168.1.255", LineType.OUTPUT)
                    addLine(activeSessionId, "  inet6 fe80::a1b2:c3d4:e5f6:7890 prefixlen 64", LineType.OUTPUT)
                    addLine(activeSessionId, "  ether AA:BB:CC:DD:EE:FF txqueuelen 1000", LineType.OUTPUT)
                    addLine(activeSessionId, "  RX packets 1234567 bytes 890123456", LineType.OUTPUT)
                    addLine(activeSessionId, "  TX packets 987654 bytes 567890123", LineType.OUTPUT)
                    addLine(activeSessionId, "", LineType.OUTPUT)
                    addLine(activeSessionId, "lo: flags=73<UP,LOOPBACK,RUNNING>", LineType.OUTPUT)
                    addLine(activeSessionId, "  inet 127.0.0.1 netmask 255.0.0.0", LineType.OUTPUT)
                }
                trimmed == "env" -> {
                    val envVars = listOf(
                        "ANDROID_ROOT=/system" to LineType.OUTPUT,
                        "ANDROID_DATA=/data" to LineType.OUTPUT,
                        "PATH=/system/bin:/system/xbin:/data/local/bin" to LineType.OUTPUT,
                        "HOME=/data/local/tmp" to LineType.OUTPUT,
                        "SHELL=/system/bin/sh" to LineType.OUTPUT,
                        "LANG=en_US.UTF-8" to LineType.OUTPUT,
                        "USER=u0_a105" to LineType.OUTPUT,
                        "HOSTNAME=localhost" to LineType.OUTPUT,
                        "TERM=xterm-256color" to LineType.OUTPUT,
                        "TROXZY_VERSION=1.0.0" to LineType.WARNING
                    )
                    envVars.forEach { (v, t) -> addLine(activeSessionId, v, t) }
                }
                trimmed.startsWith("ls") -> {
                    val dir = if (trimmed.length > 3) trimmed.removePrefix("ls ").trim() else activeSession.workingDir
                    addLine(activeSessionId, "  drwxr-xr-x  system system  4096  Jan 01 00:00  bin", LineType.OUTPUT)
                    addLine(activeSessionId, "  drwxr-xr-x  system system  4096  Jan 01 00:00  etc", LineType.OUTPUT)
                    addLine(activeSessionId, "  drwxr-xr-x  system system  4096  Jan 01 00:00  lib", LineType.OUTPUT)
                    addLine(activeSessionId, "  -rw-r--r--  system system  2048  Jan 01 00:00  build.prop", LineType.OUTPUT)
                    addLine(activeSessionId, "  -rwxr-xr-x  root   root    8192  Jan 01 00:00  troxzy", LineType.WARNING)
                    addLine(activeSessionId, "  -rw-r--r--  system system  1024  Jan 01 00:00  README.md", LineType.OUTPUT)
                }
                trimmed.startsWith("cd ") -> {
                    val targetDir = trimmed.removePrefix("cd ").trim()
                    sessions = sessions.map { session ->
                        if (session.id == activeSessionId) {
                            session.copy(workingDir = if (targetDir == "..") "/" else targetDir)
                        } else session
                    }
                    addLine(activeSessionId, "Changed directory to ${if (targetDir == "..") "/" else targetDir}", LineType.OUTPUT)
                }
                trimmed.startsWith("cat ") -> {
                    val file = trimmed.removePrefix("cat ").trim()
                    addLine(activeSessionId, "# $file", LineType.OUTPUT)
                    addLine(activeSessionId, "# TroxzyXploit Configuration", LineType.OUTPUT)
                    addLine(activeSessionId, "version=1.0.0", LineType.OUTPUT)
                    addLine(activeSessionId, "mode=stealth", LineType.OUTPUT)
                    addLine(activeSessionId, "theme=amoled", LineType.OUTPUT)
                }
                trimmed.startsWith("mkdir ") -> {
                    val dirName = trimmed.removePrefix("mkdir ").trim()
                    addLine(activeSessionId, "Created directory: $dirName", LineType.OUTPUT)
                }
                trimmed.startsWith("rm ") -> {
                    val fileName = trimmed.removePrefix("rm ").trim()
                    addLine(activeSessionId, "Removed: $fileName", LineType.OUTPUT)
                }
                trimmed.startsWith("chmod ") -> {
                    addLine(activeSessionId, "Permissions updated.", LineType.OUTPUT)
                }
                trimmed.startsWith("ping ") -> {
                    val host = trimmed.removePrefix("ping ").trim()
                    addLine(activeSessionId, "PING $host (${(100..200).random()}.${(100..200).random()}.${(1..254).random()}.${(1..254).random()}): 56 data bytes", LineType.OUTPUT)
                    repeat(4) { i ->
                        delay(400)
                        val time = (10..80).random().toDouble() / 10.0
                        addLine(activeSessionId, "64 bytes from $host: icmp_seq=$i ttl=64 time=${time}ms", LineType.OUTPUT)
                    }
                    addLine(activeSessionId, "--- $host ping statistics ---", LineType.OUTPUT)
                    addLine(activeSessionId, "4 packets transmitted, 4 received, 0% packet loss", LineType.OUTPUT)
                }
                trimmed.startsWith("grep ") -> {
                    addLine(activeSessionId, "Searching for pattern in current directory...", LineType.OUTPUT)
                    addLine(activeSessionId, "  ./config/troxzy.conf:3:mode=stealth", LineType.OUTPUT)
                    addLine(activeSessionId, "  ./logs/scan.log:15:stealth mode activated", LineType.OUTPUT)
                }
                trimmed.startsWith("find ") -> {
                    addLine(activeSessionId, "Searching...", LineType.OUTPUT)
                    addLine(activeSessionId, "  /data/local/tmp/troxzy", LineType.OUTPUT)
                    addLine(activeSessionId, "  /data/local/tmp/troxzy/config", LineType.OUTPUT)
                    addLine(activeSessionId, "  /data/local/tmp/troxzy/logs", LineType.OUTPUT)
                }
                trimmed.startsWith("kill ") -> {
                    val pid = trimmed.removePrefix("kill ").trim()
                    addLine(activeSessionId, "Process $pid terminated.", LineType.WARNING)
                }
                trimmed.startsWith("cp ") -> {
                    addLine(activeSessionId, "File copied successfully.", LineType.OUTPUT)
                }
                trimmed.startsWith("mv ") -> {
                    addLine(activeSessionId, "File moved successfully.", LineType.OUTPUT)
                }
                else -> {
                    // Try to execute the command via Runtime
                    try {
                        withContext(Dispatchers.IO) {
                            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", trimmed))
                            val reader = BufferedReader(InputStreamReader(process.inputStream))
                            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                addLine(activeSessionId, line!!, LineType.OUTPUT)
                            }
                            while (errorReader.readLine().also { line = it } != null) {
                                addLine(activeSessionId, line!!, LineType.ERROR)
                            }
                            val exitCode = process.waitFor()
                            if (exitCode != 0) {
                                addLine(activeSessionId, "Exit code: $exitCode", LineType.ERROR)
                            }
                        }
                    } catch (e: Exception) {
                        addLine(activeSessionId, "Error: ${e.message}", LineType.ERROR)
                    }
                }
            }
        }
    }

    // Auto-scroll to bottom
    LaunchedEffect(activeSession.lines.size) {
        if (activeSession.lines.isNotEmpty()) {
            listState.animateScrollToItem(activeSession.lines.size - 1)
        }
    }

    // Welcome message on first session
    LaunchedEffect(Unit) {
        if (activeSession.lines.isEmpty()) {
            addLine(activeSessionId, "╔═══════════════════════════════════════════╗", LineType.SYSTEM)
            addLine(activeSessionId, "║     TROXZY XPLOIT TERMINAL v1.0.0        ║", LineType.SYSTEM)
            addLine(activeSessionId, "║     by Troxzy | t.me/SoloBanNoTrash      ║", LineType.SYSTEM)
            addLine(activeSessionId, "╚═══════════════════════════════════════════╝", LineType.SYSTEM)
            addLine(activeSessionId, "Type 'help' for available commands.", LineType.WARNING)
        }
    }

    val lineColor = when (terminalTheme) {
        TerminalTheme.GREEN_ON_BLACK -> NeonGreen
        TerminalTheme.AMBER_ON_BLACK -> Color(0xFFFFBF00)
        TerminalTheme.CYAN_ON_BLACK -> NeonCyan
        TerminalTheme.CUSTOM -> NeonPurple
    }

    CommonScaffold(
        title = "Terminal",
        currentRoute = "terminal",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AMOLEDBlack)
                .padding(paddingValues)
        ) {
            // Session Tab Bar
            SessionTabBar(
                sessions = sessions,
                activeSessionId = activeSessionId,
                onSessionSelect = { activeSessionId = it },
                onAddSession = {
                    val newId = (sessions.maxOfOrNull { it.id } ?: 0) + 1
                    sessions = sessions + TerminalSession(newId, "Session $newId", emptyList(), emptyList(), "/")
                    activeSessionId = newId
                },
                onRemoveSession = { id ->
                    if (sessions.size > 1) {
                        sessions = sessions.filter { it.id != id }
                        if (activeSessionId == id) {
                            activeSessionId = sessions.first().id
                        }
                    }
                },
                accentColor = lineColor
            )

            // Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme",
                        tint = lineColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        sessions = sessions.map { session ->
                            if (session.id == activeSessionId) session.copy(lines = emptyList())
                            else session
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear",
                        tint = lineColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val allText = activeSession.lines.joinToString("\n") { it.text }
                        clipboardManager.setText(AnnotatedString(allText))
                        copyConfirmation = "All output copied!"
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy All",
                        tint = lineColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export",
                        tint = lineColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (copyConfirmation != null) {
                    Text(
                        text = copyConfirmation!!,
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontFamily = MonoFont
                    )
                    LaunchedEffect(copyConfirmation) {
                        delay(2000)
                        copyConfirmation = null
                    }
                }
            }

            // Terminal Output Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(AMOLEDBlack)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(activeSession.lines) { line ->
                        val lineTextColor = when (line.type) {
                            LineType.OUTPUT -> lineColor
                            LineType.ERROR -> Color(0xFFFF4444)
                            LineType.COMMAND -> NeonCyan
                            LineType.WARNING -> Color(0xFFFFFF00)
                            LineType.SYSTEM -> NeonPurple
                        }
                        val annotatedLine = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color.Gray, fontSize = (fontSize - 2).sp)) {
                                append("[${line.timestamp}] ")
                            }
                            withStyle(SpanStyle(color = lineTextColor, fontSize = fontSize.sp)) {
                                append(line.text)
                            }
                        }
                        Text(
                            text = annotatedLine,
                            fontFamily = MonoFont,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(line.text))
                                    copyConfirmation = "Line copied!"
                                },
                            softWrap = true
                        )
                    }
                }
            }

            // Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "troxzy@xploit:~$ ",
                    color = lineColor,
                    fontFamily = MonoFont,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold
                )
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = MonoFont,
                        fontSize = fontSize.sp
                    ),
                    placeholder = {
                        Text(
                            "Type command...",
                            color = Color.Gray,
                            fontFamily = MonoFont,
                            fontSize = fontSize.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.text.isNotBlank()) {
                                executeCommand(inputText.text)
                                inputText = TextFieldValue("")
                            }
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = lineColor,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                IconButton(
                    onClick = {
                        if (inputText.text.isNotBlank()) {
                            executeCommand(inputText.text)
                            inputText = TextFieldValue("")
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = lineColor
                    )
                }
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = DarkSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Terminal Settings",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MonoFont
                )

                // Theme Selector
                Text(
                    text = "Color Theme",
                    color = lineColor,
                    fontSize = 14.sp,
                    fontFamily = MonoFont
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TerminalTheme.entries.forEach { theme ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { terminalTheme = theme },
                            color = if (terminalTheme == theme) theme.outputColor.copy(alpha = 0.2f) else DarkCard,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(theme.outputColor)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = theme.label,
                                    color = if (terminalTheme == theme) theme.outputColor else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = MonoFont
                                )
                            }
                        }
                    }
                }

                // Font Size Slider
                Text(
                    text = "Font Size: ${fontSize.toInt()}sp",
                    color = lineColor,
                    fontSize = 14.sp,
                    fontFamily = MonoFont
                )
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 12f..24f,
                    colors = SliderDefaults.colors(
                        thumbColor = lineColor,
                        activeTrackColor = lineColor
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Output", color = Color.White) },
            text = {
                Text(
                    "Export all terminal output as text? This will copy the entire session log to clipboard.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val allText = activeSession.lines.joinToString("\n") { "[${it.timestamp}] ${it.text}" }
                        clipboardManager.setText(AnnotatedString(allText))
                        copyConfirmation = "Session exported to clipboard!"
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text("Export", color = Color.White)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCard)
                ) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun SessionTabBar(
    sessions: List<TerminalSession>,
    activeSessionId: Int,
    onSessionSelect: (Int) -> Unit,
    onAddSession: () -> Unit,
    onRemoveSession: (Int) -> Unit,
    accentColor: Color
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard)
            .horizontalScroll(scrollState)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        sessions.forEach { session ->
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSessionSelect(session.id) },
                color = if (session.id == activeSessionId) accentColor.copy(alpha = 0.2f) else DarkSurface,
                shape = RoundedCornerShape(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.name,
                        color = if (session.id == activeSessionId) accentColor else Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = MonoFont
                    )
                    if (sessions.size > 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close session",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onRemoveSession(session.id) }
                        )
                    }
                }
            }
        }
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onAddSession() },
            color = DarkSurface,
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New session",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "New",
                    color = accentColor,
                    fontSize = 12.sp,
                    fontFamily = MonoFont
                )
            }
        }
    }
}
