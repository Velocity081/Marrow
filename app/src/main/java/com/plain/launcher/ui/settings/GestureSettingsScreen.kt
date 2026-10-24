package com.plain.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plain.launcher.data.model.GestureAction
import com.plain.launcher.ui.components.PlainListDivider
import com.plain.launcher.ui.components.PlainSectionHeader
import com.plain.launcher.ui.components.PlainSettingsCard
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.components.PlainTextOptionSelector
import com.plain.launcher.ui.components.PlainTextRow
import com.plain.launcher.ui.navigation.Screen
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.viewmodel.MainViewModel

@Composable
fun GestureSettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val allApps by viewModel.allApps.collectAsState()

    var editingGesture by remember { mutableStateOf<String?>(null) }

    fun getAction(key: String): GestureAction {
        return when (key) {
            "swipe_up" -> settings.gestureSwipeUp
            "swipe_down" -> settings.gestureSwipeDown
            "swipe_left" -> settings.gestureSwipeLeft
            "swipe_right" -> settings.gestureSwipeRight
            "double_tap" -> settings.gestureDoubleTap
            else -> GestureAction.NONE
        }
    }

    fun updateAction(key: String, action: GestureAction) {
        val newSettings = when (key) {
            "swipe_up" -> settings.copy(gestureSwipeUp = action)
            "swipe_down" -> settings.copy(gestureSwipeDown = action)
            "swipe_left" -> settings.copy(gestureSwipeLeft = action)
            "swipe_right" -> settings.copy(gestureSwipeRight = action)
            "double_tap" -> settings.copy(gestureDoubleTap = action)
            else -> settings
        }
        viewModel.updateSettings(newSettings)
        if (action == GestureAction.OPEN_APP) {
            viewModel.navigateTo(Screen.SelectGestureApp(key))
        }
    }

    fun getAppLabel(key: String): String? {
        val pkg = settings.gestureCustomApps[key] ?: return null
        return allApps.firstOrNull { it.packageName == pkg }?.displayLabel ?: pkg
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.navigateBack() }
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

                PlainText(
                    text = "gestures",
                    color = PlainTheme.colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    PlainSectionHeader(title = "home gestures", isFirst = true)

                    val gestures = listOf(
                        "swipe_up" to "swipe up",
                        "swipe_down" to "swipe down",
                        "swipe_left" to "swipe left",
                        "swipe_right" to "swipe right",
                        "double_tap" to "double tap"
                    )

                    PlainSettingsCard {
                        gestures.forEachIndexed { index, (key, label) ->
                            val action = getAction(key)
                            val actionDesc = if (action == GestureAction.OPEN_APP) {
                                val appLabel = getAppLabel(key)
                                if (appLabel != null) "app: $appLabel" else "open app"
                            } else {
                                action.label
                            }

                            PlainTextRow(
                                title = label,
                                valueText = actionDesc,
                                onClick = {
                                    editingGesture = if (editingGesture == key) null else key
                                }
                            )

                            if (editingGesture == key) {
                                PlainTextOptionSelector(
                                    title = "choose action",
                                    options = GestureAction.values().toList(),
                                    selectedOption = action,
                                    onOptionSelected = { chosen ->
                                        updateAction(key, chosen)
                                        editingGesture = null
                                    },
                                    labelProvider = { it.label }
                                )
                            }

                            if (index < gestures.size - 1) {
                                PlainListDivider()
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
