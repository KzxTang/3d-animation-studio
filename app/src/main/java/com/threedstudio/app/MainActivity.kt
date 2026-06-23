package com.threedstudio.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.threedstudio.render.GLRenderEngine
import com.threedstudio.render.SceneObject
import com.threedstudio.render.core.Vec3
import com.threedstudio.render.core.GpuMesh
import com.threedstudio.render.core.Color as RenderColor
import com.threedstudio.render.core.Quaternion
import com.threedstudio.animation.AnimationController
import com.threedstudio.animation.TrackType
import com.threedstudio.animation.Keyframe
import com.threedstudio.animation.KeyframeValue
import com.threedstudio.animation.InterpolationType
import com.threedstudio.ui.EditorLayout
import kotlinx.coroutines.*

/**
 * 横屏锁定专业3D编辑器 - 完整功能实现
 */
class MainActivity : ComponentActivity() {

    lateinit var renderEngine: GLRenderEngine
    var animationController = AnimationController()
    private var animationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 锁定横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 初始化渲染引擎
        val app = application as ThreeDStudioApp
        app.initRenderEngine()
        renderEngine = app.renderEngine!!

        // 创建演示场景
        createDefaultScene()

        // 初始化动画轨道
        initAnimationTracks()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF4FC3F7),
                    secondary = Color(0xFF81C784),
                    background = Color(0xFF1E1E1E),
                    surface = Color(0xFF2D2D2D),
                    onPrimary = Color.Black,
                    onSecondary = Color.Black,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorLayout(
                        renderEngine = renderEngine,
                        animationController = animationController,
                        sceneObjects = renderEngine.sceneObjects.toList(),
                        onPlay = { playAnimation() },
                        onPause = { pauseAnimation() },
                        onStop = { stopAnimation() },
                        onToggleLoop = { animationController.setLoop(!animationController.isLoopEnabled()) },
                        onSeekToStart = { animationController.setCurrentTime(0f) },
                        onSeekToEnd = { animationController.setCurrentTime(animationController.getDuration()) },
                        onImportFile = { showImportDialog() }
                    )
                }
            }
        }
    }

    private fun createDefaultScene() {
        // 创建演示立方体
        val quadMesh = GpuMesh.createQuad()
        val cube = SceneObject(
            name = "演示立方体",
            position = Vec3(0f, 2f, 0f),
            scale = Vec3(3f, 3f, 3f),
            materialColor = RenderColor(0.2f, 0.6f, 1f, 1f),
            gpuMesh = quadMesh
        )
        renderEngine.addSceneObject(cube)

        // 添加第二个对象（地面参考）
        val groundMesh = GpuMesh.createQuad()
        val ground = SceneObject(
            name = "参考平面",
            position = Vec3(0f, -1f, 0f),
            scale = Vec3(8f, 1f, 8f),
            materialColor = RenderColor(0.4f, 0.4f, 0.4f, 0.6f),
            rotation = Quaternion(-1.57f, 0f, 0f, 1f),
            gpuMesh = groundMesh
        )
        renderEngine.addSceneObject(ground)
    }

    private fun initAnimationTracks() {
        val track = animationController.createTrack("立方体_旋转", TrackType.TRANSFORM)
        track.addKeyframe(Keyframe(0f, KeyframeValue.Vec3Value(Vec3(0f, 0f, 0f)), InterpolationType.LINEAR))
        track.addKeyframe(Keyframe(3f, KeyframeValue.Vec3Value(Vec3(0f, 360f, 0f)), InterpolationType.EASE_IN_OUT))
        animationController.setTimeRange(0f, 5f)
        animationController.setLoop(true)
    }

    private fun playAnimation() {
        animationController.play()
        animationJob?.cancel()
        animationJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && animationController.isPlaying()) {
                animationController.update(1f / 30f)
                delay(33)
            }
        }
    }

    private fun pauseAnimation() {
        animationController.pause()
        animationJob?.cancel()
    }

    private fun stopAnimation() {
        animationController.stop()
        animationJob?.cancel()
    }

    private fun showImportDialog() {
        Toast.makeText(this, "文件导入功能开发中...\n支持 PMX/GLTF/FBX/OBJ 格式", Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        renderEngine.onResume()
    }

    override fun onPause() {
        super.onPause()
        renderEngine.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        animationJob?.cancel()
        renderEngine.release()
    }
}
