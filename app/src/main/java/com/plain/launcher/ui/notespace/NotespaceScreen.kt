package com.plain.launcher.ui.notespace

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.plain.launcher.data.model.NoteItem
import com.plain.launcher.ui.components.PlainText
import com.plain.launcher.ui.components.PlainTextToggle
import com.plain.launcher.ui.components.PlainSwitch
import com.plain.launcher.ui.theme.PlainTheme
import com.plain.launcher.ui.theme.getAppFontFamily
import com.plain.launcher.util.HapticUtil
import com.plain.launcher.util.NoteStore
import com.plain.launcher.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotespaceScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val notes by NoteStore.notesFlow.collectAsState()

    var showNoteWindow by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<NoteItem?>(null) }

    LaunchedEffect(Unit) {
        NoteStore.init(context)
    }

    val listState = rememberLazyListState()
    var currentNotes by remember(notes) { mutableStateOf(notes) }
    var draggedNoteId by remember { mutableStateOf<String?>(null) }
    var draggedOffset by remember { mutableFloatStateOf(0f) }

    // Keep currentNotes in sync when notesFlow updates while not dragging
    LaunchedEffect(notes) {
        if (draggedNoteId == null) {
            currentNotes = notes
        }
    }

    // Smooth auto-scroll when dragging near viewport top/bottom edges
    LaunchedEffect(draggedNoteId) {
        val currentId = draggedNoteId ?: return@LaunchedEffect
        while (true) {
            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == currentId }
            if (info != null) {
                val draggedTop = info.offset + draggedOffset
                val draggedBottom = draggedTop + info.size
                val viewportStart = listState.layoutInfo.viewportStartOffset
                val viewportEnd = listState.layoutInfo.viewportEndOffset
                val threshold = 90f // pixels
                val scrollDelta = when {
                    draggedTop < viewportStart + threshold -> {
                        -((viewportStart + threshold - draggedTop) / threshold).coerceIn(0f, 1f) * 16f
                    }
                    draggedBottom > viewportEnd - threshold -> {
                        ((draggedBottom - (viewportEnd - threshold)) / threshold).coerceIn(0f, 1f) * 16f
                    }
                    else -> 0f
                }
                if (scrollDelta != 0f) {
                    listState.scrollBy(scrollDelta)
                }
            }
            delay(16)
        }
    }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 36.dp.toPx() }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    val isAnyDragging = draggedNoteId != null

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlainTheme.colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(isAnyDragging) {
                if (!isAnyDragging) {
                    detectDragGestures(
                        onDragStart = { totalDragY = 0f },
                        onDragEnd = {
                            // Swipe up exits notespace back to home
                            if (totalDragY < -swipeThresholdPx) {
                                viewModel.navigateBack()
                            }
                        },
                        onDragCancel = { totalDragY = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            totalDragY += dragAmount.y
                        }
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with back navigation and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.navigateBack() }
                        )
                        .padding(vertical = 6.dp, horizontal = 2.dp)
                ) {
                    PlainText(
                        text = "< back",
                        color = PlainTheme.colors.secondaryText,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                PlainText(
                    text = "notespace",
                    color = PlainTheme.colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Clean board with notes (draggable & reorderable)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentNotes, key = { it.id }) { note ->
                    val isDraggingThis = note.id == draggedNoteId
                    NoteCard(
                        note = note,
                        clock24Hour = settings.clock24Hour,
                        isDragging = isDraggingThis,
                        dragOffset = if (isDraggingThis) draggedOffset else 0f,
                        modifier = if (isDraggingThis) Modifier else Modifier.animateItem(),
                        onClick = {
                            noteToEdit = note
                            showNoteWindow = true
                        },
                        onDragStart = {
                            draggedNoteId = note.id
                            draggedOffset = 0f
                            HapticUtil.performClick(context, true)
                        },
                        onDrag = { dragAmount ->
                            val currentId = draggedNoteId ?: return@NoteCard
                            draggedOffset += dragAmount.y

                            val currentInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == currentId }
                            if (currentInfo != null) {
                                val fromIndex = currentNotes.indexOfFirst { it.id == currentId }
                                if (fromIndex != -1) {
                                    val draggedCenter = currentInfo.offset + currentInfo.size / 2f + draggedOffset

                                    val targetInfo = if (draggedOffset > 0) {
                                        // Dragging down: find the item below whose center we have crossed
                                        listState.layoutInfo.visibleItemsInfo
                                            .filter { it.key != currentId && it.offset > currentInfo.offset }
                                            .firstOrNull { target -> draggedCenter > target.offset + target.size / 2f }
                                    } else {
                                        // Dragging up: find the item above whose center we have crossed
                                        listState.layoutInfo.visibleItemsInfo
                                            .filter { it.key != currentId && it.offset < currentInfo.offset }
                                            .lastOrNull { target -> draggedCenter < target.offset + target.size / 2f }
                                    }

                                    if (targetInfo != null) {
                                        val toIndex = currentNotes.indexOfFirst { it.id == targetInfo.key }
                                        if (toIndex != -1 && toIndex != fromIndex) {
                                            val mutable = currentNotes.toMutableList()
                                            val item = mutable.removeAt(fromIndex)
                                            mutable.add(toIndex, item)
                                            currentNotes = mutable

                                            draggedOffset += (currentInfo.offset - targetInfo.offset)
                                            HapticUtil.performClick(context, true)
                                        }
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (draggedNoteId != null) {
                                NoteStore.reorderNotes(context, currentNotes)
                                HapticUtil.performClick(context, true)
                            }
                            draggedNoteId = null
                            draggedOffset = 0f
                        },
                        onDragCancel = {
                            draggedNoteId = null
                            draggedOffset = 0f
                            currentNotes = notes
                        },
                        onComplete = { noteId ->
                            NoteStore.deleteNote(context, noteId)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Rectangular rounded wide plus on the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(PlainTheme.colors.surface)
                    .clickable {
                        noteToEdit = null
                        showNoteWindow = true
                    },
                contentAlignment = Alignment.Center
            ) {
                PlainText(
                    text = "+",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = PlainTheme.colors.onBackground
                )
            }
        }

        // Note edit/create window dialog
        if (showNoteWindow) {
            NoteEditWindowDialog(
                context = context,
                existingNote = noteToEdit,
                selectedFont = settings.selectedFont,
                clock24Hour = settings.clock24Hour,
                onDismiss = {
                    showNoteWindow = false
                    noteToEdit = null
                },
                onSave = { note ->
                    NoteStore.saveNote(context, note)
                    showNoteWindow = false
                    noteToEdit = null
                },
                onDelete = { noteId ->
                    NoteStore.deleteNote(context, noteId)
                    showNoteWindow = false
                    noteToEdit = null
                }
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteItem,
    clock24Hour: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenWidthPx = with(LocalDensity.current) { 1000.dp.toPx() }
    val slideOffsetX = remember { Animatable(0f) }
    var isCompleting by remember { mutableStateOf(false) }

    val formattedTime = remember(note.timestamp, clock24Hour) {
        formatNoteTimestamp(note.timestamp, clock24Hour)
    }

    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    val currentOnDragCancel by rememberUpdatedState(onDragCancel)
    val currentOnComplete by rememberUpdatedState(onComplete)

    var isLongPressActive by remember { mutableStateOf(false) }
    var lastDragEndTime by remember { mutableLongStateOf(0L) }

    val isDark = PlainTheme.colors.background.luminance() < 0.5f
    val normalBg = PlainTheme.colors.surface
    val heldBg = if (isDark) {
        Color(
            red = (normalBg.red + 0.15f).coerceAtMost(1f),
            green = (normalBg.green + 0.15f).coerceAtMost(1f),
            blue = (normalBg.blue + 0.15f).coerceAtMost(1f),
            alpha = 1f
        )
    } else {
        Color.White
    }

    val cardBg by animateColorAsState(
        targetValue = if (isDragging) heldBg else normalBg,
        animationSpec = tween(durationMillis = 150),
        label = "noteCardBg"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer {
                translationX = slideOffsetX.value
                translationY = dragOffset
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
                shadowElevation = if (isDragging) 16f else 0f
                alpha = if (isCompleting) (1f - (slideOffsetX.value / (screenWidthPx * 0.7f))).coerceIn(0f, 1f) else 1f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .pointerInput(note.id) {
                if (!isCompleting) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            isLongPressActive = true
                            currentOnDragStart()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount)
                        },
                        onDragEnd = {
                            isLongPressActive = false
                            lastDragEndTime = System.currentTimeMillis()
                            currentOnDragEnd()
                        },
                        onDragCancel = {
                            isLongPressActive = false
                            lastDragEndTime = System.currentTimeMillis()
                            currentOnDragCancel()
                        }
                    )
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isCompleting && !isLongPressActive && System.currentTimeMillis() - lastDragEndTime > 300L) {
                    currentOnClick()
                }
            }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotBlank()) {
                    PlainText(
                        text = note.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PlainTheme.colors.onBackground,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.width(12.dp))

                PlainText(
                    text = formattedTime,
                    fontSize = 12.sp,
                    color = PlainTheme.colors.secondaryText.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Normal
                )
            }

            if (note.text.isNotBlank()) {
                if (note.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                }
                PlainText(
                    text = note.text,
                    fontSize = 14.sp,
                    color = PlainTheme.colors.secondaryText,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (note.isReminder) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.reminderTime != null) {
                        PlainText(
                            text = "reminder · " + formatReminderTimestamp(note.reminderTime, clock24Hour),
                            fontSize = 12.sp,
                            color = PlainTheme.colors.secondaryText,
                            fontWeight = FontWeight.Normal
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    // Circle outline button on the bottom right to complete
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .border(
                                width = 1.5.dp,
                                color = PlainTheme.colors.secondaryText.copy(alpha = 0.8f),
                                shape = CircleShape
                            )
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isCompleting) {
                                        isCompleting = true
                                        scope.launch {
                                            HapticUtil.performClick(context, true)
                                            slideOffsetX.animateTo(
                                                targetValue = screenWidthPx,
                                                animationSpec = tween(
                                                    durationMillis = 360,
                                                    easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
                                                )
                                            )
                                            currentOnComplete(note.id)
                                        }
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    }
                }
            }
        }
    }
}

private fun formatNoteTimestamp(timestamp: Long, clock24Hour: Boolean): String {
    val noteCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = Calendar.getInstance()

    val isToday = noteCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            noteCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val timePattern = if (clock24Hour) "HH:mm" else "h:mm a"

    return if (isToday) {
        SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("MMM d, $timePattern", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatReminderTimestamp(timestamp: Long, clock24Hour: Boolean): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = Calendar.getInstance()

    val isToday = cal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val timePattern = if (clock24Hour) "HH:mm" else "h:mm a"

    return if (isToday) {
        "today, " + SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("EEE, MMM d, $timePattern", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
private fun NoteEditWindowDialog(
    context: Context,
    existingNote: NoteItem?,
    selectedFont: com.plain.launcher.data.model.LauncherFont,
    clock24Hour: Boolean,
    onDismiss: () -> Unit,
    onSave: (NoteItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var text by remember { mutableStateOf(existingNote?.text ?: "") }

    var isReminder by remember { mutableStateOf(existingNote?.isReminder ?: false) }
    val reminderCalendar = remember {
        Calendar.getInstance().apply {
            if (existingNote?.reminderTime != null) {
                timeInMillis = existingNote.reminderTime
            }
        }
    }
    var reminderTime by remember { mutableLongStateOf(reminderCalendar.timeInMillis) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val canSave = title.isNotBlank() || text.isNotBlank()
    val fontFamily = getAppFontFamily(selectedFont)

    val isDark = PlainTheme.colors.background.luminance() < 0.5f
    val cardBg = if (isDark) {
        PlainTheme.colors.onBackground.copy(alpha = 0.08f)
    } else {
        Color.White
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(PlainTheme.colors.background)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Title textbox with subtle gray background and low opacity placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PlainTheme.colors.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    if (title.isEmpty()) {
                        PlainText(
                            text = "Title",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlainTheme.colors.onBackground.copy(alpha = 0.22f),
                            fontFamily = fontFamily
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PlainTheme.colors.onBackground,
                            fontFamily = fontFamily
                        ),
                        cursorBrush = SolidColor(PlainTheme.colors.onBackground),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text body textbox with subtle gray background and low opacity placeholder
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PlainTheme.colors.surface)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    if (text.isEmpty()) {
                        PlainText(
                            text = "Note",
                            fontSize = 15.sp,
                            color = PlainTheme.colors.onBackground.copy(alpha = 0.22f),
                            fontFamily = fontFamily
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = PlainTheme.colors.onBackground.copy(alpha = 0.9f),
                            fontFamily = fontFamily
                        ),
                        cursorBrush = SolidColor(PlainTheme.colors.onBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 320.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle for Reminder using standard settings switch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                ) {
                    PlainTextToggle(
                        title = "reminder",
                        checked = isReminder,
                        onCheckedChange = { willBeReminder ->
                            isReminder = willBeReminder
                            if (willBeReminder && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    )
                }

                // Date & Hour selector when reminder is toggled ON
                if (isReminder) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date button
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = reminderTime }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            reminderCalendar.set(Calendar.YEAR, year)
                                            reminderCalendar.set(Calendar.MONTH, month)
                                            reminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            reminderTime = reminderCalendar.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(reminderTime))
                            PlainText(
                                text = dateStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = PlainTheme.colors.onBackground
                            )
                        }

                        // Hour button
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(cardBg)
                                .clickable {
                                    val cal = Calendar.getInstance().apply { timeInMillis = reminderTime }
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            reminderCalendar.set(Calendar.MINUTE, minute)
                                            reminderCalendar.set(Calendar.SECOND, 0)
                                            reminderTime = reminderCalendar.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        clock24Hour
                                    ).show()
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val timePattern = if (clock24Hour) "HH:mm" else "h:mm a"
                            val timeStr = SimpleDateFormat(timePattern, Locale.getDefault()).format(Date(reminderTime))
                            PlainText(
                                text = timeStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = PlainTheme.colors.onBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Wide save button at the bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSave) PlainTheme.colors.onBackground else PlainTheme.colors.surface)
                        .clickable(enabled = canSave) {
                            val note = NoteItem(
                                id = existingNote?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                text = text.trim(),
                                timestamp = System.currentTimeMillis(),
                                isReminder = isReminder,
                                reminderTime = if (isReminder) reminderTime else null
                            )
                            onSave(note)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    PlainText(
                        text = "save",
                        color = if (canSave) PlainTheme.colors.background else PlainTheme.colors.secondaryText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Delete option if editing an existing note (Red)
                if (existingNote != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val deleteRed = Color(0xFFFF4444)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(deleteRed.copy(alpha = 0.15f))
                            .clickable { onDelete(existingNote.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        PlainText(
                            text = "delete",
                            color = deleteRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
