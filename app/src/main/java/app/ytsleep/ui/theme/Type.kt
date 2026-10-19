package app.ytsleep.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val Typography = Typography(
    bodyLarge = TextStyle(color = OnBackground, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(color = OnBackground, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(color = OnBackground.copy(alpha = 0.7f), fontSize = 12.sp),
    titleLarge = TextStyle(color = OnBackground, fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(color = OnBackground, fontSize = 16.sp),
    labelSmall = TextStyle(color = OnBackground.copy(alpha = 0.6f), fontSize = 11.sp),
)
