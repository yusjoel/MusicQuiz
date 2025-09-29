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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import java.io.File
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusicQuizTheme {
                // We handle permission and loading logic here
                MusicQuizApp()
            }
        }
    }
}

@Composable
fun MusicQuizApp() {
    val context = LocalContext.current
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Define the specific music folder path
    val musicFolderPath = Environment.getExternalStorageDirectory().absolutePath + "/Music"

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, load the songs from the specific path
            songs = loadSongsFromDirectory(musicFolderPath)
        } else {
            // Handle permission denial
            Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    // Request permission on launch
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        } else {
            // Permission already granted, load songs from the specific path
            songs = loadSongsFromDirectory(musicFolderPath)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (songs.isEmpty()) {
            // Show a loading message or a message asking for permission
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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


// 歌曲的数据类
data class Song(
    val title: String,
    val path: String
)

/**
 * 从指定文件夹加载 MP3 音乐文件
 * @param directoryPath 文件夹路径
 * @return Song 列表
 */
fun loadSongsFromDirectory(directoryPath: String): List<Song> {
    val songs = mutableListOf<Song>()
    val directory = File(directoryPath)
    if (directory.exists() && directory.isDirectory) {
        directory.listFiles { _, name -> name.endsWith(".mp3", ignoreCase = true) }
            ?.forEach { file ->
                songs.add(Song(title = file.nameWithoutExtension, path = file.absolutePath))
            }
    }
    return songs
}

// 用于表示一个竞猜题目的数据类
data class QuizQuestion(
    val correctSong: Song,
    val options: List<Song>
)

@Composable
fun QuizScreen(songs: List<Song>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    var currentQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var score by remember { mutableStateOf(0) }
    var questionsCompleted by remember { mutableStateOf(0) }
    var hasMadeWrongAttemptInCurrentQuestion by remember { mutableStateOf(false) }

    var activeSongPathForLooping by remember { mutableStateOf<String?>(null) }
    var activeSnippetStartPositionForLooping by remember { mutableStateOf(0) }

    val songQueue = remember { mutableStateListOf<Song>() }

    fun playSongSnippet(songPath: String, isNewRandomStartNeeded: Boolean) {
        try {
            if (isNewRandomStartNeeded || activeSongPathForLooping != songPath) {
                mediaPlayer.reset()
                mediaPlayer.setDataSource(songPath)
                mediaPlayer.setOnPreparedListener { mp ->
                    val songDuration = mp.duration
                    val typicalSnippetDurationMs = 15000 // 15 seconds

                    val maxPossibleRandomStart = if (songDuration > typicalSnippetDurationMs) songDuration - typicalSnippetDurationMs else 0
                    activeSnippetStartPositionForLooping = if (maxPossibleRandomStart > 0) Random.nextInt(0, maxPossibleRandomStart) else 0
                    activeSongPathForLooping = songPath

                    mp.seekTo(activeSnippetStartPositionForLooping)
                    mp.start()
                }
                mediaPlayer.setOnCompletionListener { mp ->
                    mp.seekTo(activeSnippetStartPositionForLooping)
                    mp.start()
                }
                mediaPlayer.prepareAsync()
            } else {
                // Same song, snippet start position already determined, just play/resume for loop or replay
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.seekTo(activeSnippetStartPositionForLooping)
                    mediaPlayer.start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            activeSongPathForLooping = null // Reset on error to avoid inconsistent state
        }
    }

    fun generateNextQuestionAndPlayIfNeeded(playNext: Boolean) {
        if (songQueue.isEmpty()) {
            songQueue.addAll(songs.shuffled())
        }
        if (songQueue.isEmpty()) {
            currentQuestion = null
            return
        }
        val correctSong = songQueue.removeAt(0)
        val wrongOptions = songs.filter { it != correctSong }.shuffled().take(3)
        val allOptions = (wrongOptions + correctSong).shuffled()
        currentQuestion = QuizQuestion(correctSong, allOptions)
        hasMadeWrongAttemptInCurrentQuestion = false

        if (playNext) {
            currentQuestion?.correctSong?.let { song ->
                playSongSnippet(song.path, isNewRandomStartNeeded = true)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            generateNextQuestionAndPlayIfNeeded(false)
            questionsCompleted = 0 
            score = 0 
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (songs.size < 4) {
            Text(stringResource(R.string.not_enough_songs))
            return
        }

        Text(stringResource(R.string.score, score, questionsCompleted))
        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.what_song_is_this))
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                currentQuestion?.correctSong?.let { songToPlay ->
                    val newRandomStartNeeded = activeSongPathForLooping != songToPlay.path
                    playSongSnippet(songToPlay.path, isNewRandomStartNeeded = newRandomStartNeeded)
                }
            },
            enabled = currentQuestion != null
        ) {
            Text(stringResource(R.string.play_snippet))
        }
        Spacer(modifier = Modifier.height(32.dp))

        currentQuestion?.options?.forEach { song ->
            Button(
                onClick = {
                    val isCorrect = song == currentQuestion?.correctSong
                    if (isCorrect) {
                        mediaPlayer.stop() // Stop current playback before moving on
                        Toast.makeText(context, context.getString(R.string.correct), Toast.LENGTH_SHORT).show()
                        if (!hasMadeWrongAttemptInCurrentQuestion) {
                            score++
                        }
                        questionsCompleted++
                        generateNextQuestionAndPlayIfNeeded(playNext = true)
                    } else {
                        hasMadeWrongAttemptInCurrentQuestion = true
                        Toast.makeText(context, context.getString(R.string.wrong_try_again), Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
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
        val previewSongs = listOf(
            Song("Song 1", ""),
            Song("Song 2", ""),
            Song("Song 3", ""),
            Song("Song 4", "")
        )
        QuizScreen(songs = previewSongs)
    }
}
