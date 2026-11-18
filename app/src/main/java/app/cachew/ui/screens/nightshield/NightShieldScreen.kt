package app.cachew.ui.screens.nightshield

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.NightShield
import app.cachew.data.Store
import app.cachew.reminders.Reminders
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.ChoiceLabel
import app.cachew.ui.components.ChoiceTile
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.OnboardingProgress
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Screen 10: when the night shield runs (from → to, 24 h pickers), on/off, Soft or Strict.
 * Nothing is saved until "Save"; then all waiting reminders are re-planned for the new quiet hours.
 */
@Composable
fun NightShieldScreen(onboarding: Boolean, onSaved: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val initial = remember { store.nightShield }

    // Times kept as minutes since midnight so rememberSaveable can store them.
    var fromMin by rememberSaveable { mutableStateOf(initial.from.hour * 60 + initial.from.minute) }
    var toMin by rememberSaveable { mutableStateOf(initial.to.hour * 60 + initial.to.minute) }
    var on by rememberSaveable { mutableStateOf(initial.on) }
    var strict by rememberSaveable { mutableStateOf(initial.strict) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) } // "from" / "to" / null

    val from = LocalTime.of(fromMin / 60, fromMin % 60)
    val to = LocalTime.of(toMin / 60, toMin % 60)
    val valid = NightShield.isValid(from, to)

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
                        Text(stringResource(R.string.shield_title), style = CachewType.H1.copy(fontSize = 34.sp), color = CachewColors.Title)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.shield_subtitle), style = CachewType.Body.copy(fontSize = 17.sp), color = CachewColors.TextSecondary)
                    }
                    Penny(PennyPose.Sleep, 120.dp)
                }
                Spacer(Modifier.height(24.dp))

                // from → to card; each time opens a 24 h picker.
                GlassCard(Modifier.fillMaxWidth(), selected = on, horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(vertical = 24.dp, horizontal = 12.dp)) {
                    Image(painterResource(R.drawable.ic_moon), contentDescription = null, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        TimeBlock(from.format(HHMM), stringResource(R.string.shield_from)) { picking = "from" }
                        Text("→", fontFamily = Inter, fontSize = 26.sp, color = CachewColors.TextSecondary)
                        TimeBlock(to.format(HHMM), stringResource(R.string.shield_to)) { picking = "to" }
                    }
                }
                Spacer(Modifier.height(CachewSize.Gap))

                GlassCard(Modifier.fillMaxWidth(), onClick = { on = !on }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.shield_on), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp), color = CachewColors.Title, modifier = Modifier.weight(1f))
                        Switch(
                            checked = on,
                            onCheckedChange = { on = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = CachewColors.Purple,
                                checkedThumbColor = CachewColors.Title,
                                uncheckedTrackColor = CachewColors.CardFill,
                                uncheckedBorderColor = CachewColors.CardBorder,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(CachewSize.Gap))

                // Modes only matter while the shield is on.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.alpha(if (on) 1f else 0.4f)) {
                    ChoiceTile(selected = !strict, onClick = { strict = false }, modifier = Modifier.weight(1f)) {
                        ChoiceLabel(stringResource(R.string.shield_soft))
                    }
                    ChoiceTile(selected = strict, onClick = { strict = true }, modifier = Modifier.weight(1f)) {
                        ChoiceLabel(stringResource(R.string.shield_strict))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(
                        when {
                            !on -> R.string.shield_off_caption
                            strict -> R.string.shield_strict_caption
                            else -> R.string.shield_soft_caption
                        }
                    ),
                    style = CachewType.Body,
                    color = CachewColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
                if (!valid) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.shield_same_time), style = CachewType.Body.copy(fontWeight = FontWeight.SemiBold), color = CachewColors.Heart, modifier = Modifier.padding(horizontal = 6.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            PrimaryButton(
                text = stringResource(R.string.shield_save),
                enabled = valid,
                onClick = {
                    store.nightShield = NightShield(on = on, from = from, to = to, strict = strict)
                    Reminders.rescheduleAll(context) // quiet hours may have moved
                    onSaved()
                },
            )
            Spacer(Modifier.height(16.dp))
        }
    }

    picking?.let { which ->
        TimeDialog(
            title = stringResource(if (which == "from") R.string.shield_pick_from else R.string.shield_pick_to),
            initialMinutes = if (which == "from") fromMin else toMin,
            onDismiss = { picking = null },
            onConfirm = { minutes ->
                if (which == "from") fromMin = minutes else toMin = minutes
                picking = null
            },
        )
    }
}


@Composable
private fun TimeBlock(time: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(time, fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 50.sp, letterSpacing = (-1.5).sp, color = CachewColors.Title)
        Text(label, style = CachewType.Body.copy(fontSize = 16.sp), color = CachewColors.TextSecondary)
    }
}

/** Material3 TimePicker (24 h) in a dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CachewColors.BgMid,
        titleContentColor = CachewColors.Title,
        title = { Text(title) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = CachewColors.CardFill,
                    selectorColor = CachewColors.Purple,
                    timeSelectorSelectedContainerColor = CachewColors.Purple.copy(alpha = 0.35f),
                    timeSelectorUnselectedContainerColor = CachewColors.CardFill,
                    timeSelectorSelectedContentColor = CachewColors.Title,
                    timeSelectorUnselectedContentColor = CachewColors.Text,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
