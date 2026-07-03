package geo.strummer.data.audio

// JNI surface. Every function here maps 1:1 to a C++ entry point in
// native-lib.cpp. Nothing above this layer should call these directly —
// go through NativeGuitarEngine instead.
internal object GuitarEngineBridge {
    init {
        System.loadLibrary("strummer_audio")
    }

    external fun nativeCreate(): Long
    external fun nativeDestroy(handle: Long)
    external fun nativeSetDeviceDefaults(handle: Long, sampleRate: Int, framesPerBurst: Int)
    external fun nativeStart(handle: Long): Boolean
    external fun nativeStop(handle: Long)
    external fun nativePluckString(handle: Long, stringIndex: Int, midiPitch: Int, velocity: Float)
    external fun nativeMuteString(handle: Long, stringIndex: Int)
    external fun nativeMuteAll(handle: Long)
    external fun nativeSetTone(handle: Long, brightness: Float)
    external fun nativeSetPalmMute(handle: Long, enabled: Boolean)

    external fun nativeStartRecording(handle: Long)
    external fun nativeStopRecording(handle: Long)
    external fun nativeIsRecording(handle: Long): Boolean
    external fun nativeDrainRecording(handle: Long, out: FloatArray): Int

    external fun nativeGetSampleRate(handle: Long): Int
    external fun nativeGetFramesPerBurst(handle: Long): Int
    external fun nativeGetBufferSize(handle: Long): Int
    external fun nativeGetLatencyMillis(handle: Long): Double
    external fun nativeGetXRunCount(handle: Long): Int
    external fun nativeIsLowLatency(handle: Long): Boolean
    external fun nativeIsExclusive(handle: Long): Boolean
    external fun nativeGetAudioApi(handle: Long): Int
}
