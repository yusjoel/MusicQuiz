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

// 生成一个新题目
fun generateNewQuestion(songs: List<Song>): QuizQuestion? {
    // 确保有足够的歌曲来创建题目（至少4首）
    if (songs.size < 4) return null
    val shuffledSongs = songs.shuffled()
    val correctSong = shuffledSongs.first()
    val options = shuffledSongs.take(4).shuffled()
    return QuizQuestion(correctSong, options)
}

@Composable
fun QuizScreen(songs: List<Song>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    var currentQuestion by remember { mutableStateOf<QuizQuestion?>(null) }
    var score by remember { mutableStateOf(0) }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            currentQuestion = generateNewQuestion(songs)
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

        Text(stringResource(R.string.score, score))
        Spacer(modifier = Modifier.height(16.dp))

        Text(stringResource(R.string.what_song_is_this))
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                currentQuestion?.correctSong?.path?.let { path ->
                    try {
                        mediaPlayer.reset()
                        mediaPlayer.setDataSource(path)
                        mediaPlayer.prepareAsync()
                        mediaPlayer.setOnPreparedListener { mp ->
                            mp.seekTo(15000)
                            mp.start()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
                    mediaPlayer.stop()
                    val isCorrect = song == currentQuestion?.correctSong
                    if (isCorrect) {
                        score++
                        Toast.makeText(context, context.getString(R.string.correct), Toast.LENGTH_SHORT).show()
                    } else {
                        val correctAnswer = currentQuestion?.correctSong?.title
                        Toast.makeText(context, context.getString(R.string.wrong, correctAnswer), Toast.LENGTH_SHORT).show()
                    }
                    currentQuestion = generateNewQuestion(songs)
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
