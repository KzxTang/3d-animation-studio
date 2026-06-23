package com.threedstudio.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import android.widget.Toast
import com.threedstudio.ui.EditorLayout
import com.threedstudio.render.GLRenderEngine

class MainActivity : ComponentActivity() {

    private lateinit var renderEngine: GLRenderEngine

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(this, "\u00a0\u00a0\u5df2\u9009\u62e9\u6587\u4ef6: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 锁定横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

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
                    EditorLayout(
                        renderEngine = renderEngine,
                        onImportFile = {
                            filePickerLauncher.launch(arrayOf(
                                "application/octet-stream",
                                "model/gltf-binary",
                                "model/obj",
                                "application/x-fbx"
                            ))
                        }
                    )
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
