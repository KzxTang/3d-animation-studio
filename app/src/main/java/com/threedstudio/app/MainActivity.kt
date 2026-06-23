package com.threedstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.threedstudio.ui.EditorLayout
import com.threedstudio.render.GLRenderEngine

/**
 * 主Activity - 3D动画编辑器入口
 * 使用Jetpack Compose构建专业桌面级UI
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var renderEngine: GLRenderEngine
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化渲染引擎
        val app = application as ThreeDStudioApp
        app.initRenderEngine()
        renderEngine = app.renderEngine!!
        
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF4FC3F7),
                    secondary = androidx.compose.ui.graphics.Color(0xFF81C784),
                    background = androidx.compose.ui.graphics.Color(0xFF1E1E1E),
                    surface = androidx.compose.ui.graphics.Color(0xFF2D2D2D),
                    onPrimary = androidx.compose.ui.graphics.Color.Black,
                    onSecondary = androidx.compose.ui.graphics.Color.Black,
                    onBackground = androidx.compose.ui.graphics.Color.White,
                    onSurface = androidx.compose.ui.graphics.Color.White,
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EditorLayout(renderEngine = renderEngine)
                }
            }
        }
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
        renderEngine.release()
    }
}
