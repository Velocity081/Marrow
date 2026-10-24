package com.plain.launcher.ui.drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.theme.PlainTheme

val AlphabetLetters = ('a'..'z').map { it.toString() } + "#"

@Composable
fun FastScrollIndex(
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        val totalHeightPx = constraints.maxHeight.toFloat()
        val letterCount = AlphabetLetters.size
        val letterHeightPx = totalHeightPx / letterCount

        fun selectFromY(y: Float) {
            val index = (y / letterHeightPx).toInt().coerceIn(0, letterCount - 1)
            onLetterSelected(AlphabetLetters[index])
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(28.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        selectFromY(offset.y)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        selectFromY(change.position.y)
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AlphabetLetters.forEach { letter ->
                PlainText(
                    text = letter,
                    color = PlainTheme.colors.secondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
