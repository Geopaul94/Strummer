package geo.strummer.presentation.recording

import android.content.Intent
import android.media.MediaPlayer
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun RecordingScreen(viewModel: RecordingViewModel = hiltViewModel()) {
    val isRecording by viewModel.isRecording.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val context = LocalContext.current

    // One MediaPlayer for previewing recordings; released when leaving the screen.
    val player = remember { MediaPlayer() }
    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1410))
            .padding(16.dp),
    ) {
        Text("Record", color = Color(0xFFD7A86E), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Record your performance to a WAV file, then play back or share.",
            color = Color(0xFF9E8E7E),
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(20.dp))

        // Big record / stop button.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = if (isRecording) Color(0xFF8D3B3B) else Color(0xFF5D4037),
                        shape = CircleShape,
                    )
                    .clickable { viewModel.toggleRecording() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isRecording) "■" else "⏺",
                    color = Color(0xFFEDE0D4),
                    fontSize = 32.sp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isRecording) "Recording… tap to stop" else "Tap to record",
            color = if (isRecording) Color(0xFFE57373) else Color(0xFF9E8E7E),
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Recordings", color = Color(0xFFD7A86E), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            TextButton(onClick = viewModel::refresh) { Text("Refresh", color = Color(0xFFD7A86E)) }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(recordings) { file ->
                RecordingRow(
                    file = file,
                    onPlay = { playFile(player, file) },
                    onShare = { shareFile(context, file) },
                )
            }
        }
    }
}

@Composable
private fun RecordingRow(file: File, onPlay: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = file.name.removePrefix("strummer_").removeSuffix(".wav"),
            color = Color(0xFFEDE0D4),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onPlay) { Text("▶ Play", color = Color(0xFFD7A86E)) }
        TextButton(onClick = onShare) { Text("Share", color = Color(0xFFD7A86E)) }
    }
}

private fun playFile(player: MediaPlayer, file: File) {
    runCatching {
        player.reset()
        player.setDataSource(file.absolutePath)
        player.prepare()
        player.start()
    }
}

private fun shareFile(context: android.content.Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/wav"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share recording"))
}
