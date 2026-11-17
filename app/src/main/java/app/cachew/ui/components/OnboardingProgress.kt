package app.cachew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import app.cachew.ui.theme.CachewColors

/** Segmented progress bar at the top of onboarding screens: [done] of [total] segments are lit. */
@Composable
fun OnboardingProgress(done: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            val lit = i < done
            val shape = RoundedCornerShape(50)
            var m = Modifier.weight(1f).height(5.dp)
            if (lit) m = m.shadow(6.dp, shape, ambientColor = CachewColors.Purple, spotColor = CachewColors.Purple)
            Box(
                m.clip(shape).background(
                    if (lit) CachewColors.PurpleLight else CachewColors.Sand.copy(alpha = 0.12f)
                )
            )
        }
    }
}
