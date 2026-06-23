package com.threedstudio.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.threedstudio.render.core.*
import kotlin.math.*

/**
 * GL渲染引擎 - OpenGL ES 3.0渲染管线核心
 * 负责场景管理、灯光、相机、模型渲染
 */
class GLRenderEngine {
    
    var camera: Camera = Camera()
    val lights = mutableListOf<Light>()
    val sceneObjects = mutableListOf<SceneObject>()
    
    private var shader: Shader? = null
    private var defaultShader: Shader? = null
    private var isInitialized = false
    private var viewportWidth = 1920
    private var viewportHeight = 1080
    
    // 性能选项
    var msaaLevel = 2
    var shadowsEnabled = true
    var shadowMapResolution = 1024
    var lodEnabled = true
    var useInstancedRendering = false
    
    // 环境光
    var ambientColor = Color(0.2f, 0.2f, 0.25f)
    var ambientIntensity = 1.0f
    
    fun init() {
        if (isInitialized) return
        
        // 初始化默认着色器
        defaultShader = Shader()
        if (defaultShader!!.createDefaultShader()) {
            shader = defaultShader
        }
        
        // 添加默认灯光
        if (lights.isEmpty()) {
            lights.add(Light(
                name = "Sun",
                type = LightType.DIRECTIONAL,
                direction = Vec3(0.5f, -1f, 0.3f).normalize(),
                color = Color(1f, 0.98f, 0.95f),
                intensity = 1.0f
            ))
            lights.add(Light(
                name = "Ambient",
                type = LightType.AMBIENT,
                color = ambientColor,
                intensity = ambientIntensity
            ))
        }
        
        // OpenGL全局状态
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        
        isInitialized = true
    }
    
    fun setViewport(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        GLES30.glViewport(0, 0, width, height)
        camera.setAspectRatio(width, height)
    }
    
    fun renderFrame() {
        if (!isInitialized) init()
        
        // 清屏
        GLES30.glClearColor(0.12f, 0.12f, 0.14f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        
        val shaderProgram = shader ?: return
        shaderProgram.use()
        
        // 设置相机矩阵
        camera.update()
        shaderProgram.setUniformMat4("u_viewMatrix", camera.getViewMatrix())
        shaderProgram.setUniformMat4("u_projectionMatrix", camera.getProjectionMatrix())
        shaderProgram.setUniform3f("u_cameraPosition", 
            camera.position.x, camera.position.y, camera.position.z)
        
        // 设置灯光
        shaderProgram.setUniform("u_lightCount", lights.size.coerceAtMost(8))
        shaderProgram.setUniform3f("u_ambientColor", 
            ambientColor.r, ambientColor.g, ambientColor.b)
        shaderProgram.setUniform("u_ambientIntensity", ambientIntensity)
        
        for ((i, light) in lights.withIndex()) {
            if (i >= 8) break
            shaderProgram.setUniform3f("u_lights[$i].position", 
                light.position.x, light.position.y, light.position.z)
            shaderProgram.setUniform3f("u_lights[$i].direction",
                light.direction.x, light.direction.y, light.direction.z)
            shaderProgram.setUniform3f("u_lights[$i].color",
                light.color.r, light.color.g, light.color.b)
            shaderProgram.setUniform("u_lights[$i].intensity", light.intensity)
            shaderProgram.setUniform("u_lights[$i].type", light.type.value)
        }
        
        // 渲染所有场景对象
        for (obj in sceneObjects) {
            if (!obj.visible) continue
            renderSceneObject(obj, shaderProgram)
        }
    }
    
    private fun renderSceneObject(obj: SceneObject, shader: Shader) {
        // 设置模型矩阵
        val modelMatrix = Mat4()
        modelMatrix.translate(obj.position)
        modelMatrix.rotate(obj.rotation)
        modelMatrix.scale(obj.scale)
        shader.setUniformMat4("u_modelMatrix", modelMatrix)
        
        // 设置法线矩阵
        val normalMatrix = Mat4()
        Mat4.multiply(camera.getViewMatrix(), modelMatrix, normalMatrix)
        shader.setUniformMat4("u_normalMatrix", normalMatrix)
        
        // 设置MVP矩阵
        val mvpMatrix = Mat4()
        Mat4.multiply(camera.getProjectionMatrix(), camera.getViewMatrix(), mvpMatrix)
        Mat4.multiply(mvpMatrix, modelMatrix, mvpMatrix)
        shader.setUniformMat4("u_mvpMatrix", mvpMatrix)
        
        // 设置材质
        shader.setUniform4f("u_materialDiffuse", 
            obj.materialColor.r, obj.materialColor.g, 
            obj.materialColor.b, obj.materialColor.a)
        shader.setUniform("u_hasDiffuseTexture", obj.hasTexture())
        shader.setUniform("u_materialShininess", obj.materialShininess)
        
        // 绑定并绘制网格
        obj.gpuMesh?.bind()
        obj.gpuMesh?.draw()
        obj.gpuMesh?.unbind()
    }
    
    fun handleTouchEvent(event: MotionEvent): Boolean {
        // 触控交互逻辑（在GLSurfaceView中处理）
        return true
    }
    
    fun addSceneObject(obj: SceneObject) {
        sceneObjects.add(obj)
    }
    
    fun removeSceneObject(obj: SceneObject) {
        sceneObjects.remove(obj)
    }
    
    fun clearScene() {
        sceneObjects.clear()
    }
    
    fun onResume() {
        // GLSurfaceView会自动处理
    }
    
    fun onPause() {
        // GLSurfaceView会自动处理
    }
    
    fun release() {
        shader?.delete()
        shader = null
        defaultShader?.delete()
        defaultShader = null
        for (obj in sceneObjects) {
            obj.gpuMesh?.delete()
        }
        sceneObjects.clear()
        isInitialized = false
    }
}

/**
 * 场景对象数据类
 */
data class SceneObject(
    val name: String = "Object",
    var position: Vec3 = Vec3.ZERO,
    var rotation: Quaternion = Quaternion.IDENTITY,
    var scale: Vec3 = Vec3(1f, 1f, 1f),
    var materialColor: Color = Color.WHITE,
    var materialShininess: Float = 32f,
    var gpuMesh: GpuMesh? = null,
    var visible: Boolean = true,
    var selectable: Boolean = true
) {
    fun hasTexture(): Boolean = gpuMesh?.getTextureId() ?: 0 != 0
}
