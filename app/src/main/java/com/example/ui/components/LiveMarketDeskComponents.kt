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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.data.local.AttachedMarketSetupMeta
import com.example.data.local.LiveMarketEngine
import com.example.data.local.MarketCandle
import com.example.data.local.MarketInstrumentQuote
import com.example.data.local.MarketTimeframeAnalytics
import com.example.util.SoundAndHapticFeedback
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private val BullEmerald = Color(0xFF00E676)
private val BearCrimson = Color(0xFFFF5252)
private val GoldAmber = Color(0xFFFFC400)
private val CyanAccent = Color(0xFF38BDF8)

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
        if (pinned.isNotEmpty()) pinned else quotes.take(6)
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
                        text = if (isAdmin) "LIVE ONLY (${pinnedList.size})" else "LIVE MARKET",
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
                            contentDescription = "Search & Pin Live Market",
                            tint = GoldAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "+ Search & Pin Live",
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
    onOpenSetMarketWithMessage: ((MarketInstrumentQuote) -> Unit)? = null,
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
                                    text = "● LIVE FEED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BullEmerald,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Tap any card for Pro Multi-Timeframe Chart & Target Setup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isAdmin && onOpenSetMarketWithMessage != null && displayedQuotes.isNotEmpty()) {
                        Button(
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                onOpenSetMarketWithMessage(displayedQuotes.first())
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("set_market_with_message_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BullEmerald,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Call & Msg", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
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
                            Text("Pin Live", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    TextButton(
                        onClick = { expanded = !expanded },
                        contentPadding = PaddingValues(horizontal = 6.dp)
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
                            "PINNED" to "🟢 Live Only (${quotes.count { it.isPinned }})",
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
                                isAdmin = isAdmin,
                                onClick = {
                                    SoundAndHapticFeedback.playTap(context)
                                    onSelectQuote(quote)
                                },
                                onSetTargetCall = if (isAdmin && onOpenSetMarketWithMessage != null) {
                                    {
                                        SoundAndHapticFeedback.playTap(context)
                                        onOpenSetMarketWithMessage(quote)
                                    }
                                } else {
                                    null
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
    isAdmin: Boolean,
    onClick: () -> Unit,
    onSetTargetCall: (() -> Unit)? = null
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
            .width(196.dp)
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
                if (isAdmin && onSetTargetCall != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = GoldAmber.copy(alpha = 0.20f),
                        border = BorderStroke(0.8.dp, GoldAmber.copy(alpha = 0.6f)),
                        modifier = Modifier.clickable { onSetTargetCall() }
                    ) {
                        Text(
                            text = "🎯 Set Call",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
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

/**
 * Institutional-grade Interactive Candlestick (OHLC) + Mountain Area + EMA-20 + Volume + Target Lines Chart.
 * Supports touch/drag crosshair scrubbing and live timeframe-responsive updates.
 */
@Composable
fun ProfessionalMarketChartCanvas(
    analytics: MarketTimeframeAnalytics,
    currencyPrefix: String,
    chartStyle: String, // "CANDLES" or "MOUNTAIN"
    showEma20: Boolean,
    showPivotLevels: Boolean,
    entryLevelOverride: Double? = null,
    target1LevelOverride: Double? = null,
    target2LevelOverride: Double? = null,
    stopLossLevelOverride: Double? = null,
    modifier: Modifier = Modifier
) {
    val candles = analytics.candles
    var inspectedIndex by remember(analytics.timeframe, analytics.symbolId) {
        mutableStateOf<Int?>(null)
    }
    val activeCandle: MarketCandle? = remember(candles, inspectedIndex) {
        val idx = inspectedIndex
        if (idx != null && idx in candles.indices) candles[idx] else candles.lastOrNull()
    }

    val isOverallBullish = analytics.periodChangePct >= 0.0
    val mainAccent = if (isOverallBullish) BullEmerald else BearCrimson

    val allPriceValues = remember(
        candles,
        showPivotLevels,
        entryLevelOverride,
        target1LevelOverride,
        target2LevelOverride,
        stopLossLevelOverride
    ) {
        val list = mutableListOf<Double>()
        candles.forEach {
            list.add(it.high.toDouble())
            list.add(it.low.toDouble())
        }
        if (showPivotLevels) {
            list.add(analytics.resistance1)
            list.add(analytics.support1)
        }
        entryLevelOverride?.takeIf { it > 0.0 }?.let { list.add(it) }
        target1LevelOverride?.takeIf { it > 0.0 }?.let { list.add(it) }
        target2LevelOverride?.takeIf { it > 0.0 }?.let { list.add(it) }
        stopLossLevelOverride?.takeIf { it > 0.0 }?.let { list.add(it) }
        list
    }

    val minPrice = (allPriceValues.minOrNull() ?: 0.0)
    val maxPrice = (allPriceValues.maxOrNull() ?: 1.0)
    val priceSpan = (maxPrice - minPrice).takeIf { it > 0.00001 } ?: 1.0

    fun fmtPrice(v: Double): String {
        return when {
            v >= 1000.0 -> "$currencyPrefix${String.format(Locale.US, "%,.2f", v)}"
            v >= 10.0 -> "$currencyPrefix${String.format(Locale.US, "%.2f", v)}"
            else -> "$currencyPrefix${String.format(Locale.US, "%.4f", v)}"
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top OHLCV Live Crosshair Readout Bar
        activeCandle?.let { c ->
            val cBull = c.isBullish
            val cColor = if (cBull) BullEmerald else BearCrimson
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(0.8.dp, cColor.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (inspectedIndex != null) "🎯 ${c.timeLabel}" else "● LIVE (${c.timeLabel})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (inspectedIndex != null) GoldAmber else BullEmerald
                    )
                    Text(
                        text = "O: ${fmtPrice(c.open.toDouble())}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "H: ${fmtPrice(c.high.toDouble())}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = BullEmerald
                    )
                    Text(
                        text = "L: ${fmtPrice(c.low.toDouble())}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = BearCrimson
                    )
                    Text(
                        text = "C: ${fmtPrice(c.close.toDouble())}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        color = cColor
                    )
                }
            }
        }

        // Main Chart Box with Right Price Axis
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF090F1A),
            border = BorderStroke(1.dp, mainAccent.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(235.dp)
                .testTag("professional_market_chart_surface")
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .padding(start = 8.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
                        .pointerInput(candles.size) {
                            detectTapGestures(
                                onPress = { offset ->
                                    if (candles.isNotEmpty() && size.width > 0) {
                                        val idx = ((offset.x / size.width.toFloat()) * candles.size)
                                            .toInt()
                                            .coerceIn(0, candles.lastIndex)
                                        inspectedIndex = idx
                                        tryAwaitRelease()
                                        inspectedIndex = null
                                    }
                                }
                            )
                        }
                        .pointerInput(candles.size) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    if (candles.isNotEmpty() && size.width > 0) {
                                        inspectedIndex = ((offset.x / size.width.toFloat()) * candles.size)
                                            .toInt()
                                            .coerceIn(0, candles.lastIndex)
                                    }
                                },
                                onDragEnd = { inspectedIndex = null },
                                onDragCancel = { inspectedIndex = null },
                                onDrag = { change, _ ->
                                    change.consume()
                                    if (candles.isNotEmpty() && size.width > 0) {
                                        inspectedIndex = ((change.position.x / size.width.toFloat()) * candles.size)
                                            .toInt()
                                            .coerceIn(0, candles.lastIndex)
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (candles.size < 2 || size.width <= 0f || size.height <= 0f) return@Canvas

                        val priceAreaHeight = size.height * 0.78f
                        val volumeAreaTop = size.height * 0.82f
                        val volumeAreaHeight = size.height * 0.18f

                        fun priceToY(p: Double): Float {
                            val norm = ((p - minPrice) / priceSpan).toFloat().coerceIn(0f, 1f)
                            return priceAreaHeight - (norm * (priceAreaHeight * 0.88f) + priceAreaHeight * 0.06f)
                        }

                        // 1. Horizontal Grid Lines (5 levels)
                        val gridDash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        for (g in 0..4) {
                            val gy = (priceAreaHeight * 0.06f) + (priceAreaHeight * 0.88f) * (g / 4f)
                            drawLine(
                                color = Color.White.copy(alpha = 0.07f),
                                start = Offset(0f, gy),
                                end = Offset(size.width, gy),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = gridDash
                            )
                        }

                        // 2. Vertical Grid Lines
                        for (v in 1..4) {
                            val vx = size.width * (v / 5f)
                            drawLine(
                                color = Color.White.copy(alpha = 0.05f),
                                start = Offset(vx, 0f),
                                end = Offset(vx, priceAreaHeight),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = gridDash
                            )
                        }

                        // 3. Support S1 & Resistance R1 Pivot Lines
                        if (showPivotLevels) {
                            val r1Y = priceToY(analytics.resistance1)
                            val s1Y = priceToY(analytics.support1)
                            val pivY = priceToY(analytics.pivotPoint)
                            drawLine(
                                color = BullEmerald.copy(alpha = 0.45f),
                                start = Offset(0f, r1Y),
                                end = Offset(size.width, r1Y),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                            drawLine(
                                color = GoldAmber.copy(alpha = 0.35f),
                                start = Offset(0f, pivY),
                                end = Offset(size.width, pivY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                            drawLine(
                                color = BearCrimson.copy(alpha = 0.45f),
                                start = Offset(0f, s1Y),
                                end = Offset(size.width, s1Y),
                                strokeWidth = 1.2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                            )
                        }

                        val slotWidth = size.width / candles.size.toFloat()
                        val candleBodyWidth = (slotWidth * 0.64f).coerceAtLeast(3f)
                        val maxVol = (candles.maxOfOrNull { it.volume } ?: 1f).coerceAtLeast(1f)

                        // 4. Volume Histogram Bars at bottom
                        candles.forEachIndexed { idx, c ->
                            val xCenter = (idx + 0.5f) * slotWidth
                            val vRatio = (c.volume / maxVol).coerceIn(0.08f, 1f)
                            val barH = volumeAreaHeight * vRatio
                            val vColor = if (c.isBullish) BullEmerald.copy(alpha = 0.35f) else BearCrimson.copy(alpha = 0.35f)
                            drawRect(
                                color = vColor,
                                topLeft = Offset(xCenter - candleBodyWidth / 2f, volumeAreaTop + (volumeAreaHeight - barH)),
                                size = Size(candleBodyWidth, barH)
                            )
                        }

                        // 5. Candles (OHLC) or Mountain Area
                        if (chartStyle == "MOUNTAIN") {
                            val linePath = Path()
                            val fillPath = Path()
                            candles.forEachIndexed { idx, c ->
                                val x = (idx + 0.5f) * slotWidth
                                val y = priceToY(c.close.toDouble())
                                if (idx == 0) {
                                    linePath.moveTo(x, y)
                                    fillPath.moveTo(x, priceAreaHeight)
                                    fillPath.lineTo(x, y)
                                } else {
                                    linePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                            }
                            fillPath.lineTo((candles.lastIndex + 0.5f) * slotWidth, priceAreaHeight)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(mainAccent.copy(alpha = 0.35f), Color.Transparent),
                                    startY = 0f,
                                    endY = priceAreaHeight
                                )
                            )
                            drawPath(
                                path = linePath,
                                color = mainAccent,
                                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                            )
                        } else {
                            // Full Candlesticks (OHLC)
                            candles.forEachIndexed { idx, c ->
                                val xCenter = (idx + 0.5f) * slotWidth
                                val highY = priceToY(c.high.toDouble())
                                val lowY = priceToY(c.low.toDouble())
                                val openY = priceToY(c.open.toDouble())
                                val closeY = priceToY(c.close.toDouble())
                                val cColor = if (c.isBullish) BullEmerald else BearCrimson

                                // High-Low Wick
                                drawLine(
                                    color = cColor,
                                    start = Offset(xCenter, highY),
                                    end = Offset(xCenter, lowY),
                                    strokeWidth = 1.4.dp.toPx()
                                )

                                // Open-Close Body
                                val topBody = min(openY, closeY)
                                val bodyH = max(abs(closeY - openY), 2.2.dp.toPx())
                                drawRect(
                                    color = cColor,
                                    topLeft = Offset(xCenter - candleBodyWidth / 2f, topBody),
                                    size = Size(candleBodyWidth, bodyH)
                                )
                            }
                        }

                        // 6. EMA-20 Moving Average Line
                        if (showEma20 && analytics.ema20Series.size == candles.size) {
                            val emaPath = Path()
                            analytics.ema20Series.forEachIndexed { idx, emaVal ->
                                val x = (idx + 0.5f) * slotWidth
                                val y = priceToY(emaVal.toDouble())
                                if (idx == 0) emaPath.moveTo(x, y) else emaPath.lineTo(x, y)
                            }
                            drawPath(
                                path = emaPath,
                                color = GoldAmber.copy(alpha = 0.9f),
                                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // 7. Live CMP Horizontal Dashed Line
                        val liveY = priceToY(analytics.periodClose)
                        drawLine(
                            color = mainAccent.copy(alpha = 0.85f),
                            start = Offset(0f, liveY),
                            end = Offset(size.width, liveY),
                            strokeWidth = 1.3.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                        )

                        // 8. Optional Admin Trade Setup Levels (Entry, Target 1, Target 2, Stop Loss)
                        entryLevelOverride?.takeIf { it > 0.0 }?.let { entryP ->
                            val y = priceToY(entryP)
                            drawLine(
                                color = CyanAccent,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.8.dp.toPx()
                            )
                        }
                        target1LevelOverride?.takeIf { it > 0.0 }?.let { t1P ->
                            val y = priceToY(t1P)
                            drawLine(
                                color = BullEmerald,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f), 0f)
                            )
                        }
                        target2LevelOverride?.takeIf { it > 0.0 }?.let { t2P ->
                            val y = priceToY(t2P)
                            drawLine(
                                color = GoldAmber,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 5f), 0f)
                            )
                        }
                        stopLossLevelOverride?.takeIf { it > 0.0 }?.let { slP ->
                            val y = priceToY(slP)
                            drawLine(
                                color = BearCrimson,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                            )
                        }

                        // 9. Active Crosshair Line if user is scrubbing/touching
                        inspectedIndex?.let { idx ->
                            if (idx in candles.indices) {
                                val cx = (idx + 0.5f) * slotWidth
                                val cy = priceToY(candles[idx].close.toDouble())
                                drawLine(
                                    color = GoldAmber.copy(alpha = 0.85f),
                                    start = Offset(cx, 0f),
                                    end = Offset(cx, size.height),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                                )
                                drawLine(
                                    color = GoldAmber.copy(alpha = 0.85f),
                                    start = Offset(0f, cy),
                                    end = Offset(size.width, cy),
                                    strokeWidth = 1.2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
                                )
                                drawCircle(
                                    color = GoldAmber,
                                    radius = 4.5.dp.toPx(),
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }
                }

                // Right Y-Axis Price Ladder Column
                Column(
                    modifier = Modifier
                        .width(78.dp)
                        .fillMaxSize()
                        .background(Color(0xFF0D1524))
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (step in 0..4) {
                        val levelPrice = maxPrice - (priceSpan * (step / 4.0))
                        val isMiddle = step == 2
                        Text(
                            text = fmtPrice(levelPrice),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isMiddle) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isMiddle) mainAccent else Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = mainAccent.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = analytics.timeframe,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = mainAccent,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Bottom X-Axis Time Scale Row
        if (candles.size >= 4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 78.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val indices = listOf(0, candles.size / 3, (candles.size * 2) / 3, candles.lastIndex)
                indices.forEach { i ->
                    Text(
                        text = candles[i].timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AdminMarketSearchAndPinDialog(
    allQuotes: List<MarketInstrumentQuote>,
    onTogglePinSymbol: (symbolId: String, pin: Boolean) -> Unit,
    onSetCustomPriceOverride: (symbolId: String, price: Double?) -> Unit,
    onSelectQuoteForDetail: (MarketInstrumentQuote) -> Unit,
    onOpenTradeSetupForQuote: ((MarketInstrumentQuote, String) -> Unit)? = null,
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
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(14.dp)
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
                            text = "🔎 Admin Live Market Manager",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAmber
                        )
                        Text(
                            text = "Pin instruments to show in Live Market Only mode, or tap 🎯 Call to set targets & message",
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
                    placeholder = { Text("Search Gold, Silver, Crude, Nifty, BankNifty, Copper, BTC...") },
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
                        "PINNED" to "🟢 Live Pinned (${allQuotes.count { it.isPinned }})",
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
                                                color = BullEmerald.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "LIVE PINNED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = BullEmerald,
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
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (onOpenTradeSetupForQuote != null) {
                                        OutlinedButton(
                                            onClick = {
                                                SoundAndHapticFeedback.playTap(context)
                                                onDismiss()
                                                onOpenTradeSetupForQuote(item, "BUY")
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("🎯 Call", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
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
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (item.isPinned) Icons.Default.Check else Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (item.isPinned) "Pinned" else "Pin",
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

/**
 * Full-Screen / Institutional Live Market Chart & Analytics Dialog.
 * Selecting 1m, 5m, 15m, 30m, 1H, 4H, 1D, 2D, or 1W dynamically updates the candlesticks,
 * EMA-20, Volume bars, RSI-14, VWAP, and Pivot levels.
 * Tapping BUY Call or SELL Call opens the dedicated Target & Message Setup Page instead of posting blindly.
 */
@Composable
fun LiveMarketDetailDialog(
    quote: MarketInstrumentQuote,
    isAdmin: Boolean,
    onTogglePin: () -> Unit,
    onOpenTradeSetupPage: (initialSignalType: String, selectedTimeframe: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTimeframe by rememberSaveable { mutableStateOf("15m") }
    var chartStyle by rememberSaveable { mutableStateOf("CANDLES") }
    var showEma20 by rememberSaveable { mutableStateOf(true) }
    var showPivots by rememberSaveable { mutableStateOf(true) }

    val analytics = remember(quote.symbolId, quote.livePrice, selectedTimeframe) {
        LiveMarketEngine.buildTimeframeAnalytics(quote, selectedTimeframe)
    }

    val isPeriodUp = analytics.periodChangePct >= 0.0
    val accent = if (isPeriodUp) BullEmerald else BearCrimson

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.2.dp, accent.copy(alpha = 0.5f)),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("live_market_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = quote.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accent.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = analytics.signalRating,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "${quote.symbolId} • ${quote.subLabel} • ${analytics.timeframeDisplayLabel} Chart",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Live Price + Timeframe Change Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = quote.formattedPrice(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                        val tfSign = if (analytics.periodChangePct >= 0) "+" else ""
                        Text(
                            text = "${analytics.timeframeDisplayLabel} Change: $tfSign${quote.formatRawNumber(analytics.periodChangeAbs)} ($tfSign${String.format(Locale.US, "%.2f", analytics.periodChangePct)}%)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = chartStyle == "CANDLES",
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                chartStyle = "CANDLES"
                            },
                            label = { Text("🕯️ Candles", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = chartStyle == "MOUNTAIN",
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                chartStyle = "MOUNTAIN"
                            },
                            label = { Text("📈 Area", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Professional Timeframe Selector (1m, 5m, 15m, 30m, 1H, 4H, 1D, 2D, 1W)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiveMarketEngine.TIMEFRAMES.forEach { (tfKey, tfLabel) ->
                        val isSelected = selectedTimeframe == tfKey
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                selectedTimeframe = tfKey
                            },
                            label = {
                                Text(
                                    text = "$tfKey ($tfLabel)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }

                // Indicator Overlay Toggles (EMA-20 & Support/Resistance Pivots)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = showEma20,
                        onClick = { showEma20 = !showEma20 },
                        label = { Text("━ EMA 20 (${quote.formatValue(analytics.ema20Series.lastOrNull()?.toDouble() ?: quote.livePrice)})", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = showPivots,
                        onClick = { showPivots = !showPivots },
                        label = { Text("┅ S1/R1 Pivots", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                // Interactive Professional Candlestick / Area Chart
                ProfessionalMarketChartCanvas(
                    analytics = analytics,
                    currencyPrefix = quote.currencyPrefix,
                    chartStyle = chartStyle,
                    showEma20 = showEma20,
                    showPivotLevels = showPivots
                )

                // Institutional Key Metrics Grid (Period High/Low, VWAP, RSI-14, S1, Pivot, R1)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricMiniColumn("Period Low (${analytics.timeframe})", quote.formatValue(analytics.periodLow), BearCrimson)
                            MetricMiniColumn("VWAP", quote.formatValue(analytics.vwap), CyanAccent)
                            MetricMiniColumn("RSI (14)", String.format(Locale.US, "%.1f", analytics.rsi14), GoldAmber)
                            MetricMiniColumn("Period High (${analytics.timeframe})", quote.formatValue(analytics.periodHigh), BullEmerald)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricMiniColumn("Support S1", quote.formatValue(analytics.support1), BearCrimson)
                            MetricMiniColumn("Pivot Point", quote.formatValue(analytics.pivotPoint), GoldAmber)
                            MetricMiniColumn("Resistance R1", quote.formatValue(analytics.resistance1), BullEmerald)
                            MetricMiniColumn("Resistance R2", quote.formatValue(analytics.resistance2), BullEmerald)
                        }
                    }
                }

                // Admin Actions: Opens the Dedicated "Set Target & Write Message" Page!
                if (isAdmin) {
                    HorizontalDivider()
                    Text(
                        text = "👑 Admin Trade Setup • Set Targets & Write Message",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = GoldAmber
                    )
                    Text(
                        text = "Tap BUY Call or SELL Call below to open the Target & Message Composer page with live levels:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                onOpenTradeSetupPage("BUY", selectedTimeframe)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BullEmerald, contentColor = Color.Black),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_buy_call_setup_page_button")
                        ) {
                            Text("🟢 BUY Call (Set Target & Msg)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                        }
                        Button(
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                onOpenTradeSetupPage("SELL", selectedTimeframe)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BearCrimson, contentColor = Color.White),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_sell_call_setup_page_button")
                        ) {
                            Text("🔴 SELL Call (Set Target & Msg)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniColumn(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor
        )
    }
}

/**
 * Dedicated "Set Target & Write Message" Page / Dialog opened when Admin clicks BUY Call, SELL Call,
 * or "Set Call & Msg" from the Live Market Desk or Message Composer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveMarketTradeSetupDialog(
    allQuotes: List<MarketInstrumentQuote>,
    initialQuote: MarketInstrumentQuote,
    initialSignalType: String = "BUY",
    initialTimeframe: String = "15m",
    availableTiers: List<Pair<String, String>> = listOf("ALL" to "🌐 All Members", "PAID_ONLY" to "💎 Paid Members Only"),
    onPublishToBroadcast: (
        title: String,
        formattedContentWithSetup: String,
        category: String,
        riskLevel: String,
        targetTier: String,
        signalType: String
    ) -> Unit,
    onPostToCommodity: (
        topicTag: String,
        formattedOpinionWithSetup: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedSymbolId by rememberSaveable(initialQuote.symbolId) { mutableStateOf(initialQuote.symbolId) }
    val activeQuote = remember(allQuotes, selectedSymbolId, initialQuote) {
        allQuotes.firstOrNull { it.symbolId == selectedSymbolId } ?: initialQuote
    }

    var signalType by rememberSaveable { mutableStateOf(initialSignalType.ifBlank { "BUY" }) }
    var timeframe by rememberSaveable { mutableStateOf(initialTimeframe.ifBlank { "15m" }) }
    var postDestination by rememberSaveable { mutableStateOf("BROADCAST") } // BROADCAST or COMMODITY
    var selectedTier by rememberSaveable { mutableStateOf("ALL") }
    var selectedRisk by rememberSaveable { mutableStateOf("MEDIUM") }

    var entryText by remember { mutableStateOf("") }
    var target1Text by remember { mutableStateOf("") }
    var target2Text by remember { mutableStateOf("") }
    var target3Text by remember { mutableStateOf("") }
    var stopLossText by remember { mutableStateOf("") }
    var customHeadline by remember { mutableStateOf("") }
    var customMessage by remember { mutableStateOf("") }

    fun recalculateSuggestedLevels(q: MarketInstrumentQuote, sig: String, tf: String) {
        val p = q.livePrice
        val stepFactor = when (tf) {
            "1m", "5m" -> 0.0035
            "15m", "30m" -> 0.0065
            "1H", "4H" -> 0.0110
            "1D", "2D", "1W" -> 0.0180
            else -> 0.0065
        }
        val isSell = sig.equals("SELL", ignoreCase = true)
        val t1 = if (isSell) p * (1.0 - stepFactor) else p * (1.0 + stepFactor)
        val t2 = if (isSell) p * (1.0 - stepFactor * 1.9) else p * (1.0 + stepFactor * 1.9)
        val t3 = if (isSell) p * (1.0 - stepFactor * 2.8) else p * (1.0 + stepFactor * 2.8)
        val sl = if (isSell) p * (1.0 + stepFactor * 0.75) else p * (1.0 - stepFactor * 0.75)

        entryText = q.formatRawNumber(p)
        target1Text = q.formatRawNumber(t1)
        target2Text = q.formatRawNumber(t2)
        target3Text = q.formatRawNumber(t3)
        stopLossText = q.formatRawNumber(sl)
        val emoji = when (sig) {
            "SELL" -> "🔴"
            "SCALP" -> "⚡"
            "UPDATE" -> "📊"
            else -> "🟢"
        }
        customHeadline = "$emoji $sig ${q.displayName.uppercase(Locale.ROOT)} ($tf) @ ${q.currencyPrefix}${q.formatRawNumber(p)}"
    }

    LaunchedEffect(activeQuote.symbolId, signalType, timeframe) {
        recalculateSuggestedLevels(activeQuote, signalType, timeframe)
    }

    val analytics = remember(activeQuote.symbolId, activeQuote.livePrice, timeframe) {
        LiveMarketEngine.buildTimeframeAnalytics(activeQuote, timeframe)
    }

    val entryVal = entryText.toDoubleOrNull()
    val t1Val = target1Text.toDoubleOrNull()
    val t2Val = target2Text.toDoubleOrNull()
    val slVal = stopLossText.toDoubleOrNull()

    val riskRewardText = remember(entryVal, t1Val, t2Val, slVal) {
        if (entryVal != null && slVal != null && (t2Val != null || t1Val != null)) {
            val targetRef = t2Val ?: t1Val ?: entryVal
            val risk = abs(entryVal - slVal)
            val reward = abs(targetRef - entryVal)
            if (risk > 0.00001) {
                "R:R 1 : ${String.format(Locale.US, "%.1f", reward / risk)}"
            } else {
                "R:R Custom"
            }
        } else {
            "R:R Auto"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                1.4.dp,
                if (signalType == "SELL") BearCrimson.copy(alpha = 0.65f) else BullEmerald.copy(alpha = 0.65f)
            ),
            modifier = Modifier
                .widthIn(max = 650.dp)
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("live_market_trade_setup_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🎯 Set Live Market Target & Write Message",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAmber
                        )
                        Text(
                            text = "Customize Entry, Target 1, Target 2, Stop Loss & your message before posting",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // 1. Live Market Instrument Selector Row
                Text(
                    text = "1. Select Live Market Instrument (Live CMP: ${activeQuote.formattedPrice()})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val orderedQuotes = remember(allQuotes) {
                        allQuotes.sortedByDescending { it.isPinned }
                    }
                    orderedQuotes.forEach { q ->
                        FilterChip(
                            selected = q.symbolId == activeQuote.symbolId,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                selectedSymbolId = q.symbolId
                            },
                            label = {
                                Text(
                                    text = "${q.displayName} (${q.formattedPrice()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                // 2. Signal Direction & Timeframe Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "BUY" to "🟢 BUY CALL",
                            "SELL" to "🔴 SELL CALL",
                            "SCALP" to "⚡ SCALP",
                            "UPDATE" to "📊 MARKET VIEW"
                        ).forEach { (sigKey, sigLabel) ->
                            FilterChip(
                                selected = signalType == sigKey,
                                onClick = {
                                    SoundAndHapticFeedback.playTap(context)
                                    signalType = sigKey
                                },
                                label = { Text(sigLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Timeframe:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    LiveMarketEngine.TIMEFRAMES.forEach { (tfKey, _) ->
                        FilterChip(
                            selected = timeframe == tfKey,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                timeframe = tfKey
                            },
                            label = { Text(tfKey, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Live Preview Chart showing Entry, T1, T2, and SL horizontal lines
                ProfessionalMarketChartCanvas(
                    analytics = analytics,
                    currencyPrefix = activeQuote.currencyPrefix,
                    chartStyle = "CANDLES",
                    showEma20 = true,
                    showPivotLevels = false,
                    entryLevelOverride = entryVal,
                    target1LevelOverride = t1Val,
                    target2LevelOverride = t2Val,
                    stopLossLevelOverride = slVal
                )

                // 3. Set Entry, Target 1, Target 2, Target 3 & Stop Loss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Set Entry, Targets & Stop Loss (${activeQuote.currencyPrefix})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = BullEmerald
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldAmber.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = riskRewardText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = entryText,
                        onValueChange = { entryText = it },
                        label = { Text("Entry Price (${activeQuote.currencyPrefix})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_entry_price_input")
                    )
                    OutlinedTextField(
                        value = stopLossText,
                        onValueChange = { stopLossText = it },
                        label = { Text("Stop Loss SL (${activeQuote.currencyPrefix})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_stop_loss_input")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = target1Text,
                        onValueChange = { target1Text = it },
                        label = { Text("Target 1 (${activeQuote.currencyPrefix})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_target1_input")
                    )
                    OutlinedTextField(
                        value = target2Text,
                        onValueChange = { target2Text = it },
                        label = { Text("Target 2 (${activeQuote.currencyPrefix})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_target2_input")
                    )
                    OutlinedTextField(
                        value = target3Text,
                        onValueChange = { target3Text = it },
                        label = { Text("Target 3 (Opt)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("setup_target3_input")
                    )
                }

                // 4. Write Headline & Custom Message
                Text(
                    text = "3. Write Your Trade Message & Instructions",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = customHeadline,
                    onValueChange = { customHeadline = it },
                    label = { Text("Broadcast Headline") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_headline_input")
                )

                OutlinedTextField(
                    value = customMessage,
                    onValueChange = { customMessage = it },
                    label = { Text("Write your analysis, lot size advice, or instructions...") },
                    placeholder = { Text("e.g. Enter on 15m candle close above entry. Book 50% at Target 1 and trail SL to cost.") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("setup_custom_message_input")
                )

                // Quick-Insert Commentary Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Book 50% at Target 1 & trail SL to cost.",
                        "Strict intraday Stop Loss mandatory.",
                        "High momentum breakout on $timeframe chart.",
                        "Safe traders can exit full quantity at Target 1."
                    ).forEach { phrase ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                SoundAndHapticFeedback.playTap(context)
                                customMessage = if (customMessage.isBlank()) phrase else "${customMessage.trim()}\n• $phrase"
                            },
                            label = { Text("+ $phrase", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        )
                    }
                }

                // 5. Choose Where to Send (Broadcast Stream vs Commodity Discussion)
                Text(
                    text = "4. Send Market & Message To:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GoldAmber
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = postDestination == "BROADCAST",
                        onClick = { postDestination = "BROADCAST" },
                        label = { Text("📢 Broadcast Stream (Official Call)", fontWeight = FontWeight.Bold) }
                    )
                    FilterChip(
                        selected = postDestination == "COMMODITY",
                        onClick = { postDestination = "COMMODITY" },
                        label = { Text("💬 Commodity Room Post", fontWeight = FontWeight.Bold) }
                    )
                }

                if (postDestination == "BROADCAST") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTiers.forEach { (tKey, tLabel) ->
                            FilterChip(
                                selected = selectedTier == tKey,
                                onClick = { selectedTier = tKey },
                                label = { Text(tLabel, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { rLevel ->
                            FilterChip(
                                selected = selectedRisk == rLevel,
                                onClick = { selectedRisk = rLevel },
                                label = { Text("Risk: $rLevel", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                // Submit / Publish Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val setupMeta = AttachedMarketSetupMeta(
                                symbolId = activeQuote.symbolId,
                                displayName = activeQuote.displayName,
                                currencyPrefix = activeQuote.currencyPrefix,
                                signalType = signalType,
                                timeframe = timeframe,
                                liveCmp = activeQuote.formatRawNumber(activeQuote.livePrice),
                                entryPrice = entryText.trim(),
                                target1 = target1Text.trim(),
                                target2 = target2Text.trim(),
                                target3 = target3Text.trim(),
                                stopLoss = stopLossText.trim()
                            )
                            val formattedBody = LiveMarketEngine.formatMessageWithMarketSetup(
                                setup = setupMeta,
                                customCommentary = customMessage
                            )
                            SoundAndHapticFeedback.playTradeSignal(context, signalType)
                            if (postDestination == "COMMODITY") {
                                onPostToCommodity(activeQuote.displayName, formattedBody)
                            } else {
                                val resolvedTitle = customHeadline.trim().ifBlank {
                                    "$signalType ${activeQuote.displayName} ($timeframe) @ ${activeQuote.formattedPrice()}"
                                }
                                onPublishToBroadcast(
                                    resolvedTitle,
                                    formattedBody,
                                    activeQuote.category,
                                    selectedRisk,
                                    selectedTier,
                                    signalType
                                )
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (signalType == "SELL") BearCrimson else BullEmerald,
                            contentColor = if (signalType == "SELL") Color.White else Color.Black
                        ),
                        modifier = Modifier.testTag("publish_market_trade_setup_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (postDestination == "COMMODITY") "Post to Commodity" else "Publish Broadcast Call",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rich Embedded Live Market & Target Card rendered inside Broadcast & Commodity messages
 * whenever a message has an attached Live Market setup.
 */
@Composable
fun AttachedLiveMarketSetupCard(
    setup: AttachedMarketSetupMeta,
    liveQuote: MarketInstrumentQuote?,
    onOpenLiveChart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isSell = setup.signalType.equals("SELL", ignoreCase = true)
    val accent = if (isSell) BearCrimson else BullEmerald
    val currentLiveStr = liveQuote?.formattedPrice() ?: "${setup.currencyPrefix}${setup.liveCmp}"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0A1220),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onOpenLiveChart != null) Modifier.clickable { onOpenLiveChart() } else Modifier)
            .testTag("attached_live_market_setup_card_${setup.symbolId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${if (isSell) "🔴" else "🟢"} ${setup.signalType} • ${setup.timeframe}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = setup.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Call CMP: ${setup.currencyPrefix}${setup.liveCmp} • Live Market Now: $currentLiveStr",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF94A3B8)
                    )
                }

                if (onOpenLiveChart != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldAmber.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, GoldAmber.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "📈 Live Chart",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = GoldAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Targets & Stop Loss Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (setup.entryPrice.isNotBlank()) {
                    SetupLevelPill("🎯 ENTRY", "${setup.currencyPrefix}${setup.entryPrice}", CyanAccent)
                }
                if (setup.target1.isNotBlank()) {
                    SetupLevelPill("✅ TARGET 1", "${setup.currencyPrefix}${setup.target1}", BullEmerald)
                }
                if (setup.target2.isNotBlank()) {
                    SetupLevelPill("🔥 TARGET 2", "${setup.currencyPrefix}${setup.target2}", GoldAmber)
                }
                if (setup.target3.isNotBlank()) {
                    SetupLevelPill("🚀 TARGET 3", "${setup.currencyPrefix}${setup.target3}", BullEmerald)
                }
                if (setup.stopLoss.isNotBlank()) {
                    SetupLevelPill("🛡️ STOP LOSS", "${setup.currencyPrefix}${setup.stopLoss}", BearCrimson)
                }
            }
        }
    }
}

@Composable
private fun SetupLevelPill(
    label: String,
    value: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }
    }
}
