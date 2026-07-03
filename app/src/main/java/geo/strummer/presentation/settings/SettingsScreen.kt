package geo.strummer.presentation.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import geo.strummer.domain.guitar.model.Tone
import geo.strummer.presentation.palette.PaletteBuilderScreen

// Settings hosts its own sub-screens (palette builder, credits) via simple state
// so we don't need a nav-graph dependency for a couple of leaf pages.
private enum class SettingsPage { MAIN, PALETTE, CREDITS }

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    var page by remember { mutableStateOf(SettingsPage.MAIN) }

    when (page) {
        SettingsPage.PALETTE -> PaletteBuilderScreen(onDone = { page = SettingsPage.MAIN })
        SettingsPage.CREDITS -> CreditsScreen(onDone = { page = SettingsPage.MAIN })
        SettingsPage.MAIN -> SettingsMain(
            viewModel = viewModel,
            onOpenPalette = { page = SettingsPage.PALETTE },
            onOpenCredits = { page = SettingsPage.CREDITS },
        )
    }
}

@Composable
private fun SettingsMain(
    viewModel: SettingsViewModel,
    onOpenPalette: () -> Unit,
    onOpenCredits: () -> Unit,
) {
    val tone by viewModel.tone.collectAsState()
    val capo by viewModel.capo.collectAsState()
    val palmMute by viewModel.palmMute.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(16.dp),
    ) {
        Text("Settings", color = Color(0xFFD7A86E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        // Tone
        Text("Tone", color = Color(0xFF9E8E7E), fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            viewModel.tones.forEach { t ->
                ToneChip(tone = t, selected = t == tone, onClick = { viewModel.setTone(t) })
            }
        }

        Spacer(Modifier.height(20.dp))

        // Capo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Capo", color = Color(0xFFEDE0D4), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = if (capo == 0) "Off" else "Fret $capo",
                    color = Color(0xFF9E8E7E),
                    fontSize = 12.sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Stepper("–") { viewModel.setCapo((capo - 1).coerceAtLeast(0)) }
                Text(
                    text = "$capo",
                    color = Color(0xFFEDE0D4),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Stepper("+") { viewModel.setCapo((capo + 1).coerceAtMost(7)) }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Palm mute
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Palm mute", color = Color(0xFFEDE0D4), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Damped, staccato notes", color = Color(0xFF9E8E7E), fontSize = 12.sp)
            }
            Switch(
                checked = palmMute,
                onCheckedChange = { viewModel.setPalmMute(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFFD7A86E),
                    checkedTrackColor = Color(0xFF5D4037),
                ),
            )
        }

        Spacer(Modifier.height(28.dp))

        SettingsButton("Chord palette builder") { onOpenPalette() }
        Spacer(Modifier.height(10.dp))
        SettingsButton("Credits & licenses") { onOpenCredits() }
    }
}

@Composable
private fun ToneChip(tone: Tone, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .background(
                color = if (selected) Color(0xFF5D4037) else Color(0xFF2C2018),
                shape = RoundedCornerShape(8.dp),
            )
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Color(0xFFD7A86E) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tone.label,
            color = if (selected) Color(0xFFFFDCC2) else Color(0xFF9E8E7E),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color(0xFF2C2018), RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFFD7A86E), fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF2C2018), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, color = Color(0xFFEDE0D4), fontSize = 15.sp)
    }
}
