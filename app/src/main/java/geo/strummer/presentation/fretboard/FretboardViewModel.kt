package geo.strummer.presentation.fretboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.domain.guitar.model.GuitarString
import geo.strummer.player.GuitarSession
import javax.inject.Inject

@HiltViewModel
class FretboardViewModel @Inject constructor(
    private val session: GuitarSession,
) : ViewModel() {

    val capo = session.capo
    val numFrets = 12

    // Play a single fretted note. The sounded pitch = open string pitch + fret + capo.
    fun playNote(stringIndex: Int, fret: Int) {
        val open = GuitarString.entries[stringIndex].openMidiPitch
        val pitch = open + fret + capo.value
        session.playNote(stringIndex, pitch)
    }
}
