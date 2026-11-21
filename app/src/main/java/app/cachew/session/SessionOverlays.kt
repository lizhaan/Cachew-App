package app.cachew.session

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.cachew.ui.theme.CachewTheme

/**
 * The small windows drawn on top of the shop during a session: the timer pill, Penny's circle
 * and the 5-minute check-in sheet.
 *
 * They are TYPE_ACCESSIBILITY_OVERLAY windows. Only an accessibility service may add those, and
 * that is why Cachew needs no separate "draw over other apps" permission. The pill and the circle
 * are small (wrap_content) and not focusable, so the shop underneath scrolls and taps as usual.
 */
internal class SessionOverlays(private val context: Context) {

    val state = SessionUiState()

    private val wm = context.getSystemService(WindowManager::class.java)
    private var pill: Window? = null
    private var penny: Window? = null
    private var sheet: Window? = null

    val sheetShowing: Boolean get() = sheet != null

    /** Pill + Penny's circle. Calling it again while they're up does nothing. */
    fun showSession(onPennyTap: () -> Unit) {
        if (pill == null) {
            pill = add(params(Gravity.TOP or Gravity.CENTER_HORIZONTAL, touchable = false)) { SessionPill(state) }
        }
        if (penny == null) {
            val y = (context.resources.displayMetrics.heightPixels * 0.36f).toInt()
            penny = add(params(Gravity.TOP or Gravity.END, touchable = true, y = y)) { PennyCircle(state, onPennyTap) }
        }
    }

    fun showSheet(texts: CheckInTexts, onSave: () -> Unit, onMore: () -> Unit, onDone: () -> Unit) {
        if (sheet != null) return
        val p = params(Gravity.BOTTOM, touchable = true).apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }
        sheet = add(p) { CheckInSheet(texts, onSave, onMore, onDone) }
    }

    fun hideSheet() {
        sheet?.remove()
        sheet = null
    }

    fun hideAll() {
        listOf(pill, penny, sheet).forEach { it?.remove() }
        pill = null
        penny = null
        sheet = null
        state.bubble = null
    }

    private fun params(gravity: Int, touchable: Boolean, y: Int = 0) = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            (if (touchable) 0 else WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
        PixelFormat.TRANSLUCENT,
    ).apply {
        this.gravity = gravity
        this.y = y
    }

    private fun add(params: WindowManager.LayoutParams, content: @Composable () -> Unit): Window {
        val owner = OverlayOwner().apply { start() }
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { CachewTheme(content) }
        }
        wm.addView(view, params)
        return Window(view, owner)
    }

    private inner class Window(private val view: View, private val owner: OverlayOwner) {
        fun remove() {
            runCatching { wm.removeView(view) }
            owner.stop()
        }
    }
}
