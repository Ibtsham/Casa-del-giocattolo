package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId", "date"], unique = true)]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String, // Format: YYYY-MM-DD
    val status: String, // "PRESENT", "ABSENT", "LATE", "EARLY_LEAVE"
    val notes: String = ""
) {
    companion object {
        const val STATUS_PRESENT = "PRESENT"
        const val STATUS_ABSENT = "ABSENT"
        const val STATUS_LATE = "LATE"
        const val STATUS_EARLY_LEAVE = "EARLY_LEAVE"
    }
}
