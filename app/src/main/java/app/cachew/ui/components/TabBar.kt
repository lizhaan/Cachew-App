package app.cachew.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.Inter

/** The four tabs at the bottom (design-v2/01, 08, 09; 25). */
enum class Tab(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    Den(R.string.tab_den, R.drawable.ic_tab_den),
    Waiting(R.string.tab_waiting, R.drawable.ic_tab_waiting),
    Stats(R.string.tab_stats, R.drawable.ic_tab_stats),
    Me(R.string.tab_me, R.drawable.ic_tab_me),
}

/** A tab screen: day background, the tab's content, and the glass tab bar at the bottom. */
@Composable
fun TabScaffold(current: Tab, onTab: (Tab) -> Unit, content: @Composable () -> Unit) {
    CachewBackground {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) { content() }
            TabBar(current, onTab)
        }
    }
}

@Composable
private fun TabBar(current: Tab, onTab: (Tab) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        Modifier
            .navigationBarsPadding()
            .padding(horizontal = CachewSize.ScreenPadding, vertical = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(CachewColors.BgMid.copy(alpha = 0.9f))
            .border(CachewSize.BorderWidth, CachewColors.CardBorder, shape)
            .padding(vertical = 8.dp),
    ) {
        Tab.entries.forEach { tab ->
            val color = if (tab == current) CachewColors.PurpleText else CachewColors.TextSecondary
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { if (tab != current) onTab(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(tab.icon), contentDescription = null, colorFilter = ColorFilter.tint(color), modifier = Modifier.size(24.dp))
                Spacer(Modifier.height(3.dp))
                Text(stringResource(tab.label), fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = color)
            }
        }
    }
}
