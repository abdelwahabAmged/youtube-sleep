package app.ytsleep.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.ytsleep.data.db.AppDatabase
import app.ytsleep.data.db.HistoryEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entry(
        videoId: String,
        lastPlayedAt: Long,
        positionMs: Long = 0L,
    ) = HistoryEntry(
        videoId = videoId,
        title = "Title $videoId",
        channelName = "Channel",
        thumbnailUrl = "https://example.com/$videoId.jpg",
        durationSeconds = 600,
        lastPositionMs = positionMs,
        lastPlayedAt = lastPlayedAt,
        sourcePlaylistId = null,
        sourceIndex = null,
    )

    @Test
    fun upsert_insertsNewEntry() = runTest {
        val e = entry("vid1", 1000L)
        db.historyDao().upsert(e)
        val result = db.historyDao().latest().first()
        assertNotNull(result)
        assertEquals("vid1", result?.videoId)
    }

    @Test
    fun upsert_updatesExistingEntry() = runTest {
        val dao = db.historyDao()
        dao.upsert(entry("vid1", 1000L, 0L))
        dao.upsert(entry("vid1", 2000L, 300_000L))
        val result = dao.latest().first()
        assertEquals(2000L, result?.lastPlayedAt)
        assertEquals(300_000L, result?.lastPositionMs)
    }

    @Test
    fun recent_returnsOrderedByLastPlayedAtDesc() = runTest {
        val dao = db.historyDao()
        dao.upsert(entry("vid1", 1000L))
        dao.upsert(entry("vid2", 3000L))
        dao.upsert(entry("vid3", 2000L))
        val result = dao.recent(20).first()
        assertEquals(3, result.size)
        assertEquals("vid2", result[0].videoId)
        assertEquals("vid3", result[1].videoId)
        assertEquals("vid1", result[2].videoId)
    }

    @Test
    fun recent_limitsToRequested() = runTest {
        val dao = db.historyDao()
        repeat(25) { i -> dao.upsert(entry("vid$i", i.toLong())) }
        val result = dao.recent(20).first()
        assertEquals(20, result.size)
    }

    @Test
    fun latest_returnsNullWhenEmpty() = runTest {
        val result = db.historyDao().latest().first()
        assertNull(result)
    }

    @Test
    fun latest_returnsMostRecentEntry() = runTest {
        val dao = db.historyDao()
        dao.upsert(entry("old", 100L))
        dao.upsert(entry("new", 999L))
        val result = dao.latest().first()
        assertEquals("new", result?.videoId)
    }

    @Test
    fun pruneKeepingNewest_removesOldEntries() = runTest {
        val dao = db.historyDao()
        repeat(600) { i -> dao.upsert(entry("vid$i", i.toLong())) }
        dao.pruneKeepingNewest(500)
        val result = dao.recent(600).first()
        assertEquals(500, result.size)
        // Newest 500 should be retained (vid100..vid599)
        assertEquals("vid599", result[0].videoId)
    }

    @Test
    fun pruneKeepingNewest_nopWhenBelowLimit() = runTest {
        val dao = db.historyDao()
        repeat(10) { i -> dao.upsert(entry("vid$i", i.toLong())) }
        dao.pruneKeepingNewest(500)
        val result = dao.recent(600).first()
        assertEquals(10, result.size)
    }
}
