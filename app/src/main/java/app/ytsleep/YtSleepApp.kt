package app.ytsleep

import androidx.annotation.OptIn
import android.app.Application
import androidx.media3.common.util.UnstableApi
import androidx.room.Room
import app.ytsleep.auth.AccountStore
import app.ytsleep.data.SettingsStore
import app.ytsleep.data.db.AppDatabase
import app.ytsleep.playback.PlayerConnection
import app.ytsleep.update.UpdateChecker
import app.ytsleep.youtube.ContentSource
import app.ytsleep.youtube.YouTubeContentSource
import app.ytsleep.youtube.newpipe.OkHttpDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import java.util.concurrent.TimeUnit

@OptIn(UnstableApi::class)
class AppContainer(app: Application) {

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val database: AppDatabase = Room.databaseBuilder(app, AppDatabase::class.java, "ytsleep.db")
        .build()

    val settingsStore = SettingsStore(app)

    val accountStore = AccountStore(app)

    val contentSource: ContentSource by lazy {
        YouTubeContentSource(okHttpClient, app)
    }

    val playerConnection: PlayerConnection by lazy { PlayerConnection(app) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val updateChecker: UpdateChecker by lazy {
        UpdateChecker(app, okHttpClient, appScope)
    }
}

@OptIn(UnstableApi::class)
class YtSleepApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        val downloader = OkHttpDownloader(container.okHttpClient)
        NewPipe.init(downloader, Localization("en", "US"))

        container.updateChecker.checkNow()
    }
}
