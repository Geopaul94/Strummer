package geo.strummer.presentation.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.domain.guitar.model.Tone
import geo.strummer.player.GuitarSession
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: GuitarSession,
) : ViewModel() {

    val tone = session.tone
    val capo = session.capo
    val palmMute = session.palmMute
    val tones: List<Tone> = Tone.entries

    fun setTone(tone: Tone) = session.setTone(tone)
    fun setCapo(fret: Int) = session.setCapo(fret)
    fun setPalmMute(enabled: Boolean) = session.setPalmMute(enabled)
}
