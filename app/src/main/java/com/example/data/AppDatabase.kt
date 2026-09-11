package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students ORDER BY lastName ASC, firstName ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(attendance: Attendance)

    @Query("DELETE FROM attendance WHERE studentId = :studentId")
    suspend fun deleteAttendanceForStudent(studentId: Int)
}

@Database(entities = [Student::class, Attendance::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "classeviva_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
