package com.troxzy.xploit.ui.screens.osint

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.exifinterface.media.ExifInterface
import com.troxzy.xploit.ui.components.CommonScaffold
import com.troxzy.xploit.ui.components.GlitchText
import com.troxzy.xploit.ui.components.NeonCard
import com.troxzy.xploit.ui.theme.*
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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
enum class ImageSource { Camera, Gallery, Url }

enum class SearchEngine(val displayName: String, val urlTemplate: String) {
    Google("Google Images", "https://images.google.com/searchbyimage?image_url="),
    Yandex("Yandex", "https://yandex.com/images/search?rpt=imageview&url="),
    TinEye("TinEye", "https://tineye.com/search/?url="),
    Bing("Bing", "https://www.bing.com/images/search?q=imgurl=")
}

data class ExifData(
    val cameraModel: String?,
    val gpsLatitude: String?,
    val gpsLongitude: String?,
    val gpsAltitude: String?,
    val dateTime: String?,
    val software: String?,
    val imageWidth: String?,
    val imageHeight: String?,
    val iso: String?,
    val exposureTime: String?,
    val fNumber: String?,
    val focalLength: String?,
    val flash: String?,
    val whiteBalance: String?,
    val orientation: String?,
    val mimeType: String?,
    val fileSize: String?
)

data class ImageSearchHistoryEntry(
    val source: String,
    val timestamp: Long,
    val engine: String
)

// ── EXIF Extraction ───────────────────────────────────────────────────────────
private fun extractExifData(context: Context, uri: Uri): ExifData {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) return ExifData(
            cameraModel = null, gpsLatitude = null, gpsLongitude = null,
            gpsAltitude = null, dateTime = null, software = null,
            imageWidth = null, imageHeight = null, iso = null,
            exposureTime = null, fNumber = null, focalLength = null,
            flash = null, whiteBalance = null, orientation = null,
            mimeType = null, fileSize = null
        )

        val exif = ExifInterface(inputStream)
        val latLong = exif.latLong

        inputStream.close()

        ExifData(
            cameraModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: exif.getAttribute(ExifInterface.TAG_MAKE),
            gpsLatitude = latLong?.let { String.format("%.6f", it[0]) },
            gpsLongitude = latLong?.let { String.format("%.6f", it[1]) },
            gpsAltitude = exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE),
            dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
            software = exif.getAttribute(ExifInterface.TAG_SOFTWARE),
            imageWidth = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH),
            imageHeight = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH),
            iso = exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS),
            exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER),
            focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
            flash = exif.getAttribute(ExifInterface.TAG_FLASH),
            whiteBalance = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE),
            orientation = exif.getAttribute(ExifInterface.TAG_ORIENTATION),
            mimeType = exif.getAttribute(ExifInterface.TAG_MIME_TYPE),
            fileSize = exif.getAttribute(ExifInterface.TAG_FILE_SIZE)
        )
    } catch (e: Exception) {
        ExifData(
            cameraModel = null, gpsLatitude = null, gpsLongitude = null,
            gpsAltitude = null, dateTime = null, software = null,
            imageWidth = null, imageHeight = null, iso = null,
            exposureTime = null, fNumber = null, focalLength = null,
            flash = null, whiteBalance = null, orientation = null,
            mimeType = null, fileSize = null
        )
    }
}

// ── Main Composable ───────────────────────────────────────────────────────────
@Composable
fun ImageReverseSearchScreen(
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var selectedSource by remember { mutableStateOf<ImageSource?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    var selectedEngine by remember { mutableStateOf(SearchEngine.Google) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var exifData by remember { mutableStateOf<ExifData?>(null) }
    var isExtractingExif by remember { mutableStateOf(false) }
    var showExifSection by remember { mutableStateOf(false) }
    var searchHistory by remember { mutableStateOf(loadImageSearchHistory(context)) }
    var showHistory by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            // Extract EXIF from captured image
            scope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) { isExtractingExif = true }
                val exif = extractExifData(context, imageUri!!)
                withContext(Dispatchers.Main) {
                    exifData = exif
                    showExifSection = true
                    isExtractingExif = false
                }
            }
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            scope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) { isExtractingExif = true }
                val exif = extractExifData(context, it)
                withContext(Dispatchers.Main) {
                    exifData = exif
                    showExifSection = true
                    isExtractingExif = false
                }
            }
        }
    }

    fun performSearch() {
        when (selectedSource) {
            ImageSource.Url -> {
                if (imageUrl.isBlank()) {
                    errorMessage = "Please enter an image URL"
                    return
                }
                val searchUrl = selectedEngine.urlTemplate + Uri.encode(imageUrl)
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
                    // Save to history
                    val entry = ImageSearchHistoryEntry(
                        source = imageUrl.take(50),
                        timestamp = System.currentTimeMillis(),
                        engine = selectedEngine.displayName
                    )
                    searchHistory = (listOf(entry) + searchHistory).take(20)
                    saveImageSearchHistory(context, searchHistory)
                    errorMessage = null
                } catch (e: Exception) {
                    errorMessage = "Cannot open browser: ${e.message}"
                }
            }
            ImageSource.Gallery, ImageSource.Camera -> {
                if (imageUri == null) {
                    errorMessage = "Please select or capture an image first"
                    return
                }
                // For local images, we can only search with Google Lens or upload
                // Try to open Google Lens with the image
                try {
                    val lensIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, imageUri)
                        setPackage("com.google.android.googlequicksearchbox")
                    }
                    context.startActivity(lensIntent)
                    errorMessage = null
                } catch (e: Exception) {
                    // Fallback: open the search engine directly
                    try {
                        val searchUrl = "${selectedEngine.urlTemplate}upload"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
                        errorMessage = null
                    } catch (e2: Exception) {
                        errorMessage = "Cannot open search: ${e2.message}"
                    }
                }
            }
            null -> {
                errorMessage = "Please select an image source"
            }
        }
    }

    fun launchCamera() {
        selectedSource = ImageSource.Camera
        try {
            val photoFile = File(context.cacheDir, "troxzy_photo_${System.currentTimeMillis()}.jpg")
            val uri = Uri.fromFile(photoFile)
            imageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            errorMessage = "Cannot launch camera: ${e.message}"
        }
    }

    fun launchGallery() {
        selectedSource = ImageSource.Gallery
        try {
            galleryLauncher.launch("image/*")
        } catch (e: Exception) {
            errorMessage = "Cannot launch gallery: ${e.message}"
        }
    }

    CommonScaffold(
        title = "Image Reverse Search",
        currentRoute = "osint_image_search",
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
                text = "IMAGE OSINT",
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
            )

            // ── Image Source Selector ──────────────────────────────────────
            Text("Image Source", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Camera
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (selectedSource == ImageSource.Camera) 1.5.dp else 0.5.dp,
                            color = if (selectedSource == ImageSource.Camera) NeonPurple else DarkSurface,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { launchCamera() },
                    color = if (selectedSource == ImageSource.Camera) NeonPurple.copy(alpha = 0.15f) else DarkCard,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = if (selectedSource == ImageSource.Camera) NeonPurple else TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Camera", color = if (selectedSource == ImageSource.Camera) NeonPurple else TextSecondary, fontSize = 12.sp)
                    }
                }
                // Gallery
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (selectedSource == ImageSource.Gallery) 1.5.dp else 0.5.dp,
                            color = if (selectedSource == ImageSource.Gallery) NeonGreen else DarkSurface,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { launchGallery() },
                    color = if (selectedSource == ImageSource.Gallery) NeonGreen.copy(alpha = 0.15f) else DarkCard,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = if (selectedSource == ImageSource.Gallery) NeonGreen else TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Gallery", color = if (selectedSource == ImageSource.Gallery) NeonGreen else TextSecondary, fontSize = 12.sp)
                    }
                }
                // URL
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (selectedSource == ImageSource.Url) 1.5.dp else 0.5.dp,
                            color = if (selectedSource == ImageSource.Url) NeonCyan else DarkSurface,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedSource = ImageSource.Url },
                    color = if (selectedSource == ImageSource.Url) NeonCyan.copy(alpha = 0.15f) else DarkCard,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = if (selectedSource == ImageSource.Url) NeonCyan else TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("URL", color = if (selectedSource == ImageSource.Url) NeonCyan else TextSecondary, fontSize = 12.sp)
                    }
                }
            }

            // ── Image Preview Thumbnail ────────────────────────────────────
            if (imageUri != null || imageUrl.isNotBlank()) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .border(1.dp, NeonPurple, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = "Image preview",
                                tint = NeonPurple,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Image Selected",
                                color = NeonGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                when (selectedSource) {
                                    ImageSource.Camera -> "Source: Camera capture"
                                    ImageSource.Gallery -> "Source: Gallery"
                                    ImageSource.Url -> "Source: URL"
                                    null -> ""
                                },
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            if (selectedSource == ImageSource.Url) {
                                Text(
                                    imageUrl.take(40) + if (imageUrl.length > 40) "..." else "",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // ── URL Input (shown when URL source selected) ─────────────────
            AnimatedVisibility(visible = selectedSource == ImageSource.Url, enter = fadeIn(), exit = fadeOut()) {
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it; errorMessage = null },
                    label = { Text("Image URL", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurface,
                        cursorColor = NeonCyan
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { performSearch() }),
                    leadingIcon = {
                        Icon(Icons.Default.Link, contentDescription = null, tint = NeonCyan)
                    }
                )
            }

            // ── Search Engine Selector ─────────────────────────────────────
            Text("Search Engine", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(SearchEngine.entries) { engine ->
                    val isSelected = selectedEngine == engine
                    val engineColor = when (engine) {
                        SearchEngine.Google -> Color(0xFF4285F4)
                        SearchEngine.Yandex -> Color(0xFFFF0000)
                        SearchEngine.TinEye -> Color(0xFF6C3)
                        SearchEngine.Bing -> Color(0xFF008373)
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) engineColor else DarkSurface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedEngine = engine },
                        color = if (isSelected) engineColor.copy(alpha = 0.15f) else DarkCard,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            engine.displayName,
                            color = if (isSelected) engineColor else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // ── Search Button ──────────────────────────────────────────────
            Button(
                onClick = { performSearch() },
                enabled = (selectedSource != null) && (selectedSource == ImageSource.Url && imageUrl.isNotBlank() || selectedSource != ImageSource.Url && imageUri != null),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Search with ${selectedEngine.displayName}", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // ── Action Buttons ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showExifSection = !showExifSection
                        imageUri?.let { uri ->
                            if (exifData == null && !isExtractingExif) {
                                scope.launch(Dispatchers.IO) {
                                    withContext(Dispatchers.Main) { isExtractingExif = true }
                                    val exif = extractExifData(context, uri)
                                    withContext(Dispatchers.Main) {
                                        exifData = exif
                                        isExtractingExif = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = imageUri != null,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.DataObject, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EXIF Data", fontSize = 11.sp)
                }
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
            }

            // ── Error Message ──────────────────────────────────────────────
            errorMessage?.let { error ->
                Text(error, color = NeonRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }

            // ── History Section ────────────────────────────────────────────
            AnimatedVisibility(visible = showHistory, enter = fadeIn(), exit = fadeOut()) {
                NeonCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Search History", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (searchHistory.isEmpty()) {
                            Text("No history yet", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .padding(top = 4.dp)
                            ) {
                                items(searchHistory) { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(entry.source, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(" • ${entry.engine}", color = TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── EXIF Data Section ──────────────────────────────────────────
            AnimatedVisibility(visible = showExifSection, enter = fadeIn(), exit = fadeOut()) {
                if (isExtractingExif) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Extracting EXIF data...", color = NeonCyan, fontSize = 13.sp)
                    }
                } else {
                    exifData?.let { exif ->
                        NeonCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DataObject, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("EXIF Metadata", color = NeonCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                val exifFields = listOf(
                                    "Camera Model" to exif.cameraModel,
                                    "GPS Latitude" to exif.gpsLatitude,
                                    "GPS Longitude" to exif.gpsLongitude,
                                    "GPS Altitude" to exif.gpsAltitude,
                                    "Date/Time" to exif.dateTime,
                                    "Software" to exif.software,
                                    "Image Width" to exif.imageWidth,
                                    "Image Height" to exif.imageHeight,
                                    "ISO" to exif.iso,
                                    "Exposure Time" to exif.exposureTime,
                                    "F-Number" to exif.fNumber,
                                    "Focal Length" to exif.focalLength,
                                    "Flash" to exif.flash,
                                    "White Balance" to exif.whiteBalance,
                                    "Orientation" to exif.orientation,
                                    "MIME Type" to exif.mimeType,
                                    "File Size" to exif.fileSize
                                )

                                var hasAnyData = false
                                exifFields.forEach { (label, value) ->
                                    if (value != null) {
                                        hasAnyData = true
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(label, color = TextSecondary, fontSize = 12.sp)
                                            Text(
                                                value,
                                                color = when {
                                                    label.startsWith("GPS") -> NeonGreen
                                                    label == "Camera Model" -> NeonPurple
                                                    label == "Date/Time" -> NeonCyan
                                                    else -> Color.White
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            )
                                        }
                                    }
                                }

                                if (!hasAnyData) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(DarkSurface)
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("No EXIF data found in this image", color = NeonYellow, fontSize = 12.sp)
                                    }
                                }

                                // GPS coordinates warning
                                if (exif.gpsLatitude != null || exif.gpsLongitude != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(NeonRed.copy(alpha = 0.1f))
                                            .border(1.dp, NeonRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonRed, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("GPS data detected!", color = NeonRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("This image contains geolocation metadata", color = TextSecondary, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Results Section ────────────────────────────────────────────
            Spacer(modifier = Modifier.height(8.dp))
            Text("Search Results", color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Search engine result links
                items(SearchEngine.entries) { engine ->
                    val engineColor = when (engine) {
                        SearchEngine.Google -> Color(0xFF4285F4)
                        SearchEngine.Yandex -> Color(0xFFFF0000)
                        SearchEngine.TinEye -> Color(0xFF6C3)
                        SearchEngine.Bing -> Color(0xFF008373)
                    }
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val url = if (selectedSource == ImageSource.Url && imageUrl.isNotBlank()) {
                                        engine.urlTemplate + Uri.encode(imageUrl)
                                    } else {
                                        engine.urlTemplate
                                    }
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(engineColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = engineColor, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(engine.displayName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Search for similar images on ${engine.displayName}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                            Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = engineColor, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Source pages info
                item {
                    NeonCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NeonOrange, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Search Tips", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            val tips = listOf(
                                "Google Images - Best for finding similar images and source pages",
                                "Yandex - Best for face recognition and similar faces",
                                "TinEye - Best for finding exact copies and modified versions",
                                "Bing - Good for general reverse image search"
                            )
                            tips.forEach { tip ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(NeonOrange)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(tip, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Persistence Helpers ────────────────────────────────────────────────────────
private fun loadImageSearchHistory(context: Context): List<ImageSearchHistoryEntry> {
    return try {
        val file = File(context.filesDir, "image_search_history.txt")
        if (!file.exists()) return emptyList()
        file.readLines().mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size == 3) {
                ImageSearchHistoryEntry(
                    source = parts[0],
                    timestamp = parts[1].toLongOrNull() ?: 0L,
                    engine = parts[2]
                )
            } else null
        }
    } catch (e: Exception) { emptyList() }
}

private fun saveImageSearchHistory(context: Context, history: List<ImageSearchHistoryEntry>) {
    try {
        val file = File(context.filesDir, "image_search_history.txt")
        file.writeText(history.joinToString("\n") { "${it.source}|${it.timestamp}|${it.engine}" })
    } catch (_: Exception) { }
}
