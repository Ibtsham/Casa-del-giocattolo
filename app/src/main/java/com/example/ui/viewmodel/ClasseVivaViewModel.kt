package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Attendance
import com.example.data.Student
import com.example.data.StudentRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class StudentAttendanceState(
    val student: Student,
    val attendance: Attendance?
)

class ClasseVivaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudentRepository
    val selectedDate = MutableStateFlow(getCurrentDateString())
    val searchQueries = MutableStateFlow("")

    init {
        val database = AppDatabase.getDatabase(application)
        repository = StudentRepository(database.studentDao(), database.attendanceDao())
    }

    // Helper to get day of week (1=Lun, 2=Mar, ..., 7=Dom)
    fun getDayOfWeek(dateStr: String): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        return try {
            val date = sdf.parse(dateStr) ?: Date()
            cal.time = date
            val rawDay = cal.get(Calendar.DAY_OF_WEEK)
            when (rawDay) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        } catch (e: Exception) {
            1
        }
    }

    // Expose student list directly for Student management (lists all students)
    val students: StateFlow<List<Student>> = repository.allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Expose fully reactive combined student and attendance state (Filtered by day of week)
    val attendanceStates: StateFlow<List<StudentAttendanceState>> = combine(
        repository.allStudents,
        repository.allAttendance,
        selectedDate,
        searchQueries
    ) { studentsList, allAttendance, date, query ->
        val dayOfWeek = getDayOfWeek(date)
        // Filter students that actually attend on this day of the week
        val attendingStudents = studentsList.filter { it.attendsOnDay(dayOfWeek) }
        
        val attendanceForDateMap = allAttendance.filter { it.date == date }.associateBy { it.studentId }
        val filteredStudents = if (query.isBlank()) {
            attendingStudents
        } else {
            attendingStudents.filter {
                val fullName1 = "${it.firstName} ${it.lastName}"
                val fullName2 = "${it.lastName} ${it.firstName}"
                fullName1.contains(query, ignoreCase = true) || 
                fullName2.contains(query, ignoreCase = true)
            }
        }
        filteredStudents.map { student ->
            StudentAttendanceState(student, attendanceForDateMap[student.id])
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Expose overall stats (e.g. number of students attending today, attendance percentages, etc.)
    val classStats: StateFlow<ClassStats> = combine(
        repository.allStudents,
        repository.allAttendance,
        selectedDate
    ) { studentsList, allAttendance, date ->
        val dayOfWeek = getDayOfWeek(date)
        val attendingStudents = studentsList.filter { it.attendsOnDay(dayOfWeek) }
        val totalStudents = attendingStudents.size
        
        val attendanceToday = allAttendance.filter { it.date == date && attendingStudents.any { s -> s.id == it.studentId } }
        
        val presentToday = attendanceToday.count { it.status == Attendance.STATUS_PRESENT }
        val absentToday = attendanceToday.count { it.status == Attendance.STATUS_ABSENT }
        val lateToday = attendanceToday.count { it.status == Attendance.STATUS_LATE }
        val earlyToday = attendanceToday.count { it.status == Attendance.STATUS_EARLY_LEAVE }
        
        val missingRecords = totalStudents - attendanceToday.size
        val presencePercentage = if (totalStudents > 0) {
            val actualPresent = presentToday + lateToday + earlyToday
            (actualPresent * 100f / totalStudents).toInt()
        } else {
            100
        }

        ClassStats(
            totalStudents = totalStudents,
            presentCount = presentToday,
            absentCount = absentToday,
            lateCount = lateToday,
            earlyLeaveCount = earlyToday,
            missingRecordsCount = if (missingRecords < 0) 0 else missingRecords,
            presencePercentage = presencePercentage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClassStats()
    )

    // Expose student-specific stats flow
    val studentStatsMap: Flow<Map<Int, StudentSummaryStats>> = combine(
        repository.allStudents,
        repository.allAttendance
    ) { studentsList, allAttendance ->
        studentsList.associate { student ->
            val records = allAttendance.filter { it.studentId == student.id }
            val present = records.count { it.status == Attendance.STATUS_PRESENT }
            val absent = records.count { it.status == Attendance.STATUS_ABSENT }
            val late = records.count { it.status == Attendance.STATUS_LATE }
            val early = records.count { it.status == Attendance.STATUS_EARLY_LEAVE }
            val total = records.size
            val rate = if (total > 0) ((present + late + early) * 100f / total).toInt() else 100
            student.id to StudentSummaryStats(rate, present, absent, late, early)
        }
    }

    // Methods to modify data
    fun addStudent(firstName: String, lastName: String, note: String = "", attendingDays: String = "1,2,3,4,5") {
        viewModelScope.launch {
            repository.insertStudent(
                Student(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    note = note.trim(),
                    attendingDays = attendingDays
                )
            )
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch {
            repository.updateStudent(student)
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    fun setAttendance(studentId: Int, status: String, notes: String = "") {
        viewModelScope.launch {
            val date = selectedDate.value
            val currentStates = attendanceStates.value
            val existingId = currentStates.firstOrNull { it.student.id == studentId }?.attendance?.id ?: 0
            
            repository.saveAttendance(
                Attendance(
                    id = existingId,
                    studentId = studentId,
                    date = date,
                    status = status,
                    notes = notes
                )
            )
        }
    }

    fun markAllPresent() {
        viewModelScope.launch {
            val date = selectedDate.value
            val dayOfWeek = getDayOfWeek(date)
            val studentsList = students.value.filter { it.attendsOnDay(dayOfWeek) }
            val currentStates = attendanceStates.value
            val existingMap = currentStates.associate { it.student.id to it.attendance?.id }

            studentsList.forEach { student ->
                val existingId = existingMap[student.id] ?: 0
                repository.saveAttendance(
                    Attendance(
                        id = existingId,
                        studentId = student.id,
                        date = date,
                        status = Attendance.STATUS_PRESENT
                    )
                )
            }
        }
    }

    fun setDate(date: String) {
        selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        searchQueries.value = query
    }

    // Export function to format structured text report as a table/spreadsheet
    suspend fun generateExportReport(startDateStr: String, endDateStr: String): String {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfColHeader = SimpleDateFormat("dd/MM (EEE)", Locale.ITALIAN)
        
        // Fetch all students and attendance from Flow
        val studentsList = repository.allStudents.first().sortedBy { it.lastName }
        val allAttendance = repository.allAttendance.first()

        val dates = mutableListOf<String>()
        try {
            val start = sdfDb.parse(startDateStr) ?: Date()
            val end = sdfDb.parse(endDateStr) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = start
            while (!cal.time.after(end)) {
                dates.add(sdfDb.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val builder = StringBuilder()

        // Row 1: Header (Alunno, dates, stats)
        builder.append("Alunno")
        dates.forEach { dateStr ->
            val parsedDate = sdfDb.parse(dateStr) ?: Date()
            val colLabel = sdfColHeader.format(parsedDate).replaceFirstChar { it.uppercase() }
            builder.append("\t").append(colLabel)
        }
        builder.append("\t").append("Presenze")
        builder.append("\t").append("Assenze")
        builder.append("\n")

        // Group attendance by date and then by studentId
        val attendanceByDateAndStudent = allAttendance.groupBy { it.date }.mapValues { entry ->
            entry.value.associateBy { it.studentId }
        }

        // For each student, compute attendance status for each date and overall stats
        studentsList.forEach { student ->
            builder.append(student.fullName)
            
            var totalPresences = 0
            var totalAbsences = 0
            
            dates.forEach { dateStr ->
                val dayOfWeek = getDayOfWeek(dateStr)
                val isScheduled = student.attendsOnDay(dayOfWeek)
                
                if (isScheduled) {
                    val att = attendanceByDateAndStudent[dateStr]?.get(student.id)
                    val symbol = when (att?.status) {
                        Attendance.STATUS_PRESENT -> {
                            totalPresences++
                            "P"
                        }
                        Attendance.STATUS_ABSENT -> {
                            totalAbsences++
                            "A"
                        }
                        Attendance.STATUS_LATE -> {
                            totalPresences++
                            "R"
                        }
                        Attendance.STATUS_EARLY_LEAVE -> {
                            totalPresences++
                            "U"
                        }
                        else -> {
                            ""
                        }
                    }
                    builder.append("\t").append(symbol)
                } else {
                    builder.append("\t").append("-")
                }
            }
            
            builder.append("\t").append(totalPresences)
            builder.append("\t").append(totalAbsences)
            builder.append("\n")
        }

        return builder.toString()
    }

    suspend fun getPresenzeForDateRange(startDateStr: String, endDateStr: String): List<com.example.util.Presenza> {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfDisplay = SimpleDateFormat("dd/MM", Locale.ITALIAN)
        
        val studentsList = repository.allStudents.first().sortedBy { it.lastName }
        val allAttendance = repository.allAttendance.first()

        val dates = mutableListOf<String>()
        try {
            val start = sdfDb.parse(startDateStr) ?: Date()
            val end = sdfDb.parse(endDateStr) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = start
            while (!cal.time.after(end)) {
                dates.add(sdfDb.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val attendanceByDateAndStudent = allAttendance.groupBy { it.date }.mapValues { entry ->
            entry.value.associateBy { it.studentId }
        }

        val presenzeList = mutableListOf<com.example.util.Presenza>()

        dates.forEach { dateStr ->
            val dayOfWeek = getDayOfWeek(dateStr)
            val parsedDate = try { sdfDb.parse(dateStr) } catch(e: Exception) { null }
            val formattedDate = if (parsedDate != null) sdfDisplay.format(parsedDate) else dateStr
            
            val activeStudents = studentsList.filter { it.attendsOnDay(dayOfWeek) }
            
            activeStudents.forEach { student ->
                val att = attendanceByDateAndStudent[dateStr]?.get(student.id)
                val isPresent = att?.status == Attendance.STATUS_PRESENT ||
                                att?.status == Attendance.STATUS_LATE ||
                                att?.status == Attendance.STATUS_EARLY_LEAVE
                
                val displayName = if (dates.size == 1) student.fullName else "${student.fullName} ($formattedDate)"
                presenzeList.add(
                    com.example.util.Presenza(
                        nome = displayName,
                        presente = isPresent
                    )
                )
            }
        }
        return presenzeList
    }

    suspend fun getGridDataForDateRange(startDateStr: String, endDateStr: String): Pair<List<String>, List<com.example.util.StudentGridRow>> {
        val sdfDb = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfColHeader = SimpleDateFormat("dd/MM", Locale.ITALIAN)
        
        val studentsList = repository.allStudents.first().sortedBy { it.lastName }
        val allAttendance = repository.allAttendance.first()

        val dates = mutableListOf<String>()
        try {
            val start = sdfDb.parse(startDateStr) ?: Date()
            val end = sdfDb.parse(endDateStr) ?: Date()
            val cal = Calendar.getInstance()
            cal.time = start
            while (!cal.time.after(end)) {
                dates.add(sdfDb.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val attendanceByDateAndStudent = allAttendance.groupBy { it.date }.mapValues { entry ->
            entry.value.associateBy { it.studentId }
        }

        val formattedHeaders = dates.map { dateStr ->
            val parsedDate = sdfDb.parse(dateStr) ?: Date()
            sdfColHeader.format(parsedDate)
        }

        val rows = studentsList.map { student ->
            var totalPresences = 0
            var totalAbsences = 0
            val statuses = dates.map { dateStr ->
                val dayOfWeek = getDayOfWeek(dateStr)
                if (student.attendsOnDay(dayOfWeek)) {
                    val att = attendanceByDateAndStudent[dateStr]?.get(student.id)
                    when (att?.status) {
                        Attendance.STATUS_PRESENT -> {
                            totalPresences++
                            "P"
                        }
                        Attendance.STATUS_ABSENT -> {
                            totalAbsences++
                            "A"
                        }
                        Attendance.STATUS_LATE -> {
                            totalPresences++
                            "R"
                        }
                        Attendance.STATUS_EARLY_LEAVE -> {
                            totalPresences++
                            "U"
                        }
                        else -> {
                            "-"
                        }
                    }
                } else {
                    "-"
                }
            }
            com.example.util.StudentGridRow(
                name = student.fullName,
                statuses = statuses,
                totalPresences = totalPresences,
                totalAbsences = totalAbsences
            )
        }

        return Pair(formattedHeaders, rows)
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}

data class ClassStats(
    val totalStudents: Int = 0,
    val presentCount: Int = 0,
    val absentCount: Int = 0,
    val lateCount: Int = 0,
    val earlyLeaveCount: Int = 0,
    val missingRecordsCount: Int = 0,
    val presencePercentage: Int = 100
)

data class StudentSummaryStats(
    val presenceRate: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val earlyCount: Int
)
