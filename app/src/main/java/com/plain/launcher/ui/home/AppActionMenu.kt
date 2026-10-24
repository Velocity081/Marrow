package com.plain.launcher.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plain.launcher.data.model.AppItem
import com.plain.launcher.ui.components.PlainListDivider
import com.plain.launcher.ui.components.PlainSearchField
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.util.AppIconManager
import com.plain.launcher.util.AppLauncher

@Composable
fun AnimatedVisibilityScope.AppActionMenu(
    app: AppItem,
    context: Context,
    onPinToggle: () -> Unit,
    onRename: () -> Unit,
    onDismiss: () -> Unit
) {
    val springSpec = spring<IntOffset>(
        dampingRatio = 0.72f,
        stiffness = Spring.StiffnessMediumLow
    )
    val fadeSpec = tween<Float>(durationMillis = 220, easing = LinearOutSlowInEasing)

    val enterAnim = if (PlainTheme.settings.enableAnimations) {
        slideInVertically(animationSpec = springSpec, initialOffsetY = { it }) +
            fadeIn(animationSpec = fadeSpec)
    } else {
        EnterTransition.None
    }

    val exitAnim = if (PlainTheme.settings.enableAnimations) {
        slideOutVertically(animationSpec = tween(200), targetOffsetY = { it }) +
            fadeOut(animationSpec = tween(180))
    } else {
        ExitTransition.None
    }

    val coloredIcon = remember(app.key) {
        AppIconManager.getColoredIcon(context, app)
    }

    // Flat backdrop overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlainTheme.colors.background.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .animateEnterExit(
                    enter = enterAnim,
                    exit = exitAnim
                )
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(PlainTheme.colors.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent click through */ }
                )
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Header with COLORED icon to the left of the name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                if (coloredIcon != null) {
                    Image(
                        bitmap = coloredIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    PlainText(
                        text = app.displayLabel,
                        color = PlainTheme.colors.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    PlainText(
                        text = app.packageName,
                        color = PlainTheme.colors.secondaryText,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            PlainListDivider(fraction = 1f)

            // Action: Pin / Unpin
            MenuTextAction(
                label = if (app.isFavorite) "unpin from home" else "pin to home",
                onClick = {
                    onDismiss()
                    onPinToggle()
                }
            )

            PlainListDivider(fraction = 1f)

            // Action: Rename
            MenuTextAction(
                label = "rename",
                onClick = {
                    onDismiss()
                    onRename()
                }
            )

            PlainListDivider(fraction = 1f)

            // Action: App Info
            MenuTextAction(
                label = "app info",
                onClick = {
                    onDismiss()
                    AppLauncher.openAppInfo(context, app.packageName)
                }
            )

            PlainListDivider(fraction = 1f)

            // Action: Delete (Red)
            MenuTextAction(
                label = "delete",
                onClick = {
                    onDismiss()
                    AppLauncher.uninstallApp(context, app.packageName)
                },
                color = Color(0xFFFF4444)
            )
        }
    }
}

@Composable
private fun MenuTextAction(
    label: String,
    onClick: () -> Unit,
    color: Color = PlainTheme.colors.onBackground
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
    ) {
        PlainText(
            text = label,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun RenameAppDialog(
    app: AppItem,
    onSave: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(app.customLabel ?: app.originalLabel) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlainTheme.colors.background.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(PlainTheme.colors.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* prevent click through */ }
                )
                .padding(24.dp)
        ) {
            PlainText(
                text = "rename app",
                color = PlainTheme.colors.onBackground,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            PlainText(
                text = "original: ${app.originalLabel}",
                color = PlainTheme.colors.secondaryText,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            PlainSearchField(
                query = text,
                onQueryChange = { text = it },
                hint = "new name..."
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!app.customLabel.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSave(null) }
                            )
                            .padding(8.dp)
                    ) {
                        PlainText(
                            text = "[reset]",
                            color = PlainTheme.colors.secondaryText,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                        .padding(8.dp)
                ) {
                    PlainText(
                        text = "[cancel]",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSave(text.trim()) }
                        )
                        .padding(8.dp)
                ) {
                    PlainText(
                        text = "[save]",
                        color = PlainTheme.colors.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
