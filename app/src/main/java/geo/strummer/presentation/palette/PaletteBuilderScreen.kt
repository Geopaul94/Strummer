package geo.strummer.presentation.palette

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import geo.strummer.domain.guitar.model.ChordType
import geo.strummer.domain.guitar.model.NoteName

@Composable
fun PaletteBuilderScreen(
    onDone: () -> Unit,
    viewModel: PaletteViewModel = hiltViewModel(),
) {
    val building by viewModel.building.collectAsState()
    val selectedRoot by viewModel.selectedRoot.collectAsState()
    val saved by viewModel.savedPalettes.collectAsState()
    var name by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Palette Builder", color = Color(0xFFD7A86E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDone) { Text("Done", color = Color(0xFFD7A86E)) }
        }

        Spacer(Modifier.height(8.dp))

        // Root selector.
        Text("Root", color = Color(0xFF9E8E7E), fontSize = 12.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            NoteName.entries.forEach { note ->
                Chip(
                    label = note.label,
                    selected = note == selectedRoot,
                    onClick = { viewModel.selectRoot(note) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Type buttons — tapping adds "<root><type>" to the palette.
        Text("Add chord", color = Color(0xFF9E8E7E), fontSize = 12.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            ChordType.entries.forEach { type ->
                Chip(
                    label = "${selectedRoot.label}${type.suffix}",
                    selected = false,
                    onClick = { viewModel.addChord(type) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Current palette — tap a chip to remove it.
        Text("Current palette (tap to remove)", color = Color(0xFF9E8E7E), fontSize = 12.sp)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            building.forEachIndexed { index, chord ->
                Chip(
                    label = chord.displayName,
                    selected = true,
                    onClick = { viewModel.removeChord(index) },
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Save row.
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Palette name", color = Color(0xFF6D5D4D)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { viewModel.save(name); name = "" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D7C4D)),
            ) { Text("Save") }
        }

        Spacer(Modifier.height(16.dp))

        Text("Saved palettes", color = Color(0xFFD7A86E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(saved) { palette ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(palette.name, color = Color(0xFFEDE0D4), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = palette.chords.joinToString(" ") { it.displayName },
                            color = Color(0xFF9E8E7E),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    TextButton(onClick = { viewModel.load(palette) }) { Text("Load", color = Color(0xFFD7A86E)) }
                    TextButton(onClick = { viewModel.delete(palette) }) { Text("Delete", color = Color(0xFF8D3B3B)) }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = 6.dp)
            .height(38.dp)
            .background(
                color = if (selected) Color(0xFF5D4037) else Color(0xFF2C2018),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = if (selected) Color(0xFFD7A86E) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color(0xFFFFDCC2) else Color(0xFF9E8E7E),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}
