package app.ytsleep.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history",
    indices = [Index("lastPlayedAt")]
)
data class HistoryEntry(
    @PrimaryKey val videoId: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val durationSeconds: Int?,
    val lastPositionMs: Long,
    val lastPlayedAt: Long,       // epoch ms, indexed for sorted queries
    val sourcePlaylistId: String?,
    val sourceIndex: Int?,
)
