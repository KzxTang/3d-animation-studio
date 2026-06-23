package com.threedstudio.render

import android.opengl.GLES30
import com.threedstudio.render.core.*

class GLRenderEngine {
    var camera = Camera()
    val lights = mutableListOf<Light>()
    val sceneObjects = mutableListOf<SceneObject>()

    private var shader: Shader? = null
    private var isInitialized = false

    var msaaLevel = 2
    var shadowsEnabled = true
    var shadowMapResolution = 1024
    var ambientColor = Color(0.2f, 0.2f, 0.25f)
    var ambientIntensity = 1.0f

    fun init() {
        if (isInitialized) return
        shader = Shader().also { it.createDefault() }
        if (lights.isEmpty()) {
            lights.add(Light(
                name = "Sun", type = LightType.DIRECTIONAL,
                direction = Vec3(0.5f, -1f, 0.3f).norm(),
                color = Color(1f, 0.98f, 0.95f), intensity = 1.0f
            ))
            lights.add(Light(
                name = "Ambient", type = LightType.AMBIENT,
                color = ambientColor, intensity = ambientIntensity
            ))
        }
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        isInitialized = true
    }

    fun setViewport(width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        camera.setAspectRatio(width, height)
    }

    fun renderFrame() {
        if (!isInitialized) init()
        GLES30.glClearColor(0.12f, 0.12f, 0.14f, 1.0f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val s = shader ?: return
        s.use()
        camera.update()

        s.setUniformMat4("u_viewMatrix", camera.getViewMatrix())
        s.setUniformMat4("u_projectionMatrix", camera.getProjectionMatrix())
        s.setUniform3f("u_cameraPosition", camera.position.x, camera.position.y, camera.position.z)

        s.setUniform("u_lightCount", lights.size.coerceAtMost(8))
        s.setUniform3f("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b)
        s.setUniform("u_ambientIntensity", ambientIntensity)

        for ((i, light) in lights.withIndex()) {
            if (i >= 8) break
            s.setUniform3f("u_lights[$i].position", light.position.x, light.position.y, light.position.z)
            s.setUniform3f("u_lights[$i].direction", light.direction.x, light.direction.y, light.direction.z)
            s.setUniform3f("u_lights[$i].color", light.color.r, light.color.g, light.color.b)
            s.setUniform("u_lights[$i].intensity", light.intensity)
            s.setUniform("u_lights[$i].type", light.type.value)
        }

        for (obj in sceneObjects) {
            if (obj.visible) renderSceneObject(obj, s)
        }
    }

    private fun renderSceneObject(obj: SceneObject, s: Shader) {
        val modelMatrix = Mat4().tr(obj.position).rot(obj.rotation).sc(obj.scale)
        s.setUniformMat4("u_modelMatrix", modelMatrix)

        val normalMatrix = Mat4()
        Mat4.mul(camera.getViewMatrix(), modelMatrix, normalMatrix)
        s.setUniformMat4("u_normalMatrix", normalMatrix)

        val mvpMatrix = Mat4()
        Mat4.mul(camera.getProjectionMatrix(), camera.getViewMatrix(), mvpMatrix)
        Mat4.mul(mvpMatrix, modelMatrix, mvpMatrix)
        s.setUniformMat4("u_mvpMatrix", mvpMatrix)

        s.setUniform4f("u_materialDiffuse", obj.materialColor.r, obj.materialColor.g, obj.materialColor.b, obj.materialColor.a)
        s.setUniform("u_hasDiffuseTexture", obj.hasTexture())
        s.setUniform("u_materialShininess", obj.materialShininess)

        obj.gpuMesh?.bind()
        obj.gpuMesh?.draw()
        obj.gpuMesh?.unbind()
    }

    fun addSceneObject(obj: SceneObject) { sceneObjects.add(obj) }
    fun clearScene() { sceneObjects.clear() }
    fun onResume() {}
    fun onPause() {}

    fun release() {
        shader?.delete(); shader = null
        for (obj in sceneObjects) obj.gpuMesh?.delete()
        sceneObjects.clear()
        isInitialized = false
    }
}

data class SceneObject(
    val name: String = "Object",
    var position: Vec3 = Vec3.ZERO,
    var rotation: Quaternion = Quaternion.IDENTITY,
    var scale: Vec3 = Vec3(1f, 1f, 1f),
    var materialColor: Color = Color.WHITE,
    var materialShininess: Float = 32f,
    var gpuMesh: GpuMesh? = null,
    var visible: Boolean = true
) {
    fun hasTexture(): Boolean = (gpuMesh?.getTextureId() ?: 0) != 0
}
