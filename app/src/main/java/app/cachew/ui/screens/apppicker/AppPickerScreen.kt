package app.cachew.ui.screens.apppicker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cachew.R
import app.cachew.data.InstalledApp
import app.cachew.data.InstalledApps
import app.cachew.data.Store
import app.cachew.ui.components.CachewBackground
import app.cachew.ui.components.GlassCard
import app.cachew.ui.components.Penny
import app.cachew.ui.components.PennyPose
import app.cachew.ui.theme.CachewColors
import app.cachew.ui.theme.CachewSize
import app.cachew.ui.theme.CachewType
import app.cachew.ui.theme.Inter

/**
 * "+ Other app": every launchable app on the phone (icon + name, A–Z, search on top, Cachew excluded).
 * Tapping a row selects that app in [Store] and returns to the grid, where it appears as a ticked tile.
 * No mockup exists for this screen, so it reuses the day background and glass cards.
 */
@Composable
fun AppPickerScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val store = remember { Store.get(context) }
    val selected by store.selectedPackages.collectAsStateWithLifecycle()

    // null while loading; icons are decoded on a background thread.
    val apps by produceState<List<InstalledApp>?>(null) { value = InstalledApps.loadAll(context) }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = apps?.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }

    BackHandler(onBack = onDone)

    CachewBackground {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .padding(horizontal = CachewSize.ScreenPadding),
        ) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "←",
                    fontFamily = Inter,
                    fontSize = 24.sp,
                    color = CachewColors.Text,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.back), onClick = onDone)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.app_picker_title), style = CachewType.H2, color = CachewColors.Title)
            }
            Spacer(Modifier.height(16.dp))
            SearchField(query = query, onQueryChange = { query = it })
            Spacer(Modifier.height(CachewSize.Gap))

            when {
                visible == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Penny(PennyPose.Run, 140.dp)
                }
                visible.isEmpty() -> Text(
                    stringResource(R.string.app_picker_empty),
                    style = CachewType.Body,
                    color = CachewColors.TextSecondary,
                    modifier = Modifier.padding(top = 24.dp),
                )
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    items(visible, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            isSelected = app.packageName in selected,
                            onClick = {
                                store.setSelected(app.packageName, true)
                                onDone()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = CachewType.Body.copy(color = CachewColors.Text),
        cursorBrush = SolidColor(CachewColors.PurpleLight),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(CachewColors.CardFill)
                    .border(CachewSize.BorderWidth, CachewColors.CardBorder, shape)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (query.isEmpty()) {
                    Text(stringResource(R.string.app_picker_search), style = CachewType.Body, color = CachewColors.TextSecondary)
                }
                inner()
            }
        },
    )
}

@Composable
private fun AppRow(app: InstalledApp, isSelected: Boolean, onClick: () -> Unit) {
    GlassCard(
        selected = isSelected,
        radius = 16.dp,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(app.icon, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
            Spacer(Modifier.width(14.dp))
            Text(
                text = app.label,
                style = CachewType.Body.copy(fontWeight = FontWeight.SemiBold),
                color = CachewColors.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isSelected) {
                Text(stringResource(R.string.app_picker_selected), style = CachewType.Label, color = CachewColors.PurpleText)
            }
        }
    }
}
