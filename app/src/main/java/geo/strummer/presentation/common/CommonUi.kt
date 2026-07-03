package geo.strummer.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import geo.strummer.domain.guitar.model.Chord

// Shared chord palette grid used by the Strum and Patterns screens.
@Composable
fun ChordPaletteGrid(
    chords: List<Chord>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    heightDp: Int = 100,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.height(heightDp.dp),
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
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
