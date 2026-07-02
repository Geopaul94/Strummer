package geo.strummer.data.audio

import android.content.Context
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import geo.strummer.domain.audio.AudioApi
import geo.strummer.domain.audio.AudioDiagnostics
import geo.strummer.domain.audio.GuitarEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeGuitarEngine @Inject constructor(
    @ApplicationContext context: Context,
) : GuitarEngine {

    private val handle: Long = GuitarEngineBridge.nativeCreate()

    init {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRate = audioManager
            .getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull() ?: 48000
        val framesPerBurst = audioManager
            .getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)?.toIntOrNull() ?: 192
        GuitarEngineBridge.nativeSetDeviceDefaults(handle, sampleRate, framesPerBurst)
    }

    override fun start() { GuitarEngineBridge.nativeStart(handle) }

    override fun stop() = GuitarEngineBridge.nativeStop(handle)

    override fun pluckString(stringIndex: Int, midiPitch: Int, velocity: Float) =
        GuitarEngineBridge.nativePluckString(handle, stringIndex, midiPitch, velocity)

    override fun muteString(stringIndex: Int) =
        GuitarEngineBridge.nativeMuteString(handle, stringIndex)

    override fun muteAll() = GuitarEngineBridge.nativeMuteAll(handle)

    override fun diagnostics(): AudioDiagnostics = AudioDiagnostics(
        sampleRate = GuitarEngineBridge.nativeGetSampleRate(handle),
        framesPerBurst = GuitarEngineBridge.nativeGetFramesPerBurst(handle),
        bufferSizeFrames = GuitarEngineBridge.nativeGetBufferSize(handle),
        latencyMillis = GuitarEngineBridge.nativeGetLatencyMillis(handle),
        xRunCount = GuitarEngineBridge.nativeGetXRunCount(handle),
        isLowLatency = GuitarEngineBridge.nativeIsLowLatency(handle),
        isExclusive = GuitarEngineBridge.nativeIsExclusive(handle),
        audioApi = when (GuitarEngineBridge.nativeGetAudioApi(handle)) {
            1 -> AudioApi.OPEN_SL_ES
            2 -> AudioApi.AAUDIO
            else -> AudioApi.UNSPECIFIED
        },
    )

    override fun release() = GuitarEngineBridge.nativeDestroy(handle)
}
