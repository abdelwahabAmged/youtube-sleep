package app.ytsleep.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}
