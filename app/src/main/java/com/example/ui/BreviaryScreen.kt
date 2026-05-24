package com.example.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

enum class LayoutMode {
    SINGLE,
    SPLIT_LATIN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreviaryScreen(
    viewModel: BreviaryViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedHour by viewModel.selectedHour.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val dailyStatus by viewModel.dailyStatus.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val useRomanCalendar by viewModel.useRomanCalendar.collectAsState()
    val remindersEnabled by viewModel.remindersEnabled.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val bookmarks by viewModel.allBookmarks.collectAsState()

    val activeLanguage by viewModel.activeLanguage.collectAsState()
    val soloMode by viewModel.soloMode.collectAsState()
    val simplifiedRubrics by viewModel.simplifiedRubrics.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val apiKeyStatus by viewModel.apiKeyStatus.collectAsState()
    val isCheckingApiKey by viewModel.isCheckingApiKey.collectAsState()

    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val hideApiWarning by viewModel.hideApiWarning.collectAsState()
    val showOnboardingGuide by viewModel.showOnboardingGuide.collectAsState()

    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val speechSpeed by viewModel.speechSpeed.collectAsState()
    val activeSentenceText by viewModel.activeSentenceText.collectAsState()

    var layoutMode by remember { mutableStateOf(LayoutMode.SINGLE) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = remember(context) { context as? android.app.Activity }
    DisposableEffect(activity) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
    var textScale by remember { mutableFloatStateOf(16f) }

    val localDensity = androidx.compose.ui.platform.LocalDensity.current
    val headerHeight = 168.dp
    val headerHeightPx = with(localDensity) { headerHeight.toPx() }
    var headerOffsetHeightPx by remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember(headerHeightPx) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val delta = available.y
                val newOffset = headerOffsetHeightPx + delta
                headerOffsetHeightPx = newOffset.coerceIn(-headerHeightPx, 0f)
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val realTimeHour = remember {
        val cal = Calendar.getInstance()
        LiturgicalHour.fromHourOfDay(cal.get(Calendar.HOUR_OF_DAY))
    }

    // Storage Access Framework Activity Results for Backup & Restore
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.let { outputStream ->
                    viewModel.exportBackup(outputStream) { success ->
                        if (success) {
                            Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Backup export failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    viewModel.importBackup(inputStream) { success ->
                        if (success) {
                            Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Restore failed. Invalid file.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val triggerDatePicker = {
        val calendar = Calendar.getInstance()
        try {
            val d = sdfIso.parse(selectedDate)
            if (d != null) calendar.time = d
        } catch (_: Exception) {}

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance()
                chosen.set(year, month, dayOfMonth)
                viewModel.selectDate(sdfIso.format(chosen.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "PHILIPPINES BREVIARY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "CBCP Calendar Liturgy",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = triggerDatePicker,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showBookmarksSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Bookmarks",
                            tint = if (bookmarks.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.refreshCurrentPrayer() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync from AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (uiState is PrayerUiState.Success) {
                val success = uiState as PrayerUiState.Success
                BottomActionBar(
                    isCompleted = success.isCompleted,
                    onToggleComplete = { viewModel.togglePrayerCompleted() },
                    textScale = textScale,
                    onScaleChange = { textScale = it },
                    isSpeaking = isSpeaking,
                    onToggleSpeak = {
                        val fullSpeechText = buildSpeechText(success.prayer, activeLanguage, soloMode)
                        viewModel.speakCurrentPrayer(fullSpeechText)
                    },
                    speechSpeed = speechSpeed,
                    onSpeedChange = { viewModel.setSpeechSpeed(it) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // 1. Collapsible part
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .offset(y = with(localDensity) { headerOffsetHeightPx.toDp() })
            ) {
                // Liturgical week date picker
                WeekDayBar(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )

                // Hour selection pill row
                HourSelectorRow(
                    selectedHour = selectedHour,
                    recommendedHour = realTimeHour,
                    dailyStatus = dailyStatus,
                    onHourSelected = { viewModel.selectHour(it) }
                )

                // Dynamic Active Timeframe Display Card
                val slotText = remember(selectedHour) {
                    when (selectedHour) {
                        LiturgicalHour.OFFICE_READINGS -> "Office of Readings: Recommended 12:00 AM - 5:00 AM"
                        LiturgicalHour.LAUDS -> "Morning Prayer (Lauds): Recommended 5:00 AM - 9:00 AM"
                        LiturgicalHour.MIDDAY -> "Midday Prayer: Recommended 9:00 AM - 4:00 PM"
                        LiturgicalHour.VESPERS -> "Evening Prayer (Vespers): Recommended 4:00 PM - 8:00 PM"
                        LiturgicalHour.COMPLINE -> "Night Prayer (Compline): Recommended 8:00 PM - 12:00 AM"
                    }
                }
                
                val isCurrentlyInSlot = remember(selectedHour) {
                    val currentHourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    currentHourOfDay in selectedHour.startHour until selectedHour.endHour
                }
                
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentlyInSlot) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, if (isCurrentlyInSlot) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyInSlot) Icons.Default.Info else Icons.Default.Warning,
                            contentDescription = "Timeframe status",
                            tint = if (isCurrentlyInSlot) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = slotText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = if (isCurrentlyInSlot) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isCurrentlyInSlot) FontWeight.Bold else FontWeight.Normal
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrentlyInSlot) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE NOW",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 2. Fixed/Scrollable content container (pushed down by custom offset top padding)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight + with(localDensity) { headerOffsetHeightPx.toDp() })
            ) {
                // Quick Language and Layout Toggles Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Part: Language Selector
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val isLangSelected = lang == activeLanguage
                            FilterChip(
                                selected = isLangSelected,
                                onClick = { viewModel.selectLanguage(lang) },
                                label = { Text(lang.displayName, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Right Part: Visual Mode Toggle (Single vs side-by-side with Latin)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                layoutMode = if (layoutMode == LayoutMode.SINGLE) LayoutMode.SPLIT_LATIN
                                             else LayoutMode.SINGLE
                            }
                        ) {
                            Icon(
                                imageVector = if (layoutMode == LayoutMode.SPLIT_LATIN) Icons.Default.List else Icons.Default.Info,
                                contentDescription = "Toggle side-by-side split Latin view",
                                tint = if (layoutMode == LayoutMode.SPLIT_LATIN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        }
                        Text(
                            text = if (layoutMode == LayoutMode.SPLIT_LATIN) "Split-Latin" else "Single-View",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.clickable {
                                layoutMode = if (layoutMode == LayoutMode.SINGLE) LayoutMode.SPLIT_LATIN
                                             else LayoutMode.SINGLE
                            }
                        )
                    }
                }

                Divider(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                // Main Content Area
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
                ) {
                    when (val state = uiState) {
                        is PrayerUiState.Loading -> {
                            LoadingStateView()
                        }
                        is PrayerUiState.Error -> {
                            ErrorStateView(
                                errorMsg = state.message,
                                onRetry = { viewModel.refreshCurrentPrayer() }
                            )
                        }
                        is PrayerUiState.Success -> {
                            PrayerBookView(
                                prayer = state.prayer,
                                selectedHour = selectedHour,
                                textScale = textScale,
                                isCompleted = state.isCompleted,
                                bookmarks = bookmarks,
                                activeLanguage = activeLanguage,
                                layoutMode = layoutMode,
                                soloMode = soloMode,
                                simplifiedRubrics = simplifiedRubrics,
                                activeSentenceText = activeSentenceText,
                                isOfflineMode = customApiKey.isBlank(),
                                onToggleBookmark = { type, title, snippet ->
                                    viewModel.toggleSectionBookmark(type, title, snippet)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SettingsSheetContent(
                    useRomanCalendar = useRomanCalendar,
                    onToggleRomanCalendar = { viewModel.toggleRomanCalendar() },
                    remindersEnabled = remindersEnabled,
                    onToggleReminders = { viewModel.toggleReminders() },
                    onTriggerTestNotification = { viewModel.triggerTestNotification() },
                    downloadStatus = downloadStatus,
                    onDownloadHours = { viewModel.downloadTodayHours() },
                    soloMode = soloMode,
                    onToggleSoloMode = { viewModel.toggleSoloMode() },
                    simplifiedRubrics = simplifiedRubrics,
                    onToggleSimplifiedRubrics = { viewModel.toggleSimplifiedRubrics() },
                    onExportBackup = { createDocumentLauncher.launch("breviary_backup.json") },
                    onImportBackup = { openDocumentLauncher.launch(arrayOf("application/json")) },
                    customApiKey = customApiKey,
                    apiKeyStatus = apiKeyStatus,
                    isCheckingApiKey = isCheckingApiKey,
                    onCustomApiKeyChange = { viewModel.saveCustomApiKey(it) },
                    onTestApiKey = { viewModel.testCustomApiKey(it) },
                    onShowOnboardingGuide = { viewModel.setShowOnboardingGuide(true) }
                )
            }
        }

        if (showBookmarksSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBookmarksSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                BookmarksSheetContent(
                    bookmarks = bookmarks,
                    onBookmarkClick = { bm ->
                        viewModel.selectDate(bm.date)
                        try {
                            viewModel.selectHour(com.example.data.LiturgicalHour.valueOf(bm.hour))
                        } catch (_: Exception) {}
                        showBookmarksSheet = false
                    },
                    onDeleteBookmark = { bmId ->
                        viewModel.deleteBookmark(bmId)
                    }
                )
            }
        }

        if (showOnboardingGuide) {
            OnboardingGuideOverlay(
                isFirstLaunch = isFirstLaunch,
                onDismiss = { hideAgain ->
                    viewModel.dismissOnboarding(hideAgain)
                },
                onGoToSettings = {
                    viewModel.completeFirstLaunchAndGoToSettings()
                    showSettingsSheet = true
                }
            )
        }
}

@Composable
fun WeekDayBar(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sdfDayName = SimpleDateFormat("EEE", Locale.getDefault())
    val sdfDayNum = SimpleDateFormat("d", Locale.getDefault())

    // Generate surrounding week
    val dayList = remember(selectedDate) {
        val list = mutableListOf<Date>()
        val calendar = Calendar.getInstance()
        try {
            val d = sdfIso.parse(selectedDate)
            if (d != null) calendar.time = d
        } catch (_: Exception) {}

        calendar.add(Calendar.DAY_OF_YEAR, -3)
        for (i in 0..6) {
            list.add(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(dayList) { date ->
                val dateStr = sdfIso.format(date)
                val isSelected = dateStr == selectedDate
                val dayName = sdfDayName.format(date)
                val dayNum = sdfDayNum.format(date)
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        )
                        .then(
                            if (!isSelected) Modifier.border(
                                BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                RoundedCornerShape(16.dp)
                            ) else Modifier
                        )
                        .clickable { onDateSelected(dateStr) }
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayNum,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HourSelectorRow(
    selectedHour: LiturgicalHour,
    recommendedHour: LiturgicalHour,
    dailyStatus: Map<LiturgicalHour, Boolean>,
    onHourSelected: (LiturgicalHour) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(LiturgicalHour.entries.toList()) { hour ->
            val isSelected = hour == selectedHour
            val isRecommended = hour == recommendedHour
            val isCompleted = dailyStatus[hour] ?: false

            val borderStroke = when {
                isSelected -> null
                isRecommended -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            }

            val bgColor = when {
                isSelected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgColor)
                    .then(
                        if (borderStroke != null) Modifier.border(borderStroke, CircleShape)
                        else Modifier
                    )
                    .clickable { onHourSelected(hour) }
                    .padding(vertical = 8.dp, horizontal = 14.dp)
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Completed",
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                } else if (isRecommended) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                    )
                }

                Text(
                    text = hour.displayName,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }
    }
}

@Composable
fun LoadingStateView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preparing CBCP Philippine Liturgy...",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        )
    }
}

@Composable
fun ErrorStateView(
    errorMsg: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alert",
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Liturgy Retrieval Pause",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = errorMsg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Re-synchronize Prayer")
        }
    }
}

@Composable
fun PrayerBookView(
    prayer: LiturgicalPrayer,
    selectedHour: LiturgicalHour,
    textScale: Float,
    isCompleted: Boolean,
    bookmarks: List<com.example.data.database.Bookmark> = emptyList(),
    activeLanguage: AppLanguage,
    layoutMode: LayoutMode,
    soloMode: Boolean,
    simplifiedRubrics: Boolean,
    activeSentenceText: String? = null,
    isOfflineMode: Boolean = false,
    onToggleBookmark: (sectionType: String, sectionTitle: String, snippet: String) -> Unit = { _, _, _ -> }
) {
    val scrollState = rememberLazyListState()

    LaunchedEffect(prayer.title) {
        scrollState.scrollToItem(0)
    }

    LazyColumn(
        state = scrollState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        if (isOfflineMode) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📜", fontSize = 18.sp)
                        Text(
                            text = "Using Offline Liturgy. Enter your free Gemini API key to unlock dynamic daily updates.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium,
                                lineHeight = 14.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Book Header (Title & Season)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cbcpBadgeText(prayer.season),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp
                        )
                    )
                    Text(
                        text = "2026",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                if (layoutMode == LayoutMode.SPLIT_LATIN) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedHour.displayName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedHour.latinName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontStyle = FontStyle.Italic
                                )
                            )
                        }
                    }
                } else {
                    Text(
                        text = prayer.getTitle(activeLanguage),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground,
                            letterSpacing = (-1).sp
                        )
                    )
                }

                if (isCompleted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(vertical = 6.dp, horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Prayed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "OFFERED & COMPLETED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                             )
                        )
                    }
                }
            }
        }

        // 1. Introduction
        item {
            val callText = prayer.getOpeningVerseCall(activeLanguage)
            val isIntroBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Introduction" }
            SimpleSectionCard(
                title = "INTRODUCTION",
                isBookmarked = isIntroBookmarked,
                onBookmarkToggle = { onToggleBookmark("Introduction", "Introduction", callText) }
            ) {
                if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                HighlightableText("V. " + prayer.getOpeningVerseCall(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(4.dp))
                                HighlightableText("R. " + prayer.getOpeningVerseResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.primary, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                HighlightableText("V. " + prayer.getOpeningVerseCall(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(4.dp))
                                HighlightableText("R. " + prayer.getOpeningVerseResponse(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.primary, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                            }
                        }
                    }
                } else {
                    Column {
                        HighlightableText("V. " + prayer.getOpeningVerseCall(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, fontSize = (textScale + 1).sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(4.dp))
                        HighlightableText("R. " + prayer.getOpeningVerseResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.primary, fontSize = (textScale + 1).sp), activeSentenceText = activeSentenceText)
                    }
                }

                prayer.getInvitatoryAntiphon(activeLanguage)?.let { antiphon ->
                    Spacer(modifier = Modifier.height(12.dp))
                    if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                RubricLabelText("INVITATORY ANTIPHON", antiphon, textScale, activeSentenceText)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                RubricLabelText("INVITATORY ANTIPHON", prayer.getInvitatoryAntiphon(AppLanguage.LATIN) ?: "", textScale, activeSentenceText)
                            }
                        }
                    } else {
                        RubricLabelText("INVITATORY ANTIPHON", antiphon, textScale, activeSentenceText)
                    }
                }
            }
        }

        // 2. Hymn
        item {
            val hymnTitle = prayer.getHymnTitle(activeLanguage)
            val isHymnBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Hymn" }
            SimpleSectionCard(
                title = "HYMN",
                isBookmarked = isHymnBookmarked,
                onBookmarkToggle = { onToggleBookmark("Hymn", hymnTitle, prayer.getHymnText(activeLanguage)) }
            ) {
                if (!simplifiedRubrics) {
                    Text(
                        text = "Rubric: ${prayer.rubricHymn ?: "Stand during recitation"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, color = Color(0xFFC62828)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                HighlightableText(prayer.getHymnTitle(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(4.dp))
                                HighlightableText(prayer.getHymnText(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale * 1.45).sp), activeSentenceText = activeSentenceText)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                HighlightableText(prayer.getHymnTitle(AppLanguage.LATIN).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(4.dp))
                                HighlightableText(prayer.getHymnText(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale * 1.45).sp), activeSentenceText = activeSentenceText)
                            }
                        }
                    }
                } else {
                    Column {
                        HighlightableText(prayer.getHymnTitle(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(4.dp))
                        HighlightableText(prayer.getHymnText(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale * 1.45).sp), activeSentenceText = activeSentenceText)
                    }
                }
            }
        }

        // 3. Psalmody Title
        item {
            RubricHeader(title = "PSALMODY")
        }

        // Psalms List (Integrated trilingual view and Split Latin side-by-side)
        items(prayer.psalms) { psalm ->
            val isPsalmBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Psalm" && it.sectionTitle == psalm.title }
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = psalm.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black)
                        )
                        IconButton(
                            onClick = { onToggleBookmark("Psalm", psalm.title, psalm.getText(activeLanguage)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Bookmark",
                                tint = if (isPsalmBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (!simplifiedRubrics) {
                        Text(
                            text = "Rubric: ${prayer.rubricPsalm ?: "Sit during psalmody"}",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, color = Color(0xFFC62828)),
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }

                    if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                psalm.getSubtitle(activeLanguage)?.let {
                                    HighlightableText(it, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                HighlightableText("ANTIPHON: " + psalm.getAntiphonBefore(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(10.dp))
                                StyledLiturgyText(psalm.getText(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(10.dp))
                                HighlightableText("ANTIPHON repetition: " + psalm.getAntiphonAfter(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                psalm.getSubtitle(AppLanguage.LATIN)?.let {
                                    HighlightableText(it, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                HighlightableText("ANTIPHON: " + psalm.getAntiphonBefore(AppLanguage.LATIN).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(10.dp))
                                StyledLiturgyText(psalm.getText(AppLanguage.LATIN), AppLanguage.LATIN, soloMode, textScale, fontStyle = FontStyle.Italic, activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(10.dp))
                                HighlightableText("ANTIPHON repetition: " + psalm.getAntiphonAfter(AppLanguage.LATIN).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                            }
                        }
                    } else {
                        Column {
                            psalm.getSubtitle(activeLanguage)?.let {
                                HighlightableText(it, style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            HighlightableText("ANTIPHON: " + psalm.getAntiphonBefore(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(10.dp))
                            StyledLiturgyText(psalm.getText(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(10.dp))
                            HighlightableText("ANTIPHON repetition: " + psalm.getAntiphonAfter(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                        }
                    }
                }
            }
        }

        // 4. Scripture Reading
        item {
            val isReadingBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Reading" }
            SimpleSectionCard(
                title = "SCRIPTURE READING (${prayer.readingReference})",
                isBookmarked = isReadingBookmarked,
                onBookmarkToggle = { onToggleBookmark("Reading", prayer.readingReference, prayer.getReadingText(activeLanguage)) }
            ) {
                if (!simplifiedRubrics) {
                    Text(
                        text = "Rubric: ${prayer.rubricReading ?: "Sit and listen to scripture prayerfully"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, color = Color(0xFFC62828)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getReadingText(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale * 1.5).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(4.dp))
                            HighlightableText(prayer.getReadingResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getReadingText(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale * 1.5).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(4.dp))
                            HighlightableText(prayer.getReadingResponse(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                        }
                    }
                } else {
                    Column {
                        HighlightableText(prayer.getReadingText(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale * 1.5).sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(4.dp))
                        HighlightableText(prayer.getReadingResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                    }
                }
            }
        }

        // 5. Responsory (GILH 171 Compliant Handling)
        item {
            val isResponsoryBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Responsory" }
            SimpleSectionCard(
                title = "RESPONSORY",
                isBookmarked = isResponsoryBookmarked,
                onBookmarkToggle = { onToggleBookmark("Responsory", "Responsory", prayer.getResponsoryVersicle(activeLanguage)) }
            ) {
                if (soloMode) {
                    // Under GILH 171: sequential flowing stream of text without repetitious back-to-back reprisal.
                    Text(
                        text = "GILH Compliant Sequential Direct Reading (Solo Mode Active)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                HighlightableText("V. ${cleanCue(prayer.getResponsoryVersicle(activeLanguage))} — R. ${cleanCue(prayer.getResponsoryResponse(activeLanguage))}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale*1.5).sp), activeSentenceText = activeSentenceText)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                HighlightableText("V. ${cleanCue(prayer.getResponsoryVersicle(AppLanguage.LATIN))} — R. ${cleanCue(prayer.getResponsoryResponse(AppLanguage.LATIN))}", style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale*1.5).sp), activeSentenceText = activeSentenceText)
                            }
                        }
                    } else {
                        HighlightableText("V. ${cleanCue(prayer.getResponsoryVersicle(activeLanguage))} — R. ${cleanCue(prayer.getResponsoryResponse(activeLanguage))}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale * 1.5).sp), activeSentenceText = activeSentenceText)
                    }
                } else {
                    // Traditional style: alternate versicles and responses in beautiful Crimson Red
                    if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                StyledLiturgyText(prayer.getResponsoryVersicle(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(4.dp))
                                StyledLiturgyText(prayer.getResponsoryResponse(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                StyledLiturgyText(prayer.getResponsoryVersicle(AppLanguage.LATIN), AppLanguage.LATIN, soloMode, textScale, fontStyle = FontStyle.Italic, activeSentenceText = activeSentenceText)
                                Spacer(modifier = Modifier.height(4.dp))
                                StyledLiturgyText(prayer.getResponsoryResponse(AppLanguage.LATIN), AppLanguage.LATIN, soloMode, textScale, fontStyle = FontStyle.Italic, activeSentenceText = activeSentenceText)
                            }
                        }
                    } else {
                        Column {
                            StyledLiturgyText(prayer.getResponsoryVersicle(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(4.dp))
                            StyledLiturgyText(prayer.getResponsoryResponse(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                        }
                    }
                }
            }
        }

        // 6. Gospel Canticle
        item {
            val isCanticleBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Gospel Canticle" }
            SimpleSectionCard(
                title = "GOSPEL CANTICLE",
                isBookmarked = isCanticleBookmarked,
                onBookmarkToggle = { onToggleBookmark("Gospel Canticle", prayer.getCanticleTitle(activeLanguage), prayer.getCanticleText(activeLanguage)) }
            ) {
                if (!simplifiedRubrics) {
                    Text(
                        text = "Rubric: ${prayer.rubricCanticle ?: "Stand and sign yourself with the Cross"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, color = Color(0xFFC62828)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText("ANTIPHON: " + prayer.getCanticleAntiphonBefore(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(8.dp))
                            HighlightableText(prayer.getCanticleTitle(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(6.dp))
                            StyledLiturgyText(prayer.getCanticleText(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(8.dp))
                            HighlightableText("ANTIPHON repetition: " + prayer.getCanticleAntiphonAfter(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText("ANTIPHON: " + prayer.getCanticleAntiphonBefore(AppLanguage.LATIN).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(8.dp))
                            HighlightableText(prayer.getCanticleTitle(AppLanguage.LATIN).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(6.dp))
                            StyledLiturgyText(prayer.getCanticleText(AppLanguage.LATIN), AppLanguage.LATIN, soloMode, textScale, fontStyle = FontStyle.Italic, activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(8.dp))
                            HighlightableText("ANTIPHON repetition: " + prayer.getCanticleAntiphonAfter(AppLanguage.LATIN).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                        }
                    }
                } else {
                    Column {
                        HighlightableText("ANTIPHON: " + prayer.getCanticleAntiphonBefore(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(8.dp))
                        HighlightableText(prayer.getCanticleTitle(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary, fontSize = (textScale - 1).sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(6.dp))
                        StyledLiturgyText(prayer.getCanticleText(activeLanguage), activeLanguage, soloMode, textScale, activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(8.dp))
                        HighlightableText("ANTIPHON repetition: " + prayer.getCanticleAntiphonAfter(activeLanguage).uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = (textScale - 2).sp), activeSentenceText = activeSentenceText)
                    }
                }
            }
        }

        // 7. Intercessions
        item {
            val isIntercessionsBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Intercessions" }
            SimpleSectionCard(
                title = "INTERCESSIONS",
                isBookmarked = isIntercessionsBookmarked,
                onBookmarkToggle = { onToggleBookmark("Intercessions", "Intercessions", prayer.getIntercessionsPreface(activeLanguage)) }
            ) {
                if (!simplifiedRubrics) {
                    Text(
                        text = "Rubric: ${prayer.rubricIntercessions ?: "Stand to present our prayers to Christ"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, color = Color(0xFFC62828)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getIntercessionsPreface(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale*1.4).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(8.dp))
                            HighlightableText("RESPONSE: " + prayer.getIntercessionsResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(10.dp))
                            prayer.intercessionsList.forEach { item ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    HighlightableText("• " + item.getPetition(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                                    HighlightableText("R. " + item.getResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary, fontSize = textScale.sp), modifier = Modifier.padding(start = 12.dp), activeSentenceText = activeSentenceText)
                                }
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getIntercessionsPreface(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale*1.4).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(8.dp))
                            HighlightableText("RESPONSE: " + prayer.getIntercessionsResponse(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(10.dp))
                            prayer.intercessionsList.forEach { item ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    HighlightableText("• " + item.getPetition(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                                    HighlightableText("R. " + item.getResponse(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary, fontSize = textScale.sp), modifier = Modifier.padding(start = 12.dp), activeSentenceText = activeSentenceText)
                                }
                            }
                        }
                    }
                } else {
                    Column {
                        HighlightableText(prayer.getIntercessionsPreface(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale*1.4).sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(8.dp))
                        HighlightableText("RESPONSE: " + prayer.getIntercessionsResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(10.dp))
                        prayer.intercessionsList.forEach { item ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                HighlightableText("• " + item.getPetition(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                                HighlightableText("R. " + item.getResponse(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.secondary, fontSize = textScale.sp), modifier = Modifier.padding(start = 12.dp), activeSentenceText = activeSentenceText)
                            }
                        }
                    }
                }
            }
        }

        // 8. Lord's Prayer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "THE LORD'S PRAYER",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getLordPrayer(activeLanguage), style = MaterialTheme.typography.bodyMedium.copy(fontSize = textScale.sp, lineHeight = (textScale * 1.45).sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(), activeSentenceText = activeSentenceText)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getLordPrayer(AppLanguage.LATIN), style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale * 1.45).sp, textAlign = TextAlign.Center), modifier = Modifier.fillMaxWidth(), activeSentenceText = activeSentenceText)
                        }
                    }
                } else {
                    HighlightableText(
                        text = prayer.getLordPrayer(activeLanguage),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = textScale.sp,
                            lineHeight = (textScale * 1.45).sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        activeSentenceText = activeSentenceText
                    )
                }
            }
        }

        // 9. Concluding Prayer and Dismissal
        item {
            val isClosingBookmarked = bookmarks.any { it.date == prayer.dateString && it.hour == selectedHour.name && it.sectionType == "Concluding Prayer" }
            SimpleSectionCard(
                title = "CONCLUDING PRAYER",
                isBookmarked = isClosingBookmarked,
                onBookmarkToggle = { onToggleBookmark("Concluding Prayer", "Concluding Prayer", prayer.getClosingPrayer(activeLanguage)) }
            ) {
                if (!simplifiedRubrics) {
                    Text(
                        text = "Rubric: ${prayer.rubricClosing ?: "Stand for concluding prayer and lay blessing"}",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, color = Color(0xFFC62828)),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                HighlightableText("Let us pray.", style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.tertiary, fontSize = (textScale - 1).sp), modifier = Modifier.padding(bottom = 6.dp), activeSentenceText = activeSentenceText)

                if (layoutMode == LayoutMode.SPLIT_LATIN && activeLanguage != AppLanguage.LATIN) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getClosingPrayer(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale * 1.5).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("DISMISSAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.height(4.dp))
                            HighlightableText(getLayOrClericalDismissal(prayer, activeLanguage, soloMode), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            HighlightableText(prayer.getClosingPrayer(AppLanguage.LATIN), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp, lineHeight = (textScale * 1.5).sp), activeSentenceText = activeSentenceText)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("DISMISSAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                            Spacer(modifier = Modifier.height(4.dp))
                            HighlightableText(getLayOrClericalDismissal(prayer, AppLanguage.LATIN, soloMode), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                        }
                    }
                } else {
                    Column {
                        HighlightableText(prayer.getClosingPrayer(activeLanguage), style = MaterialTheme.typography.bodyLarge.copy(fontSize = textScale.sp, lineHeight = (textScale * 1.5).sp), activeSentenceText = activeSentenceText)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("DISMISSAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        Spacer(modifier = Modifier.height(4.dp))
                        HighlightableText(getLayOrClericalDismissal(prayer, activeLanguage, soloMode), style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, fontSize = textScale.sp), activeSentenceText = activeSentenceText)
                    }
                }
            }
        }

        // Ornamental liturgical end glyph
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.width(40.dp))
                    Text(
                        text = " ✠ ",
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), modifier = Modifier.width(40.dp))
                }
            }
        }
    }
}

@Composable
fun RubricHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Divider(
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
            modifier = Modifier.weight(1.0f)
        )
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
                letterSpacing = 2.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Divider(
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
            modifier = Modifier.weight(1.0f)
        )
    }
}

@Composable
fun SimpleSectionCard(
    title: String,
    isBookmarked: Boolean = false,
    onBookmarkToggle: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (onBookmarkToggle != null) {
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Save Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}

fun buildHighlightableString(
    text: String,
    activeSentenceText: String?,
    highlightColor: Color
): androidx.compose.ui.text.AnnotatedString {
    if (activeSentenceText.isNullOrBlank()) {
        return androidx.compose.ui.text.AnnotatedString(text)
    }

    val cleanSearch = activeSentenceText.trim()
    if (cleanSearch.length < 4) {
        return androidx.compose.ui.text.AnnotatedString(text)
    }

    // Try finding exact substring
    var index = text.indexOf(cleanSearch, ignoreCase = true)
    var matchLength = cleanSearch.length

    // If not found, try without trailing punctuation
    if (index == -1) {
        val searchNoPunct = cleanSearch.trimEnd { it in ".,!?;:" }
        if (searchNoPunct.length >= 4) {
            index = text.indexOf(searchNoPunct, ignoreCase = true)
            matchLength = searchNoPunct.length
        }
    }

    // If still not found, check if a sentence-by-sentence comparison matches
    if (index == -1) {
        val activeNormalized = cleanSearch.lowercase().filter { it.isLetterOrDigit() }
        if (activeNormalized.isNotEmpty()) {
            return androidx.compose.ui.text.buildAnnotatedString {
                val builder = this
                val lines = text.split("\n")
                lines.forEachIndexed { lineIdx, line ->
                    if (lineIdx > 0) {
                        builder.append("\n")
                    }
                    val sentences = line.split(Regex("(?<=[.!?])\\s+"))
                    sentences.forEachIndexed { sentIdx, sentence ->
                        if (sentIdx > 0) {
                            builder.append(" ")
                        }
                        val normalizedSentence = sentence.lowercase().filter { it.isLetterOrDigit() }
                        val isMatch = normalizedSentence.isNotEmpty() && 
                            (normalizedSentence.contains(activeNormalized) || activeNormalized.contains(normalizedSentence))
                        
                        if (isMatch) {
                            builder.pushStyle(style = androidx.compose.ui.text.SpanStyle(background = highlightColor))
                            builder.append(sentence)
                            builder.pop()
                        } else {
                            builder.append(sentence)
                        }
                    }
                }
            }
        }
    }

    if (index != -1) {
        return androidx.compose.ui.text.buildAnnotatedString {
            append(text.substring(0, index))
            pushStyle(style = androidx.compose.ui.text.SpanStyle(background = highlightColor))
            append(text.substring(index, index + matchLength))
            pop()
            append(text.substring(index + matchLength))
        }
    }

    return androidx.compose.ui.text.AnnotatedString(text)
}

@Composable
fun HighlightableText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
    activeSentenceText: String? = null,
    color: Color = Color.Unspecified
) {
    val highlightColor = if (androidx.compose.foundation.isSystemInDarkTheme()) Color(0xFF3B2E66) else Color(0xFFE8E0FF)
    Text(
        text = buildHighlightableString(text, activeSentenceText, highlightColor),
        style = style,
        modifier = modifier,
        color = color
    )
}

@Composable
fun RubricLabelText(label: String, text: String, textScale: Float, activeSentenceText: String? = null) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.2.sp
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        HighlightableText(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = FontStyle.Italic,
                fontSize = textScale.sp,
                lineHeight = (textScale * 1.45).sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            activeSentenceText = activeSentenceText
        )
    }
}

@Composable
fun StyledLiturgyText(
    text: String,
    lang: AppLanguage,
    soloMode: Boolean,
    textScale: Float,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle? = null,
    textAlign: TextAlign? = null,
    color: Color = MaterialTheme.colorScheme.onBackground,
    activeSentenceText: String? = null
) {
    val lines = text.split("\n")
    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val trimmed = line.trim()
            val hasCue = trimmed.startsWith("L:") || trimmed.startsWith("R:") || trimmed.startsWith("A:") ||
                         trimmed.startsWith("V:") || trimmed.startsWith("R.") || trimmed.startsWith("P:") ||
                         trimmed.startsWith("V.") || trimmed.startsWith("C:") || trimmed.startsWith("Tugon:") ||
                         trimmed.startsWith("T:")
            
            if (soloMode) {
                val cleanedText = if (hasCue) {
                    val idx = trimmed.indexOf(":")
                    if (idx != -1) trimmed.substring(idx + 1).trim()
                    else {
                        val idxDot = trimmed.indexOf(".")
                        if (idxDot != -1) trimmed.substring(idxDot + 1).trim()
                        else trimmed
                    }
                } else trimmed
                
                if (cleanedText.isNotEmpty()) {
                    HighlightableText(
                        text = cleanedText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = textScale.sp,
                            lineHeight = (textScale * 1.5).sp,
                            fontWeight = fontWeight,
                            fontStyle = fontStyle,
                            color = color,
                            textAlign = textAlign ?: TextAlign.Start
                        ),
                        modifier = Modifier.padding(vertical = 2.dp),
                        activeSentenceText = activeSentenceText
                    )
                }
            } else {
                if (hasCue) {
                    val separatorIndex = if (trimmed.indexOf(":") != -1) trimmed.indexOf(":") else trimmed.indexOf(".")
                    val prefix = if (separatorIndex != -1) trimmed.substring(0, separatorIndex + 1) else ""
                    val suffix = if (separatorIndex != -1) trimmed.substring(separatorIndex + 1).trim() else trimmed
                    
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = "$prefix ",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textScale.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFC62828) // Deep Crimson Red for Communal choir indicators
                            )
                        )
                        HighlightableText(
                            text = suffix,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textScale.sp,
                                lineHeight = (textScale * 1.5).sp,
                                fontWeight = fontWeight,
                                fontStyle = fontStyle,
                                color = color,
                                textAlign = textAlign ?: TextAlign.Start
                            ),
                            modifier = Modifier.weight(1f),
                            activeSentenceText = activeSentenceText
                        )
                    }
                } else {
                    if (trimmed.isNotEmpty()) {
                        HighlightableText(
                            text = trimmed,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = textScale.sp,
                                lineHeight = (textScale * 1.5).sp,
                                fontWeight = fontWeight,
                                fontStyle = fontStyle,
                                color = color,
                                textAlign = textAlign ?: TextAlign.Start
                            ),
                            modifier = Modifier.padding(vertical = 2.dp),
                            activeSentenceText = activeSentenceText
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomActionBar(
    isCompleted: Boolean,
    onToggleComplete: () -> Unit,
    textScale: Float,
    onScaleChange: (Float) -> Unit,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit,
    speechSpeed: Float,
    onSpeedChange: (Float) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 12.dp, top = 8.dp, start = 16.dp, end = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
        ) {
            // Speed controls chip row above main buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("TTS Audio Speed:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                listOf(0.8f, 1.0f, 1.2f, 1.5f).forEach { speed ->
                    val isSpeedSelected = speechSpeed == speed
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSpeedSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onSpeedChange(speed) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("${speed}x", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSpeedSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Reading Scale Adjusters & TTS Play button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { if (textScale > 12f) onScaleChange(textScale - 1f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("a-", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                    }
                    
                    Text(
                        text = "${textScale.toInt()}sp",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    )

                    IconButton(
                        onClick = { if (textScale < 24f) onScaleChange(textScale + 1f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("A+", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary))
                    }

                    Box(modifier = Modifier.width(8.dp))

                    // TTS Trigger Play Button
                    FloatingActionButton(
                        onClick = onToggleSpeak,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        containerColor = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop Text-to-Speech playback" else "Listen via Text-to-Speech Audio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Gilded Complete Action Button
                Button(
                    onClick = onToggleComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant
                                         else MaterialTheme.colorScheme.primary,
                        contentColor = if (isCompleted) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = CircleShape,
                    border = BorderStroke(
                        1.5.dp,
                        if (isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                            contentDescription = "Offer Prayer Completed",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isCompleted) "Offer Completed" else "Offer Prayer",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSheetContent(
    useRomanCalendar: Boolean,
    onToggleRomanCalendar: () -> Unit,
    remindersEnabled: Boolean,
    onToggleReminders: () -> Unit,
    onTriggerTestNotification: () -> Unit,
    downloadStatus: String?,
    onDownloadHours: () -> Unit,
    soloMode: Boolean,
    onToggleSoloMode: () -> Unit,
    simplifiedRubrics: Boolean,
    onToggleSimplifiedRubrics: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    customApiKey: String,
    apiKeyStatus: String,
    isCheckingApiKey: Boolean,
    onCustomApiKeyChange: (String) -> Unit,
    onTestApiKey: (String) -> Unit,
    onShowOnboardingGuide: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PREFERENCES & UTILITIES",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        
        // 1. Solo Mode Option
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Layperson Solo Mode (GILH 171)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Reformats blessings to layperson formulas, simplifies responsories sequentially, and hides choral choir indicators.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = soloMode,
                    onCheckedChange = { onToggleSoloMode() }
                )
            }
        }

        // 2. Simplified Rubrics Switch
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hide Liturgical Rubrics",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Completely silences and hides italicized red rubrical text directions from the cards.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = simplifiedRubrics,
                    onCheckedChange = { onToggleSimplifiedRubrics() }
                )
            }
        }

        // 3. Backup and Restore Row
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Local Backup & Restore",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Export or import your saved bookmarks, logs, and layout configurations via a local JSON file.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onExportBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Export")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onImportBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Import")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restore Backup", fontSize = 11.sp)
                    }
                }
            }
        }

        // 4. Reminders Option
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Prayer Reminders",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Get notified at the starting timeframe of each Liturgical Hour.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { onToggleReminders() }
                    )
                }
                if (remindersEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onTriggerTestNotification,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Notifications, contentDescription = "Test")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Trigger Test Notification Now", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 5. Offline Download Option (Uses Philippine CBCP configuration fallback caching)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Offline Access & Pre-Caching",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Pre-download and cache all 5 Hours for today securely so you can pray off-grid.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )
                
                if (downloadStatus != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = downloadStatus,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = onDownloadHours,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Download")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pre-Download Today's Hours", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // 7. Custom API Provider Section
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        
        Text(
            text = "CUSTOM API PROVIDER",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Bring Your Own Gemini API Key",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Supply your personal Google AI Studio Gemini API Key. This app runs exclusively on your custom key when online. If no key is set, the app runs entirely in high-fidelity offline mode using pre-cached contents and traditional local generator.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                )
                
                var keyVisible by remember { mutableStateOf(false) }
                
                OutlinedTextField(
                    value = customApiKey,
                    onValueChange = onCustomApiKeyChange,
                    label = { Text("Gemini API Key", style = MaterialTheme.typography.bodySmall) },
                    singleLine = true,
                    visualTransformation = if (keyVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Text(
                                text = if (keyVisible) "HIDE" else "SHOW",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val statusColor = when {
                        apiKeyStatus.contains("Verified", ignoreCase = true) || apiKeyStatus.contains("Active", ignoreCase = true) -> Color(0xFF2E7D32) // Green
                        apiKeyStatus.contains("Invalid", ignoreCase = true) -> Color(0xFFD32F2F) // Red
                        apiKeyStatus.contains("Limit", ignoreCase = true) || apiKeyStatus.contains("Quota", ignoreCase = true) || apiKeyStatus.contains("Exhausted", ignoreCase = true) -> Color(0xFFE65100) // Orange/Red
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status: $apiKeyStatus",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        )
                    }
                    
                    Button(
                        onClick = { onTestApiKey(customApiKey) },
                        enabled = !isCheckingApiKey && customApiKey.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isCheckingApiKey) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Test Key", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                TextButton(
                    onClick = onShowOnboardingGuide,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Help")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("How do I get a free API Key?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BookmarksSheetContent(
    bookmarks: List<com.example.data.database.Bookmark>,
    onBookmarkClick: (com.example.data.database.Bookmark) -> Unit,
    onDeleteBookmark: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Text(
            text = "SAVED BOOKMARKS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        if (bookmarks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Empty Bookmarks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved bookmarks yet.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks) { bm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookmarkClick(bm) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = bm.hour,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = bm.date,
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bm.sectionType.uppercase() + ": " + bm.sectionTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = bm.snippet,
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// Helpers for Lay Blessing vs Clerical Dismissal
fun getLayOrClericalDismissal(prayer: LiturgicalPrayer, lang: AppLanguage, soloMode: Boolean): String {
    if (!soloMode) return prayer.getDismissal(lang)

    return when (lang) {
        AppLanguage.ENGLISH -> "May the Lord bless us, protect us from all evil, and bring us to everlasting life. Amen."
        AppLanguage.TAGALOG -> "Pagpalain nawa tayo ng Panginoon, iligtas tayo sa rurok ng kasamaan at ihatid tayo sa buhay na walang hanggan. Amen."
        AppLanguage.LATIN -> "Nos benedícat Dóminus, et ab omni malo deféndat, et dedúcat ad vitam ætérnam. Amen."
    }
}

fun cleanCue(text: String): String {
    val trimmed = text.trim()
    val idx = trimmed.indexOf(":")
    if (idx != -1) return trimmed.substring(idx + 1).trim()
    val idxDot = trimmed.indexOf(".")
    if (idxDot != -1) return trimmed.substring(idxDot + 1).trim()
    return trimmed
}

fun cbcpBadgeText(season: String): String {
    if (season.contains("Santo Niño", ignoreCase = true)) return "SANTO NIÑO SOLEMNITY"
    if (season.contains("Simbang Gabi", ignoreCase = true)) return "SIMBANG GABI NOVENA"
    if (season.contains("Pedro Calungsod", ignoreCase = true)) return "FEAST OF SAN PEDRO CALUNGSOD"
    if (season.contains("Lorenzo Ruiz", ignoreCase = true)) return "FEAST OF SAN LORENZO RUIZ"
    return season.uppercase()
}

// Speaks the entire prayer with impeccable flow
fun buildSpeechText(prayer: LiturgicalPrayer, lang: AppLanguage, soloActive: Boolean): String {
    val sb = StringBuilder()
    sb.append(prayer.getTitle(lang)).append(". ")
    sb.append("Intended Versicle. ").append(prayer.getOpeningVerseCall(lang)).append(". ")
    sb.append("Response. ").append(prayer.getOpeningVerseResponse(lang)).append(". ")
    
    sb.append("Hymn. ").append(prayer.getHymnTitle(lang)).append(". ")
    sb.append(prayer.getHymnText(lang)).append(". ")
    
    prayer.psalms.forEach { psalm ->
        sb.append(psalm.title).append(". ")
        psalm.getSubtitle(lang)?.let { sb.append(it).append(". ") }
        sb.append("Antiphon. ").append(psalm.getAntiphonBefore(lang)).append(". ")
        sb.append(psalm.getText(lang)).append(". ")
        sb.append("Antiphon repeat. ").append(psalm.getAntiphonAfter(lang)).append(". ")
    }
    
    sb.append("Scripture. ").append(prayer.getReadingText(lang)).append(". ")
    sb.append(prayer.getReadingResponse(lang)).append(". ")
    
    if (soloActive) {
        sb.append("Versicle. ").append(cleanCue(prayer.getResponsoryVersicle(lang)))
          .append(". Response. ").append(cleanCue(prayer.getResponsoryResponse(lang))).append(". ")
    } else {
        sb.append("Versicle: ").append(prayer.getResponsoryVersicle(lang)).append(". ")
        sb.append("Response: ").append(prayer.getResponsoryResponse(lang)).append(". ")
    }
    
    sb.append("Gospel Canticle: ").append(prayer.getCanticleTitle(lang)).append(". ")
    sb.append("Antiphon before. ").append(prayer.getCanticleAntiphonBefore(lang)).append(". ")
    sb.append(prayer.getCanticleText(lang)).append(". ")
    sb.append("Antiphon after. ").append(prayer.getCanticleAntiphonAfter(lang)).append(". ")
    
    sb.append("Intercessions: ").append(prayer.getIntercessionsPreface(lang)).append(". ")
    sb.append("General Response: ").append(prayer.getIntercessionsResponse(lang)).append(". ")
    prayer.intercessionsList.forEach { item ->
        sb.append(item.getPetition(lang)).append(". ")
        sb.append("Response: ").append(item.getResponse(lang)).append(". ")
    }
    
    sb.append("The Lord's Prayer. ").append(prayer.getLordPrayer(lang)).append(". ")
    sb.append("Concluding Prayer. ").append(prayer.getClosingPrayer(lang)).append(". ")
    
    val finalDismissal = getLayOrClericalDismissal(prayer, lang, soloActive)
    sb.append("Dismissal. ").append(finalDismissal)
    
    return sb.toString()
}

@Composable
fun OnboardingGuideOverlay(
    isFirstLaunch: Boolean,
    onDismiss: (Boolean) -> Unit,
    onGoToSettings: () -> Unit
) {
    var hideAgainChecked by remember { mutableStateOf(false) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!isFirstLaunch) onDismiss(hideAgainChecked) },
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = !isFirstLaunch,
            dismissOnClickOutside = !isFirstLaunch,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .wrapContentHeight()
                    .padding(vertical = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Headline and Icon
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(
                            onClick = {},
                            enabled = false,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text("⚠️", fontSize = 24.sp)
                        }
                        Text(
                            text = "ACTION REQUIRED: Free Gemini Key Needed for Daily Prayers Online",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Body
                    Text(
                        text = "To access real-time, daily liturgical prayers online, this app requires you to supply your own free API Key. Without a key, the app will run entirely in Offline Fallback Mode.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Interactive Steps Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🔑 Step-by-Step API Setup",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            
                            val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                            
                            ClickableStepRow(
                                stepNumber = "1",
                                text = "Open Google AI Studio",
                                linkText = "aistudio.google.com",
                                onClick = {
                                    uriHandler.openUri("https://aistudio.google.com/")
                                }
                            )

                            ClickableStepRow(
                                stepNumber = "2",
                                text = "Log in with any Google Account",
                                linkText = null,
                                onClick = {}
                            )

                            ClickableStepRow(
                                stepNumber = "3",
                                text = "Click \"Get API Key\" then \"Create API Key\"",
                                linkText = null,
                                onClick = {}
                            )

                            ClickableStepRow(
                                stepNumber = "4",
                                text = "Copy the key, return to Settings, and paste it",
                                linkText = null,
                                onClick = {}
                            )
                        }
                    }

                    // For scenario B (Subsequent launches): Show "Don't show again" checkbox
                    if (!isFirstLaunch) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hideAgainChecked = !hideAgainChecked }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = hideAgainChecked,
                                onCheckedChange = { hideAgainChecked = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Don't show this intro on startup",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip / Close option (Only if subsequent launch)
                        if (!isFirstLaunch) {
                            TextButton(
                                onClick = { onDismiss(hideAgainChecked) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Skip / Use Offline")
                            }
                        }

                        Button(
                            onClick = onGoToSettings,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth(if (isFirstLaunch) 1f else 0.6f)
                        ) {
                            Text("Got it, Go to Settings", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClickableStepRow(
    stepNumber: String,
    text: String,
    linkText: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = linkText != null) { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp)
            )
            if (linkText != null) {
                Text(
                    text = linkText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                )
            }
        }
        if (linkText != null) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Open Link",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)
            )
        }
    }
}
