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

## AI Agent的问题和功能限制

*   步骤12：只能提供git操作的命令，commit message的建议，不能直接帮助提交
*   步骤14：遇到了NoSuchMethodError，建议通过更新bom到最新版本（最流行版本）来修改，但最终没有成功。对于没有编程基础的人基本就卡死在这里了
*   步骤16：遇到免费账号的限制，从Gemini 2.5 Pro 一路降级，直到Default Model

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
11. `需要支持多语种，至少支持英文和简体中文`
12. `很好，commit。`
13. `我需要把随机选择歌曲改成洗牌的方式随机打乱歌曲顺序，直到所有歌曲都被播放过之后，再次洗牌。避免短时间内遇到相同的歌曲多次。`
14. `Explain: FATAL EXCEPTION: main Process: com.joel.musicquiz, PID: 10523 java.lang.NoSuchMethodError: No virtual method removeFirst()Ljava/lang/Object; in class Landroidx/compose/runtime/snapshots/SnapshotStateList; or its super classes (declaration of '''androidx.compose.runtime.snapshots.SnapshotStateList''' appears in /data/app/~~0HdqwZfl00_l1ZzhFuWf-g==/com.joel.musicquiz-kBOfVKyj5ZIKPdA7UoXQmA==/base.apk) at com.joel.musicquiz.MainActivityKt.QuizScreen$generateNextQuestion(MainActivity.kt:167) at com.joel.musicquiz.MainActivityKt.access$QuizScreen$generateNextQuestion(MainActivity.kt:1) with tag AndroidRuntime`
15. `为我执行方案2`
16. `继续继续`
17. `回答对题目后，直接开始下一题。`
18. `你理解的有问题，不要去除Toast提示。只在回答正确时，立即执行【播放片段】的操作。继续继续继续`
19. `回答错误的时候不要停止音乐，不要自动切换到下一题。还是允许玩家继续选择，但是不再加分，直到选对为止。`
20. `打错后再答对不用提示玩家“答对了，本题不计分”，正常提示玩家“答对了”就行。答对之后立刻播放下一题的歌曲，和正常答对流程一样。分数显示改成 分数/题目数。`
21. `每次播放歌曲不要从固定时间开始，改成随机的时间，如果播放完毕要循环播放`
22. `把今天的User Prompts添加到README.md的【与 Agent 的交互记录 (User Prompts)】模块的后面`
