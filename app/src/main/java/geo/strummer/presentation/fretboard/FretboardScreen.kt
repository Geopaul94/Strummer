package geo.strummer.presentation.fretboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import geo.strummer.domain.guitar.model.GuitarString

// Solo/lead mode: a playable fretboard for single-note noodling between strums.
@Composable
fun FretboardScreen(viewModel: FretboardViewModel = hiltViewModel()) {
    val capo by viewModel.capo.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(12.dp),
    ) {
        Text("Solo / Fretboard", color = Color(0xFFD7A86E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Tap any fret to play a single note" + if (capo > 0) " · capo $capo" else "",
            color = Color(0xFF9E8E7E),
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))

        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            // Fret-number header.
            Row {
                Spacer(Modifier.width(labelWidth))
                for (fret in 0..viewModel.numFrets) {
                    Box(
                        modifier = Modifier.size(cellWidth, 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "$fret",
                            color = Color(0xFF6D5D4D),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Strings: high E (index 5) on top → low E (index 0) at bottom.
            for (stringIndex in 5 downTo 0) {
                val gs = GuitarString.entries[stringIndex]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(labelWidth, cellHeight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = gs.stringName,
                            color = Color(0xFFEDE0D4),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    for (fret in 0..viewModel.numFrets) {
                        FretCell(
                            fret = fret,
                            stringColor = stringColorFor(stringIndex),
                            onTap = { viewModel.playNote(stringIndex, fret) },
                        )
                    }
                }
            }
        }
    }
}

private val labelWidth = 32.dp
private val cellWidth = 48.dp
private val cellHeight = 44.dp

@Composable
private fun FretCell(fret: Int, stringColor: Color, onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(cellWidth, cellHeight)
            .padding(2.dp)
            .background(
                color = if (fret == 0) Color(0xFF2C2018) else Color(0xFF241C14),
                shape = RoundedCornerShape(6.dp),
            )
            .border(0.5.dp, Color(0xFF3E2723), RoundedCornerShape(6.dp))
            .clickable { onTap() },
        contentAlignment = Alignment.Center,
    ) {
        // The string line through the cell.
        Box(
            modifier = Modifier
                .size(cellWidth, 2.dp)
                .background(stringColor.copy(alpha = 0.5f)),
        )
        // Fret-marker dots at common inlay positions.
        if (fret in intArrayOf(3, 5, 7, 9, 12)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF4E3B2B), CircleShape),
            )
        }
    }
}

private fun stringColorFor(index: Int): Color = when (index) {
    0 -> Color(0xFFCD7F32)
    1 -> Color(0xFFD4A04A)
    2 -> Color(0xFFDDB86A)
    3 -> Color(0xFFE8D08C)
    4 -> Color(0xFFC0C0C0)
    else -> Color(0xFFD8D8D8)
}
