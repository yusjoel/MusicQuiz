package com.joel.musicquiz

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.joel.musicquiz.ui.theme.MusicQuizTheme
import kotlinx.coroutines.delay
import java.io.File
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicQuizTheme {
                MusicQuizApp()
            }
        }
    }
}

@Composable
fun MusicQuizApp() {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val musicFolderPath = Environment.getExternalStorageDirectory().absolutePath + "/Music"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            songs = loadSongsFromDirectory(musicFolderPath)
        } else {
            Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        } else {
            songs = loadSongsFromDirectory(musicFolderPath)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (songs.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.loading_songs, musicFolderPath))
            }
        } else {
            QuizScreen(
                songs = songs,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

data class Song(val title: String, val path: String)

fun loadSongsFromDirectory(directoryPath: String): List<Song> {
    val songs = mutableListOf<Song>()
    val directory = File(directoryPath)
    val supportedExtensions = listOf(".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac")
    if (directory.exists() && directory.isDirectory) {
        directory.listFiles { file ->
            file.isFile && supportedExtensions.any { extension ->
                file.name.endsWith(extension, ignoreCase = true)
            }
        }?.forEach { file ->
            songs.add(Song(title = file.nameWithoutExtension, path = file.absolutePath))
        }
    }
    return songs
}

data class QuizQuestion(val correctSong: Song, val options: List<Song>)

@Composable
fun QuizScreen(songs: List<Song>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    var currentQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var score by remember { mutableStateOf(0) }
    var questionsCompleted by remember { mutableStateOf(0) }
    var hasMadeWrongAttemptInCurrentQuestion by remember { mutableStateOf(false) }

    var isAdvancedMode by remember { mutableStateOf(false) }
    var advancedModePlayCount by remember { mutableStateOf(0) }

    var activeSongPathForLooping by remember { mutableStateOf<String?>(null) }
    var activeSnippetStartPositionForLooping by remember { mutableStateOf(0) }

    var currentPositionMs by remember { mutableStateOf(0) }
    var totalDurationMs by remember { mutableStateOf(0) }
    var isPlayingState by remember { mutableStateOf(false) }

    val songQueue = remember { mutableStateListOf<Song>() }

    fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    LaunchedEffect(isPlayingState, activeSongPathForLooping, isAdvancedMode, advancedModePlayCount) {
        while (isPlayingState && activeSongPathForLooping != null) {
            if (mediaPlayer.isPlaying) {
                 currentPositionMs = mediaPlayer.currentPosition
            }

            if (isAdvancedMode) {
                val elapsedTime = currentPositionMs - activeSnippetStartPositionForLooping
                if (advancedModePlayCount == 0 && elapsedTime >= 10000) {
                    mediaPlayer.pause()
                    isPlayingState = false
                } else if (advancedModePlayCount == 1 && elapsedTime >= 15000) {
                    mediaPlayer.pause()
                    isPlayingState = false
                }
            }
            delay(100)
        }
    }

    fun playSongSnippet(songPath: String, isNewRandomStartNeeded: Boolean) {
        try {
            if (isNewRandomStartNeeded || activeSongPathForLooping != songPath) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(songPath)
                mediaPlayer.setOnPreparedListener { mp ->
                    totalDurationMs = mp.duration
                    val typicalSnippetDurationMs = 15000
                    val maxPossibleRandomStart = if (totalDurationMs > typicalSnippetDurationMs) totalDurationMs - typicalSnippetDurationMs else 0
                    activeSnippetStartPositionForLooping = if (maxPossibleRandomStart > 0) Random.nextInt(0, maxPossibleRandomStart) else 0
                    activeSongPathForLooping = songPath

                    mp.seekTo(activeSnippetStartPositionForLooping)
                    currentPositionMs = activeSnippetStartPositionForLooping
                    mp.start()
                    isPlayingState = true
                }
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.seekTo(0)
                    currentPositionMs = 0
                    mp.start()
                }
                mediaPlayer.prepareAsync()
            } else {
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                    isPlayingState = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Reset state on error
        }
    }

    fun stopAndResetMediaPlayer() {
        if (mediaPlayer.isPlaying) mediaPlayer.stop()
        isPlayingState = false
        currentPositionMs = 0
        activeSongPathForLooping = null
        advancedModePlayCount = 0
    }

    fun generateNextQuestionAndPlayIfNeeded(playNext: Boolean) {
        stopAndResetMediaPlayer()
        if (songQueue.isEmpty()) songQueue.addAll(songs.shuffled())
        if (songQueue.isEmpty()) {
            currentQuestion = null
            return
        }
        val correctSong = songQueue.removeAt(0)
        val wrongOptions = songs.filter { it != correctSong }.shuffled().take(3)
        currentQuestion = QuizQuestion(correctSong, (wrongOptions + correctSong).shuffled())
        hasMadeWrongAttemptInCurrentQuestion = false

        if (playNext) {
            currentQuestion?.correctSong?.let { playSongSnippet(it.path, true) }
        }
    }

    DisposableEffect(Unit) { onDispose { mediaPlayer.release() } }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            generateNextQuestionAndPlayIfNeeded(true)
            questionsCompleted = 0
            score = 0
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (songs.size < 4) {
            Text(stringResource(R.string.not_enough_songs)); return
        }

        Text(stringResource(R.string.score, score, questionsCompleted))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isAdvancedMode, onCheckedChange = { isAdvancedMode = it })
            Text("Advanced Mode")
        }
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.what_song_is_this))
        Spacer(Modifier.height(32.dp))

        if (activeSongPathForLooping != null || totalDurationMs > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LinearProgressIndicator(
                    progress = { if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text("${formatTime(currentPositionMs)} / ${formatTime(totalDurationMs)}")
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    if (activeSongPathForLooping != null) {
                        if (mediaPlayer.isPlaying) {
                            mediaPlayer.pause()
                            isPlayingState = false
                        } else {
                            if (isAdvancedMode && !isPlayingState) {
                                advancedModePlayCount++
                            }
                            mediaPlayer.start()
                            isPlayingState = true
                        }
                    } else {
                        currentQuestion?.correctSong?.let { playSongSnippet(it.path, true) }
                    }
                }) {
                    Icon(
                        imageVector = if (isPlayingState) Icons.Filled.Close else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlayingState) "Close" else "Play"
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        currentQuestion?.options?.forEach { song ->
            Button(
                onClick = {
                    val isCorrect = song == currentQuestion?.correctSong
                    if (isCorrect) {
                        if (!hasMadeWrongAttemptInCurrentQuestion) {
                            if (isAdvancedMode) {
                                score += when (advancedModePlayCount) {
                                    0 -> 3
                                    1 -> 2
                                    else -> 1
                                }
                            } else {
                                score++
                            }
                        }
                        questionsCompleted++
                        Toast.makeText(context, context.getString(R.string.correct), Toast.LENGTH_SHORT).show()
                        generateNextQuestionAndPlayIfNeeded(true)
                    } else {
                        hasMadeWrongAttemptInCurrentQuestion = true
                        Toast.makeText(context, context.getString(R.string.wrong_try_again), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Text(song.title)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuizScreenPreview() {
    MusicQuizTheme {
        QuizScreen(songs = listOf(
            Song("Song 1", ""), Song("Song 2", ""), Song("Song 3", ""), Song("Song 4", "")
        ))
    }
}
