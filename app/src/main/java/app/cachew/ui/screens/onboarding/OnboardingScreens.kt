package app.cachew.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cachew.R
import app.cachew.data.Money
import app.cachew.data.Quiz
import app.cachew.data.Store
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.OnboardingProgress
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.components.PrimaryButton
import app.cachew.ui.components.TextAction
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * First-run onboarding (design-v2/onboarding/): Welcome → facts → About you → quiz (2 questions)
 * → result → Meet Penny → How it works → then the existing shop picker, permission and night
 * shield screens. Answers are stored on the phone only (Store.profile).
 */

/** Common layout: scrollable content and the main button pinned at the bottom. */
@Composable
private fun OnboardingLayout(
    button: String,
    onButton: () -> Unit,
    buttonEnabled: Boolean = true,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    CachewBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = CachewSize.ScreenPadding),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), content = content)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(text = button, onClick = onButton, enabled = buttonEnabled)
            if (secondary != null) TextAction(secondary, onSecondary) else Spacer(Modifier.height(16.dp))
        }
    }
}

// ---- 01 Welcome -------------------------------------------------------------------------

@Composable
fun WelcomeScreen(onStart: () -> Unit) {
    OnboardingLayout(button = stringResource(R.string.ob_welcome_start), onButton = onStart) {
        Spacer(Modifier.height(48.dp))
        Penny(PennyPose.BigCurl, 280.dp, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name), style = CachewType.H1.copy(fontSize = 48.sp, fontWeight = FontWeight.Black), color = CachewColors.Title, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(stringResource(R.string.ob_welcome_tagline), style = CachewType.Body.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold), color = CachewColors.PurpleText, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.ob_welcome_body), style = CachewType.Body.copy(fontSize = 16.sp), color = CachewColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ---- 02 Facts ---------------------------------------------------------------------------

@Composable
fun FactsScreen(onNext: () -> Unit) {
    OnboardingLayout(button = stringResource(R.string.ob_facts_button), onButton = onNext) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ob_facts_title), style = CachewType.H1, color = CachewColors.Title, modifier = Modifier.weight(1f))
            Penny(PennyPose.Love, 96.dp)
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.ob_facts_body), style = CachewType.Body, color = CachewColors.TextSecondary)
        Spacer(Modifier.height(16.dp))
        val numbers = stringArrayResource(R.array.ob_fact_numbers)
        val texts = stringArrayResource(R.array.ob_fact_texts)
        val sources = stringArrayResource(R.array.ob_fact_sources)
        numbers.indices.forEach { i ->
            GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(numbers[i], fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = CachewColors.Gold, modifier = Modifier.width(72.dp))
                    Column(Modifier.weight(1f)) {
                        Text(texts[i], style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp), color = CachewColors.Title)
                        Text(sources[i], style = CachewType.Body.copy(fontSize = 12.sp), color = CachewColors.TextSecondary)
                    }
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth(), selected = true, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.ob_facts_good_news), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title, textAlign = TextAlign.Center)
            Text(stringResource(R.string.ob_facts_good_news_body), style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
    }
}

// ---- 03 About you (optional) ------------------------------------------------------------

/** One "About you" question: key in Store.profile, option ids and their labels (strings.xml arrays). */
private class Field(val key: String, val title: Int, val ids: List<String>, val labels: Int)

private val ABOUT_FIELDS = listOf(
    Field("age", R.string.ob_about_age, listOf("18_24", "25_34", "35_44", "45_plus"), R.array.ob_about_age_options),
    Field("gender", R.string.ob_about_gender, listOf("woman", "man", "non_binary", "no_say"), R.array.ob_about_gender_options),
    Field("category", R.string.ob_about_category, listOf("clothes", "beauty", "home", "gadgets", "food"), R.array.ob_about_category_options),
    Field("source", R.string.ob_about_source, listOf("tiktok", "instagram", "reddit", "friend", "app_store"), R.array.ob_about_source_options),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutYouScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    // One string "key=id;key=id" so it survives rotation (rememberSaveable needs simple types).
    var answersRaw by rememberSaveable { mutableStateOf(store.profile.filterKeys { k -> ABOUT_FIELDS.any { it.key == k } }.entries.joinToString(";") { "${it.key}=${it.value}" }) }
    val answers = answersRaw.split(";").filter { "=" in it }.associate { it.substringBefore("=") to it.substringAfter("=") }

    OnboardingLayout(
        button = stringResource(R.string.questions_continue),
        onButton = {
            store.profile = store.profile + answers
            onNext()
        },
        secondary = stringResource(R.string.goal_skip),
        onSecondary = onNext,
    ) {
        Spacer(Modifier.height(16.dp))
        OnboardingProgress(done = 1, total = 4)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.ob_about_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ob_about_title), style = CachewType.H1, color = CachewColors.Title)
        Text(stringResource(R.string.ob_about_body), style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
        ABOUT_FIELDS.forEach { field ->
            Spacer(Modifier.height(18.dp))
            Text(stringResource(field.title), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = CachewColors.Title)
            Spacer(Modifier.height(8.dp))
            val labels = stringArrayResource(field.labels)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                field.ids.forEachIndexed { i, id ->
                    OptionChip(labels[i], selected = answers[field.key] == id) {
                        answersRaw = (answers + (field.key to id)).entries.joinToString(";") { "${it.key}=${it.value}" }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OptionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) CachewColors.Purple.copy(alpha = 0.25f) else CachewColors.CardFill)
            .border(if (selected) CachewSize.BorderWidthSelected else CachewSize.BorderWidth, if (selected) CachewColors.SelectedBorder else CachewColors.CardBorder, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(text, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = CachewColors.Text)
    }
}

// ---- 04/05 Quiz ------------------------------------------------------------------------

/** Quiz question [number] of 2: one answer, saved under [key] in Store.profile. */
@Composable
fun QuizScreen(number: Int, onNext: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val key = if (number == 1) "when" else "spend"
    val ids = if (number == 1) Quiz.WHEN else Quiz.SPEND_PER_MONTH.keys.toList()
    val labels = stringArrayResource(if (number == 1) R.array.ob_quiz_when_options else R.array.ob_quiz_spend_options)
    var picked by rememberSaveable { mutableStateOf(store.profile[key]) }

    OnboardingLayout(
        button = stringResource(R.string.ob_quiz_next),
        buttonEnabled = picked != null,
        onButton = {
            store.profile = store.profile + (key to picked!!)
            onNext()
        },
    ) {
        Spacer(Modifier.height(16.dp))
        OnboardingProgress(done = number, total = 4)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.ob_quiz_label, number, 2).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(if (number == 1) R.string.ob_quiz_when else R.string.ob_quiz_spend), style = CachewType.H1, color = CachewColors.Title)
        Spacer(Modifier.height(20.dp))
        ids.forEachIndexed { i, id ->
            val selected = picked == id
            GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap), selected = selected, onClick = { picked = id }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(labels[i], style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title, modifier = Modifier.weight(1f))
                    RadioDot(selected)
                }
            }
        }
    }
}

/** Empty ring, or a purple circle with a check mark when chosen. */
@Composable
private fun RadioDot(selected: Boolean) {
    Box(
        Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(if (selected) CachewColors.Purple else Color.Transparent)
            .border(1.5.dp, if (selected) CachewColors.PurpleLight else CachewColors.CardBorder, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Text("✓", fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CachewColors.Title)
    }
}

// ---- 06 Result -------------------------------------------------------------------------

@Composable
fun QuizResultScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    val profile = remember { Store.get(context).profile }
    val yearly = Quiz.yearlyCost(profile["spend"])
    val peak = Quiz.peakBar(profile["when"])
    val whenIndex = Quiz.WHEN.indexOf(profile["when"]).coerceAtLeast(0)

    OnboardingLayout(button = stringResource(R.string.ob_result_button), onButton = onNext) {
        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.ob_result_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
        Spacer(Modifier.height(6.dp))
        // "Impulse buys cost you about $1,200 a year", the amount in purple.
        val amount = Money.format(yearly.toDouble(), Money.DEFAULT_CURRENCY)
        val title = stringResource(R.string.ob_result_title, amount)
        Text(
            buildAnnotatedString {
                val at = title.indexOf(amount)
                append(title.substring(0, at.coerceAtLeast(0)))
                withStyle(SpanStyle(color = CachewColors.PurpleLight)) { append(amount) }
                if (at >= 0) append(title.substring(at + amount.length))
            },
            style = CachewType.H1,
            color = CachewColors.Title,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringArrayResource(R.array.ob_result_when)[whenIndex] + " " + stringResource(R.string.ob_result_guard),
            style = CachewType.Body,
            color = CachewColors.TextSecondary,
        )
        Spacer(Modifier.height(20.dp))
        // Illustration of the risky window: the bar for the answer to question 1 is tall and purple.
        GlassCard(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                Quiz.CHART_HOURS.forEachIndexed { i, hour ->
                    val tall = i == peak
                    val height = if (tall) 1f else listOf(0.22f, 0.3f, 0.38f, 0.33f, 0.3f)[i]
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(height)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (tall) Brush.verticalGradient(listOf(CachewColors.PurpleSoft, CachewColors.Purple))
                                        else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.1f)))
                                    ),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("h a", Locale.US)),
                            fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = CachewColors.TextSecondary,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(CachewSize.Gap))
        GlassCard(Modifier.fillMaxWidth()) {
            Row {
                Text(stringResource(R.string.ob_result_kept), style = CachewType.Body.copy(fontWeight = FontWeight.Bold), color = CachewColors.Title, modifier = Modifier.weight(1f))
                Text(stringArrayResource(R.array.ob_result_things)[Quiz.keptThing(yearly)], style = CachewType.Body.copy(fontWeight = FontWeight.Bold), color = CachewColors.Gold)
            }
        }
    }
}

// ---- 07 Meet Penny ---------------------------------------------------------------------

@Composable
fun MeetPennyScreen(onNext: () -> Unit) {
    OnboardingLayout(button = stringResource(R.string.ob_penny_button), onButton = onNext) {
        Spacer(Modifier.height(24.dp))
        Penny(PennyPose.Wave, 220.dp, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.ob_penny_title), style = CachewType.H1.copy(fontSize = 34.sp), color = CachewColors.Title, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.ob_penny_body), style = CachewType.Body, color = CachewColors.TextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        PennyRow(R.drawable.ic_flame, R.string.ob_penny_streak, R.string.ob_penny_streak_body)
        PennyRow(R.drawable.ic_cashew, R.string.ob_penny_cashews, R.string.ob_penny_cashews_body)
        PennyRow(R.drawable.ic_heart, R.string.ob_penny_hearts, R.string.ob_penny_hearts_body)
    }
}

@Composable
private fun PennyRow(icon: Int, title: Int, body: Int) {
    GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(stringResource(title), style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp), color = CachewColors.Title)
                Text(stringResource(body), style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
            }
        }
    }
}

// ---- 08 How it works --------------------------------------------------------------------

@Composable
fun HowItWorksScreen(onNext: () -> Unit) {
    val context = LocalContext.current
    val hours = remember { Store.get(context).defaultWaitHours }
    OnboardingLayout(button = stringResource(R.string.ob_how_button), onButton = onNext) {
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ob_how_label).uppercase(), style = CachewType.Label, color = CachewColors.PurpleText)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.ob_how_title), style = CachewType.H1, color = CachewColors.Title)
            }
            Penny(PennyPose.Wave, 100.dp)
        }
        Spacer(Modifier.height(18.dp))
        val titles = stringArrayResource(R.array.ob_how_steps)
        val bodies = listOf(
            stringResource(R.string.ob_how_step1),
            stringResource(R.string.ob_how_step2),
            stringResource(R.string.ob_how_step3, hours),
            stringResource(R.string.ob_how_step4),
        )
        titles.forEachIndexed { i, title ->
            GlassCard(Modifier.fillMaxWidth().padding(bottom = CachewSize.Gap)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(Brush.linearGradient(listOf(CachewColors.PurpleLight, CachewColors.Purple))),
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", fontFamily = Inter, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = CachewColors.Title) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(title, style = CachewType.Body.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp), color = CachewColors.Title)
                        Text(bodies[i], style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
                    }
                }
            }
        }
        GlassCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_heart), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.ob_how_heart), style = CachewType.Body.copy(fontWeight = FontWeight.Bold), color = CachewColors.Title, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.ob_how_heart_count), style = CachewType.Body.copy(fontSize = 14.sp), color = CachewColors.TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}
