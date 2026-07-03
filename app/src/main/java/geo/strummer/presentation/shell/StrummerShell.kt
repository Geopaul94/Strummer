package geo.strummer.presentation.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import geo.strummer.presentation.fretboard.FretboardScreen
import geo.strummer.presentation.patterns.PatternsScreen
import geo.strummer.presentation.recording.RecordingScreen
import geo.strummer.presentation.settings.SettingsScreen
import geo.strummer.presentation.strum.StrumScreen

// Text glyphs stand in for icons — keeps us off the material-icons-extended
// dependency (which is large) for a 5-tab bar.
private enum class Tab(val label: String, val glyph: String) {
    STRUM("Strum", "🎸"),
    PATTERNS("Patterns", "🎵"),
    FRETBOARD("Solo", "🎼"),
    RECORD("Record", "⏺"),
    SETTINGS("Settings", "⚙"),
}

@Composable
fun StrummerShell() {
    // Instantiate the shell VM so the engine starts (and stops on finish).
    hiltViewModel<ShellViewModel>()

    var currentTab by remember { mutableStateOf(Tab.STRUM) }

    Scaffold(
        containerColor = Color(0xFF1C1410),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF2C2018)) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Text(tab.glyph, fontSize = 18.sp) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1C1410),
                            selectedTextColor = Color(0xFFD7A86E),
                            indicatorColor = Color(0xFFD7A86E),
                            unselectedIconColor = Color(0xFF9E8E7E),
                            unselectedTextColor = Color(0xFF9E8E7E),
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (currentTab) {
                Tab.STRUM -> StrumScreen()
                Tab.PATTERNS -> PatternsScreen()
                Tab.FRETBOARD -> FretboardScreen()
                Tab.RECORD -> RecordingScreen()
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
