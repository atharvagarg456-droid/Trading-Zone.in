package com.example.data.local

import com.example.data.model.MarketWatchConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

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
    val lastUpdatedMillis: Long = System.currentTimeMillis()
) {
    val isBullish: Boolean get() = changePercent24h >= 0.0
    val tickDirection: Int
        get() = when {
            livePrice > previousPrice + 0.0001 -> 1
            livePrice < previousPrice - 0.0001 -> -1
            else -> 0
        }

    fun formattedPrice(): String {
        return when {
            livePrice >= 1000.0 -> "$currencyPrefix${String.format(Locale.US, "%,.2f", livePrice)}"
            livePrice >= 10.0 -> "$currencyPrefix${String.format(Locale.US, "%.2f", livePrice)}"
            else -> "$currencyPrefix${String.format(Locale.US, "%.4f", livePrice)}"
        }
    }

    fun formattedChangePercent(): String {
        val sign = if (changePercent24h >= 0) "+" else ""
        val arrow = if (changePercent24h >= 0) "▲" else "▼"
        return "$sign${String.format(Locale.US, "%.2f", changePercent24h)}% $arrow"
    }

    fun technicalRatingLabel(): String {
        return when {
            changePercent24h >= 1.25 -> "STRONG BUY"
            changePercent24h >= 0.15 -> "BUY"
            changePercent24h <= -1.25 -> "STRONG SELL"
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

    init {
        CATALOG.forEach { spec ->
            currentPrices[spec.symbolId] = spec.basePrice
            previousPrices[spec.symbolId] = spec.basePrice
            currentChangesPct[spec.symbolId] = spec.baseChangePct
            val pts = mutableListOf<Float>()
            var p = spec.basePrice.toFloat()
            for (i in 0 until 18) {
                val delta = (Random.nextFloat() - 0.47f) * 0.004f * p
                p += delta
                pts.add(p)
            }
            pts.add(spec.basePrice.toFloat())
            sparklines[spec.symbolId] = pts
        }
    }

    suspend fun refreshMarketQuotes(config: MarketWatchConfig): List<MarketInstrumentQuote> = withContext(Dispatchers.IO) {
        // 1. Try fetching live 24hr ticker prices from public Binance endpoint for Gold (PAXG), BTC, ETH, SOL, EUR
        try {
            val symbolsJson = """["PAXGUSDT","BTCUSDT","ETHUSDT","SOLUSDT","EURUSDT"]"""
            val url = URL("https://api.binance.com/api/v3/ticker/24hr?symbols=$symbolsJson")
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
                        val computed = if (spec.derivedFromGoldInrFactor != null) {
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
            // Network restricted or offline — continue with deterministic micro-ticks
        }

        // 2. Apply realistic live micro-ticks + Admin custom overrides so the market desk pulses in real time
        val pinnedSet = config.pinnedSymbols.map { it.uppercase(Locale.ROOT) }.toSet()
        val now = System.currentTimeMillis()

        CATALOG.map { spec ->
            val adminOverride = config.customOverrides[spec.symbolId]
            val oldPrice = currentPrices[spec.symbolId] ?: spec.basePrice
            val nextPrice = if (adminOverride != null && adminOverride > 0.0) {
                // Micro jitter around Admin's custom benchmark price
                val micro = (Random.nextDouble(-0.0005, 0.0006)) * adminOverride
                adminOverride + micro
            } else {
                val micro = (Random.nextDouble(-0.0012, 0.0013)) * oldPrice
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
            while (hist.size > 22) {
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
                lastUpdatedMillis = now
            )
        }
    }
}
