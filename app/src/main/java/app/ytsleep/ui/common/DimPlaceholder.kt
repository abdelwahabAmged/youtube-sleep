package app.ytsleep.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DimPlaceholder(
    width: Dp = 120.dp,
    height: Dp = 68.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width, height)
            .background(Color(0xFF1A1A22))
    )
}

/** Static loading state: a few dim rows shaped like list items. No shimmer, no motion (FR-014). */
@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier, rows: Int = 5) {
    Column(modifier.fillMaxWidth()) {
        repeat(rows) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                DimPlaceholder()
                Spacer(Modifier.width(12.dp))
                Column {
                    DimPlaceholder(width = 180.dp, height = 14.dp)
                    Spacer(Modifier.height(8.dp))
                    DimPlaceholder(width = 100.dp, height = 12.dp)
                }
            }
        }
    }
}
