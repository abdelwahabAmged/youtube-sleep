package app.ytsleep.update

sealed class UpdateState {
    data object NONE : UpdateState()
    data object DOWNLOADING : UpdateState()
    data class READY(val versionName: String, val notes: String?) : UpdateState()
    data object INSTALLING : UpdateState()
    data class DISMISSED(val versionName: String, val notes: String?) : UpdateState()
    data class FAILED(val versionName: String?) : UpdateState()
}
