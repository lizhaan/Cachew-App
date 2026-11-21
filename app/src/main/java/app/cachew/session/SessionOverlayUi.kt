package app.cachew.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.SecondaryButton
import app.cachew.ui.theme.CachewBrush
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/** What the overlays show. The service changes these values; Compose redraws on its own. */
internal class SessionUiState {
    /** "9:12 left · just looking" */
    var pill by mutableStateOf("")
    /** Speech bubble next to Penny's circle; null = only the circle. */
    var bubble by mutableStateOf<String?>(null)
    /** Changed to make Penny's circle jump ("2 minutes left"). */
    var bounce by mutableIntStateOf(0)
}

/** Texts of the 5-minute check-in sheet (built by the service from strings.xml). */
internal data class CheckInTexts(
    val title: String,
    val body: String,
    val saveTitle: String,
    val saveBody: String,
    val more: String,
    val done: String,
)

/** Timer pill at the top of the shop (design-v2/03): "🐿 9:12 left · just looking". */
@Composable
internal fun SessionPill(state: SessionUiState) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .padding(top = 6.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(PillColor)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🐿", fontSize = 15.sp)
        Spacer(Modifier.width(8.dp))
        Text(state.pill, fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = CachewColors.Title)
    }
}

/** Penny's circle on the right edge, with an optional speech bubble to its left. */
@Composable
internal fun PennyCircle(state: SessionUiState, onTap: () -> Unit) {
    val jump = remember { Animatable(0f) }
    LaunchedEffect(state.bounce) {
        if (state.bounce == 0) return@LaunchedEffect
        repeat(3) {
            jump.animateTo(-18f, tween(140))
            jump.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }
    Row(
        Modifier.padding(end = 8.dp).offset(y = jump.value.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = state.bubble != null, enter = fadeIn(), exit = fadeOut()) {
            val shape = RoundedCornerShape(16.dp)
            Text(
                text = state.bubble.orEmpty(),
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = BubbleText,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .widthIn(max = 190.dp)
                    .shadow(6.dp, shape)
                    .clip(shape)
                    .background(Color.White)
                    .clickable(onClick = onTap)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        Box(
            Modifier
                .size(68.dp)
                .shadow(14.dp, CircleShape, ambientColor = CachewColors.Purple, spotColor = CachewColors.Purple)
                .clip(CircleShape)
                .background(CachewColors.BgMid)
                .border(3.dp, CachewColors.PurpleLight, CircleShape)
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            // penny_peek.png is 4:3 (240×180). A 4:3 picture fits completely inside a circle when its
            // diagonal equals the diameter: 68 dp circle → 54 × 40 dp. So the whole Penny is visible,
            // ears and paws included.
            Image(
                painterResource(PennyPose.Peek.res),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(width = 54.dp, height = 40.dp),
            )
        }
    }
}

/**
 * 5-minute check-in (design-v2/04, "just looking" only): the shop dims, a sheet slides up
 * with Penny and three choices.
 */
@Composable
internal fun CheckInSheet(texts: CheckInTexts, onSave: () -> Unit, onMore: () -> Unit, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.BottomCenter) {
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(CachewBrush.Day)
                .clickable(enabled = false) {} // taps on the sheet don't fall through to the shop
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Penny(PennyPose.Look, 96.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(texts.title, style = CachewType.H2, color = CachewColors.Title)
                    Spacer(Modifier.height(4.dp))
                    Text(texts.body, style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(16.dp))
            GlassCard(Modifier.fillMaxWidth(), selected = true, onClick = onSave) {
                Text(texts.saveTitle, style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(texts.saveBody, style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Image(painterResource(R.drawable.ic_cashew), contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(CachewSize.Gap))
            SecondaryButton(texts.more, onMore)
            Spacer(Modifier.height(CachewSize.Gap))
            SecondaryButton(texts.done, onDone)
        }
    }
}

// Dark green pill and white speech bubble, as in design-v2/03.
private val PillColor = CachewColors.BgMid.copy(alpha = 0.96f)
private val BubbleText = Color(0xFF1B1B1B)
