package com.threedstudio.render

import com.threedstudio.render.core.Vec3
import com.threedstudio.render.core.Color
import com.threedstudio.render.core.Mat4
import com.threedstudio.render.core.Quaternion
import kotlin.math.*

/**
 * 灯光类型枚举
 */
enum class LightType(val value: Int) {
    AMBIENT(0),
    DIRECTIONAL(1),
    POINT(2),
    SPOT(3)
}

/**
 * 灯光数据类 - 支持所有四种灯光类型
 */
data class Light(
    val name: String = "Light",
    var type: LightType = LightType.DIRECTIONAL,
    var position: Vec3 = Vec3(0f, 5f, 0f),
    var direction: Vec3 = Vec3(0f, -1f, 0f),
    var color: Color = Color.WHITE,
    var intensity: Float = 1f,
    var angle: Float = 45f,        // 聚光灯角度（度）
    var range: Float = 10f,        // 点光源/聚光灯范围
    var castsShadows: Boolean = false,
    var shadowMapSize: Int = 1024
) {
    fun setFrom(other: Light) {
        type = other.type
        position = other.position.copy()
        direction = other.direction.copy()
        color = other.color.copy()
        intensity = other.intensity
        angle = other.angle
        range = other.range
    }
    
    fun toShaderArray(): FloatArray {
        return floatArrayOf(
            type.value.toFloat(),
            position.x, position.y, position.z,
            direction.x, direction.y, direction.z,
            color.r, color.g, color.b,
            intensity,
            angle * PI.toFloat() / 180f,
            range
        )
    }
}

/**
 * 相机模式枚举
 */
enum class CameraMode {
    FREE_ROAM,     // 自由漫游
    ORBIT          // 轨道环绕
}

/**
 * 相机系统 - 支持自由漫游和轨道环绕双模式
 */
class Camera {
    var mode: CameraMode = CameraMode.ORBIT
    
    // 位置与朝向
    var position: Vec3 = Vec3(0f, 3f, 10f)
    var target: Vec3 = Vec3(0f, 1f, 0f)
    var up: Vec3 = Vec3.UP
    
    // 轨道参数
    var orbitDistance: Float = 10f
    var orbitYaw: Float = 0f       // 水平旋转角（弧度）
    var orbitPitch: Float = 0.5f   // 垂直旋转角（弧度）
    var orbitMinDistance: Float = 1f
    var orbitMaxDistance: Float = 100f
    var orbitSensitivity: Float = 0.005f
    
    // 投影参数
    var fov: Float = 45f           // 视场角（度）
    var near: Float = 0.1f
    var far: Float = 1000f
    var aspect: Float = 16f / 9f
    
    // 景深参数
    var depthOfFieldEnabled: Boolean = false
    var focalDistance: Float = 5f
    var apertureSize: Float = 0.5f
    
    // 自由漫游参数
    var moveSpeed: Float = 5f
    var rotateSpeed: Float = 2f
    var freeYaw: Float = 0f
    var freePitch: Float = 0f
    
    private var viewMatrix = Mat4()
    private var projectionMatrix = Mat4()
    private var needsUpdate = true
    
    fun setMode(mode: CameraMode) {
        if (this.mode != mode) {
            this.mode = mode
            needsUpdate = true
        }
    }
    
    fun setAspectRatio(width: Int, height: Int) {
        aspect = if (height > 0) width.toFloat() / height.toFloat() else 16f / 9f
        needsUpdate = true
    }
    
    fun orbitRotate(deltaYaw: Float, deltaPitch: Float) {
        if (mode != CameraMode.ORBIT) return
        orbitYaw += deltaYaw * orbitSensitivity
        orbitPitch += deltaPitch * orbitSensitivity
        orbitPitch = orbitPitch.coerceIn(-PI.toFloat() / 2f + 0.1f, PI.toFloat() / 2f - 0.1f)
        needsUpdate = true
    }
    
    fun orbitZoom(delta: Float) {
        if (mode != CameraMode.ORBIT) return
        orbitDistance = (orbitDistance - delta * 0.1f).coerceIn(orbitMinDistance, orbitMaxDistance)
        needsUpdate = true
    }
    
    fun freeMove(forward: Float, right: Float, upAmount: Float) {
        if (mode != CameraMode.FREE_ROAM) return
        val forwardDir = getForward()
        val rightDir = getRight()
        position += forwardDir * forward * moveSpeed
        position += rightDir * right * moveSpeed
        position += Vec3.UP * upAmount * moveSpeed
        target = position + forwardDir
        needsUpdate = true
    }
    
    fun freeRotate(deltaYaw: Float, deltaPitch: Float) {
        if (mode != CameraMode.FREE_ROAM) return
        freeYaw += deltaYaw * 0.01f
        freePitch = (freePitch + deltaPitch * 0.01f).coerceIn(-PI.toFloat() / 2f, PI.toFloat() / 2f)
        needsUpdate = true
    }
    
    fun getForward(): Vec3 {
        val forward = Vec3(
            -sin(freeYaw) * cos(freePitch),
            sin(freePitch),
            -cos(freeYaw) * cos(freePitch)
        ).normalize()
        return forward
    }
    
    fun getRight(): Vec3 {
        return getForward().cross(Vec3.UP).normalize()
    }
    
    fun update() {
        if (mode == CameraMode.ORBIT) {
            val x = orbitDistance * cos(orbitPitch) * sin(orbitYaw)
            val y = orbitDistance * sin(orbitPitch)
            val z = orbitDistance * cos(orbitPitch) * cos(orbitYaw)
            position = Vec3(target.x + x, target.y + y, target.z + z)
        }
        
        viewMatrix.lookAt(position, target, up)
        projectionMatrix.perspective(
            fov * PI.toFloat() / 180f,
            aspect, near, far
        )
        needsUpdate = false
    }
    
    fun getViewMatrix(): Mat4 {
        if (needsUpdate) update()
        return viewMatrix
    }
    
    fun getProjectionMatrix(): Mat4 {
        if (needsUpdate) update()
        return projectionMatrix
    }
    
    fun reset() {
        position = Vec3(0f, 3f, 10f)
        target = Vec3(0f, 1f, 0f)
        orbitDistance = 10f
        orbitYaw = 0f
        orbitPitch = 0.5f
        freeYaw = 0f
        freePitch = 0f
        needsUpdate = true
    }
}
