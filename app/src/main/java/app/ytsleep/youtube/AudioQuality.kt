package app.ytsleep.youtube

/** How much audio data to spend; chosen separately for Wi-Fi and mobile data in Settings. */
enum class AudioQuality(val label: String, val mbPerHour: Int) {
    /** The smallest stream YouTube offers (~48–50 kbps). */
    DATA_SAVER("Data saver", 22),

    /** The best stream up to ~80 kbps (Opus 70 kbps in practice): sounds the same for sleep audio. */
    NORMAL("Normal", 31),

    /** The best stream available (~128–160 kbps). */
    HIGH("High", 70),
}
