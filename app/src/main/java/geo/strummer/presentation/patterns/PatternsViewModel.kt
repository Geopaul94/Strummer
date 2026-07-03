package geo.strummer.presentation.patterns

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.domain.guitar.PatternLibrary
import geo.strummer.domain.guitar.model.StrumPattern
import geo.strummer.player.GuitarSession
import javax.inject.Inject

@HiltViewModel
class PatternsViewModel @Inject constructor(
    private val session: GuitarSession,
) : ViewModel() {

    val strumPatterns: List<StrumPattern> = PatternLibrary.strumPatterns
    val fingerpickPatterns: List<StrumPattern> = PatternLibrary.fingerpickPatterns

    val palette = session.palette
    val selectedChordIndex = session.selectedChordIndex
    val selectedPattern = session.selectedPattern
    val isPlaying = session.isPatternPlaying
    val bpm = session.bpm

    fun selectChord(index: Int) = session.selectChord(index)
    fun selectPattern(pattern: StrumPattern) = session.selectPattern(pattern)
    fun togglePlay() = session.togglePattern()
    fun setBpm(value: Int) = session.setBpm(value)
}
