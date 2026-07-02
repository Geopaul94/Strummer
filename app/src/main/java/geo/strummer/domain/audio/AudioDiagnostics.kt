package geo.strummer.domain.audio

data class AudioDiagnostics(
    val sampleRate: Int,
    val framesPerBurst: Int,
    val bufferSizeFrames: Int,
    val latencyMillis: Double,
    val xRunCount: Int,
    val isLowLatency: Boolean,
    val isExclusive: Boolean,
    val audioApi: AudioApi,
) {
    companion object {
        val UNAVAILABLE = AudioDiagnostics(
            sampleRate = 0,
            framesPerBurst = 0,
            bufferSizeFrames = 0,
            latencyMillis = -1.0,
            xRunCount = -1,
            isLowLatency = false,
            isExclusive = false,
            audioApi = AudioApi.UNSPECIFIED,
        )
    }
}

enum class AudioApi { UNSPECIFIED, OPEN_SL_ES, AAUDIO }
