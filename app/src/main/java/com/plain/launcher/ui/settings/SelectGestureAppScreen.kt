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
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.viewmodel.MainViewModel

@Composable
fun SelectGestureAppScreen(
    gestureKey: String,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allApps by viewModel.allApps.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val sortedApps = remember(allApps) {
        allApps.filterNot { it.isHidden }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayLabel })
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
                    text = "select app",
                    color = PlainTheme.colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sortedApps, key = { it.key }) { app ->
                    val isSelected = settings.gestureCustomApps[gestureKey] == app.packageName
                    val text = if (isSelected) "[${app.displayLabel}]" else app.displayLabel

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    val newMap = settings.gestureCustomApps.toMutableMap()
                                    newMap[gestureKey] = app.packageName
                                    viewModel.updateSettings(settings.copy(gestureCustomApps = newMap))
                                    viewModel.navigateBack()
                                }
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        PlainText(
                            text = text,
                            color = if (isSelected) PlainTheme.colors.onBackground else PlainTheme.colors.secondaryText,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
