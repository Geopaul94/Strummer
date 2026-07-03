package geo.strummer.presentation.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import geo.strummer.domain.guitar.model.StrumPattern
import geo.strummer.presentation.common.ChordPaletteGrid

@Composable
fun PatternsScreen(viewModel: PatternsViewModel = hiltViewModel()) {
    val palette by viewModel.palette.collectAsState()
    val selectedChordIndex by viewModel.selectedChordIndex.collectAsState()
    val selectedPattern by viewModel.selectedPattern.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val bpm by viewModel.bpm.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(16.dp),
    ) {
        Text(
            text = "Patterns",
            color = Color(0xFFD7A86E),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Pick a chord, choose a pattern, hit play. Change chords live.",
            color = Color(0xFF9E8E7E),
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(12.dp))

        ChordPaletteGrid(
            chords = palette,
            selectedIndex = selectedChordIndex,
            onSelect = viewModel::selectChord,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(16.dp))

        Text("Strum", color = Color(0xFFD7A86E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        PatternRow(viewModel.strumPatterns, selectedPattern, viewModel::selectPattern)

        Spacer(Modifier.height(12.dp))

        Text("Fingerpicking", color = Color(0xFFD7A86E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        PatternRow(viewModel.fingerpickPatterns, selectedPattern, viewModel::selectPattern)

        Spacer(Modifier.height(24.dp))

        // BPM control
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BPM",
                color = Color(0xFF9E8E7E),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$bpm",
                color = Color(0xFFEDE0D4),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        Slider(
            value = bpm.toFloat(),
            onValueChange = { viewModel.setBpm(it.toInt()) },
            valueRange = 40f..240f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFD7A86E),
                activeTrackColor = Color(0xFFD7A86E),
                inactiveTrackColor = Color(0xFF3E2723),
            ),
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = viewModel::togglePlay,
            enabled = selectedPattern != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isPlaying) Color(0xFF8D3B3B) else Color(0xFF5D7C4D),
            ),
        ) {
            Text(
                text = if (isPlaying) "■  Stop" else "▶  Play",
                color = Color(0xFFEDE0D4),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun PatternRow(
    patterns: List<StrumPattern>,
    selected: StrumPattern?,
    onSelect: (StrumPattern) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        patterns.forEach { pattern ->
            val isSelected = selected?.name == pattern.name
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .background(
                        color = if (isSelected) Color(0xFF5D4037) else Color(0xFF2C2018),
                        shape = RoundedCornerShape(8.dp),
                    )
                    .border(
                        width = if (isSelected) 2.dp else 0.dp,
                        color = if (isSelected) Color(0xFFD7A86E) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clickable { onSelect(pattern) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = pattern.name,
                    color = if (isSelected) Color(0xFFFFDCC2) else Color(0xFF9E8E7E),
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
