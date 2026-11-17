package app.cachew.ui.screens.permission

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.cachew.R
import app.cachew.service.ShopWatchService
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.OnboardingProgress
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/**
 * Screen 09: explains what Cachew can and can't see, then sends the user to
 * Settings → Accessibility. When they come back (ON_RESUME) we re-check whether the
 * service is on and switch the button to "Continue".
 */
@Composable
fun PermissionScreen(onboarding: Boolean, onContinue: () -> Unit) {
    val context = LocalContext.current
    var enabled by rememberSaveable { mutableStateOf(ShopWatchService.isEnabled(context)) }
    var triedSettings by rememberSaveable { mutableStateOf(false) }
    var showWhy by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        enabled = ShopWatchService.isEnabled(context)
    }

    CachewBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = CachewSize.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(16.dp))
                if (onboarding) OnboardingProgress(done = 4, total = 4) else Spacer(Modifier.height(5.dp))
                Spacer(Modifier.height(56.dp))
                ShieldBadge()
                Spacer(Modifier.height(28.dp))
                Text(
                    stringResource(R.string.permission_title),
                    style = CachewType.H1,
                    color = CachewColors.Title,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                InfoCard(stringResource(R.string.permission_see_title), stringResource(R.string.permission_see_body))
                Spacer(Modifier.height(CachewSize.Gap))
                InfoCard(stringResource(R.string.permission_never_title), stringResource(R.string.permission_never_body))
                if (enabled) {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "✓  " + stringResource(R.string.permission_is_on),
                        style = CachewType.Body.copy(fontWeight = FontWeight.SemiBold),
                        color = CachewColors.PurpleText,
                    )
                } else if (triedSettings) {
                    // Back from Settings but still off: most likely Android 13+ "restricted settings".
                    Spacer(Modifier.height(20.dp))
                    Text(
                        stringResource(R.string.permission_restricted_hint),
                        style = CachewType.Body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = CachewColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            if (enabled) {
                PrimaryButton(text = stringResource(R.string.permission_continue), onClick = onContinue)
            } else {
                PrimaryButton(
                    text = stringResource(R.string.permission_allow),
                    onClick = {
                        triedSettings = true
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                )
            }
            Text(
                text = stringResource(R.string.permission_why),
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = CachewColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showWhy = true }
                    .padding(vertical = 16.dp),
            )
        }
    }

    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            confirmButton = { TextButton(onClick = { showWhy = false }) { Text(stringResource(R.string.ok)) } },
            title = { Text(stringResource(R.string.permission_why_title)) },
            text = { Text(stringResource(R.string.permission_why_body)) },
            containerColor = CachewColors.BgMid,
            titleContentColor = CachewColors.Title,
            textContentColor = CachewColors.Text,
        )
    }
}

@Composable
private fun ShieldBadge() {
    val shape = RoundedCornerShape(30.dp)
    Box(
        Modifier
            .size(96.dp)
            .shadow(24.dp, shape, ambientColor = CachewColors.Purple, spotColor = CachewColors.Purple)
            .clip(shape)
            .background(CachewColors.NightTop)
            .border(CachewSize.BorderWidthSelected, CachewColors.PurpleSoft.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_shield_check), contentDescription = null, modifier = Modifier.size(40.dp))
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    GlassCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 18.dp)) {
        Text(title, style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title)
        Spacer(Modifier.height(4.dp))
        Text(body, style = CachewType.Body, color = CachewColors.TextSecondary)
    }
}
