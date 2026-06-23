package com.threedstudio.ui

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
import androidx.compose.ui.unit.dp
import com.threedstudio.render.GLRenderEngine

/**
 * 专业级3D编辑器主布局
 * 参考Blender/Maya桌面编辑器设计
 * 布局：顶部菜单栏 | 左侧层级面板 | 中央3D视口 | 右侧属性面板 | 底部时间轴
 */
@Composable
fun EditorLayout(renderEngine: GLRenderEngine) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部菜单栏
        TopMenuBar()
        
        // 主体三栏布局
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 左侧：层级视图 + 项目资源
            LeftPanel(modifier = Modifier.weight(0.15f))
            
            // 中央：3D视口
            CenterViewport(
                renderEngine = renderEngine,
                modifier = Modifier.weight(0.65f)
            )
            
            // 右侧：属性面板
            RightPanel(modifier = Modifier.weight(0.2f))
        }
        
        // 底部：时间轴与播放控制
        BottomTimeline(modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 150.dp))
    }
}

@Composable
fun TopMenuBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF252526),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { }) { Text("文件", color = Color.LightGray) }
            TextButton(onClick = { }) { Text("编辑", color = Color.LightGray) }
            TextButton(onClick = { }) { Text("视图", color = Color.LightGray) }
            TextButton(onClick = { }) { Text("模型", color = Color.LightGray) }
            TextButton(onClick = { }) { Text("动画", color = Color.LightGray) }
            TextButton(onClick = { }) { Text("渲染", color = Color.LightGray) }
            TextButton(onClick = { }) { Text("帮助", color = Color.LightGray) }
            Spacer(modifier = Modifier.weight(1f))
            Text("3D Animation Studio", color = Color(0xFF4FC3F7))
        }
    }
}

@Composable
fun LeftPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color(0xFF1E1E1E)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 场景层级
            Text(
                "场景层级",
                color = Color.Gray,
                modifier = Modifier.padding(8.dp)
            )
            Divider(color = Color(0xFF3E3E3E))
            // 层级列表占位
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
            ) {
                Text(
                    "暂无对象\n拖拽模型文件或点击导入",
                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun CenterViewport(renderEngine: GLRenderEngine, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color(0xFF0D0D0D)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 这里应嵌入GLSurfaceView
            // 实际实现中需要使用AndroidView包装GLSurfaceView
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context ->
                    android.opengl.GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        preserveEGLContextOnPause = true
                        setRenderer(object : android.opengl.GLSurfaceView.Renderer {
                            override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
                                renderEngine.init()
                            }
                            override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
                                renderEngine.setViewport(width, height)
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
            
            // 视图标签
            Text(
                "3D 视口",
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun RightPanel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color(0xFF1E1E1E)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Text("属性面板", color = Color.Gray)
            Divider(color = Color(0xFF3E3E3E))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Transform区域
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
fun BottomTimeline(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E1E1E)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
            // 播放控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.SkipPrevious, "跳到首帧", tint = Color.LightGray)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.PlayArrow, "播放", tint = Color(0xFF4FC3F7))
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Pause, "暂停", tint = Color.LightGray)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.SkipNext, "跳到尾帧", tint = Color.LightGray)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Loop, "循环", tint = Color.LightGray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("帧: 0/300 | 30fps", color = Color.Gray)
            }
            
            // 时间轴占位
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2D2D2D))
            ) {
                Text(
                    "时间轴编辑器 (多轨道: 变换 | 表情 | 相机 | 灯光)",
                    color = Color.DarkGray,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
