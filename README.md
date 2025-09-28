# MusicQuiz

这是一个简单的 Android 猜音乐应用。

## 功能

*   从设备本地存储的 `/storage/emulated/0/Music` 文件夹中读取 `.mp3` 格式的音乐文件。
*   随机选择一首歌曲并播放其中的一小段（从第 15 秒开始）。
*   在界面上提供四个随机的歌曲名称选项。
*   用户选择答案后，应用会判断对错，并给出相应的提示。
*   答对后，分数会增加。
*   无论对错，应用都会自动生成下一道题。

这个应用是使用 Jetpack Compose 构建的，并处理了 Android 运行时权限（`READ_MEDIA_AUDIO`）。

---

## 开发环境

*   **IDE**: Android Studio Narwhal 3 Feature Drop | 2025.1.3
*   **AI Agent**: Gemini - Agent(Preview) - Gemini 2.5 Pro

---

## 与 Agent 的交互记录 (User Prompts)

1.  `我要做一个猜音乐的功能，指定一个本地的文件夹，读取里面所有的mp3格式的音乐。随机播放其中一首音乐的一个片段，然后改出4个选项，让玩家猜测是哪个。`
2.  `在 MainActivity.kt 文件中添加这些代码`
3.  `按照1、2、3的步骤继续`
4.  `继续`
5.  `处理文件权限和加载问题`
6.  `好的，继续`
7.  `提示permission denied，怎么办？`
8.  `我希望指定一个存放音乐的路径，如 sdcard/Music`
9.  (用户隐式地报告了一个代码编译错误，Agent 随后进行了修复)
10. `为我创建README.md，简要描述这个项目的内容。记录IDE的版本，Gemini的版本，我对Agent说的话。`
