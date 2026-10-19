package app.ytsleep.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Upsert
    suspend fun upsert(entry: HistoryEntry)

    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC LIMIT 1")
    fun latest(): Flow<HistoryEntry?>

    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun recent(limit: Int = 20): Flow<List<HistoryEntry>>

    @Query("DELETE FROM history WHERE videoId = :videoId")
    suspend fun delete(videoId: String)

    @Query("UPDATE history SET lastPositionMs = :positionMs, lastPlayedAt = :playedAt WHERE videoId = :videoId")
    suspend fun updatePosition(videoId: String, positionMs: Long, playedAt: Long)

    @Query("""
        DELETE FROM history WHERE videoId NOT IN (
            SELECT videoId FROM history ORDER BY lastPlayedAt DESC LIMIT :keepCount
        )
    """)
    suspend fun pruneKeepingNewest(keepCount: Int = 500)
}
