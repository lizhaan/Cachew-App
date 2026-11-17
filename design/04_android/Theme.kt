package app.cachew.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object CachewColors {
    val BgTop = Color(0xFF2B5A45); val BgMid = Color(0xFF163222); val BgBottom = Color(0xFF09170F)
    val NightTop = Color(0xFF22213F); val NightMid = Color(0xFF0F1A16); val NightBottom = Color(0xFF060C09)
    val Purple = Color(0xFF8B5CF6); val PurpleLight = Color(0xFFA98BFF); val PurpleDark = Color(0xFF6D28D9)
    val PurpleText = Color(0xFFB79CFF); val PurpleSoft = Color(0xFFC4B5FD)
    val Text = Color(0xFFF2EBDD); val Title = Color(0xFFFBF6EC); val Sand = Color(0xFFE8DCC4)
    val TextSecondary = Sand.copy(alpha = 0.68f)
    val Gold = Color(0xFFF4B63F); val Flame = Color(0xFFFBBF24); val Heart = Color(0xFFF472B6)
    val CardFill = Color.White.copy(alpha = 0.075f); val CardBorder = Sand.copy(alpha = 0.13f)
}

object CachewBrush {
    // Day screens: radial from top centre. Night screens (pause, questions, decision, waiting list): NightTop -> NightBottom.
    val Day = Brush.verticalGradient(listOf(CachewColors.BgTop, CachewColors.BgMid, CachewColors.BgBottom))
    val Night = Brush.verticalGradient(listOf(CachewColors.NightTop, CachewColors.NightMid, CachewColors.NightBottom))
    val PrimaryButton = Brush.linearGradient(listOf(CachewColors.PurpleLight, CachewColors.Purple, CachewColors.PurpleDark))
}

// Font: Inter (Google Fonts). Add via Downloadable Fonts or put Inter .ttf files into res/font.
object CachewType {
    val H1 = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp, letterSpacing = (-0.7).sp)
    val H2 = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
    val Body = TextStyle(fontSize = 15.5.sp, lineHeight = 22.sp)
    val Label = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp) // UPPERCASE, PurpleText
    val Button = TextStyle(fontSize = 17.5.sp, fontWeight = FontWeight.Bold)
    val BigNumber = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp)
}

object CachewSize {
    val ScreenPadding = 22.dp      // left/right
    val CardRadius = 22.dp
    val CardPadding = 16.dp
    val ButtonHeight = 58.dp
    val ButtonRadius = 18.dp
    val Gap = 12.dp                // between cards
    val BorderWidth = 1.dp         // card border; selected = 1.5.dp, color 0xFF9B74FF + soft purple glow
}
