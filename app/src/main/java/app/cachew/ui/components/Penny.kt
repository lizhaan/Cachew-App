package app.cachew.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import app.cachew.R

/** Penny's poses (res/drawable-nodpi/penny_*.png). See the pose table in CLAUDE.md for where each is used. */
enum class PennyPose(@param:DrawableRes val res: Int) {
    BigCurl(R.drawable.penny_big_curl),
    Wave(R.drawable.penny_wave),
    Sleep(R.drawable.penny_sleep),
    Look(R.drawable.penny_look),
    Peek(R.drawable.penny_peek),
    Question(R.drawable.penny_question),
    Calm(R.drawable.penny_calm),
    Calm2(R.drawable.penny_calm2),
    Love(R.drawable.penny_love),
    Love2(R.drawable.penny_love2),
    Lie(R.drawable.penny_lie),
    Back(R.drawable.penny_back),
    Happy(R.drawable.penny_happy),
    Sparkle(R.drawable.penny_sparkle),
    Run(R.drawable.penny_run),
    Determined(R.drawable.penny_determined),
    BigSide(R.drawable.penny_big_side),
    Ball(R.drawable.penny_ball),
}

/** Penny in a square box of [size]; the image keeps its aspect ratio. Decorative, so no content description. */
@Composable
fun Penny(image: PennyPose, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(image.res),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}
