package app.cachew.ui.screens.me

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.Store
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TextAction
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * "What are you saving for?": a name and an amount (Tokyo, $900). Every "Let it go" moves it
 * closer. "Skip" leaves it for later. Reached goals are listed underneath.
 */
@Composable
fun GoalScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val current = remember { store.goal }
    val done = remember { store.goalsDone() }
    var name by rememberSaveable { mutableStateOf(current?.name.orEmpty()) }
    var amount by rememberSaveable { mutableStateOf(current?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty()) }
    val parsed = Money.parse(amount)
    val valid = name.isNotBlank() && parsed != null

    CachewBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = CachewSize.ScreenPadding),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.goal_title), style = CachewType.H1, color = CachewColors.Title, modifier = Modifier.weight(1f))
                    Penny(PennyPose.Sparkle, 100.dp)
                }
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.goal_body), style = CachewType.Body, color = CachewColors.TextSecondary)
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    name, { name = it },
                    label = { Text(stringResource(R.string.goal_name)) },
                    placeholder = { Text(stringResource(R.string.goal_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    amount, { amount = it },
                    label = { Text(stringResource(R.string.goal_amount) + " ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (done.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.goal_reached).uppercase(), style = CachewType.Label, color = CachewColors.Gold)
                    Spacer(Modifier.height(10.dp))
                    done.forEach { g ->
                        GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap)) {
                            Row {
                                Text("✓ " + g.name, style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title, modifier = Modifier.weight(1f))
                                Text(Money.format(g.amount, Money.DEFAULT_CURRENCY), style = CachewType.Body.copy(fontWeight = FontWeight.Bold), color = CachewColors.Gold)
                            }
                            g.doneAt?.let {
                                Text(
                                    Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)),
                                    style = CachewType.Body.copy(fontSize = 13.sp),
                                    color = CachewColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
            PrimaryButton(
                text = stringResource(R.string.goal_set),
                enabled = valid,
                onClick = {
                    store.setGoal(name.trim(), parsed!!)
                    onDone()
                },
            )
            TextAction(stringResource(R.string.goal_skip), onDone)
        }
    }
}
