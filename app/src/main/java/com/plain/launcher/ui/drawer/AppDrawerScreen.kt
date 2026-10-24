package com.plain.launcher.ui.drawer

import android.content.Context
import android.content.ClipboardManager
import android.content.ClipData
import com.plain.launcher.util.MathEvaluator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.ui.components.PlainAppRow
import com.plain.launcher.ui.components.PlainSearchField
import com.plain.launcher.ui.components.PlainText
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.plain.launcher.ui.components.MagnifyingGlassIcon
import com.plain.launcher.ui.navigation.Screen
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.util.AppLauncher
import com.plain.launcher.util.HapticUtil
import com.plain.launcher.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppDrawerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectedAppForMenu by viewModel.selectedAppForMenu.collectAsState()
    val renameApp by viewModel.renameApp.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    val mathResult = remember(searchQuery, settings.enableWebLookup) {
        if (settings.enableWebLookup) MathEvaluator.evaluate(searchQuery) else null
    }

    // Auto-open keyboard if enabled
    LaunchedEffect(Unit) {
        if (settings.autoOpenKeyboardInSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Auto-launch if exactly one search result remains and query is not empty
    LaunchedEffect(searchResults, searchQuery, settings.autoLaunchSingleSearchResult) {
        if (settings.autoLaunchSingleSearchResult && searchQuery.isNotBlank() && searchResults.size == 1) {
            val single = searchResults.first()
            viewModel.setSearchQuery("")
            viewModel.onHomePressed()
            viewModel.launchApp(context, single)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlainTheme.colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header with navigation actions and search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                viewModel.setSearchQuery("")
                                viewModel.navigateBack()
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    PlainText(
                        text = "< back",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                viewModel.navigateTo(Screen.Settings)
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    PlainText(
                        text = "settings >",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = 14.sp
                    )
                }
            }

            // Search input field + Web search pill button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PlainSearchField(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            if (searchResults.isNotEmpty()) {
                                viewModel.launchApp(context, searchResults.first())
                            }
                        }
                    )
                )

                if (settings.enableWebLookup) {
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .width(72.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color(0xFFFFD600))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    HapticUtil.performClick(context, settings.enableHapticFeedback)
                                    keyboardController?.hide()
                                    AppLauncher.openWebSearch(context, searchQuery)
                                    viewModel.setSearchQuery("")
                                    viewModel.navigateBack()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MagnifyingGlassIcon(color = Color.Black)
                    }
                }
            }

            if (settings.enableWebLookup && mathResult != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                HapticUtil.performClick(context, settings.enableHapticFeedback)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = ClipData.newPlainText("Math Result", mathResult.result)
                                clipboard?.setPrimaryClip(clip)
                            }
                        )
                ) {
                    PlainText(
                        text = "${mathResult.caption} =",
                        color = PlainTheme.colors.secondaryText.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    PlainText(
                        text = mathResult.result,
                        color = Color(0xFFFFD600),
                        fontSize = 56.sp,
                        lineHeight = 60.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main app list + Fast-scroll index
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (searchResults.isEmpty()) {
                    if (mathResult == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            PlainText(
                                text = "no matching apps",
                                color = PlainTheme.colors.secondaryText
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 28.dp)
                    ) {
                        itemsIndexed(
                            items = searchResults,
                            key = { _, item -> item.key }
                        ) { _, app ->
                            PlainAppRow(
                                label = app.displayLabel,
                                settings = settings,
                                app = app,
                                onClick = {
                                    keyboardController?.hide()
                                    viewModel.launchApp(context, app)
                                },
                                onLongClick = {
                                    keyboardController?.hide()
                                    viewModel.openAppMenu(app)
                                }
                            )
                        }
                    }

                    // Fast scroll letter index on right edge (when not searching)
                    if (searchQuery.isBlank()) {
                        FastScrollIndex(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            onLetterSelected = { letter ->
                                coroutineScope.launch {
                                    val targetIndex = if (letter == "#") {
                                        searchResults.indexOfFirst {
                                            !it.displayLabel.first().isLetter()
                                        }
                                    } else {
                                        searchResults.indexOfFirst {
                                            it.displayLabel.startsWith(letter, ignoreCase = true)
                                        }
                                    }
                                    if (targetIndex >= 0) {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
