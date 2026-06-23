package com.threedstudio.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.threedstudio.render.GLRenderEngine
import com.threedstudio.render.SceneObject
import com.threedstudio.render.core.Vec3
import com.threedstudio.animation.AnimationController
import com.threedstudio.animation.AnimationTrack
import com.threedstudio.animation.TrackType
import com.threedstudio.animation.Keyframe
import com.threedstudio.animation.KeyframeValue
import com.threedstudio.animation.InterpolationType

/**
 * 专业级3D编辑器横屏布局
 * 五区：菜单栏 | 层级面板 | 3D视口 | 属性面板 | 时间轴
 */
@Composable
fun EditorLayout(
    renderEngine: GLRenderEngine,
    animationController: AnimationController,
    sceneObjects: List<SceneObject>,
    onPlay: () -> Unit = {},
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onToggleLoop: () -> Unit = {},
    onSeekToStart: () -> Unit = {},
    onSeekToEnd: () -> Unit = {},
    onImportFile: () -> Unit = {}
) {
    var selectedObjectIndex by remember { mutableIntStateOf(-1) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoopEnabled by remember { mutableStateOf(animationController.isLoopEnabled()) }
    var currentTime by remember { mutableStateOf(0f) }
    val duration by remember { mutableStateOf(animationController.getDuration()) }

    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            currentTime = animationController.getCurrentTime()
            kotlinx.coroutines.delay(16L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // 顶部菜单栏
        TopMenuBar(onImport = onImportFile)

        // 主体三栏
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // 左侧：层级面板
            HierarchyPanel(
                sceneObjects = sceneObjects,
                selectedIndex = selectedObjectIndex,
                onSelectObject = { selectedObjectIndex = it },
                modifier = Modifier.weight(0.15f)
            )
            // 中央：3D视口
            CenterViewport(
                renderEngine = renderEngine,
                modifier = Modifier.weight(0.65f)
            )
            // 右侧：属性面板
            PropertiesPanel(
                renderEngine = renderEngine,
                selectedObject = if (selectedObjectIndex in sceneObjects.indices) sceneObjects[selectedObjectIndex] else null,
                modifier = Modifier.weight(0.2f)
            )
        }

        // 底部：时间轴
        BottomTimeline(
            currentTime = currentTime,
            duration = duration,
            frameRate = animationController.getFrameRate(),
            isPlaying = isPlaying,
            isLoopEnabled = isLoopEnabled,
            tracks = animationController.getAllTracks(),
            onPlay = { isPlaying = true; onPlay() },
            onPause = { isPlaying = false; onPause() },
            onStop = { isPlaying = false; onStop(); currentTime = 0f },
            onToggleLoop = { isLoopEnabled = !isLoopEnabled; onToggleLoop() },
            onSeekToStart = { onSeekToStart(); currentTime = 0f },
            onSeekToEnd = { onSeekToEnd(); currentTime = duration },
            onAddKeyframe = {
                animationController.getAllTracks().firstOrNull()?.let { track ->
                    track.addKeyframe(
                        Keyframe(
                            time = currentTime,
                            value = KeyframeValue.Vec3Value(Vec3.ZERO),
                            interpolation = InterpolationType.LINEAR
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 160.dp)
        )
    }
}

// ──── 顶部菜单栏 ────

@Composable
fun TopMenuBar(onImport: () -> Unit = {}) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1E1E1E),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(36.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var showFileMenu by remember { mutableStateOf(false) }
            Box {
                TextButton(
                    onClick = { showFileMenu = !showFileMenu },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("文件", color = Color.LightGray, fontSize = 13.sp)
                }
                DropdownMenu(expanded = showFileMenu, onDismissRequest = { showFileMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("导入模型") },
                        leadingIcon = { Icon(Icons.Default.FolderOpen, null) },
                        onClick = { showFileMenu = false; onImport() }
                    )
                    DropdownMenuItem(
                        text = { Text("新建项目") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = { showFileMenu = false }
                    )
                    DropdownMenuItem(
                        text = { Text("导出视频") },
                        leadingIcon = { Icon(Icons.Default.VideoFile, null) },
                        onClick = { showFileMenu = false }
                    )
                }
            }
            TextButton(onClick = { }, modifier = Modifier.height(32.dp)) { Text("编辑", color = Color.LightGray, fontSize = 13.sp) }
            TextButton(onClick = { }, modifier = Modifier.height(32.dp)) { Text("视图", color = Color.LightGray, fontSize = 13.sp) }
            TextButton(onClick = { }, modifier = Modifier.height(32.dp)) { Text("模型", color = Color.LightGray, fontSize = 13.sp) }
            TextButton(onClick = { }, modifier = Modifier.height(32.dp)) { Text("动画", color = Color.LightGray, fontSize = 13.sp) }
            TextButton(onClick = { }, modifier = Modifier.height(32.dp)) { Text("渲染", color = Color.LightGray, fontSize = 13.sp) }
            Spacer(modifier = Modifier.weight(1f))
            Text("3D Animation Studio v1.0", color = Color(0xFF4FC3F7), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

// ──── 左侧层级面板 ────

@Composable
fun HierarchyPanel(
    sceneObjects: List<SceneObject>,
    selectedIndex: Int,
    onSelectObject: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier.fillMaxHeight(), color = Color(0xFF1A1A1A)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF252526)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccountTree, "层级", tint = Color(0xFF4FC3F7), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("场景层级", color = Color.LightGray, fontSize = 12.sp)
            }
            HorizontalDivider(color = Color(0xFF3E3E3E), thickness = 1.dp)
            if (sceneObjects.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF141414)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("场景为空\n点击「文件 → 导入模型」", color = Color.DarkGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF141414))) {
                    items(sceneObjects.size) { index ->
                        val obj = sceneObjects[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (index == selectedIndex) Color(0xFF094771) else Color.Transparent)
                                .clickable { onSelectObject(index) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (obj.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                null,
                                tint = if (obj.visible) Color.Gray else Color.DarkGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                obj.name,
                                color = if (index == selectedIndex) Color.White else Color.LightGray,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFF3E3E3E), thickness = 1.dp)
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF252526)).padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, "添加", tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                IconButton(onClick = { }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, "删除", tint = Color.Gray, modifier = Modifier.size(16.dp)) }
                IconButton(onClick = { }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.ContentCopy, "复制", tint = Color.Gray, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

// ──── 中央3D视口 ────

@Composable
fun CenterViewport(renderEngine: GLRenderEngine, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxHeight(), color = Color(0xFF0D0D0D)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
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
            // 左上角标签
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                color = Color(0xAA1E1E1E),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("3D 视口 | 透视", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            // 右下角信息
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                color = Color(0xAA1E1E1E),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("FPS: 30 | OpenGL ES 3.0", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
            }
        }
    }
}

// ──── 右侧属性面板 ────

@Composable
fun PropertiesPanel(
    renderEngine: GLRenderEngine,
    selectedObject: SceneObject?,
    modifier: Modifier = Modifier
) {
    var posX by remember(selectedObject) { mutableStateOf(selectedObject?.position?.x ?: 0f) }
    var posY by remember(selectedObject) { mutableStateOf(selectedObject?.position?.y ?: 0f) }
    var posZ by remember(selectedObject) { mutableStateOf(selectedObject?.position?.z ?: 0f) }
    var rotX by remember(selectedObject) { mutableStateOf(0f) }
    var rotY by remember(selectedObject) { mutableStateOf(0f) }
    var rotZ by remember(selectedObject) { mutableStateOf(0f) }
    var scaX by remember(selectedObject) { mutableStateOf(selectedObject?.scale?.x ?: 1f) }
    var scaY by remember(selectedObject) { mutableStateOf(selectedObject?.scale?.y ?: 1f) }
    var scaZ by remember(selectedObject) { mutableStateOf(selectedObject?.scale?.z ?: 1f) }
    var visible by remember(selectedObject) { mutableStateOf(selectedObject?.visible ?: true) }

    LaunchedEffect(posX, posY, posZ) { selectedObject?.position = Vec3(posX, posY, posZ) }
    LaunchedEffect(scaX, scaY, scaZ) { selectedObject?.scale = Vec3(scaX, scaY, scaZ) }
    LaunchedEffect(visible) { selectedObject?.visible = visible }

    Surface(modifier = modifier.fillMaxHeight(), color = Color(0xFF1A1A1A)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            Text("属性面板", color = Color(0xFF4FC3F7), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Color(0xFF3E3E3E), modifier = Modifier.padding(vertical = 4.dp))

            if (selectedObject == null) {
                Text("未选中对象", color = Color.DarkGray, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            } else {
                Text(selectedObject.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                // 位置
                SectionHeader("变换 > 位置")
                PropSlider("X", posX, -10f, 10f) { posX = it }
                PropSlider("Y", posY, -10f, 10f) { posY = it }
                PropSlider("Z", posZ, -10f, 10f) { posZ = it }
                Spacer(modifier = Modifier.height(8.dp))

                // 旋转
                SectionHeader("变换 > 旋转 (°)")
                PropSlider("X", rotX, -180f, 180f) { rotX = it }
                PropSlider("Y", rotY, -180f, 180f) { rotY = it }
                PropSlider("Z", rotZ, -180f, 180f) { rotZ = it }
                Spacer(modifier = Modifier.height(8.dp))

                // 缩放
                SectionHeader("变换 > 缩放")
                PropSlider("X", scaX, 0.1f, 10f) { scaX = it }
                PropSlider("Y", scaY, 0.1f, 10f) { scaY = it }
                PropSlider("Z", scaZ, 0.1f, 10f) { scaZ = it }
                Spacer(modifier = Modifier.height(8.dp))

                // 材质颜色预览
                SectionHeader("材质")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("颜色", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(androidx.compose.ui.graphics.Color(
                                red = selectedObject.materialColor.r,
                                green = selectedObject.materialColor.g,
                                blue = selectedObject.materialColor.b
                            ))
                            .border(1.dp, Color.Gray)
                    )
                }

                // 可见性
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("可见", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = visible,
                        onCheckedChange = { visible = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF4FC3F7),
                            checkedTrackColor = Color(0xFF1565C0)
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader("渲染设置")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MSAA", color = Color.LightGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("2x", color = Color.Gray, fontSize = 11.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("阴影", color = Color.LightGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text(if (renderEngine.shadowsEnabled) "开启" else "关闭", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun PropSlider(label: String, value: Float, min: Float, max: Float, onValueChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.width(16.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.weight(1f).height(20.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF4FC3F7),
                activeTrackColor = Color(0xFF4FC3F7),
                inactiveTrackColor = Color(0xFF3E3E3E)
            )
        )
        Text(
            String.format("%.2f", value),
            color = Color.Gray,
            fontSize = 9.sp,
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        color = Color(0xFF4FC3F7),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

// ──── 底部时间轴 ────

@Composable
fun BottomTimeline(
    currentTime: Float,
    duration: Float,
    frameRate: Float,
    isPlaying: Boolean,
    isLoopEnabled: Boolean,
    tracks: List<AnimationTrack>,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onToggleLoop: () -> Unit,
    onSeekToStart: () -> Unit,
    onSeekToEnd: () -> Unit,
    onAddKeyframe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = Color(0xFF1A1A1A), shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 播放控制栏
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0xFF252526)).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSeekToStart, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipPrevious, "起始", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { if (isPlaying) onPause() else onPlay() }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = Color(0xFF4FC3F7),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = onStop, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Stop, "停止", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onSeekToEnd, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipNext, "结尾", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleLoop, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Loop,
                        "循环",
                        tint = if (isLoopEnabled) Color(0xFF4FC3F7) else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onAddKeyframe, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FiberManualRecord, "添加关键帧", tint = Color(0xFFFFEB3B), modifier = Modifier.size(14.dp))
                }
                VerticalDivider(modifier = Modifier.width(1.dp).height(24.dp), color = Color(0xFF3E3E3E))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "帧: ${(currentTime * frameRate).toInt()} | ${String.format("%.1f", currentTime)}s / ${String.format("%.1f", duration)}s | ${frameRate.toInt()}fps",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text("${tracks.size} 轨道", color = Color.DarkGray, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 时间轴标尺
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF141414))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progress = if (duration > 0f) currentTime / duration else 0f
                    val playheadX = progress * size.width
                    // 刻度线
                    for (i in 0..10) {
                        val x = i * size.width / 10f
                        drawLine(Color(0xFF3E3E3E), Offset(x, 0f), Offset(x, size.height), 1f)
                    }
                    // 播放头
                    drawLine(Color(0xFFFF5252), Offset(playheadX, 0f), Offset(playheadX, size.height), 2f)
                }
            }

            // 轨道列表
            if (tracks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp * tracks.size.coerceAtMost(3))
                        .background(Color(0xFF1E1E1E))
                ) {
                    items(tracks.take(3)) { track ->
                        Row(
                            modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (track.type) {
                                    TrackType.TRANSFORM -> Icons.Default.SwapHoriz
                                    TrackType.CAMERA -> Icons.Default.Videocam
                                    TrackType.LIGHT -> Icons.Default.Lightbulb
                                    TrackType.MORPH -> Icons.Default.Face
                                },
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(track.name, color = Color.LightGray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                            Text("${track.getKeyframeCount()} 关键帧", color = Color.DarkGray, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}
