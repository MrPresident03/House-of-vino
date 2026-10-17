package com.houseofvino.app.ui.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.houseofvino.app.AppGraph
import com.houseofvino.app.ui.common.GraphViewModel
import com.houseofvino.app.ui.common.Load
import com.houseofvino.app.ui.common.appGraph
import com.houseofvino.app.ui.common.userMessage
import com.houseofvino.app.ui.design.EmptyState
import com.houseofvino.app.ui.design.ErrorState
import com.houseofvino.app.ui.design.Hv
import com.houseofvino.app.ui.design.HvTopBar
import com.houseofvino.app.ui.design.LoadingRows
import com.houseofvino.app.ui.design.ProvenanceCaption
import com.houseofvino.app.ui.design.SectionHeader
import com.houseofvino.app.ui.design.ShareBar
import com.houseofvino.app.ui.design.StatBlock
import com.houseofvino.app.ui.design.formatted
import com.houseofvino.app.ui.journal.monthLabel
import com.houseofvino.core.insights.DrinkingWindow
import com.houseofvino.core.insights.InsightMath
import com.houseofvino.core.insights.MoneySummary
import com.houseofvino.core.search.WindowFilter
import com.houseofvino.data.repo.Breakdown
import com.houseofvino.data.repo.Insights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InsightsViewModel(graph: AppGraph) : GraphViewModel(graph) {
    private val _state = MutableStateFlow<Load<Insights>>(Load.Loading)
    val state: StateFlow<Load<Insights>> = _state.asStateFlow()

    init {
        observe("lot", "movement", "valuation", "vintage", "wine", "exchange_rate", "setting") {
            _state.value = try {
                val p = graph.platform
                Load.Ready(io { graph.insights.compute(collectionId, p.currentYear(), graph.settings.preferences().currency, graph.converter(), p.today()) })
            } catch (e: Exception) { Load.Failed(e.userMessage()) }
        }
    }
}

@Composable
fun InsightsScreen(onBack: () -> Unit) {
    val graph = appGraph()
    val vm = viewModel { InsightsViewModel(graph) }
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        HvTopBar("Insights", onBack = onBack)
        when (val s = state) {
            Load.Loading -> LoadingRows(4)
            is Load.Failed -> ErrorState(s.message, null)
            is Load.Ready -> if (s.value.totals.bottles == 0 && s.value.consumption.isEmpty()) {
                EmptyState("Nothing to analyse yet", "Insights appear once you have bottles in your cellar.")
            } else InsightsBody(s.value, graph.platform.currentYear())
        }
    }
}

@Composable
private fun InsightsBody(i: Insights, year: Int) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            StatBlock(i.totals.bottles.toString(), "bottles")
            StatBlock((i.totals.standardEquivalentsCentis / 100).toString(), "in 75 cl bottles")
            StatBlock(i.totals.vintages.toString(), "wines")
        }

        SectionHeader("What you paid")
        MoneyLines(i.cost, emptyMessage = "No purchase prices recorded.")
        if (i.totals.unpricedBottles > 0) Note("${i.totals.unpricedBottles} bottle(s) have no purchase price.")

        SectionHeader("Estimated value")
        MoneyLines(i.value, emptyMessage = "No valuations recorded. Add values with their source on each wine.")
        if (i.valueSources.isNotEmpty()) {
            i.valueSources.distinctBy { it.source to it.asOf }.sortedByDescending { it.asOf }.take(6).forEach {
                ProvenanceCaption(it.source, it.asOf.toString(), Modifier.padding(horizontal = 16.dp))
            }
        }
        if (i.totals.unvaluedBottles > 0) Note("${i.totals.unvaluedBottles} bottle(s) have no valuation and aren't included.")

        SectionHeader("Drinking windows")
        val windowTotal = i.windows.values.sum()
        listOf(WindowFilter.READY, WindowFilter.DRINK_SOON, WindowFilter.HOLD, WindowFilter.PAST, WindowFilter.UNKNOWN).forEach { w ->
            val n = i.windows[w] ?: 0
            if (n > 0) ShareBar(DrinkingWindow.label(w), n, if (windowTotal == 0) 0 else n * 1000 / windowTotal)
        }
        HorizonChart(i.horizon, year)

        if (i.consumption.isNotEmpty()) {
            SectionHeader("Last 12 months")
            i.consumption.forEach { m ->
                val parts = listOfNotNull(m.drunk.takeIf { it > 0 }?.let { "$it drunk" }, m.sold.takeIf { it > 0 }?.let { "$it sold" },
                    m.gifted.takeIf { it > 0 }?.let { "$it given" }, m.lost.takeIf { it > 0 }?.let { "$it lost" })
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(monthLabel(m.month), style = Hv.type.bodyDense, color = Hv.colors.ink, modifier = Modifier.weight(1f))
                    Text(parts.joinToString(" · "), style = Hv.type.bodyDense, color = Hv.colors.inkMuted)
                }
            }
        }

        BreakdownSection("By colour", i.byColor) { it.lowercase().replaceFirstChar(Char::uppercase).replace("Rose", "Rosé") }
        BreakdownSection("By country", i.byCountry)
        BreakdownSection("By region", i.byRegion)
        BreakdownSection("By main grape", i.byGrape)
        BreakdownSection("By decade", i.byDecade)
        BreakdownSection("By location", i.byLocation)
    }
}

@Composable
private fun MoneyLines(m: MoneySummary, emptyMessage: String) {
    if (m.byCurrency.isEmpty()) { Note(emptyMessage); return }
    Column(Modifier.padding(horizontal = 16.dp)) {
        m.byCurrency.values.sortedByDescending { it.minor }.forEach { Text(it.formatted(), style = Hv.type.title, color = Hv.colors.ink) }
        m.converted?.let { c ->
            if (m.byCurrency.size > 1 && c.ratesUsed.isNotEmpty()) {
                Text("≈ ${c.total.formatted()} in total", style = Hv.type.body, color = Hv.colors.ink)
                c.ratesUsed.forEach { r -> Text("1 ${r.base.code} = ${r.rateText()} ${r.quote.code} (${r.source}, ${r.asOf})", style = Hv.type.caption, color = Hv.colors.inkMuted) }
                if (c.unconverted.isNotEmpty()) Text("Not converted (no rate): ${c.unconverted.keys.joinToString { it.code }}", style = Hv.type.caption, color = Hv.colors.inkMuted)
            }
        }
    }
}

@Composable
private fun Note(text: String) { Text(text, style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }

@Composable
private fun BreakdownSection(title: String, rows: List<Breakdown>, label: (String) -> String = { it }) {
    if (rows.isEmpty()) return
    SectionHeader(title)
    val shares = InsightMath.breakdown(rows.associate { it.label to it.bottles })
    shares.forEach { (k, permille) -> ShareBar(label(k), rows.first { it.label == k }.bottles, permille) }
}

/** Ten-year horizon: stacked columns of ready / soon / hold / past bottles per year. */
@Composable
private fun HorizonChart(horizon: List<Map<WindowFilter, Int>>, fromYear: Int) {
    if (horizon.isEmpty()) return
    val max = horizon.maxOf { it.values.sum() }.coerceAtLeast(1)
    val c = Hv.colors
    val order = listOf(WindowFilter.PAST to c.past, WindowFilter.DRINK_SOON to c.drinkSoon, WindowFilter.READY to c.ready, WindowFilter.HOLD to c.hold)
    Text("Next ten years, if nothing is drunk", style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.padding(start = 16.dp, top = 12.dp))
    Row(Modifier.fillMaxWidth().height(120.dp).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        horizon.forEachIndexed { idx, year ->
            val summary = order.joinToString(", ") { (w, _) -> "${year[w] ?: 0} ${DrinkingWindow.label(w).lowercase()}" }
            Column(Modifier.weight(1f).fillMaxHeight().semantics(mergeDescendants = true) { contentDescription = "${fromYear + idx}: $summary" },
                verticalArrangement = Arrangement.Bottom) {
                order.forEach { (w, color) ->
                    val n = year[w] ?: 0
                    if (n > 0) Box(Modifier.fillMaxWidth().weight(n.toFloat() / max, fill = true).background(color))
                }
                val rest = (max - year.values.sum()).toFloat() / max
                if (rest > 0f) Box(Modifier.fillMaxWidth().weight(rest).background(Color.Transparent))
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(fromYear.toString(), style = Hv.type.caption, color = Hv.colors.inkMuted, modifier = Modifier.weight(1f))
        Text((fromYear + horizon.size - 1).toString(), style = Hv.type.caption, color = Hv.colors.inkMuted)
    }
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        order.reversed().forEach { (w, color) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(10.dp).height(10.dp).background(color))
                Text(" " + DrinkingWindow.label(w), style = Hv.type.caption, color = Hv.colors.inkMuted)
            }
        }
    }
}
