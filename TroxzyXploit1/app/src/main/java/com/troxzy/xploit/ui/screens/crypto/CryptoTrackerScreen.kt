package com.troxzy.xploit.ui.screens.crypto

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.text.NumberFormat
import java.util.Locale

@Serializable
private data class CoinGeckoMarket(
    val id: String = "",
    val symbol: String = "",
    val name: String = "",
    val current_price: Double = 0.0,
    val price_change_percentage_24h: Double = 0.0,
    val market_cap: Double = 0.0,
    val total_volume: Double = 0.0,
    val sparkline_in_7d: SparklineData? = null,
    val image: String = "",
    val high_24h: Double = 0.0,
    val low_24h: Double = 0.0,
    val circulating_supply: Double = 0.0,
    val total_supply: Double? = null
)

@Serializable
private data class SparklineData(
    val price: List<Double> = emptyList()
)

private data class CoinDisplay(
    val id: String,
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val change24h: Double,
    val marketCap: Double,
    val volume: Double,
    val sparkline: List<Double>,
    val high24h: Double,
    val low24h: Double,
    val circulatingSupply: Double,
    val totalSupply: Double?
)

private data class PortfolioHolding(
    val coinId: String,
    val symbol: String,
    val name: String,
    val amount: Double,
    val buyPrice: Double
)

private data class PriceAlert(
    val coinId: String,
    val symbol: String,
    val targetPrice: Double,
    val isAbove: Boolean
)

private data class GasInfo(
    val network: String,
    val slow: String,
    val standard: String,
    val fast: String
)

private enum class Currency(val code: String, val symbol: String) {
    USD("usd", "$"), IDR("idr", "Rp"), EUR("eur", "€"), BTC("btc", "₿")
}

private enum class ChartRange(val label: String, val days: String) {
    ONE_HOUR("1h", "1"), ONE_DAY("24h", "1"), SEVEN_DAYS("7d", "7"),
    THIRTY_DAYS("30d", "30"), ONE_YEAR("1y", "365")
}

private enum class RefreshInterval(val label: String, val seconds: Long) {
    SEC_15("15s", 15), SEC_30("30s", 30), MIN_1("1m", 60), MIN_5("5m", 300)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CryptoTrackerScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val json = remember { Json { ignoreUnknownKeys = true } }

    var selectedCurrency by remember { mutableStateOf(Currency.USD) }
    var coins by remember { mutableStateOf<List<CoinDisplay>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCoin by remember { mutableStateOf<CoinDisplay?>(null) }
    var refreshInterval by remember { mutableStateOf(RefreshInterval.MIN_1) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Portfolio
    val portfolioHoldings = remember { mutableStateListOf<PortfolioHolding>() }
    var showAddHoldingSheet by remember { mutableStateOf(false) }
    var showAddAlertSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Price alerts
    val priceAlerts = remember { mutableStateListOf<PriceAlert>() }

    // Gas info
    val gasInfo = remember {
        mutableStateListOf(
            GasInfo("Ethereum", "12 Gwei", "18 Gwei", "25 Gwei"),
            GasInfo("BSC", "3 Gwei", "5 Gwei", "7 Gwei"),
            GasInfo("Polygon", "30 Gwei", "50 Gwei", "80 Gwei")
        )
    }

    // Chart data for detail view
    var chartData by remember { mutableStateOf<List<Double>>(emptyList()) }
    var selectedChartRange by remember { mutableStateOf(ChartRange.SEVEN_DAYS) }

    fun formatPrice(price: Double, currency: Currency = selectedCurrency): String {
        return when (currency) {
            Currency.USD -> NumberFormat.getCurrencyInstance(Locale.US).format(price)
            Currency.IDR -> NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(price)
            Currency.EUR -> NumberFormat.getCurrencyInstance(Locale.GERMANY).format(price)
            Currency.BTC -> String.format("%.8f BTC", price)
        }
    }

    fun formatLargeNumber(num: Double): String {
        return when {
            num >= 1e12 -> String.format("%.2fT", num / 1e12)
            num >= 1e9 -> String.format("%.2fB", num / 1e9)
            num >= 1e6 -> String.format("%.2fM", num / 1e6)
            num >= 1e3 -> String.format("%.2fK", num / 1e3)
            else -> String.format("%.2f", num)
        }
    }

    suspend fun fetchCoins() {
        withContext(Dispatchers.IO) {
            try {
                isRefreshing = true
                val url = "https://api.coingecko.com/api/v3/coins/markets?vs_currency=${selectedCurrency.code}&order=market_cap_desc&per_page=100&page=1&sparkline=true"
                val response = URL(url).readText()
                val marketData = json.decodeFromString<List<CoinGeckoMarket>>(response)
                coins = marketData.map { m ->
                    CoinDisplay(
                        id = m.id,
                        symbol = m.symbol.uppercase(),
                        name = m.name,
                        currentPrice = m.current_price,
                        change24h = m.price_change_percentage_24h,
                        marketCap = m.market_cap,
                        volume = m.total_volume,
                        sparkline = m.sparkline_in_7d?.price ?: emptyList(),
                        high24h = m.high_24h,
                        low24h = m.low_24h,
                        circulatingSupply = m.circulating_supply,
                        totalSupply = m.total_supply
                    )
                }
                errorMessage = null
            } catch (e: Exception) {
                // If API fails, show demo data
                if (coins.isEmpty()) {
                    coins = generateDemoCoins()
                    errorMessage = "Using demo data (API unavailable)"
                }
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    suspend fun fetchChartData(coinId: String, range: ChartRange) {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.coingecko.com/api/v3/coins/$coinId/market_chart?vs_currency=${selectedCurrency.code}&days=${range.days}"
                val response = URL(url).readText()
                val jsonElement = json.parseToJsonElement(response)
                val pricesArray = jsonElement.jsonObject["prices"]?.jsonArray
                chartData = pricesArray?.map { it.jsonArray[1].jsonPrimitive.double } ?: emptyList()
            } catch (e: Exception) {
                // Generate demo chart data
                chartData = generateDemoChartData(selectedCoin?.currentPrice ?: 50000.0)
            }
        }
    }

    // Initial load
    LaunchedEffect(selectedCurrency) {
        isLoading = true
        fetchCoins()
    }

    // Auto-refresh
    LaunchedEffect(refreshInterval) {
        while (true) {
            delay(refreshInterval.seconds * 1000L)
            fetchCoins()
        }
    }

    // Load chart data when coin selected
    LaunchedEffect(selectedCoin, selectedChartRange) {
        selectedCoin?.let { coin ->
            fetchChartData(coin.id, selectedChartRange)
        }
    }

    // Detail view
    if (selectedCoin != null) {
        CoinDetailView(
            coin = selectedCoin!!,
            chartData = chartData,
            selectedChartRange = selectedChartRange,
            onChartRangeChange = { selectedChartRange = it },
            onBack = { selectedCoin = null },
            onAddToPortfolio = {
                showAddHoldingSheet = true
            },
            formatPrice = ::formatPrice,
            formatLargeNumber = ::formatLargeNumber,
            selectedCurrency = selectedCurrency
        )
        return
    }

    CommonScaffold(
        title = "Crypto Tracker",
        currentRoute = "crypto",
        onNavigate = onNavigate,
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AMOLEDBlack)
                .padding(paddingValues)
        ) {
            // Currency selector and refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                var currencyExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { currencyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${selectedCurrency.symbol} ${selectedCurrency.name}",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color.Gray,
                            focusedContainerColor = DarkCard,
                            unfocusedContainerColor = DarkCard
                        ),
                        textStyle = TextStyle(fontSize = 14.sp)
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false },
                        containerColor = DarkSurface
                    ) {
                        Currency.entries.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text("${currency.symbol} ${currency.name}", color = Color.White) },
                                onClick = {
                                    selectedCurrency = currency
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = NeonPurple, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { showAddAlertSheet = true }) {
                        Icon(Icons.Default.Notifications, "Alerts", tint = NeonCyan, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search coins...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    )
                )
            }

            // Tabs
            val tabs = listOf("Market", "Portfolio", "Gas Fees")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkCard,
                contentColor = NeonPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonPurple
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                color = if (selectedTab == index) NeonPurple else Color.Gray,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Market tab
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonPurple)
                        }
                    } else {
                        val filteredCoins = coins.filter { coin ->
                            searchQuery.isBlank() ||
                                coin.name.contains(searchQuery, ignoreCase = true) ||
                                coin.symbol.contains(searchQuery, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (errorMessage != null) {
                                item {
                                    Text(
                                        text = errorMessage!!,
                                        color = Color(0xFFFFFF00),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }
                            }
                            items(filteredCoins) { coin ->
                                CoinListItem(
                                    coin = coin,
                                    onClick = { selectedCoin = coin },
                                    formatPrice = ::formatPrice,
                                    formatLargeNumber = ::formatLargeNumber
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // Portfolio tab
                    PortfolioTab(
                        holdings = portfolioHoldings,
                        coins = coins,
                        onAddHolding = { showAddHoldingSheet = true },
                        onRemoveHolding = { portfolioHoldings.remove(it) },
                        formatPrice = ::formatPrice,
                        formatLargeNumber = ::formatLargeNumber,
                        selectedCurrency = selectedCurrency
                    )
                }
                2 -> {
                    // Gas Fees tab
                    GasFeesTab(gasInfo = gasInfo)
                }
            }
        }
    }

    // Add Holding Sheet
    if (showAddHoldingSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddHoldingSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = DarkSurface
        ) {
            AddHoldingForm(
                coins = coins,
                onAdd = { holding ->
                    portfolioHoldings.add(holding)
                    showAddHoldingSheet = false
                },
                onDismiss = { showAddHoldingSheet = false }
            )
        }
    }

    // Price Alert Sheet
    if (showAddAlertSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddAlertSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = DarkSurface
        ) {
            AddAlertForm(
                coins = coins,
                onAdd = { alert ->
                    priceAlerts.add(alert)
                    showAddAlertSheet = false
                },
                onDismiss = { showAddAlertSheet = false }
            )
        }
    }

    // Settings Sheet
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Settings", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("Refresh Interval", color = NeonCyan, fontSize = 14.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RefreshInterval.entries.forEach { interval ->
                        OutlinedButton(
                            onClick = { refreshInterval = interval },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (refreshInterval == interval) NeonPurple.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (refreshInterval == interval) NeonPurple else Color.Gray
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = refreshInterval == interval)
                        ) {
                            Text(interval.label, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun CoinListItem(
    coin: CoinDisplay,
    onClick: () -> Unit,
    formatPrice: (Double, Currency) -> String,
    formatLargeNumber: (Double) -> String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = DarkCard,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = coin.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = coin.symbol,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MCap: ${formatLargeNumber(coin.marketCap)}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Vol: ${formatLargeNumber(coin.volume)}",
                        color = Color.Gray,
                        fontSize = 10.sp
                    )
                }
            }
            // Sparkline
            if (coin.sparkline.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .width(60.dp)
                        .height(30.dp)
                ) {
                    val prices = coin.sparkline
                    val min = prices.minOrNull() ?: 0.0
                    val max = prices.maxOrNull() ?: 1.0
                    val range = max - min
                    val path = Path()
                    prices.forEachIndexed { index, price ->
                        val x = (index.toFloat() / (prices.size - 1)) * size.width
                        val y = size.height - ((price - min) / range).toFloat() * size.height
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path = path,
                        color = if (coin.change24h >= 0) NeonGreen else Color.Red,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(coin.currentPrice, Currency.USD),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%.2f%%", coin.change24h),
                    color = if (coin.change24h >= 0) NeonGreen else Color.Red,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CoinDetailView(
    coin: CoinDisplay,
    chartData: List<Double>,
    selectedChartRange: ChartRange,
    onChartRangeChange: (ChartRange) -> Unit,
    onBack: () -> Unit,
    onAddToPortfolio: () -> Unit,
    formatPrice: (Double, Currency) -> String,
    formatLargeNumber: (Double) -> String,
    selectedCurrency: Currency
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AMOLEDBlack)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Back", tint = NeonPurple)
            }
            Text(
                text = "${coin.name} (${coin.symbol})",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddToPortfolio) {
                Icon(Icons.Default.Star, "Add to Portfolio", tint = NeonCyan)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Price
            item {
                Text(
                    text = formatPrice(coin.currentPrice, selectedCurrency),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%.2f%% 24h", coin.change24h),
                    color = if (coin.change24h >= 0) NeonGreen else Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 24h High/Low
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("24h Low", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            formatPrice(coin.low24h, selectedCurrency),
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("24h High", color = Color.Gray, fontSize = 12.sp)
                        Text(
                            formatPrice(coin.high24h, selectedCurrency),
                            color = NeonGreen,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Chart range tabs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChartRange.entries.forEach { range ->
                        OutlinedButton(
                            onClick = { onChartRangeChange(range) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selectedChartRange == range) NeonPurple.copy(alpha = 0.2f) else Color.Transparent,
                                contentColor = if (selectedChartRange == range) NeonPurple else Color.Gray
                            )
                        ) {
                            Text(range.label, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Price chart
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (chartData.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            val min = chartData.minOrNull() ?: 0.0
                            val max = chartData.maxOrNull() ?: 1.0
                            val range = max - min
                            val path = Path()
                            chartData.forEachIndexed { index, price ->
                                val x = (index.toFloat() / (chartData.size - 1)) * size.width
                                val y = size.height - ((price - min) / range).toFloat() * size.height
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(
                                path = path,
                                color = NeonPurple,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            // Fill below line
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                color = NeonPurple.copy(alpha = 0.1f)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonPurple, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Market stats
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Market Stats", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        StatRow("Market Cap", formatLargeNumber(coin.marketCap))
                        StatRow("24h Volume", formatLargeNumber(coin.volume))
                        StatRow("Circulating Supply", formatLargeNumber(coin.circulatingSupply) + " ${coin.symbol}")
                        StatRow(
                            "Total Supply",
                            coin.totalSupply?.let { formatLargeNumber(it) + " ${coin.symbol}" } ?: "∞"
                        )
                    }
                }
            }

            // Add to portfolio button
            item {
                Button(
                    onClick = onAddToPortfolio,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Portfolio", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
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

@Composable
private fun PortfolioTab(
    holdings: List<PortfolioHolding>,
    coins: List<CoinDisplay>,
    onAddHolding: () -> Unit,
    onRemoveHolding: (PortfolioHolding) -> Unit,
    formatPrice: (Double, Currency) -> String,
    formatLargeNumber: (Double) -> String,
    selectedCurrency: Currency
) {
    val totalValue = holdings.sumOf { h ->
        val currentPrice = coins.find { it.id == h.coinId }?.currentPrice ?: h.buyPrice
        h.amount * currentPrice
    }
    val totalCost = holdings.sumOf { it.amount * it.buyPrice }
    val totalPL = totalValue - totalCost

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Portfolio summary
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Portfolio Value", color = Color.Gray, fontSize = 12.sp)
                    Text(
                        formatPrice(totalValue, selectedCurrency),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "P/L: ${formatPrice(totalPL, selectedCurrency)}",
                        color = if (totalPL >= 0) NeonGreen else Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Pie chart
        if (holdings.isNotEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    color = DarkCard,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    val pieColors = listOf(NeonPurple, NeonCyan, NeonGreen, Color(0xFFFF6600), Color(0xFFFFFF00), Color.Magenta)
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val total = holdings.sumOf { it.amount * (coins.find { c -> c.id == it.coinId }?.currentPrice ?: it.buyPrice) }
                        if (total > 0) {
                            var startAngle = -90f
                            holdings.forEachIndexed { index, holding ->
                                val value = holding.amount * (coins.find { c -> c.id == holding.coinId }?.currentPrice ?: holding.buyPrice)
                                val sweepAngle = (value / total) * 360f
                                drawArc(
                                    color = pieColors[index % pieColors.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = true
                                )
                                // Label
                                val midAngle = startAngle + sweepAngle / 2
                                val labelRadius = size.minDimension * 0.25f
                                val labelX = center.x + labelRadius * kotlin.math.cos(Math.toRadians(midAngle.toDouble())).toFloat()
                                val labelY = center.y + labelRadius * kotlin.math.sin(Math.toRadians(midAngle.toDouble())).toFloat()
                                drawContext.canvas.nativeCanvas.drawText(
                                    holding.symbol,
                                    labelX,
                                    labelY,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.WHITE
                                        textSize = 24f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }
                }
            }
        }

        // Holdings list
        items(holdings.toList()) { holding ->
            val currentPrice = coins.find { it.id == holding.coinId }?.currentPrice ?: holding.buyPrice
            val holdingValue = holding.amount * currentPrice
            val holdingPL = holdingValue - (holding.amount * holding.buyPrice)
            val plPercent = if (holding.buyPrice > 0) (holdingPL / (holding.amount * holding.buyPrice)) * 100 else 0.0

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
                color = DarkCard,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(holding.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${holding.amount} ${holding.symbol} @ ${formatPrice(holding.buyPrice, selectedCurrency)}",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(formatPrice(holdingValue, selectedCurrency), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            String.format("%+.2f%%", plPercent),
                            color = if (holdingPL >= 0) NeonGreen else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = { onRemoveHolding(holding) }) {
                        Icon(Icons.Default.Close, "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Add holding button
        item {
            Button(
                onClick = onAddHolding,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Holding", color = Color.White)
            }
        }
    }
}

@Composable
private fun GasFeesTab(gasInfo: List<GasInfo>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Gas Fee Tracker", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Current gas prices for major networks", color = Color.Gray, fontSize = 12.sp)
        }
        items(gasInfo) { gas ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkCard,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(gas.network, color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Slow", color = Color.Gray, fontSize = 12.sp)
                            Text(gas.slow, color = NeonGreen, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Standard", color = Color.Gray, fontSize = 12.sp)
                            Text(gas.standard, color = Color(0xFFFFFF00), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Fast", color = Color.Gray, fontSize = 12.sp)
                            Text(gas.fast, color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddHoldingForm(
    coins: List<CoinDisplay>,
    onAdd: (PortfolioHolding) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCoinId by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var buyPriceText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Holding", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = coins.find { it.id == selectedCoinId }?.let { "${it.name} (${it.symbol})" } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select coin", color = Color.Gray) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonPurple,
                    unfocusedBorderColor = Color.Gray,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = DarkSurface
            ) {
                coins.take(20).forEach { coin ->
                    DropdownMenuItem(
                        text = { Text("${coin.name} (${coin.symbol})", color = Color.White) },
                        onClick = {
                            selectedCoinId = coin.id
                            buyPriceText = coin.currentPrice.toString()
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonPurple,
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            )
        )

        OutlinedTextField(
            value = buyPriceText,
            onValueChange = { buyPriceText = it },
            label = { Text("Buy Price", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonPurple,
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Cancel", color = Color.White)
            }
            Button(
                onClick = {
                    val coin = coins.find { it.id == selectedCoinId }
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0
                    if (coin != null && amount > 0 && buyPrice > 0) {
                        onAdd(
                            PortfolioHolding(
                                coinId = coin.id,
                                symbol = coin.symbol,
                                name = coin.name,
                                amount = amount,
                                buyPrice = buyPrice
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
            ) {
                Text("Add", color = Color.White)
            }
        }
    }
}

@Composable
private fun AddAlertForm(
    coins: List<CoinDisplay>,
    onAdd: (PriceAlert) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCoinId by remember { mutableStateOf("") }
    var targetPriceText by remember { mutableStateOf("") }
    var isAbove by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Price Alert", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = coins.find { it.id == selectedCoinId }?.let { "${it.name} (${it.symbol})" } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select coin", color = Color.Gray) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.Gray,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = DarkSurface
            ) {
                coins.take(20).forEach { coin ->
                    DropdownMenuItem(
                        text = { Text("${coin.name} (${coin.symbol})", color = Color.White) },
                        onClick = {
                            selectedCoinId = coin.id
                            expanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = targetPriceText,
            onValueChange = { targetPriceText = it },
            label = { Text("Target Price", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.Gray,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { isAbove = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isAbove) NeonGreen.copy(alpha = 0.2f) else Color.Transparent,
                    contentColor = if (isAbove) NeonGreen else Color.Gray
                )
            ) {
                Text("Above", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { isAbove = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (!isAbove) Color.Red.copy(alpha = 0.2f) else Color.Transparent,
                    contentColor = if (!isAbove) Color.Red else Color.Gray
                )
            ) {
                Text("Below", fontSize = 12.sp)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
            ) {
                Text("Cancel", color = Color.White)
            }
            Button(
                onClick = {
                    val coin = coins.find { it.id == selectedCoinId }
                    val targetPrice = targetPriceText.toDoubleOrNull() ?: 0.0
                    if (coin != null && targetPrice > 0) {
                        onAdd(
                            PriceAlert(
                                coinId = coin.id,
                                symbol = coin.symbol,
                                targetPrice = targetPrice,
                                isAbove = isAbove
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text("Set Alert", color = AMOLEDBlack)
            }
        }
    }
}

private fun generateDemoCoins(): List<CoinDisplay> {
    return listOf(
        CoinDisplay("bitcoin", "BTC", "Bitcoin", 67234.5, 2.34, 1320000000000.0, 28500000000.0, generateDemoChartData(67234.5), 68450.0, 65120.0, 19700000.0, 21000000.0),
        CoinDisplay("ethereum", "ETH", "Ethereum", 3521.67, -1.23, 423000000000.0, 15200000000.0, generateDemoChartData(3521.67), 3620.0, 3410.0, 120000000.0, 120000000.0),
        CoinDisplay("tether", "USDT", "Tether", 1.0, 0.01, 95000000000.0, 52000000000.0, generateDemoChartData(1.0), 1.001, 0.999, 95000000000.0, null),
        CoinDisplay("bnb", "BNB", "BNB", 598.43, 3.45, 92000000000.0, 1800000000.0, generateDemoChartData(598.43), 612.0, 580.0, 153000000.0, 200000000.0),
        CoinDisplay("solana", "SOL", "Solana", 172.56, 5.67, 76000000000.0, 3200000000.0, generateDemoChartData(172.56), 178.0, 165.0, 440000000.0, null),
        CoinDisplay("xrp", "XRP", "XRP", 0.62, -2.12, 34000000000.0, 1200000000.0, generateDemoChartData(0.62), 0.65, 0.60, 55000000000.0, 100000000000.0),
        CoinDisplay("usdc", "USDC", "USD Coin", 1.0, 0.00, 33000000000.0, 6200000000.0, generateDemoChartData(1.0), 1.001, 0.999, 33000000000.0, null),
        CoinDisplay("cardano", "ADA", "Cardano", 0.48, 1.89, 17000000000.0, 420000000.0, generateDemoChartData(0.48), 0.50, 0.46, 35000000000.0, 45000000000.0),
        CoinDisplay("dogecoin", "DOGE", "Dogecoin", 0.165, 8.34, 23000000000.0, 1800000000.0, generateDemoChartData(0.165), 0.172, 0.152, 144000000000.0, null),
        CoinDisplay("avalanche", "AVAX", "Avalanche", 38.92, -0.56, 15000000000.0, 620000000.0, generateDemoChartData(38.92), 40.10, 37.50, 390000000.0, 720000000.0),
        CoinDisplay("polkadot", "DOT", "Polkadot", 7.42, 2.11, 10000000000.0, 280000000.0, generateDemoChartData(7.42), 7.65, 7.20, 1380000000.0, null),
        CoinDisplay("tron", "TRX", "TRON", 0.12, 0.45, 10500000000.0, 450000000.0, generateDemoChartData(0.12), 0.123, 0.118, 87000000000.0, null),
        CoinDisplay("chainlink", "LINK", "Chainlink", 14.87, 3.22, 8700000000.0, 520000000.0, generateDemoChartData(14.87), 15.30, 14.20, 587000000.0, 1000000000.0),
        CoinDisplay("polygon", "MATIC", "Polygon", 0.72, -1.45, 7100000000.0, 340000000.0, generateDemoChartData(0.72), 0.74, 0.70, 9800000000.0, 10000000000.0),
        CoinDisplay("shiba-inu", "SHIB", "Shiba Inu", 0.00002654, 12.34, 15600000000.0, 980000000.0, generateDemoChartData(0.00002654), 0.0000278, 0.0000235, 589000000000000.0, null)
    )
}

private fun generateDemoChartData(basePrice: Double): List<Double> {
    val data = mutableListOf<Double>()
    var price = basePrice * 0.9
    repeat(168) {
        price += (Math.random() - 0.48) * basePrice * 0.01
        price = price.coerceAtLeast(basePrice * 0.7)
        data.add(price)
    }
    return data
}

private fun generateDemoChartData(basePrice: Double, points: Int = 100): List<Double> {
    val data = mutableListOf<Double>()
    var price = basePrice * 0.95
    repeat(points) {
        price += (Math.random() - 0.48) * basePrice * 0.005
        price = price.coerceAtLeast(basePrice * 0.8)
        data.add(price)
    }
    return data
}
