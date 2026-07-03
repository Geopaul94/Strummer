package geo.strummer.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CreditsScreen(onDone: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Credits & Licenses", color = Color(0xFFD7A86E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = onDone) { Text("Done", color = Color(0xFFD7A86E)) }
        }

        Spacer(Modifier.height(16.dp))

        CreditItem(
            "Oboe",
            "Low-latency audio I/O by Google. Apache License 2.0.",
        )
        CreditItem(
            "Sound synthesis",
            "Guitar tones are generated on-device with Karplus-Strong physical " +
                "modeling — no sampled audio, no third-party soundfont bundled in " +
                "this build.",
        )
        CreditItem(
            "Chord voicings",
            "Standard, factual guitar chord fingerings (open + moveable barre " +
                "shapes). Not derived from any copyrighted chord database.",
        )
        CreditItem(
            "TinySoundFont",
            "MIT-licensed SF2 synth header, bundled for a future sampled-tone " +
                "upgrade. © Bernhard Schelling.",
        )

        Spacer(Modifier.height(24.dp))
        Text(
            text = "Strummer is built to be honest: the core instrument stays free " +
                "and playable, and ads never interrupt your playing.",
            color = Color(0xFF9E8E7E),
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun CreditItem(title: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, color = Color(0xFFEDE0D4), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Color(0xFF9E8E7E), fontSize = 13.sp)
    }
}
