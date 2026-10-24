package com.plain.launcher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
fun ReorderFavoritesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allApps by viewModel.allApps.collectAsState()
    val favorites = remember(allApps) {
        allApps.filter { it.isFavorite }.sortedBy { it.favoriteOrder }
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
                    text = "reorder favorites",
                    color = PlainTheme.colors.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (favorites.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    PlainText(
                        text = "no favorites pinned yet",
                        color = PlainTheme.colors.secondaryText
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(favorites, key = { _, item -> item.key }) { index, app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                PlainText(
                                    text = "${index + 1}. ${app.displayLabel}",
                                    color = PlainTheme.colors.onBackground,
                                    fontSize = 15.sp
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Move Up
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    val keys = favorites.map { it.key }.toMutableList()
                                                    val temp = keys[index]
                                                    keys[index] = keys[index - 1]
                                                    keys[index - 1] = temp
                                                    viewModel.reorderFavorites(keys)
                                                }
                                            )
                                            .padding(6.dp)
                                    ) {
                                        PlainText(
                                            text = "[up]",
                                            color = PlainTheme.colors.onBackground,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Move Down
                                if (index < favorites.size - 1) {
                                    Box(
                                        modifier = Modifier
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {
                                                    val keys = favorites.map { it.key }.toMutableList()
                                                    val temp = keys[index]
                                                    keys[index] = keys[index + 1]
                                                    keys[index + 1] = temp
                                                    viewModel.reorderFavorites(keys)
                                                }
                                            )
                                            .padding(6.dp)
                                    ) {
                                        PlainText(
                                            text = "[down]",
                                            color = PlainTheme.colors.onBackground,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                // Unpin
                                Box(
                                    modifier = Modifier
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { viewModel.togglePin(app) }
                                        )
                                        .padding(6.dp)
                                ) {
                                    PlainText(
                                        text = "[unpin]",
                                        color = PlainTheme.colors.secondaryText,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
