package geo.strummer.presentation.strum

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import geo.strummer.domain.audio.AudioDiagnostics
import geo.strummer.domain.guitar.model.GuitarString

// Guitar string colors: thicker strings are darker/warmer, thinner are brighter.
private val stringColors = listOf(
    Color(0xFFCD7F32), // E2 — bronze wound
    Color(0xFFD4A04A), // A2
    Color(0xFFDDB86A), // D3
    Color(0xFFE8D08C), // G3
    Color(0xFFC0C0C0), // B3 — plain steel
    Color(0xFFD8D8D8), // E4 — plain steel
)

@Composable
fun Phase0Screen(viewModel: Phase0ViewModel = hiltViewModel()) {
    val diagnostics by viewModel.diagnostics.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Strummer — Phase 0",
            color = Color(0xFFD7A86E),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = "Tap a string to pluck (open tuning EADGBE)",
            color = Color(0xFF9E8E7E),
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(24.dp))

        // Six tappable string bars.
        GuitarString.entries.forEach { gs ->
            StringButton(
                guitarString = gs,
                color = stringColors[gs.index],
                onPluck = { viewModel.pluckString(gs) },
                onMute = { viewModel.muteString(gs) },
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.muteAll() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF5D4037),
            ),
        ) {
            Text("Mute All", color = Color(0xFFEDE0D4))
        }

        Spacer(Modifier.height(16.dp))

        HorizontalDivider(color = Color(0xFF3E2723))
        Spacer(Modifier.height(8.dp))

        DiagnosticsPanel(diagnostics)
    }
}

@Composable
private fun StringButton(
    guitarString: GuitarString,
    color: Color,
    onPluck: () -> Unit,
    onMute: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // String label.
        Text(
            text = guitarString.stringName,
            color = Color(0xFFEDE0D4),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center,
        )

        // The tappable "string" — a colored bar. Tap to pluck, long-press to mute.
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .background(
                    color = color.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(8.dp),
                )
                .pointerInput(guitarString) {
                    detectTapGestures(
                        onTap = { onPluck() },
                        onLongPress = { onMute() },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            // The "string" line itself.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stringThickness(guitarString.index).dp)
                    .background(color),
            )
        }

        Spacer(Modifier.width(8.dp))

        Text(
            text = "MIDI ${guitarString.openMidiPitch}",
            color = Color(0xFF6D5D4D),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

private fun stringThickness(index: Int): Int = when (index) {
    0 -> 5
    1 -> 4
    2 -> 3
    3 -> 3
    4 -> 2
    else -> 2
}

@Composable
private fun DiagnosticsPanel(diag: AudioDiagnostics) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "Audio Diagnostics",
            color = Color(0xFFD7A86E),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        DiagRow("API", diag.audioApi.name)
        DiagRow("Sample rate", "${diag.sampleRate} Hz")
        DiagRow("Burst", "${diag.framesPerBurst} frames")
        DiagRow("Buffer", "${diag.bufferSizeFrames} frames")
        DiagRow("Latency", if (diag.latencyMillis >= 0) "%.1f ms".format(diag.latencyMillis) else "N/A")
        DiagRow("XRuns", if (diag.xRunCount >= 0) "${diag.xRunCount}" else "N/A")
        DiagRow("Low latency", if (diag.isLowLatency) "YES" else "no")
        DiagRow("Exclusive", if (diag.isExclusive) "YES" else "no")
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, color = Color(0xFF9E8E7E), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(
            text = value,
            color = if (value == "YES") Color(0xFF81C784) else Color(0xFFEDE0D4),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
