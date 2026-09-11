package com.example.data

import kotlinx.coroutines.flow.Flow

class StudentRepository(
    private val studentDao: StudentDao,
    private val attendanceDao: AttendanceDao
) {
    val allStudents: Flow<List<Student>> = studentDao.getAllStudents()
    val allAttendance: Flow<List<Attendance>> = attendanceDao.getAllAttendance()

    fun getAttendanceForDate(date: String): Flow<List<Attendance>> {
        return attendanceDao.getAttendanceForDate(date)
    }

    suspend fun insertStudent(student: Student): Long {
        return studentDao.insertStudent(student)
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
    }

    suspend fun deleteStudent(student: Student) {
        studentDao.deleteStudent(student)
    }

    suspend fun saveAttendance(attendance: Attendance) {
        attendanceDao.insertOrUpdateAttendance(attendance)
    }
}
