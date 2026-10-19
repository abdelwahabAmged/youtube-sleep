package app.ytsleep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.ytsleep.ui.AppNavigation
import app.ytsleep.ui.theme.YtSleepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YtSleepTheme {
                AppNavigation()
            }
        }
    }
}
