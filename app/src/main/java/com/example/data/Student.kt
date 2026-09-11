package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val note: String = "",
    val attendingDays: String = "1,2,3,4,5" // Comma-separated day indices: 1 = Lun, 2 = Mar, 3 = Mer, 4 = Gio, 5 = Ven, 6 = Sab, 7 = Dom
) {
    val fullName: String get() = "$lastName $firstName"

    fun attendsOnDay(dayOfWeek1to7: Int): Boolean {
        return attendingDays.split(",").map { it.trim() }.contains(dayOfWeek1to7.toString())
    }
}
