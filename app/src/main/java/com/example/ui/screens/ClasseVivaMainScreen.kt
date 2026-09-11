package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Attendance
import com.example.data.Student
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClassStats
import com.example.ui.viewmodel.ClasseVivaViewModel
import com.example.ui.viewmodel.StudentAttendanceState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClasseVivaMainScreen(
    viewModel: ClasseVivaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val selectedDateStr by viewModel.selectedDate.collectAsStateWithLifecycle()
    val attendanceStates by viewModel.attendanceStates.collectAsStateWithLifecycle()
    val allStudents by viewModel.students.collectAsStateWithLifecycle()
    val classStats by viewModel.classStats.collectAsStateWithLifecycle()
    val studentStatsMap by viewModel.studentStatsMap.collectAsState(initial = emptyMap())
    val searchQuery by viewModel.searchQueries.collectAsStateWithLifecycle()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Appello, 1: Studenti
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var showAddNotesDialogForStudentId by remember { mutableStateOf<Int?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    // Parse selected date to display formatted
    val calendar = Calendar.getInstance()
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("EEEE d MMMM yyyy", Locale.ITALIAN)
    
    val formattedDateDisplay = remember(selectedDateStr) {
        try {
            val date = sdfDb.parse(selectedDateStr) ?: Date()
            sdfDisplay.format(date).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            selectedDateStr
        }
    }

    // Function to launch DatePickerDialog
    fun showDatePicker() {
        try {
            val parsedDate = sdfDb.parse(selectedDateStr) ?: Date()
            val tempCal = Calendar.getInstance()
            tempCal.time = parsedDate
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance()
                    selectedCal.set(year, month, dayOfMonth)
                    viewModel.setDate(sdfDb.format(selectedCal.time))
                },
                tempCal.get(Calendar.YEAR),
                tempCal.get(Calendar.MONTH),
                tempCal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.show()
        } catch (e: Exception) {
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                viewModel.setDate(sdfDb.format(selectedCal.time))
            }, currentYear, currentMonth, currentDay).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Diario Casa del Giocattolo",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.5.sp,
                                fontSize = 18.sp
                            )
                        )
                        Text(
                            text = "REGISTRO PRESENZE giornaliero",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = GrayTextSecondary,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Info or logo */ }) {
                        Icon(
                            imageVector = Icons.Default.VolunteerActivism,
                            contentDescription = "No Profit Logo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Esporta Registro",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDatePicker() }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Apri Calendario",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (activeTab == 1) {
                ExtendedFloatingActionButton(
                    text = { Text("Aggiungi Bambino") },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = { showAddStudentDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
            } else if (attendanceStates.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("Tutti Presenti") },
                    icon = { Icon(Icons.Default.DoneAll, contentDescription = null) },
                    onClick = { viewModel.markAllPresent() },
                    containerColor = StatusPresent,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Horizontal calendar strip
            CalendarStrip(
                selectedDateStr = selectedDateStr,
                onDateSelected = { viewModel.setDate(it) }
            )

            // Dynamic Dashboard Stats Card
            DashboardStatsCard(
                formattedDateDisplay = formattedDateDisplay,
                stats = classStats,
                onCalendarClick = { showDatePicker() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab bar
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Appello Oggi", fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Iscritti", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            // Search Bar for Students
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Cerca bambino...", color = GrayTextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayTextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Pulisci", tint = GrayTextSecondary)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )

            // Content based on active tab
            Crossfade(targetState = activeTab, label = "tabTransition") { tab ->
                when (tab) {
                    0 -> AppelloTabContent(
                        attendanceStates = attendanceStates,
                        searchQuery = searchQuery,
                        onStatusChange = { studentId, status ->
                            viewModel.setAttendance(studentId, status)
                        },
                        onAddNotesClick = { studentId ->
                            showAddNotesDialogForStudentId = studentId
                        }
                    )
                    1 -> StudentiTabContent(
                        studentsList = allStudents,
                        searchQuery = searchQuery,
                        studentStatsMap = studentStatsMap,
                        onEditStudent = { editingStudent = it },
                        onDeleteStudent = { viewModel.deleteStudent(it) }
                    )
                }
            }
        }
    }

    // Add Student Dialog
    if (showAddStudentDialog) {
        StudentFormDialog(
            title = "Aggiungi Alunno",
            onDismiss = { showAddStudentDialog = false },
            onSave = { first, last, note, days ->
                viewModel.addStudent(first, last, note, days)
                showAddStudentDialog = false
            }
        )
    }

    // Edit Student Dialog
    editingStudent?.let { student ->
        StudentFormDialog(
            title = "Modifica Alunno",
            student = student,
            onDismiss = { editingStudent = null },
            onSave = { first, last, note, days ->
                viewModel.updateStudent(student.copy(firstName = first, lastName = last, note = note, attendingDays = days))
                editingStudent = null
            }
        )
    }

    // Add/Edit Attendance Notes Dialog
    showAddNotesDialogForStudentId?.let { studentId ->
        val existingAttendance = attendanceStates.firstOrNull { it.student.id == studentId }?.attendance
        val studentName = attendanceStates.firstOrNull { it.student.id == studentId }?.student?.fullName ?: ""
        
        AttendanceNotesDialog(
            studentName = studentName,
            initialNotes = existingAttendance?.notes ?: "",
            initialStatus = existingAttendance?.status ?: Attendance.STATUS_PRESENT,
            onDismiss = { showAddNotesDialogForStudentId = null },
            onSave = { notes, status ->
                viewModel.setAttendance(studentId, status, notes)
                showAddNotesDialogForStudentId = null
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportRegisterDialog(
            onDismiss = { showExportDialog = false },
            onShare = { start, end ->
                coroutineScope.launch {
                    val reportText = viewModel.generateExportReport(start, end)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, reportText)
                        putExtra(Intent.EXTRA_SUBJECT, "Registro Diario Casa del Giocattolo")
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Esporta Registro")
                    context.startActivity(shareIntent)
                }
                showExportDialog = false
            },
            onCopy = { start, end ->
                coroutineScope.launch {
                    val reportText = viewModel.generateExportReport(start, end)
                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clipData = android.content.ClipData.newPlainText("Registro", reportText)
                    clipboardManager.setPrimaryClip(clipData)
                    android.widget.Toast.makeText(context, "Tabella copiata negli appunti! Ora puoi incollarlo su Google Sheets.", android.widget.Toast.LENGTH_LONG).show()
                }
                showExportDialog = false
            },
            onExportPdf = { start, end ->
                coroutineScope.launch {
                    val pdfFile = if (start == end) {
                        val presenzeList = viewModel.getPresenzeForDateRange(start, end)
                        com.example.util.PdfExporter.exportPresenzeToPdf(
                            context = context,
                            lista = presenzeList,
                            fileName = "presenze_${start}.pdf"
                        )
                    } else {
                        val (headers, gridRows) = viewModel.getGridDataForDateRange(start, end)
                        com.example.util.PdfExporter.exportRegistroGridToPdf(
                            context = context,
                            dates = headers,
                            rows = gridRows,
                            fileName = "registro_${start}_al_${end}.pdf"
                        )
                    }
                    if (pdfFile != null && pdfFile.exists()) {
                        com.example.util.PdfExporter.sharePdf(context, pdfFile)
                    } else {
                        android.widget.Toast.makeText(context, "Errore nella generazione del PDF", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                showExportDialog = false
            }
        )
    }
}

@Composable
fun CalendarStrip(
    selectedDateStr: String,
    onDateSelected: (String) -> Unit
) {
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEE", Locale.ITALIAN)
    val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())

    val dayMonthFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    // Base calendar/center date tracker to keep list generation stable
    var centerDate by remember {
        mutableStateOf(
            try {
                sdfDb.parse(selectedDateStr) ?: Date()
            } catch (e: Exception) {
                Date()
            }
        )
    }

    // Generate a generous window of dates around the center date
    // 60 days before, 60 days after (121 days total)
    val dates = remember(centerDate) {
        val list = mutableListOf<Date>()
        val cal = Calendar.getInstance()
        cal.time = centerDate
        cal.add(Calendar.DAY_OF_YEAR, -60)
        for (i in 0 until 121) {
            list.add(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    // Shift the center date only if selected date falls completely outside our current generated list
    LaunchedEffect(selectedDateStr) {
        val isInList = dates.any { sdfDb.format(it) == selectedDateStr }
        if (!isInList) {
            try {
                val parsed = sdfDb.parse(selectedDateStr)
                if (parsed != null) {
                    centerDate = parsed
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidthDp = configuration.screenWidthDp
    val itemWidthDp = 58.dp

    val centerOffsetPx = with(density) {
        ((screenWidthDp.dp - itemWidthDp) / 2).toPx().toInt()
    }

    // Smoothly scroll the selected date to center view
    LaunchedEffect(selectedDateStr, dates) {
        val index = dates.indexOfFirst { sdfDb.format(it) == selectedDateStr }
        if (index != -1) {
            listState.animateScrollToItem(index, -centerOffsetPx)
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(dates) { date ->
            val dateStr = sdfDb.format(date)
            val isSelected = dateStr == selectedDateStr
            
            val isToday = remember {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                dateStr == todayStr
            }

            Column(
                modifier = Modifier
                    .width(itemWidthDp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else Color.Transparent
                    )
                    .clickable { onDateSelected(dateStr) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayFormat.format(date).replace(".", "").uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else if (isToday) MaterialTheme.colorScheme.primary 
                                else GrayTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayMonthFormat.format(date),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                else if (isToday) MaterialTheme.colorScheme.primary 
                                else GrayTextPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun DashboardStatsCard(
    formattedDateDisplay: String,
    stats: ClassStats,
    onCalendarClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onCalendarClick() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDateDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = GrayTextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${stats.presencePercentage}% Presenze",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Counts Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Presenti",
                    value = stats.presentCount.toString(),
                    color = StatusPresent,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Assenti",
                    value = stats.absentCount.toString(),
                    color = StatusAbsent,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Ritardi",
                    value = stats.lateCount.toString(),
                    color = StatusLate,
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Uscite",
                    value = stats.earlyLeaveCount.toString(),
                    color = StatusEarly,
                    modifier = Modifier.weight(1f)
                )
            }

            if (stats.missingRecordsCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mancano ancora ${stats.missingRecordsCount} registrazioni per oggi.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Black,
                color = color,
                fontSize = 24.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = GrayTextSecondary,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun AppelloTabContent(
    attendanceStates: List<StudentAttendanceState>,
    searchQuery: String,
    onStatusChange: (Int, String) -> Unit,
    onAddNotesClick: (Int) -> Unit
) {
    if (attendanceStates.isEmpty()) {
        if (searchQuery.isNotBlank()) {
            EmptyStateView(
                title = "Nessun risultato",
                subtitle = "Nessun bambino corrisponde alla ricerca per il giorno selezionato."
            )
        } else {
            EmptyStateView(
                title = "Nessun bambino previsto oggi",
                subtitle = "Non ci sono bambini registrati per questo giorno della settimana. Aggiungili dal menu 'Iscritti' o cambia giorno."
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
        ) {
            items(attendanceStates, key = { it.student.id }) { state ->
                val student = state.student
                val attendance = state.attendance

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudentInitialsAvatar(
                                firstName = student.firstName,
                                lastName = student.lastName,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GrayTextPrimary
                                    )
                                )
                                if (attendance?.notes?.isNotEmpty() == true) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notes,
                                            contentDescription = null,
                                            tint = GrayTextSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = attendance.notes,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = GrayTextSecondary,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onAddNotesClick(student.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (attendance?.notes?.isNotEmpty() == true) Icons.Default.EditNote else Icons.Default.AddComment,
                                    contentDescription = "Aggiungi Nota",
                                    tint = if (attendance?.notes?.isNotEmpty() == true) MaterialTheme.colorScheme.primary else GrayTextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Selection row for attendance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AttendanceBadgeOption(
                                letter = "P",
                                label = "Pres.",
                                color = StatusPresent,
                                isSelected = attendance?.status == Attendance.STATUS_PRESENT,
                                onClick = { onStatusChange(student.id, Attendance.STATUS_PRESENT) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AttendanceBadgeOption(
                                letter = "A",
                                label = "Ass.",
                                color = StatusAbsent,
                                isSelected = attendance?.status == Attendance.STATUS_ABSENT,
                                onClick = { onStatusChange(student.id, Attendance.STATUS_ABSENT) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AttendanceBadgeOption(
                                letter = "R",
                                label = "Rit.",
                                color = StatusLate,
                                isSelected = attendance?.status == Attendance.STATUS_LATE,
                                onClick = { onStatusChange(student.id, Attendance.STATUS_LATE) },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AttendanceBadgeOption(
                                letter = "U",
                                label = "Usc.",
                                color = StatusEarly,
                                isSelected = attendance?.status == Attendance.STATUS_EARLY_LEAVE,
                                onClick = { onStatusChange(student.id, Attendance.STATUS_EARLY_LEAVE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceBadgeOption(
    letter: String,
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isSelected) color else color.copy(alpha = 0.08f),
        border = if (isSelected) null else BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = letter,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = if (isSelected) Color.White else color,
                    fontSize = 14.sp
                )
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else GrayTextPrimary,
                    fontSize = 9.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StudentInitialsAvatar(
    firstName: String,
    lastName: String,
    modifier: Modifier = Modifier
) {
    val initials = remember(firstName, lastName) {
        val f = firstName.firstOrNull()?.uppercaseChar() ?: '?'
        val l = lastName.firstOrNull()?.uppercaseChar() ?: '?'
        "$l$f"
    }

    val backgroundColor = remember(firstName, lastName) {
        val sum = firstName.hashCode() + lastName.hashCode()
        val index = kotlin.math.abs(sum) % 5
        when (index) {
            0 -> Color(0xFF0061A4)
            1 -> Color(0xFF10B981)
            2 -> Color(0xFF6750A4)
            3 -> Color(0xFFF59E0B)
            else -> Color(0xFF14B8A6)
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
        )
    }
}

@Composable
fun StudentiTabContent(
    studentsList: List<Student>,
    searchQuery: String,
    studentStatsMap: Map<Int, com.example.ui.viewmodel.StudentSummaryStats>,
    onEditStudent: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit
) {
    if (studentsList.isEmpty()) {
        EmptyStateView(
            title = "Nessun bambino iscritto",
            subtitle = "Premi sul bottone '+' in basso a destra per inserire i primi iscritti."
        )
    } else {
        val filteredStudents = remember(studentsList, searchQuery) {
            if (searchQuery.isBlank()) {
                studentsList.sortedBy { it.lastName }
            } else {
                studentsList.filter {
                    val fullName1 = "${it.firstName} ${it.lastName}"
                    val fullName2 = "${it.lastName} ${it.firstName}"
                    fullName1.contains(searchQuery, ignoreCase = true) ||
                    fullName2.contains(searchQuery, ignoreCase = true)
                }.sortedBy { it.lastName }
            }
        }

        if (filteredStudents.isEmpty()) {
            EmptyStateView(
                title = "Nessun risultato",
                subtitle = "Nessun bambino corrisponde alla ricerca."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
            ) {
                items(filteredStudents, key = { it.id }) { student ->
                val stats = studentStatsMap[student.id]
                var expanded by remember { mutableStateOf(false) }

                val weekdaysLabel = remember(student.attendingDays) {
                    val days = student.attendingDays.split(",")
                    val labels = mutableListOf<String>()
                    if (days.contains("1")) labels.add("Lun")
                    if (days.contains("2")) labels.add("Mar")
                    if (days.contains("3")) labels.add("Mer")
                    if (days.contains("4")) labels.add("Gio")
                    if (days.contains("5")) labels.add("Ven")
                    if (days.contains("6")) labels.add("Sab")
                    if (days.contains("7")) labels.add("Dom")
                    labels.joinToString(", ")
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { expanded = !expanded },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudentInitialsAvatar(
                                firstName = student.firstName,
                                lastName = student.lastName,
                                modifier = Modifier.size(44.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.fullName,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = GrayTextPrimary
                                    )
                                )
                                Text(
                                    text = "Giorni: $weekdaysLabel",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            stats?.let {
                                val rateColor = when {
                                    it.presenceRate >= 90 -> StatusPresent
                                    it.presenceRate >= 75 -> StatusLate
                                    else -> StatusAbsent
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(rateColor.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${it.presenceRate}%",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = rateColor
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = GrayTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedVisibility(
                            visible = expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Divider(color = GrayTextSecondary.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(12.dp))

                                if (student.note.isNotEmpty()) {
                                    Text(
                                        text = "Note Personali:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = GrayTextSecondary
                                        )
                                    )
                                    Text(
                                        text = student.note,
                                        style = MaterialTheme.typography.bodyMedium.copy(color = GrayTextPrimary),
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                }

                                stats?.let {
                                    Text(
                                        text = "RIEPILOGO PRESENZE",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = GrayTextSecondary,
                                            letterSpacing = 1.sp
                                        ),
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        MiniStatDisplay("Pres.", it.presentCount, StatusPresent)
                                        MiniStatDisplay("Ass.", it.absentCount, StatusAbsent)
                                        MiniStatDisplay("Rit.", it.lateCount, StatusLate)
                                        MiniStatDisplay("Usc.", it.earlyCount, StatusEarly)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = { onDeleteStudent(student) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Elimina")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { onEditStudent(student) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Modifica")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun MiniStatDisplay(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        modifier = Modifier.width(72.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = color
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = GrayTextSecondary,
                    fontSize = 9.sp
                )
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolunteerActivism,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = GrayTextPrimary
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = GrayTextSecondary
            ),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentFormDialog(
    title: String,
    student: Student? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var firstName by remember { mutableStateOf(student?.firstName ?: "") }
    var lastName by remember { mutableStateOf(student?.lastName ?: "") }
    var note by remember { mutableStateOf(student?.note ?: "") }

    // Parse attending days
    val initialDays = remember(student) {
        student?.attendingDays?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() 
            ?: setOf(1, 2, 3, 4, 5) // Defaults to Lun-Ven
    }
    val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(initialDays) } }

    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { 
                        lastName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Cognome") },
                    placeholder = { Text("Es. Rossi") },
                    isError = isError && lastName.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { 
                        firstName = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Nome") },
                    placeholder = { Text("Es. Mario") },
                    isError = isError && firstName.isBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note personali (facoltative)") },
                    placeholder = { Text("Es. Delegato al ritiro, allergie") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Weekday selection
                Text(
                    text = "Giorni di Frequenza Settimanale:",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GrayTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Row of weekday chips
                val daysOfWeek = listOf(
                    1 to "Lun",
                    2 to "Mar",
                    3 to "Mer",
                    4 to "Gio",
                    5 to "Ven",
                    6 to "Sab",
                    7 to "Dom"
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    daysOfWeek.forEach { (index, name) ->
                        val isSelected = selectedDays.contains(index)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (selectedDays.size > 1) {
                                        selectedDays.remove(index)
                                    }
                                } else {
                                    selectedDays.add(index)
                                }
                            },
                            label = { Text(name) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (isError) {
                    Text(
                        text = "Nome e Cognome sono obbligatori.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annulla")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (firstName.isNotBlank() && lastName.isNotBlank()) {
                                val daysStr = selectedDays.sorted().joinToString(",")
                                onSave(firstName, lastName, note, daysStr)
                            } else {
                                isError = true
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salva")
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceNotesDialog(
    studentName: String,
    initialNotes: String,
    initialStatus: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var notes by remember { mutableStateOf(initialNotes) }
    var selectedStatus by remember { mutableStateOf(initialStatus) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Aggiungi Nota di Appello",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = studentName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GrayTextSecondary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Status select
                Text(
                    text = "Cambia Stato:",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = GrayTextSecondary)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Attendance.STATUS_PRESENT to "Pres.",
                        Attendance.STATUS_ABSENT to "Ass.",
                        Attendance.STATUS_LATE to "Rit.",
                        Attendance.STATUS_EARLY_LEAVE to "Usc."
                    ).forEach { (status, label) ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(label) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Nota o giustificazione") },
                    placeholder = { Text("Es. Entra alle 10:30, visita medica") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annulla")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onSave(notes, selectedStatus) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Conferma")
                    }
                }
            }
        }
    }
}

@Composable
fun ExportRegisterDialog(
    onDismiss: () -> Unit,
    onShare: (String, String) -> Unit,
    onCopy: (String, String) -> Unit,
    onExportPdf: (String, String) -> Unit
) {
    val context = LocalContext.current
    val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfDisplay = SimpleDateFormat("dd MMMM yyyy", Locale.ITALIAN)

    // Default: Start date is 7 days ago, End date is today
    val today = Calendar.getInstance()
    val endStr = sdfDb.format(today.time)
    
    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val startStr = sdfDb.format(sevenDaysAgo.time)

    var startDateStr by remember { mutableStateOf(startStr) }
    var endDateStr by remember { mutableStateOf(endStr) }

    val formattedStart = remember(startDateStr) {
        try {
            val d = sdfDb.parse(startDateStr) ?: Date()
            sdfDisplay.format(d)
        } catch (e: Exception) { startDateStr }
    }

    val formattedEnd = remember(endDateStr) {
        try {
            val d = sdfDb.parse(endDateStr) ?: Date()
            sdfDisplay.format(d)
        } catch (e: Exception) { endDateStr }
    }

    fun showDatePicker(isStart: Boolean) {
        val currentSelectedStr = if (isStart) startDateStr else endDateStr
        val cal = Calendar.getInstance()
        try {
            val parsed = sdfDb.parse(currentSelectedStr) ?: Date()
            cal.time = parsed
        } catch (e: Exception) {
            // fallback
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val chosenCal = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                val formatted = sdfDb.format(chosenCal.time)
                if (isStart) {
                    startDateStr = formatted
                } else {
                    endDateStr = formatted
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Esporta Registro Presenze",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scegli l'intervallo di date per esportare o stampare l'appello dei bambini.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = GrayTextSecondary)
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Start Date selection
                Text(
                    text = "Data Inizio:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayTextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { showDatePicker(isStart = true) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = formattedStart, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // End Date selection
                Text(
                    text = "Data Fine:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayTextSecondary)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = { showDatePicker(isStart = false) },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineVariant),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = formattedEnd, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Annulla")
                        }
                        OutlinedButton(
                            onClick = { onCopy(startDateStr, endDateStr) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copia")
                        }
                        OutlinedButton(
                            onClick = { onShare(startDateStr, endDateStr) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Condividi Testo")
                        }
                    }
                    Button(
                        onClick = { onExportPdf(startDateStr, endDateStr) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Esporta in PDF Tabellare")
                    }
                }
            }
        }
    }
}
