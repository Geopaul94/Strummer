package geo.strummer.domain.audio

// Domain contract for the guitar audio engine. No Android imports — ViewModels
// and use cases depend on this interface, not the native implementation.
interface GuitarEngine {
    fun start()
    fun stop()

    // Pluck a specific string (0-5, low E to high E) at the given MIDI pitch
    // and velocity (0..1). Re-plucking an already-sounding string cuts the
    // previous note automatically (per-string monophony).
    fun pluckString(stringIndex: Int, midiPitch: Int, velocity: Float)

    fun muteString(stringIndex: Int)
    fun muteAll()

    // Tone brightness 0..1 (nylon ≈ 0.3, acoustic ≈ 0.5, electric ≈ 0.75).
    fun setTone(brightness: Float)
    // Palm mute: damped staccato notes.
    fun setPalmMute(enabled: Boolean)

    // Recording (captures the mixed stereo output).
    fun startRecording()
    fun stopRecording()
    fun isRecording(): Boolean
    // Drain available recorded samples into [out]; returns count of floats written.
    fun drainRecording(out: FloatArray): Int

    fun diagnostics(): AudioDiagnostics
    fun release()
}
