package app.cachew.ui.screens.setup

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.cachew.R
import app.cachew.data.PhoneSetup
import app.cachew.data.Store
import app.cachew.service.ShopWatchService
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.OnboardingProgress
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/**
 * "Set up Cachew": everything the phone needs so the pause shows up on time, for every brand.
 *  1. Allow restricted settings (Android 13+ only): apps installed from a file can't get
 *     Accessibility until this is allowed in Cachew's app info. Ticked by the user, or done
 *     automatically once Accessibility is on.
 *  2. Accessibility (checked)
 *  3. Work in the background (can't be checked: the user ticks it; text and button adapt to the
 *     phone maker, see PhoneSetup.openBackground)
 *  4. Notifications (checked)
 * Then a real test: open a shop. Shown in onboarding right after picking shops, and in Me.
 */
@Composable
fun SetupScreen(onboarding: Boolean, onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val brand = remember { PhoneSetup.brand }

    // Re-checked every time the user comes back from Settings.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumes++ }
    val service = remember(resumes) { ShopWatchService.state(context) }
    val notifications = remember(resumes) { PhoneSetup.notificationsOn(context) }
    var confirmed by remember { mutableStateOf(store.setupConfirmed) }
    val backgroundDone = PhoneSetup.STEP_BACKGROUND in confirmed
    // Restricted settings only exist on Android 13+; once Accessibility is on they were clearly allowed.
    val needsRestricted = Build.VERSION.SDK_INT >= 33
    val restrictedDone = service == ShopWatchService.State.On || PhoneSetup.STEP_RESTRICTED in confirmed
    var number = 0
    fun toggle(step: String) {
        val next = if (step in confirmed) confirmed - step else confirmed + step
        store.setupConfirmed = next
        confirmed = next
    }

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { resumes++ }

    CachewBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = CachewSize.ScreenPadding),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(16.dp))
                if (onboarding) {
                    OnboardingProgress(done = 4, total = 4)
                    Spacer(Modifier.height(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.setup_title), style = CachewType.H1, color = CachewColors.Title)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.setup_body), style = CachewType.Body, color = CachewColors.TextSecondary)
                    }
                    Penny(PennyPose.Determined, 96.dp)
                }
                Spacer(Modifier.height(18.dp))

                // 1. Allow restricted settings (Android 13+)
                if (needsRestricted) {
                    StepCard(
                        number = ++number,
                        done = restrictedDone,
                        title = stringResource(R.string.setup_restricted_title),
                        body = stringResource(R.string.setup_restricted_body),
                        hint = stringResource(R.string.setup_restricted_hint),
                        actions = listOf(stringResource(R.string.setup_open_settings) to { PhoneSetup.openAppInfo(context) }),
                        confirm = if (service == ShopWatchService.State.On) null else
                            stringResource(if (PhoneSetup.STEP_RESTRICTED in confirmed) R.string.setup_undo else R.string.setup_did_it) to { toggle(PhoneSetup.STEP_RESTRICTED) },
                    )
                }

                // 2. Accessibility
                StepCard(
                    number = ++number,
                    done = service == ShopWatchService.State.On,
                    title = stringResource(R.string.setup_access_title),
                    body = stringResource(
                        when (service) {
                            ShopWatchService.State.On -> R.string.setup_access_on
                            ShopWatchService.State.Stuck -> R.string.permission_stuck
                            ShopWatchService.State.Off -> R.string.setup_access_off
                        }
                    ),
                    hint = stringResource(if (service == ShopWatchService.State.Off && needsRestricted) R.string.setup_access_grey else R.string.setup_access_privacy),
                    actions = if (service == ShopWatchService.State.On) emptyList() else
                        listOf(stringResource(R.string.setup_open) to { PhoneSetup.openAccessibility(context) }),
                )

                // 3. Background (phone maker's switches; the user confirms)
                StepCard(
                    number = ++number,
                    done = backgroundDone,
                    title = stringResource(R.string.setup_background_title),
                    body = stringResource(
                        when (brand) {
                            PhoneSetup.Brand.Xiaomi -> R.string.setup_background_xiaomi
                            PhoneSetup.Brand.Huawei -> R.string.setup_background_huawei
                            PhoneSetup.Brand.Oppo -> R.string.setup_background_oppo
                            PhoneSetup.Brand.Vivo -> R.string.setup_background_vivo
                            PhoneSetup.Brand.Samsung -> R.string.setup_background_samsung
                            PhoneSetup.Brand.Other -> R.string.setup_background_other
                        }
                    ),
                    hint = stringResource(R.string.setup_background_why),
                    actions = buildList {
                        add(stringResource(R.string.setup_open) to { PhoneSetup.openBackground(context) })
                        if (brand == PhoneSetup.Brand.Xiaomi) add(stringResource(R.string.setup_autostart) to { PhoneSetup.openAutostart(context) })
                    },
                    confirm = stringResource(if (backgroundDone) R.string.setup_undo else R.string.setup_did_it) to { toggle(PhoneSetup.STEP_BACKGROUND) },
                )

                // 4. Notifications
                StepCard(
                    number = ++number,
                    done = notifications,
                    title = stringResource(R.string.setup_notif_title),
                    body = stringResource(R.string.setup_notif_body),
                    actions = if (notifications) emptyList() else listOf(
                        stringResource(R.string.setup_allow) to {
                            // First time: Android's own dialog. After a "no", only Settings can change it.
                            if (Build.VERSION.SDK_INT >= 33 && !store.notificationsAsked) {
                                store.notificationsAsked = true
                                askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                PhoneSetup.openNotificationSettings(context)
                            }
                        }
                    ),
                )

                // The real test.
                GlassCard(Modifier.fillMaxWidth(), selected = true) {
                    Text(stringResource(R.string.setup_test_title), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title)
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.setup_test_body), style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
                }
                Spacer(Modifier.height(16.dp))
            }
            PrimaryButton(
                text = stringResource(if (onboarding) R.string.permission_continue else R.string.setup_done),
                onClick = onDone,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** One numbered step: title, explanation, buttons, and a ✓ when it's done. */
@Composable
private fun StepCard(
    number: Int,
    done: Boolean,
    title: String,
    body: String,
    hint: String? = null,
    actions: List<Pair<String, () -> Unit>> = emptyList(),
    confirm: Pair<String, () -> Unit>? = null,
) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepBadge(number, done)
            Spacer(Modifier.width(12.dp))
            Text(title, style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text(body, style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.Text)
        if (hint != null && !done) {
            Spacer(Modifier.height(6.dp))
            Text(hint, style = CachewType.Body.copy(fontSize = 13.sp), color = CachewColors.TextSecondary)
        }
        if (actions.isNotEmpty() || confirm != null) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { (label, onClick) -> SmallButton(label, filled = true, onClick = onClick) }
                confirm?.let { (label, onClick) -> SmallButton(label, filled = false, onClick = onClick) }
            }
        }
    }
}

@Composable
private fun StepBadge(number: Int, done: Boolean) {
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (done) CachewColors.Purple else Color.Transparent)
            .border(1.5.dp, if (done) CachewColors.PurpleLight else CachewColors.CardBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (done) "✓" else "$number", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CachewColors.Title)
    }
}

@Composable
private fun SmallButton(label: String, filled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Text(
        label,
        fontFamily = Inter,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = if (filled) CachewColors.Title else CachewColors.PurpleText,
        modifier = Modifier
            .clip(shape)
            .background(if (filled) CachewColors.Purple.copy(alpha = 0.55f) else Color.Transparent)
            .border(1.dp, if (filled) CachewColors.PurpleLight.copy(alpha = 0.6f) else CachewColors.CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
