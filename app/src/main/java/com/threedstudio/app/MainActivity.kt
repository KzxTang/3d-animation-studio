package com.threedstudio.app

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import com.threedstudio.ui.EditorLayout
import com.threedstudio.render.GLRenderEngine
import com.threedstudio.render.SceneObject
import com.threedstudio.render.core.Vec3
import com.threedstudio.render.core.Color as RenderColor
import com.threedstudio.modelio.ModelImportManager
import com.threedstudio.modelio.ModelFormat
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {

    private lateinit var renderEngine: GLRenderEngine
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importModel(uri.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        val app = application as ThreeDStudioApp
        app.initRenderEngine()
        renderEngine = app.renderEngine!!

        // 创建默认可见场景（参考网格）
        createDefaultScene()

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

    private fun createDefaultScene() {
        // 添加一个可见的参考平面（使用四边形作为地面）
        val mesh = com.threedstudio.render.core.GpuMesh.createQuad()
        val ground = SceneObject(
            name = "参考地面",
            position = Vec3(0f, -2f, 0f),
            rotation = com.threedstudio.render.core.Quaternion(0.707f, 0f, 0f, 0.707f),
            scale = Vec3(10f, 1f, 10f),
            materialColor = RenderColor(0.3f, 0.4f, 0.5f, 0.7f),
            gpuMesh = mesh
        )
        renderEngine.addSceneObject(ground)
    }

    private fun importModel(uriString: String) {
        scope.launch {
            try {
                Toast.makeText(this@MainActivity, "正在导入模型...", Toast.LENGTH_SHORT).show()

                // 获取文件路径
                val filePath = uriString.removePrefix("file://")
                
                // 识别格式
                val format = when {
                    filePath.endsWith(".pmx", true) -> ModelFormat.PMX
                    filePath.endsWith(".gltf", true) -> ModelFormat.GLTF
                    filePath.endsWith(".glb", true) -> ModelFormat.GLB
                    filePath.endsWith(".fbx", true) -> ModelFormat.FBX
                    filePath.endsWith(".obj", true) -> ModelFormat.OBJ
                    else -> {
                        Toast.makeText(this@MainActivity, "不支持的格式", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                // 解析模型
                val manager = ModelImportManager()
                val modelData = manager.loadModel(filePath, format)

                // 将模型网格添加到场景
                for ((index, meshData) in modelData.meshes.withIndex()) {
                    val objName = if (modelData.meshes.size == 1) modelData.name else "${modelData.name}_${index}"
                    val sceneObj = SceneObject(
                        name = objName,
                        position = Vec3(0f, 1f, 0f),
                        scale = Vec3(1f, 1f, 1f),
                        materialColor = if (modelData.materialColors.isNotEmpty()) 
                            modelData.materialColors[index.coerceAtMost(modelData.materialColors.size - 1)]
                        else 
                            RenderColor(0.7f, 0.7f, 0.8f, 1f)
                    )
                    
                    // 通过安全队列添加到渲染引擎
                    renderEngine.queueSceneObject(sceneObj, meshData)
                }

                Toast.makeText(
                    this@MainActivity,
                    "导入成功: ${modelData.name} (${modelData.meshes.size} 网格)",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "导入失败: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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
        scope.cancel()
        renderEngine.release()
    }
}
