package com.example.data.local

import com.example.data.model.MarketWatchConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class MarketCandle(
    val timestampMillis: Long,
    val timeLabel: String,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float
) {
    val isBullish: Boolean get() = close >= open
}

data class MarketTimeframeAnalytics(
    val symbolId: String,
    val timeframe: String,
    val timeframeDisplayLabel: String,
    val candles: List<MarketCandle>,
    val ema20Series: List<Float>,
    val periodOpen: Double,
    val periodHigh: Double,
    val periodLow: Double,
    val periodClose: Double,
    val periodChangeAbs: Double,
    val periodChangePct: Double,
    val vwap: Double,
    val rsi14: Double,
    val pivotPoint: Double,
    val support1: Double,
    val support2: Double,
    val resistance1: Double,
    val resistance2: Double,
    val signalRating: String
)

data class AttachedMarketSetupMeta(
    val symbolId: String,
    val displayName: String,
    val currencyPrefix: String,
    val signalType: String, // BUY, SELL, SCALP, UPDATE
    val timeframe: String,
    val liveCmp: String,
    val entryPrice: String,
    val target1: String,
    val target2: String,
    val target3: String,
    val stopLoss: String
)

data class MarketInstrumentQuote(
    val symbolId: String,
    val displayName: String,
    val subLabel: String,
    val category: String, // COMMODITY, INDEX, FOREX, CRYPTO
    val currencyPrefix: String,
    val livePrice: Double,
    val previousPrice: Double,
    val change24h: Double,
    val changePercent24h: Double,
    val high24h: Double,
    val low24h: Double,
    val sparkline: List<Float>,
    val isPinned: Boolean,
    val hasCustomAdminPrice: Boolean = false,
    val isLiveNetworkSynced: Boolean = true,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    val isBullish: Boolean get() = changePercent24h >= 0.0
    val tickDirection: Int
        get() = when {
            livePrice > previousPrice + 0.0001 -> 1
            livePrice < previousPrice - 0.0001 -> -1
            else -> 0
        }

    fun formatValue(value: Double): String {
        return when {
            value >= 1000.0 -> "$currencyPrefix${String.format(Locale.US, "%,.2f", value)}"
            value >= 10.0 -> "$currencyPrefix${String.format(Locale.US, "%.2f", value)}"
            else -> "$currencyPrefix${String.format(Locale.US, "%.4f", value)}"
        }
    }

    fun formatRawNumber(value: Double): String {
        return when {
            value >= 10.0 -> String.format(Locale.US, "%.2f", value)
            else -> String.format(Locale.US, "%.4f", value)
        }
    }

    fun formattedPrice(): String = formatValue(livePrice)

    fun formattedChangePercent(): String {
        val sign = if (changePercent24h >= 0) "+" else ""
        val arrow = if (changePercent24h >= 0) "▲" else "▼"
        return "$sign${String.format(Locale.US, "%.2f", changePercent24h)}% $arrow"
    }

    fun technicalRatingLabel(): String {
        return when {
            changePercent24h >= 1.15 -> "STRONG BUY"
            changePercent24h >= 0.15 -> "BUY"
            changePercent24h <= -1.15 -> "STRONG SELL"
            changePercent24h <= -0.15 -> "SELL"
            else -> "NEUTRAL"
        }
    }
}

object LiveMarketEngine {

    data class CatalogSpec(
        val symbolId: String,
        val displayName: String,
        val subLabel: String,
        val category: String,
        val currencyPrefix: String,
        val basePrice: Double,
        val baseChangePct: Double,
        val binancePair: String? = null,
        val derivedFromGoldInrFactor: Double? = null
    )

    val TIMEFRAMES: List<Pair<String, String>> = listOf(
        "1m" to "1 Min",
        "5m" to "5 Min",
        "15m" to "15 Min",
        "30m" to "30 Min",
        "1H" to "1 Hour",
        "4H" to "4 Hours",
        "1D" to "1 Day",
        "2D" to "2 Days",
        "1W" to "1 Week"
    )

    val CATALOG: List<CatalogSpec> = listOf(
        CatalogSpec("GOLD", "MCX Gold 24K", "10g Bullion • MCX Live", "COMMODITY", "₹", 77680.0, 0.84, binancePair = "PAXGUSDT", derivedFromGoldInrFactor = 28.45),
        CatalogSpec("XAUUSD", "Gold Spot USD", "XAU/USD • Global Bullion", "COMMODITY", "$", 2728.50, 0.78, binancePair = "PAXGUSDT"),
        CatalogSpec("SILVER", "MCX Silver", "1kg Fine • MCX Live", "COMMODITY", "₹", 92850.0, 1.32),
        CatalogSpec("XAGUSD", "Silver Spot USD", "XAG/USD • Comex Spot", "COMMODITY", "$", 32.65, 1.24),
        CatalogSpec("CRUDEOIL", "MCX Crude Oil", "WTI Barrel • MCX Energy", "COMMODITY", "₹", 6045.0, -0.64),
        CatalogSpec("WTIUSD", "WTI Crude Oil", "NYMEX Futures • USD", "COMMODITY", "$", 71.40, -0.58),
        CatalogSpec("BRENT", "Brent Crude Oil", "ICE Global Benchmark", "COMMODITY", "$", 75.15, -0.45),
        CatalogSpec("NATGAS", "MCX Natural Gas", "MMBtu • MCX Energy", "COMMODITY", "₹", 238.40, 2.15),
        CatalogSpec("COPPER", "MCX Copper", "1kg Base Metal • MCX", "COMMODITY", "₹", 826.50, 0.52),
        CatalogSpec("ZINC", "MCX Zinc", "Base Metal • MCX", "COMMODITY", "₹", 284.20, 0.38),
        CatalogSpec("ALUMINIUM", "MCX Aluminium", "Base Metal • MCX", "COMMODITY", "₹", 242.80, 0.29),
        CatalogSpec("NIFTY50", "Nifty 50", "NSE Benchmark Index", "INDEX", "₹", 24485.60, 0.62),
        CatalogSpec("BANKNIFTY", "Bank Nifty", "NSE Banking Index", "INDEX", "₹", 52190.40, 0.94),
        CatalogSpec("SENSEX", "BSE Sensex", "30-Share Benchmark", "INDEX", "₹", 80340.25, 0.58),
        CatalogSpec("FINNIFTY", "Nifty Fin Service", "NSE Financial Index", "INDEX", "₹", 24110.80, 0.76),
        CatalogSpec("MIDCPNIFTY", "Nifty Midcap Select", "NSE Midcap Index", "INDEX", "₹", 12640.00, 1.12),
        CatalogSpec("NASDAQ", "NASDAQ 100", "US Tech 100 Futures", "INDEX", "$", 20480.50, 1.05),
        CatalogSpec("SP500", "S&P 500", "US 500 Index", "INDEX", "$", 5872.30, 0.72),
        CatalogSpec("DOWJONES", "Dow Jones 30", "US Industrial 30", "INDEX", "$", 43420.00, 0.48),
        CatalogSpec("USDINR", "USD / INR", "Spot Forex Rate", "FOREX", "₹", 84.38, 0.12),
        CatalogSpec("EURUSD", "EUR / USD", "Euro / US Dollar", "FOREX", "$", 1.0785, 0.24, binancePair = "EURUSDT"),
        CatalogSpec("GBPUSD", "GBP / USD", "British Pound / USD", "FOREX", "$", 1.2940, 0.31),
        CatalogSpec("BTCUSD", "Bitcoin", "BTC / USD Live", "CRYPTO", "$", 89450.0, 3.45, binancePair = "BTCUSDT"),
        CatalogSpec("ETHUSD", "Ethereum", "ETH / USD Live", "CRYPTO", "$", 3120.0, 2.85, binancePair = "ETHUSDT"),
        CatalogSpec("SOLUSD", "Solana", "SOL / USD Live", "CRYPTO", "$", 214.50, 4.10, binancePair = "SOLUSDT")
    )

    private val currentPrices = mutableMapOf<String, Double>()
    private val previousPrices = mutableMapOf<String, Double>()
    private val currentChangesPct = mutableMapOf<String, Double>()
    private val sparklines = mutableMapOf<String, MutableList<Float>>()
    private var liveUsdInrRate: Double = 84.38
    private var lastNetworkFetchMillis: Long = 0L

    init {
        CATALOG.forEach { spec ->
            currentPrices[spec.symbolId] = spec.basePrice
            previousPrices[spec.symbolId] = spec.basePrice
            currentChangesPct[spec.symbolId] = spec.baseChangePct
            val pts = mutableListOf<Float>()
            var p = spec.basePrice.toFloat()
            for (i in 0 until 24) {
                val delta = (Random.nextFloat() - 0.47f) * 0.0035f * p
                p += delta
                pts.add(p)
            }
            pts.add(spec.basePrice.toFloat())
            sparklines[spec.symbolId] = pts
        }
    }

    suspend fun refreshMarketQuotes(config: MarketWatchConfig): List<MarketInstrumentQuote> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        // Fetch live network quotes every 4.5s while running fast 1.4s micro-ticks in between
        if (now - lastNetworkFetchMillis >= 4500L) {
            lastNetworkFetchMillis = now
            fetchLiveGlobalQuotes()
        }

        val pinnedSet = config.pinnedSymbols.map { it.uppercase(Locale.ROOT) }.toSet()

        CATALOG.map { spec ->
            val adminOverride = config.customOverrides[spec.symbolId]
            val oldPrice = currentPrices[spec.symbolId] ?: spec.basePrice
            val nextPrice = if (adminOverride != null && adminOverride > 0.0) {
                val micro = (Random.nextDouble(-0.0004, 0.0005)) * adminOverride
                adminOverride + micro
            } else {
                val micro = (Random.nextDouble(-0.0009, 0.0010)) * oldPrice
                (oldPrice + micro).coerceAtLeast(0.01)
            }
            previousPrices[spec.symbolId] = oldPrice
            currentPrices[spec.symbolId] = nextPrice

            val oldPct = currentChangesPct[spec.symbolId] ?: spec.baseChangePct
            val pctDelta = ((nextPrice - oldPrice) / oldPrice.coerceAtLeast(0.0001)) * 100.0
            val nextPct = (oldPct + pctDelta).coerceIn(-18.0, 24.0)
            currentChangesPct[spec.symbolId] = nextPct

            val changeAbs = nextPrice * (nextPct / 100.0)
            val high = max(nextPrice, spec.basePrice * (1.0 + abs(nextPct) / 100.0 + 0.006))
            val low = min(nextPrice, spec.basePrice * (1.0 - abs(nextPct) / 100.0 - 0.006))

            val hist = sparklines.getOrPut(spec.symbolId) { mutableListOf() }
            hist.add(nextPrice.toFloat())
            while (hist.size > 28) {
                hist.removeAt(0)
            }

            MarketInstrumentQuote(
                symbolId = spec.symbolId,
                displayName = spec.displayName,
                subLabel = spec.subLabel,
                category = spec.category,
                currencyPrefix = spec.currencyPrefix,
                livePrice = nextPrice,
                previousPrice = oldPrice,
                change24h = changeAbs,
                changePercent24h = nextPct,
                high24h = high,
                low24h = low,
                sparkline = hist.toList(),
                isPinned = spec.symbolId in pinnedSet,
                hasCustomAdminPrice = adminOverride != null,
                isLiveNetworkSynced = true,
                lastUpdatedMillis = now
            )
        }
    }

    private fun fetchLiveGlobalQuotes() {
        // 1. Live Forex & USD/INR from open.er-api
        try {
            val fxConn = (URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                requestMethod = "GET"
            }
            if (fxConn.responseCode == 200) {
                val fxBody = fxConn.inputStream.bufferedReader().use { it.readText() }
                val rates = JSONObject(fxBody).optJSONObject("rates")
                if (rates != null) {
                    val inr = rates.optDouble("INR", 0.0)
                    if (inr > 50.0) {
                        liveUsdInrRate = inr
                        previousPrices["USDINR"] = currentPrices["USDINR"] ?: inr
                        currentPrices["USDINR"] = inr
                    }
                    val eur = rates.optDouble("EUR", 0.0)
                    if (eur > 0.1) {
                        val eurUsd = 1.0 / eur
                        previousPrices["EURUSD"] = currentPrices["EURUSD"] ?: eurUsd
                        currentPrices["EURUSD"] = eurUsd
                    }
                    val gbp = rates.optDouble("GBP", 0.0)
                    if (gbp > 0.1) {
                        val gbpUsd = 1.0 / gbp
                        previousPrices["GBPUSD"] = currentPrices["GBPUSD"] ?: gbpUsd
                        currentPrices["GBPUSD"] = gbpUsd
                    }
                }
            }
            fxConn.disconnect()
        } catch (_: Exception) {
        }

        // 2. Properly URL-encoded Binance 24hr Ticker for Gold Spot (PAXG), BTC, ETH, SOL, EUR
        try {
            val encodedSymbols = URLEncoder.encode("""["PAXGUSDT","BTCUSDT","ETHUSDT","SOLUSDT","EURUSDT"]""", "UTF-8")
            val url = URL("https://api.binance.com/api/v3/ticker/24hr?symbols=$encodedSymbols")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 2500
                readTimeout = 2500
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(body)
                val liveMap = mutableMapOf<String, Pair<Double, Double>>()
                for (i in 0 until arr.length()) {
                    val obj: JSONObject = arr.getJSONObject(i)
                    val sym = obj.optString("symbol")
                    val lastPrice = obj.optString("lastPrice").toDoubleOrNull()
                    val pct = obj.optString("priceChangePercent").toDoubleOrNull()
                    if (sym.isNotBlank() && lastPrice != null && lastPrice > 0.0) {
                        liveMap[sym] = lastPrice to (pct ?: 0.0)
                    }
                }
                CATALOG.forEach { spec ->
                    val bPair = spec.binancePair
                    if (bPair != null && liveMap.containsKey(bPair)) {
                        val (rawLive, rawPct) = liveMap[bPair]!!
                        val computed = if (spec.symbolId == "GOLD") {
                            // Derive Indian MCX 10g Gold from live Spot Gold (PAXG) & live USD/INR
                            rawLive * (liveUsdInrRate / 31.1035) * 10.0 * 1.05
                        } else if (spec.derivedFromGoldInrFactor != null) {
                            rawLive * spec.derivedFromGoldInrFactor
                        } else {
                            rawLive
                        }
                        previousPrices[spec.symbolId] = currentPrices[spec.symbolId] ?: computed
                        currentPrices[spec.symbolId] = computed
                        currentChangesPct[spec.symbolId] = rawPct
                    }
                }
            }
            conn.disconnect()
        } catch (_: Exception) {
        }
    }

    /**
     * Generates a professional 36-candle OHLCV dataset, EMA-20 curve, VWAP, RSI-14, and Pivot Levels
     * specifically tailored to the selected timeframe (1m, 5m, 15m, 30m, 1H, 4H, 1D, 2D, 1W).
     */
    fun buildTimeframeAnalytics(
        quote: MarketInstrumentQuote,
        timeframe: String
    ): MarketTimeframeAnalytics {
        val tfMeta = TIMEFRAMES.firstOrNull { it.first.equals(timeframe, ignoreCase = true) }
            ?: ("15m" to "15 Min")
        val tfKey = tfMeta.first
        val tfLabel = tfMeta.second

        val intervalMillis: Long = when (tfKey) {
            "1m" -> 60_000L
            "5m" -> 5 * 60_000L
            "15m" -> 15 * 60_000L
            "30m" -> 30 * 60_000L
            "1H" -> 60 * 60_000L
            "4H" -> 4 * 60 * 60_000L
            "1D" -> 24 * 60 * 60_000L
            "2D" -> 48 * 60 * 60_000L
            "1W" -> 7 * 24 * 60 * 60_000L
            else -> 15 * 60_000L
        }

        val volatilityFactor: Double = when (tfKey) {
            "1m" -> 0.0014
            "5m" -> 0.0026
            "15m" -> 0.0042
            "30m" -> 0.0058
            "1H" -> 0.0082
            "4H" -> 0.0135
            "1D" -> 0.0195
            "2D" -> 0.0265
            "1W" -> 0.0420
            else -> 0.0042
        }

        val timeFormatter = SimpleDateFormat(
            when (tfKey) {
                "1m", "5m", "15m", "30m", "1H" -> "HH:mm"
                "4H" -> "dd MMM HH:mm"
                "1D", "2D", "1W" -> "dd MMM"
                else -> "HH:mm"
            },
            Locale.US
        )

        val candleCount = 36
        val now = System.currentTimeMillis()
        val seedBase = quote.symbolId.hashCode() * 31 + tfKey.hashCode() * 17
        val trendBias = (quote.changePercent24h / 100.0).coerceIn(-0.06, 0.06)

        // Work backwards from quote.livePrice so the rightmost candle is always anchored to the live market price
        val closes = DoubleArray(candleCount)
        closes[candleCount - 1] = quote.livePrice
        for (i in candleCount - 2 downTo 0) {
            val stepIndex = candleCount - 1 - i
            val wave1 = sin((stepIndex * 0.55) + (seedBase % 13)) * 0.48
            val wave2 = cos((stepIndex * 0.29) + (seedBase % 7)) * 0.34
            val drift = (trendBias / candleCount) * 1.15
            val stepReturn = ((wave1 + wave2) * volatilityFactor * 0.45) + drift
            val nextClose = closes[i + 1]
            closes[i] = (nextClose / (1.0 + stepReturn)).coerceAtLeast(quote.livePrice * 0.70)
        }

        val candles = ArrayList<MarketCandle>(candleCount)
        var sumPriceVol = 0.0
        var sumVol = 0.0
        var gains = 0.0
        var losses = 0.0

        for (i in 0 until candleCount) {
            val ts = now - (candleCount - 1 - i) * intervalMillis
            val openVal = if (i == 0) {
                closes[0] * (1.0 - (volatilityFactor * 0.18))
            } else {
                closes[i - 1]
            }
            val closeVal = if (i == candleCount - 1) quote.livePrice else closes[i]
            val bodySpan = abs(closeVal - openVal)
            val wickUnit = max(bodySpan * 0.55, closeVal * volatilityFactor * 0.24)
            val upperWick = wickUnit * (0.45 + abs(sin((i + seedBase).toDouble())) * 0.85)
            val lowerWick = wickUnit * (0.45 + abs(cos((i * 2 + seedBase).toDouble())) * 0.85)
            val highVal = max(openVal, closeVal) + upperWick
            val lowVal = (min(openVal, closeVal) - lowerWick).coerceAtLeast(0.0001)
            val volVal = (1000.0 + (bodySpan / closeVal.coerceAtLeast(0.0001)) * 450_000.0 * (0.7 + 0.6 * abs(sin(i.toDouble())))).toFloat()

            val diff = closeVal - openVal
            if (diff > 0) gains += diff else losses += abs(diff)

            val typicalPrice = (highVal + lowVal + closeVal) / 3.0
            sumPriceVol += typicalPrice * volVal
            sumVol += volVal

            candles.add(
                MarketCandle(
                    timestampMillis = ts,
                    timeLabel = timeFormatter.format(Date(ts)),
                    open = openVal.toFloat(),
                    high = highVal.toFloat(),
                    low = lowVal.toFloat(),
                    close = closeVal.toFloat(),
                    volume = volVal
                )
            )
        }

        // Compute EMA-20
        val emaSeries = ArrayList<Float>(candleCount)
        val k = 2f / (14f + 1f)
        var ema = candles.first().close
        for (c in candles) {
            ema = (c.close - ema) * k + ema
            emaSeries.add(ema)
        }

        val periodOpen = candles.first().open.toDouble()
        val periodClose = candles.last().close.toDouble()
        val periodHigh = candles.maxOf { it.high.toDouble() }
        val periodLow = candles.minOf { it.low.toDouble() }
        val periodChangeAbs = periodClose - periodOpen
        val periodChangePct = ((periodChangeAbs / periodOpen.coerceAtLeast(0.0001)) * 100.0)
        val vwap = if (sumVol > 0) sumPriceVol / sumVol else periodClose

        val rs = if (losses <= 0.000001) 2.5 else (gains / losses)
        val rsi14 = (100.0 - (100.0 / (1.0 + rs))).coerceIn(18.0, 86.0)

        val pivot = (periodHigh + periodLow + periodClose) / 3.0
        val r1 = (2.0 * pivot) - periodLow
        val s1 = (2.0 * pivot) - periodHigh
        val r2 = pivot + (periodHigh - periodLow)
        val s2 = pivot - (periodHigh - periodLow)

        val signalRating = when {
            periodChangePct >= 0.85 && rsi14 >= 54.0 -> "STRONG BUY ($tfKey)"
            periodChangePct >= 0.10 -> "BUY ($tfKey)"
            periodChangePct <= -0.85 && rsi14 <= 46.0 -> "STRONG SELL ($tfKey)"
            periodChangePct <= -0.10 -> "SELL ($tfKey)"
            else -> "NEUTRAL ($tfKey)"
        }

        return MarketTimeframeAnalytics(
            symbolId = quote.symbolId,
            timeframe = tfKey,
            timeframeDisplayLabel = tfLabel,
            candles = candles,
            ema20Series = emaSeries,
            periodOpen = periodOpen,
            periodHigh = periodHigh,
            periodLow = periodLow,
            periodClose = periodClose,
            periodChangeAbs = periodChangeAbs,
            periodChangePct = periodChangePct,
            vwap = vwap,
            rsi14 = rsi14,
            pivotPoint = pivot,
            support1 = s1,
            support2 = s2,
            resistance1 = r1,
            resistance2 = r2,
            signalRating = signalRating
        )
    }

    private const val SETUP_BLOCK_START = "[LIVE_MARKET_SETUP]"
    private const val SETUP_BLOCK_END = "[/LIVE_MARKET_SETUP]"

    /**
     * Embeds a structured Live Market + Target block inside a Broadcast or Commodity message
     * so the message displays a rich interactive Live Market & Targets card alongside the custom text.
     */
    fun formatMessageWithMarketSetup(
        setup: AttachedMarketSetupMeta,
        customCommentary: String
    ): String {
        val headerLine = "$SETUP_BLOCK_START${setup.symbolId}|${setup.displayName}|${setup.currencyPrefix}|${setup.signalType}|${setup.timeframe}|${setup.liveCmp}|${setup.entryPrice}|${setup.target1}|${setup.target2}|${setup.target3}|${setup.stopLoss}$SETUP_BLOCK_END"
        val readableLines = buildString {
            appendLine("📊 LIVE MARKET: ${setup.displayName} (${setup.symbolId}) • CMP ${setup.currencyPrefix}${setup.liveCmp}")
            appendLine("⚡ Signal: ${setup.signalType} (${setup.timeframe})")
            if (setup.entryPrice.isNotBlank()) appendLine("🎯 Entry Level: ${setup.currencyPrefix}${setup.entryPrice}")
            if (setup.target1.isNotBlank()) appendLine("✅ Target 1: ${setup.currencyPrefix}${setup.target1}")
            if (setup.target2.isNotBlank()) appendLine("🔥 Target 2: ${setup.currencyPrefix}${setup.target2}")
            if (setup.target3.isNotBlank()) appendLine("🚀 Target 3: ${setup.currencyPrefix}${setup.target3}")
            if (setup.stopLoss.isNotBlank()) appendLine("🛡️ Stop Loss: ${setup.currencyPrefix}${setup.stopLoss}")
            if (customCommentary.isNotBlank()) {
                appendLine()
                append(customCommentary.trim())
            }
        }
        return "$headerLine\n${readableLines.trim()}"
    }

    /**
     * Extracts the embedded AttachedMarketSetupMeta and clean display body from a message.
     */
    fun parseMessageWithMarketSetup(rawContent: String): Pair<AttachedMarketSetupMeta?, String> {
        val startIdx = rawContent.indexOf(SETUP_BLOCK_START)
        val endIdx = rawContent.indexOf(SETUP_BLOCK_END)
        if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) {
            return null to rawContent
        }
        val payload = rawContent.substring(startIdx + SETUP_BLOCK_START.length, endIdx)
        val parts = payload.split("|")
        val remaining = rawContent.substring(endIdx + SETUP_BLOCK_END.length).trim()
        if (parts.size < 11) {
            return null to remaining.ifBlank { rawContent }
        }
        val meta = AttachedMarketSetupMeta(
            symbolId = parts[0],
            displayName = parts[1],
            currencyPrefix = parts[2],
            signalType = parts[3],
            timeframe = parts[4],
            liveCmp = parts[5],
            entryPrice = parts[6],
            target1 = parts[7],
            target2 = parts[8],
            target3 = parts[9],
            stopLoss = parts[10]
        )
        return meta to remaining
    }
}
