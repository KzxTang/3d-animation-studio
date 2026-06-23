package com.threedstudio.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.threedstudio.render.GLRenderEngine

@Composable
fun EditorLayout(
    renderEngine: GLRenderEngine,
    onImportFile: () -> Unit = {}
) {
    var isPlaying by remember { mutableStateOf(false) }
    var isLoopEnabled by remember { mutableStateOf(false) }
    var currentFrame by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                currentFrame = (currentFrame + 1) % 300
                kotlinx.coroutines.delay(33L)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopMenuBar(
            onImportFile = onImportFile,
            context = context
        )
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LeftPanel(onImportFile = onImportFile, modifier = Modifier.weight(0.15f))
            CenterViewport(renderEngine = renderEngine, modifier = Modifier.weight(0.65f))
            RightPanel(modifier = Modifier.weight(0.2f))
        }
        BottomTimeline(
            currentFrame = currentFrame,
            isPlaying = isPlaying,
            isLoopEnabled = isLoopEnabled,
            onPlay = { isPlaying = true },
            onPause = { isPlaying = false },
            onStop = { isPlaying = false; currentFrame = 0 },
            onToggleLoop = { isLoopEnabled = !isLoopEnabled },
            onSeekToStart = { currentFrame = 0 },
            onSeekToEnd = { currentFrame = 299 },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 150.dp)
        )
    }
}

@Composable
fun TopMenuBar(onImportFile: () -> Unit, context: android.content.Context) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF252526), shadowElevation = 4.dp) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            // 文件按钮 - 真正调用文件选择器
            TextButton(onClick = onImportFile) {
                Icon(Icons.Default.FolderOpen, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("文件", color = Color.LightGray)
            }
            // 其他菜单全部有Toast
            TextButton(onClick = { Toast.makeText(context, "编辑功能开发中", Toast.LENGTH_SHORT).show() }) { Text("编辑", color = Color.LightGray) }
            TextButton(onClick = { Toast.makeText(context, "视图功能开发中", Toast.LENGTH_SHORT).show() }) { Text("视图", color = Color.LightGray) }
            TextButton(onClick = { Toast.makeText(context, "模型功能开发中", Toast.LENGTH_SHORT).show() }) { Text("模型", color = Color.LightGray) }
            TextButton(onClick = { Toast.makeText(context, "动画功能开发中", Toast.LENGTH_SHORT).show() }) { Text("动画", color = Color.LightGray) }
            TextButton(onClick = { Toast.makeText(context, "渲染功能开发中", Toast.LENGTH_SHORT).show() }) { Text("渲染", color = Color.LightGray) }
            TextButton(onClick = { Toast.makeText(context, "帮助功能开发中", Toast.LENGTH_SHORT).show() }) { Text("帮助", color = Color.LightGray) }
            Spacer(modifier = Modifier.weight(1f))
            Text("3D Animation Studio", color = Color(0xFF4FC3F7))
        }
    }
}

@Composable
fun LeftPanel(onImportFile: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxHeight(), color = Color(0xFF1E1E1E)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text("场景层级", color = Color.Gray, modifier = Modifier.padding(8.dp))
            Divider(color = Color(0xFF3E3E3E))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF1A1A1A))) {
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text("暂无对象", color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onImportFile) {
                        Text("点击导入模型", color = Color(0xFF4FC3F7))
                    }
                }
            }
        }
    }
}

@Composable
fun CenterViewport(renderEngine: GLRenderEngine, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxHeight(), color = Color(0xFF0D0D0D)) {
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.opengl.GLSurfaceView(ctx).apply {
                        setEGLContextClientVersion(3)
                        preserveEGLContextOnPause = true
                        setRenderer(object : android.opengl.GLSurfaceView.Renderer {
                            override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
                                renderEngine.init()
                            }
                            override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, w: Int, h: Int) {
                                renderEngine.setViewport(w, h)
                            }
                            override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
                                renderEngine.renderFrame()
                            }
                        })
                        renderMode = android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Text("3D 视口", color = Color.Gray, modifier = Modifier.align(Alignment.TopStart).padding(8.dp))
        }
    }
}

@Composable
fun RightPanel(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxHeight(), color = Color(0xFF1E1E1E)) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text("属性面板", color = Color.Gray)
            Divider(color = Color(0xFF3E3E3E))
            Spacer(modifier = Modifier.height(8.dp))
            Text("变换", color = Color.White)
            PropertySlider("位置 X", remember { mutableFloatStateOf(0f) })
            PropertySlider("位置 Y", remember { mutableFloatStateOf(0f) })
            PropertySlider("位置 Z", remember { mutableFloatStateOf(0f) })
            Spacer(modifier = Modifier.height(8.dp))
            PropertySlider("旋转 X", remember { mutableFloatStateOf(0f) })
            PropertySlider("旋转 Y", remember { mutableFloatStateOf(0f) })
            PropertySlider("旋转 Z", remember { mutableFloatStateOf(0f) })
            Spacer(modifier = Modifier.height(8.dp))
            PropertySlider("缩放 X", remember { mutableFloatStateOf(1f) })
            PropertySlider("缩放 Y", remember { mutableFloatStateOf(1f) })
            PropertySlider("缩放 Z", remember { mutableFloatStateOf(1f) })
        }
    }
}

@Composable
fun PropertySlider(label: String, valueState: MutableFloatState) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.LightGray, modifier = Modifier.width(50.dp))
        Slider(
            value = valueState.floatValue,
            onValueChange = { valueState.floatValue = it },
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4FC3F7),
                activeTrackColor = Color(0xFF4FC3F7),
                inactiveTrackColor = Color(0xFF3E3E3E)
            )
        )
    }
}

@Composable
fun BottomTimeline(
    currentFrame: Int,
    isPlaying: Boolean,
    isLoopEnabled: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onToggleLoop: () -> Unit,
    onSeekToStart: () -> Unit,
    onSeekToEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fps = 30
    val totalFrames = 300
    Surface(modifier = modifier, color = Color(0xFF1E1E1E)) {
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onSeekToStart) { Icon(Icons.Default.SkipPrevious, "跳到首帧", tint = Color.LightGray) }
                IconButton(onClick = { if (isPlaying) onPause() else onPlay() }) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "暂停" else "播放", tint = Color(0xFF4FC3F7))
                }
                IconButton(onClick = onStop) { Icon(Icons.Default.Stop, "停止", tint = Color.LightGray) }
                IconButton(onClick = onSeekToEnd) { Icon(Icons.Default.SkipNext, "跳到尾帧", tint = Color.LightGray) }
                IconButton(onClick = onToggleLoop) { Icon(Icons.Default.Loop, "循环", tint = if (isLoopEnabled) Color(0xFF4FC3F7) else Color.LightGray) }
                Spacer(modifier = Modifier.weight(1f))
                Text("帧: $currentFrame/$totalFrames | ${fps}fps", color = Color.Gray)
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Color(0xFF2D2D2D))) {
                Text("时间轴编辑器 (多轨道: 变换 | 表情 | 相机 | 灯光)", color = Color.DarkGray, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
