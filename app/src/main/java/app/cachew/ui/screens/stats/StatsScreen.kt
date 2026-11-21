package app.cachew.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.ShopApps
import app.cachew.data.ShopLabels
import app.cachew.data.Stats
import app.cachew.data.Store
import app.cachew.ui.components.GlassCard
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Stats tab (design-v2/09), all from this month's real data: money kept with a bar per week,
 * pauses, how many wants were let go, the riskiest hour and the shop that triggers most.
 * Dashes until there's data.
 */
@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val version by store.gameVersion.collectAsStateWithLifecycle()
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val stats = remember(version, resumes) { Stats.month(store.waitItems(), store.pauseLog(), YearMonth.now()) }
    val dash = stringResource(R.string.stats_dash)

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CachewSize.ScreenPadding, vertical = 16.dp),
    ) {
        Text(stringResource(R.string.stats_title), style = CachewType.H1, color = CachewColors.Title)
        Spacer(Modifier.height(6.dp))
        val month = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.US)
        Text(stringResource(R.string.stats_subtitle, month), style = CachewType.Body, color = CachewColors.TextSecondary)
        Spacer(Modifier.height(16.dp))

        // WINTER STASH: money kept + 4 gold bars, one per week.
        GlassCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.stats_stash_label).uppercase(), style = CachewType.Label, color = CachewColors.Gold)
            Spacer(Modifier.height(6.dp))
            Text(Money.format(stats.kept, Money.DEFAULT_CURRENCY), style = CachewType.BigNumber, color = CachewColors.Title)
            Text(stringResource(R.string.stats_stash_caption), style = CachewType.Body, color = CachewColors.TextSecondary)
            Spacer(Modifier.height(16.dp))
            val max = stats.weeks.maxOrNull()?.takeIf { it > 0 } ?: 1.0
            Row(
                Modifier.fillMaxWidth().height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                stats.weeks.forEachIndexed { i, amount ->
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        // A thin bar even for empty weeks, so all four are visible.
                        val fraction = (amount / max).toFloat().coerceIn(0.08f, 1f)
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(listOf(CachewColors.Gold, CachewColors.Gold.copy(alpha = 0.75f)))),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("W${i + 1}", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CachewColors.TextSecondary)
                    }
                }
            }
        }
        Spacer(Modifier.height(CachewSize.Gap))

        Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
            StatTile(if (stats.pauses > 0) stats.pauses.toString() else dash, stringResource(R.string.stats_pauses), Modifier.weight(1f))
            StatTile(
                if (stats.decided > 0) stringResource(R.string.stats_let_go_value, stats.letGo, stats.decided) else dash,
                stringResource(R.string.stats_let_go),
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(CachewSize.Gap))
        Row(horizontalArrangement = Arrangement.spacedBy(CachewSize.Gap)) {
            StatTile(
                stats.riskiestHour?.let { LocalTime.of(it, 0).format(DateTimeFormatter.ofPattern("h a", Locale.US)) } ?: dash,
                stringResource(R.string.stats_riskiest_hour),
                Modifier.weight(1f),
            )
            StatTile(
                stats.topShop?.let { ShopApps.find(it)?.name ?: appLabel(context, it) } ?: dash,
                stringResource(R.string.stats_top_shop),
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier) {
    GlassCard(modifier) {
        Text(value, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = CachewColors.Title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(label, style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
    }
}

private fun appLabel(context: android.content.Context, pkg: String): String = runCatching {
    val pm = context.packageManager
    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
}.getOrElse { ShopLabels.quickName(pkg) }
