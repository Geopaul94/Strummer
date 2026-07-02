package geo.strummer.presentation.strum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import geo.strummer.domain.audio.AudioDiagnostics
import geo.strummer.domain.guitar.model.Chord
import geo.strummer.domain.guitar.model.GuitarString
import geo.strummer.domain.guitar.model.StrumDirection
import kotlin.math.abs

// String visual colors — thicker wound strings are bronze, plain steel are silver.
private val stringColors = listOf(
    Color(0xFFCD7F32), // E2 — bronze wound
    Color(0xFFD4A04A), // A2
    Color(0xFFDDB86A), // D3
    Color(0xFFE8D08C), // G3
    Color(0xFFC0C0C0), // B3 — plain steel
    Color(0xFFD8D8D8), // E4
)

@Composable
fun StrumScreen(viewModel: StrumViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410)),
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Strummer",
                color = Color(0xFFD7A86E),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            state.selectedChord?.let { chord ->
                Text(
                    text = chord.displayName,
                    color = Color(0xFFEDE0D4),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        // Chord palette
        ChordPalette(
            chords = state.palette,
            selectedIndex = state.selectedChordIndex,
            onSelect = viewModel::selectChord,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        // Strum area — the hero
        StrumArea(
            voicingFrets = state.selectedChord?.voicing?.frets,
            onStrum = viewModel::strum,
            onPickString = viewModel::pickString,
            onMuteAll = viewModel::muteAll,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
        )

        // Diagnostics (collapsible later)
        HorizontalDivider(color = Color(0xFF3E2723), modifier = Modifier.padding(horizontal = 16.dp))
        DiagnosticsBar(state.diagnostics)
    }
}

@Composable
private fun ChordPalette(
    chords: List<Chord>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier.height(100.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(chords) { index, chord ->
            val isSelected = index == selectedIndex
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
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chord.displayName,
                    color = if (isSelected) Color(0xFFFFDCC2) else Color(0xFF9E8E7E),
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StrumArea(
    voicingFrets: List<Int>?,
    onStrum: (StrumDirection, Float) -> Unit,
    onPickString: (Int) -> Unit,
    onMuteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .background(
                color = Color(0xFF241C14),
                shape = RoundedCornerShape(12.dp),
            )
            .pointerInput(Unit) {
                // Separate detector for vertical swipes (strum) vs taps (pick).
                // We use a raw pointer input to get better velocity data.
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitPointerEvent()
                        val downPos = down.changes.firstOrNull()?.position ?: continue
                        val downTime = System.nanoTime()

                        // Wait for up or significant drag.
                        var lastPos = downPos
                        var isDrag = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break

                            if (change.pressed) {
                                lastPos = change.position
                                val dy = lastPos.y - downPos.y
                                if (abs(dy) > with(density) { 20.dp.toPx() }) {
                                    isDrag = true
                                }
                            } else {
                                // Pointer up.
                                change.consume()
                                if (isDrag) {
                                    val dy = lastPos.y - downPos.y
                                    val dt = (System.nanoTime() - downTime) / 1_000_000_000f
                                    val speed = if (dt > 0) abs(dy / dt) else 1000f

                                    // Down-strum = finger moves downward (positive Y).
                                    // Up-strum = finger moves upward (negative Y).
                                    val direction = if (dy > 0) StrumDirection.DOWN
                                                    else StrumDirection.UP

                                    // Convert px/s to a usable speed value. Scale by density
                                    // so the feel is consistent across screen sizes.
                                    val normalizedSpeed = speed / density.density
                                    onStrum(direction, normalizedSpeed)
                                } else {
                                    // Tap — pick the string at this Y position.
                                    val areaHeight = size.height.toFloat()
                                    val stringIndex = ((downPos.y / areaHeight) * 6)
                                        .toInt()
                                        .coerceIn(0, 5)
                                    onPickString(stringIndex)
                                }
                                break
                            }
                        }
                    }
                }
            },
    ) {
        // Draw the 6 strings visually.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            GuitarString.entries.forEachIndexed { index, gs ->
                StringRow(
                    guitarString = gs,
                    color = stringColors[index],
                    fret = voicingFrets?.getOrNull(index) ?: 0,
                    isMuted = voicingFrets?.getOrNull(index)?.let { it < 0 } ?: false,
                )
            }
        }

        // Strum hint overlay.
        Text(
            text = "↕ Swipe to strum · Tap to pick",
            color = Color(0xFF6D5D4D),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
        )
    }
}

@Composable
private fun StringRow(
    guitarString: GuitarString,
    color: Color,
    fret: Int,
    isMuted: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // String label.
        Text(
            text = if (isMuted) "X" else guitarString.stringName,
            color = if (isMuted) Color(0xFF5D4037) else Color(0xFFEDE0D4),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
        )

        // The visual string line.
        Box(
            modifier = Modifier
                .weight(1f)
                .height(stringThickness(guitarString.index).dp)
                .background(
                    if (isMuted) color.copy(alpha = 0.15f) else color.copy(alpha = 0.7f)
                ),
        )

        // Fret indicator.
        Text(
            text = if (isMuted) "×" else if (fret == 0) "○" else "$fret",
            color = if (isMuted) Color(0xFF5D4037) else Color(0xFF9E8E7E),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center,
        )
    }
}

private fun stringThickness(index: Int): Int = when (index) {
    0 -> 4
    1 -> 3
    2 -> 3
    3 -> 2
    4 -> 2
    else -> 1
}

@Composable
private fun DiagnosticsBar(diag: AudioDiagnostics) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val items = listOf(
            diag.audioApi.name,
            "${diag.sampleRate}Hz",
            if (diag.latencyMillis >= 0) "%.0fms".format(diag.latencyMillis) else "—",
            "xr:${if (diag.xRunCount >= 0) diag.xRunCount else "—"}",
            if (diag.isLowLatency) "LL" else "—",
            if (diag.isExclusive) "EX" else "—",
        )
        items.forEach { text ->
            Text(
                text = text,
                color = Color(0xFF6D5D4D),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
