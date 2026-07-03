package geo.strummer.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import geo.strummer.data.audio.WavFileWriter
import geo.strummer.domain.audio.AudioDiagnostics
import geo.strummer.domain.audio.GuitarEngine
import geo.strummer.domain.guitar.ChordLibrary
import geo.strummer.domain.guitar.StrumEngine
import geo.strummer.domain.guitar.model.Chord
import geo.strummer.domain.guitar.model.PatternStep
import geo.strummer.domain.guitar.model.StrumDirection
import geo.strummer.domain.guitar.model.StrumEvent
import geo.strummer.domain.guitar.model.StrumPattern
import geo.strummer.domain.guitar.model.Tone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// The single coordinator between the UI and the audio engine. All screens share
// this one instance (it's a @Singleton), so the "held chord", tone, capo,
// pattern, etc. stay consistent no matter which screen you're on — and only ONE
// object owns the engine's lifecycle (no ViewModels fighting over start/stop).
@Singleton
class GuitarSession @Inject constructor(
    private val engine: GuitarEngine,
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Shared state (observed by every screen) ─────────────────────────────
    private val _palette = MutableStateFlow(ChordLibrary.defaultPalette())
    val palette: StateFlow<List<Chord>> = _palette.asStateFlow()

    private val _selectedChordIndex = MutableStateFlow(0)
    val selectedChordIndex: StateFlow<Int> = _selectedChordIndex.asStateFlow()

    private val _tone = MutableStateFlow(Tone.ACOUSTIC)
    val tone: StateFlow<Tone> = _tone.asStateFlow()

    private val _capo = MutableStateFlow(0)
    val capo: StateFlow<Int> = _capo.asStateFlow()

    private val _palmMute = MutableStateFlow(false)
    val palmMute: StateFlow<Boolean> = _palmMute.asStateFlow()

    private val _bpm = MutableStateFlow(100)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    private val _selectedPattern = MutableStateFlow<StrumPattern?>(null)
    val selectedPattern: StateFlow<StrumPattern?> = _selectedPattern.asStateFlow()

    private val _isPatternPlaying = MutableStateFlow(false)
    val isPatternPlaying: StateFlow<Boolean> = _isPatternPlaying.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _diagnostics = MutableStateFlow(AudioDiagnostics.UNAVAILABLE)
    val diagnostics: StateFlow<AudioDiagnostics> = _diagnostics.asStateFlow()

    val selectedChord: Chord? get() = _palette.value.getOrNull(_selectedChordIndex.value)

    private var patternJob: Job? = null
    private var recordJob: Job? = null
    private var started = false

    // ── Audio focus (OEM hardening) ──────────────────────────────────────────
    // When a call comes in or another app grabs audio, we stop any running
    // pattern and pause the stream, then resume when focus returns. Without this,
    // some OEM builds leave a zombie stream or keep looping in the background.
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                stopPattern()
                engine.stop()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (started) engine.start()
            }
        }
    }

    private val focusRequest: AudioFocusRequest =
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setOnAudioFocusChangeListener(focusListener)
            .build()

    // ── Lifecycle (called from MainActivity) ────────────────────────────────
    fun start() {
        if (started) return
        started = true
        audioManager.requestAudioFocus(focusRequest)
        engine.start()
        engine.setTone(_tone.value.brightness)
        engine.setPalmMute(_palmMute.value)
        // Slow diagnostics poll so the on-screen readout stays live.
        scope.launch {
            while (isActive) {
                _diagnostics.value = engine.diagnostics()
                delay(500)
            }
        }
    }

    fun shutdown() {
        stopPattern()
        if (_isRecording.value) stopRecording()
        audioManager.abandonAudioFocusRequest(focusRequest)
        engine.stop()
        engine.release()
        started = false
    }

    // ── Chord palette ───────────────────────────────────────────────────────
    fun selectChord(index: Int) { _selectedChordIndex.value = index }

    fun setPalette(chords: List<Chord>) {
        _palette.value = chords
        if (_selectedChordIndex.value >= chords.size) _selectedChordIndex.value = 0
    }

    // ── Playing ──────────────────────────────────────────────────────────────
    fun strum(direction: StrumDirection, swipeSpeed: Float) {
        val chord = selectedChord ?: return
        val events = StrumEngine.strum(chord.voicing, direction, swipeSpeed, capo = _capo.value)
        scope.launch { playRake(events) }
    }

    fun pickString(stringIndex: Int) {
        val chord = selectedChord ?: return
        val event = StrumEngine.pick(chord.voicing, stringIndex, capo = _capo.value) ?: return
        engine.pluckString(event.stringIndex, event.midiPitch, event.velocity)
    }

    // Play a single note directly (fretboard/solo mode).
    fun playNote(stringIndex: Int, midiPitch: Int, velocity: Float = 0.8f) {
        engine.pluckString(stringIndex, midiPitch, velocity)
    }

    fun muteAll() = engine.muteAll()

    // ── Tone / feel ───────────────────────────────────────────────────────────
    fun setTone(tone: Tone) {
        _tone.value = tone
        engine.setTone(tone.brightness)
    }

    fun setCapo(fret: Int) { _capo.value = fret.coerceIn(0, 7) }

    fun setPalmMute(enabled: Boolean) {
        _palmMute.value = enabled
        engine.setPalmMute(enabled)
    }

    // ── Patterns ──────────────────────────────────────────────────────────────
    fun selectPattern(pattern: StrumPattern) { _selectedPattern.value = pattern }

    fun setBpm(value: Int) { _bpm.value = value.coerceIn(40, 240) }

    fun togglePattern() {
        if (_isPatternPlaying.value) stopPattern() else playPattern()
    }

    fun playPattern() {
        if (_selectedPattern.value == null) return
        if (patternJob?.isActive == true) return
        _isPatternPlaying.value = true
        patternJob = scope.launch {
            var stepIndex = 0
            while (isActive) {
                val pattern = _selectedPattern.value ?: break
                val chord = selectedChord ?: break
                // Reading BPM/pattern/chord fresh each step lets the user change
                // them live without restarting playback.
                val stepMs = (60_000.0 / _bpm.value / pattern.stepsPerBeat).toLong()
                playStep(pattern.steps[stepIndex % pattern.steps.size], chord)
                stepIndex = (stepIndex + 1) % pattern.steps.size
                delay(stepMs.coerceAtLeast(1))
            }
            _isPatternPlaying.value = false
        }
    }

    fun stopPattern() {
        patternJob?.cancel()
        patternJob = null
        _isPatternPlaying.value = false
    }

    private suspend fun playStep(step: PatternStep, chord: Chord) {
        when (step) {
            is PatternStep.Strum -> {
                // Patterns use a consistent medium rake for an even feel.
                val events = StrumEngine.strum(
                    chord.voicing, step.direction, swipeSpeed = 1500f, capo = _capo.value,
                )
                // Fire the rake in a child coroutine so the step clock stays precise.
                scope.launch { playRake(events) }
            }
            is PatternStep.Pick -> {
                for (stringIndex in step.strings) {
                    val pitch = chord.voicing.midiPitch(stringIndex, _capo.value)
                    if (pitch >= 0) engine.pluckString(stringIndex, pitch, 0.75f)
                }
            }
            PatternStep.Rest -> { /* silence */ }
        }
    }

    // Play a strum's events with their rake delays.
    private suspend fun playRake(events: List<StrumEvent>) {
        var last = 0L
        for (e in events) {
            val wait = e.delayMs - last
            if (wait > 0) delay(wait)
            engine.pluckString(e.stringIndex, e.midiPitch, e.velocity)
            last = e.delayMs
        }
    }

    // ── Recording ─────────────────────────────────────────────────────────────
    // Returns the target file; audio is written until stopRecording().
    fun startRecording(): File {
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        // Deterministic-ish name using diagnostics tick (no Date on this host).
        val file = File(dir, "strummer_${System.currentTimeMillis()}.wav")
        val sr = engine.diagnostics().sampleRate.takeIf { it > 0 } ?: 48000
        val writer = WavFileWriter(file, sampleRate = sr, channels = 2)
        writer.open()
        engine.startRecording()
        _isRecording.value = true

        recordJob = scope.launch {
            val buffer = FloatArray(8192)
            while (isActive && engine.isRecording()) {
                val n = engine.drainRecording(buffer)
                if (n > 0) writer.writeFloats(buffer, n)
                delay(40)
            }
            // Final drain to catch the tail after stop.
            var n = engine.drainRecording(buffer)
            while (n > 0) {
                writer.writeFloats(buffer, n)
                n = engine.drainRecording(buffer)
            }
            writer.close()
        }
        return file
    }

    fun stopRecording() {
        engine.stopRecording()
        _isRecording.value = false
        // recordJob finishes on its own once engine.isRecording() is false.
    }

    fun listRecordings(): List<File> {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "wav" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
