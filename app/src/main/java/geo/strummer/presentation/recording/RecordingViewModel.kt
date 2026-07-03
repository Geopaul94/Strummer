package geo.strummer.presentation.recording

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import geo.strummer.player.GuitarSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val session: GuitarSession,
) : ViewModel() {

    val isRecording: StateFlow<Boolean> = session.isRecording

    private val _recordings = MutableStateFlow(session.listRecordings())
    val recordings: StateFlow<List<File>> = _recordings.asStateFlow()

    fun toggleRecording() {
        if (isRecording.value) {
            session.stopRecording()
            // Give the writer a beat to finalize the WAV header, then refresh.
            refreshSoon()
        } else {
            session.startRecording()
        }
    }

    fun refresh() {
        _recordings.value = session.listRecordings()
    }

    private fun refreshSoon() {
        // The record coroutine finalizes the file asynchronously; refresh now and
        // the list will pick it up (lastModified sort). A manual pull-to-refresh
        // button is also provided in the UI.
        refresh()
    }
}
