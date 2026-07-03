package geo.strummer.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

// Shows a brief first-launch intro, then the app. Once dismissed it never shows
// again (persisted via DataStore). Honest onboarding: it just teaches the gesture
// and gets out of the way — no account, no paywall.
@Composable
fun OnboardingGate(
    viewModel: OnboardingViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val onboarded by viewModel.isOnboarded.collectAsState()

    when (onboarded) {
        null -> Box(Modifier.fillMaxSize().background(Color(0xFF1C1410))) // brief load
        true -> content()
        false -> OnboardingContent(onStart = viewModel::finishOnboarding)
    }
}

@Composable
private fun OnboardingContent(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎸", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Strummer",
            color = Color(0xFFD7A86E),
            fontSize = 34.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Step("1", "Pick a chord from the palette")
        Step("2", "Swipe across the strings to strum")
        Step("3", "Tap a string to pick a single note")
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
        ) {
            Text("Start strumming", color = Color(0xFFEDE0D4), fontSize = 18.sp)
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$number.  $text",
            color = Color(0xFFEDE0D4),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}
