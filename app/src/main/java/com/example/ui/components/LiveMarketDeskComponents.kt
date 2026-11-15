package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.MarketInstrumentQuote
import com.example.util.SoundAndHapticFeedback
import java.util.Locale

private val BullEmerald = Color(0xFF00E676)
private val BearCrimson = Color(0xFFFF5252)
private val GoldAmber = Color(0xFFFFC400)

@Composable
fun LiveMarketTopTickerStrip(
    quotes: List<MarketInstrumentQuote>,
    isAdmin: Boolean,
    onSelectQuote: (MarketInstrumentQuote) -> Unit,
    onOpenAdminSearchDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pinnedList = remember(quotes) {
        val pinned = quotes.filter { it.isPinned }
        if (pinned.isNotEmpty()) pinned else quotes.take(8)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_market_top_ticker_strip")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = BullEmerald.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, BullEmerald.copy(alpha = 0.45f)),
                modifier = Modifier.clickable {
                    SoundAndHapticFeedback.playTap(context)
                    if (isAdmin) {
                        onOpenAdminSearchDialog()
                    } else if (pinnedList.isNotEmpty()) {
                        onSelectQuote(pinnedList.first())
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(BullEmerald)
                    )
                    Text(
                        text = if (isAdmin) "LIVE MARKET + PIN" else "LIVE MARKET",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = BullEmerald
                    )
                }
            }

            pinnedList.forEach { quote ->
                val isUp = quote.isBullish
                val accent = if (isUp) BullEmerald else BearCrimson
                val flashBg by animateColorAsState(
                    targetValue = when (quote.tickDirection) {
                        1 -> BullEmerald.copy(alpha = 0.16f)
                        -1 -> BearCrimson.copy(alpha = 0.16f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
                    },
                    animationSpec = tween(220),
                    label = "tickerFlash"
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = flashBg,
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
                    modifier = Modifier
                        .clickable {
                            SoundAndHapticFeedback.playTap(context)
                            onSelectQuote(quote)
                        }
                        .testTag("ticker_item_${quote.symbolId}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = quote.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = quote.formattedPrice(),
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = quote.formattedChangePercent(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1
                        )
                    }
                }
            }

            if (isAdmin) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldAmber.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .clickable {
                            SoundAndHapticFeedback.playTap(context)
                            onOpenAdminSearchDialog()
                        }
                        .testTag("admin_search_pin_market_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search & Pin Market",
                            tint = GoldAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+ Search & Pin Market",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAmber
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveMarketWatchboardCard(
    quotes: List<MarketInstrumentQuote>,
    isAdmin: Boolean,
    onSelectQuote: (MarketInstrumentQuote) -> Unit,
    onOpenAdminSearchDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by rememberSaveable { mutableStateOf(true) }
    var selectedCategory by rememberSaveable { mutableStateOf("PINNED") }

    val displayedQuotes = remember(quotes, selectedCategory) {
        when (selectedCategory) {
            "PINNED" -> {
                val pinned = quotes.filter { it.isPinned }
                if (pinned.isNotEmpty()) pinned else quotes.take(6)
            }
            "ALL" -> quotes
            else -> quotes.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_market_watchboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            SoundAndHapticFeedback.playTap(context)
                            expanded = !expanded
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BullEmerald.copy(alpha = 0.16f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Live Market Desk",
                                tint = BullEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Live Market Desk",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = BullEmerald.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = "● LIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BullEmerald,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Real-time Gold, Silver, Crude, Nifty & Global Quotes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAdmin) {
                        Button(
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                onOpenAdminSearchDialog()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("open_admin_market_search_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAmber,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search & Pin", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(if (expanded) "Hide" else "Show (${displayedQuotes.size})")
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "PINNED" to "📌 Pinned (${quotes.count { it.isPinned }})",
                            "COMMODITY" to "🥇 Commodities",
                            "INDEX" to "📈 Indices",
                            "FOREX" to "💱 Forex",
                            "CRYPTO" to "₿ Crypto",
                            "ALL" to "🌐 All (${quotes.size})"
                        ).forEach { (catKey, catLabel) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = {
                                    SoundAndHapticFeedback.playTap(context)
                                    selectedCategory = catKey
                                },
                                label = {
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        displayedQuotes.forEach { quote ->
                            LiveMarketQuoteMiniCard(
                                quote = quote,
                                onClick = {
                                    SoundAndHapticFeedback.playTap(context)
                                    onSelectQuote(quote)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMarketQuoteMiniCard(
    quote: MarketInstrumentQuote,
    onClick: () -> Unit
) {
    val isUp = quote.isBullish
    val accent = if (isUp) BullEmerald else BearCrimson
    val borderGlow by animateColorAsState(
        targetValue = when (quote.tickDirection) {
            1 -> BullEmerald.copy(alpha = 0.85f)
            -1 -> BearCrimson.copy(alpha = 0.85f)
            else -> accent.copy(alpha = 0.32f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardGlow"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.2.dp, borderGlow),
        modifier = Modifier
            .width(182.dp)
            .clickable { onClick() }
            .testTag("market_card_${quote.symbolId}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quote.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = quote.subLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isUp) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = quote.formattedPrice(),
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )

            SparklineCanvas(
                points = quote.sparkline,
                lineColor = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = quote.formattedChangePercent(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = quote.technicalRatingLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
            }
        }
    }
}

@Composable
fun SparklineCanvas(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2 || size.width <= 0f || size.height <= 0f) return@Canvas
        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val span = (maxVal - minVal).takeIf { it > 0.00001f } ?: 1f

        val path = Path()
        val fillPath = Path()
        val stepX = size.width / (points.size - 1).coerceAtLeast(1)

        points.forEachIndexed { idx, value ->
            val x = idx * stepX
            val normalizedY = (value - minVal) / span
            val y = size.height - (normalizedY * (size.height * 0.82f) + size.height * 0.09f)
            if (idx == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(size.width, size.height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
        val lastY = size.height - (((points.last() - minVal) / span) * (size.height * 0.82f) + size.height * 0.09f)
        drawCircle(
            color = lineColor,
            radius = 3.5.dp.toPx(),
            center = Offset(size.width, lastY)
        )
    }
}

@Composable
fun AdminMarketSearchAndPinDialog(
    allQuotes: List<MarketInstrumentQuote>,
    onTogglePinSymbol: (symbolId: String, pin: Boolean) -> Unit,
    onSetCustomPriceOverride: (symbolId: String, price: Double?) -> Unit,
    onSelectQuoteForDetail: (MarketInstrumentQuote) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("ALL") }
    var editingOverrideQuote by remember { mutableStateOf<MarketInstrumentQuote?>(null) }
    var overridePriceText by remember { mutableStateOf("") }

    val filtered = remember(allQuotes, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        allQuotes.filter { item ->
            val matchCat = selectedCategory == "ALL" ||
                (selectedCategory == "PINNED" && item.isPinned) ||
                item.category.equals(selectedCategory, ignoreCase = true)
            val matchSearch = q.isEmpty() ||
                item.symbolId.lowercase(Locale.ROOT).contains(q) ||
                item.displayName.lowercase(Locale.ROOT).contains(q) ||
                item.subLabel.lowercase(Locale.ROOT).contains(q) ||
                item.category.lowercase(Locale.ROOT).contains(q)
            matchCat && matchSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, GoldAmber.copy(alpha = 0.55f)),
            modifier = Modifier
                .widthIn(max = 620.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔎 Admin Live Market Search & Pin",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAmber
                        )
                        Text(
                            text = "Search Gold, Silver, Crude, Nifty, Forex or Crypto — Pin to show live across Android & Website",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search instrument (e.g. Gold, Silver, Crude, Nifty, Copper, BTC)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_market_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "ALL" to "All (${allQuotes.size})",
                        "PINNED" to "📌 Pinned (${allQuotes.count { it.isPinned }})",
                        "COMMODITY" to "🥇 Commodities",
                        "INDEX" to "📈 Indices",
                        "FOREX" to "💱 Forex",
                        "CRYPTO" to "₿ Crypto"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedCategory == key,
                            onClick = { selectedCategory = key },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.symbolId }) { item ->
                        val isUp = item.isBullish
                        val accent = if (isUp) BullEmerald else BearCrimson

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                1.dp,
                                if (item.isPinned) GoldAmber.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onSelectQuoteForDetail(item)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (item.isPinned) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = GoldAmber.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "PINNED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = GoldAmber,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${item.symbolId} • ${item.subLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = item.formattedPrice(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = item.formattedChangePercent(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = accent
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            editingOverrideQuote = item
                                            overridePriceText = String.format(Locale.US, "%.2f", item.livePrice)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Set Custom Benchmark Price",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            SoundAndHapticFeedback.playSuccess(context)
                                            onTogglePinSymbol(item.symbolId, !item.isPinned)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (item.isPinned) MaterialTheme.colorScheme.surfaceVariant else GoldAmber,
                                            contentColor = if (item.isPinned) MaterialTheme.colorScheme.onSurface else Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isPinned) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (item.isPinned) "Pinned" else "Pin Live",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    editingOverrideQuote?.let { target ->
        AlertDialog(
            onDismissRequest = { editingOverrideQuote = null },
            title = { Text("Calibrate Live Benchmark • ${target.displayName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Optionally set an exact MCX/Spot benchmark price for ${target.displayName} (or tap Reset to use automatic global feed):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = overridePriceText,
                        onValueChange = { overridePriceText = it },
                        label = { Text("Live Benchmark Price (${target.currencyPrefix})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = overridePriceText.trim().toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            onSetCustomPriceOverride(target.symbolId, parsed)
                        }
                        editingOverrideQuote = null
                    }
                ) {
                    Text("Save Price")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (target.hasCustomAdminPrice) {
                        OutlinedButton(
                            onClick = {
                                onSetCustomPriceOverride(target.symbolId, null)
                                editingOverrideQuote = null
                            }
                        ) {
                            Text("Reset Auto")
                        }
                    }
                    TextButton(onClick = { editingOverrideQuote = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
fun LiveMarketDetailDialog(
    quote: MarketInstrumentQuote,
    isAdmin: Boolean,
    onTogglePin: () -> Unit,
    onAdminQuickBroadcastQuote: (title: String, content: String, signalType: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTimeframe by rememberSaveable { mutableStateOf("15m") }
    val isUp = quote.isBullish
    val accent = if (isUp) BullEmerald else BearCrimson

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = quote.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "${quote.symbolId} • ${quote.subLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = quote.technicalRatingLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = quote.formattedPrice(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = quote.formattedChangePercent(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("1m", "5m", "15m", "1H", "4H", "1D").forEach { tf ->
                        FilterChip(
                            selected = selectedTimeframe == tf,
                            onClick = { selectedTimeframe = tf },
                            label = { Text(tf, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        SparklineCanvas(
                            points = quote.sparkline,
                            lineColor = accent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("24h Low", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${quote.currencyPrefix}${String.format(Locale.US, "%,.2f", quote.low24h)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = BearCrimson
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("24h High", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${quote.currencyPrefix}${String.format(Locale.US, "%,.2f", quote.high24h)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = BullEmerald
                        )
                    }
                }

                if (isAdmin) {
                    HorizontalDivider()
                    Text(
                        text = "👑 Admin 1-Tap Live Market Actions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAmber
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val priceStr = quote.formattedPrice()
                                val t1 = String.format(Locale.US, "%.2f", quote.livePrice * 1.008)
                                val t2 = String.format(Locale.US, "%.2f", quote.livePrice * 1.016)
                                val sl = String.format(Locale.US, "%.2f", quote.livePrice * 0.993)
                                SoundAndHapticFeedback.playTradeSignal(context, "BUY")
                                onAdminQuickBroadcastQuote(
                                    "🟢 BUY ${quote.displayName.uppercase(Locale.ROOT)} @ $priceStr",
                                    "LIVE MARKET CALL • ${quote.displayName}\nEntry CMP: $priceStr\nTarget 1: ${quote.currencyPrefix}$t1\nTarget 2: ${quote.currencyPrefix}$t2\nStop Loss: ${quote.currencyPrefix}$sl\nTechnical Sentiment: ${quote.technicalRatingLabel()}",
                                    "BUY"
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BullEmerald, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🟢 1-Tap BUY Call", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = {
                                val priceStr = quote.formattedPrice()
                                val t1 = String.format(Locale.US, "%.2f", quote.livePrice * 0.992)
                                val t2 = String.format(Locale.US, "%.2f", quote.livePrice * 0.984)
                                val sl = String.format(Locale.US, "%.2f", quote.livePrice * 1.007)
                                SoundAndHapticFeedback.playTradeSignal(context, "SELL")
                                onAdminQuickBroadcastQuote(
                                    "🔴 SELL ${quote.displayName.uppercase(Locale.ROOT)} @ $priceStr",
                                    "LIVE MARKET CALL • ${quote.displayName}\nEntry CMP: $priceStr\nTarget 1: ${quote.currencyPrefix}$t1\nTarget 2: ${quote.currencyPrefix}$t2\nStop Loss: ${quote.currencyPrefix}$sl\nTechnical Sentiment: ${quote.technicalRatingLabel()}",
                                    "SELL"
                                )
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BearCrimson, contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔴 1-Tap SELL Call", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    OutlinedButton(
                        onClick = onTogglePin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (quote.isPinned) "Unpin from Live Market Bar" else "Pin to Live Market Bar")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
