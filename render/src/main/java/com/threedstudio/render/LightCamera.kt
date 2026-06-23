package com.threedstudio.render

import com.threedstudio.render.core.Vec3
import com.threedstudio.render.core.Color
import com.threedstudio.render.core.Mat4
import com.threedstudio.render.core.Quaternion
import kotlin.math.*

enum class LightType(val value: Int) {
    AMBIENT(0), DIRECTIONAL(1), POINT(2), SPOT(3)
}

data class Light(
    val name: String = "Light",
    var type: LightType = LightType.DIRECTIONAL,
    var position: Vec3 = Vec3(0f, 5f, 0f),
    var direction: Vec3 = Vec3(0f, -1f, 0f),
    var color: Color = Color.WHITE,
    var intensity: Float = 1f,
    var angle: Float = 45f,
    var range: Float = 10f,
    var castsShadows: Boolean = false,
    var shadowMapSize: Int = 1024
)

enum class CameraMode { FREE_ROAM, ORBIT }

class Camera {
    var mode: CameraMode = CameraMode.ORBIT
    var position: Vec3 = Vec3(0f, 3f, 10f)
    var target: Vec3 = Vec3(0f, 1f, 0f)
    var up: Vec3 = Vec3.UP
    var orbitDistance: Float = 10f
    var orbitYaw: Float = 0f
    var orbitPitch: Float = 0.5f
    var orbitMinDistance: Float = 1f
    var orbitMaxDistance: Float = 100f
    var orbitSensitivity: Float = 0.005f
    var fov: Float = 45f
    var near: Float = 0.1f
    var far: Float = 1000f
    var aspect: Float = 16f / 9f
    var depthOfFieldEnabled: Boolean = false
    var focalDistance: Float = 5f
    var apertureSize: Float = 0.5f
    var moveSpeed: Float = 5f
    var rotateSpeed: Float = 2f
    var freeYaw: Float = 0f
    var freePitch: Float = 0f

    private var viewMatrix = Mat4()
    private var projectionMatrix = Mat4()
    private var needsUpdate = true

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
        position = Vec3(position.x + forwardDir.x * forward * moveSpeed,
                        position.y + forwardDir.y * forward * moveSpeed,
                        position.z + forwardDir.z * forward * moveSpeed)
        position = Vec3(position.x + rightDir.x * right * moveSpeed,
                        position.y + rightDir.y * right * moveSpeed,
                        position.z + rightDir.z * right * moveSpeed)
        position = Vec3(position.x + upAmount * moveSpeed,
                        position.y + upAmount * moveSpeed,
                        position.z + upAmount * moveSpeed)
        target = Vec3(position.x + forwardDir.x, position.y + forwardDir.y, position.z + forwardDir.z)
        needsUpdate = true
    }

    fun freeRotate(deltaYaw: Float, deltaPitch: Float) {
        if (mode != CameraMode.FREE_ROAM) return
        freeYaw += deltaYaw * 0.01f
        freePitch = (freePitch + deltaPitch * 0.01f).coerceIn(-PI.toFloat() / 2f, PI.toFloat() / 2f)
        needsUpdate = true
    }

    fun getForward(): Vec3 {
        val x = -sin(freeYaw) * cos(freePitch)
        val y = sin(freePitch)
        val z = -cos(freeYaw) * cos(freePitch)
        val len = sqrt(x * x + y * y + z * z)
        return if (len > 1e-8f) Vec3(x / len, y / len, z / len) else Vec3.FORWARD
    }

    fun getRight(): Vec3 {
        val fwd = getForward()
        val cx = fwd.y * Vec3.UP.z - fwd.z * Vec3.UP.y
        val cy = fwd.z * Vec3.UP.x - fwd.x * Vec3.UP.z
        val cz = fwd.x * Vec3.UP.y - fwd.y * Vec3.UP.x
        val len = sqrt(cx * cx + cy * cy + cz * cz)
        return if (len > 1e-8f) Vec3(cx / len, cy / len, cz / len) else Vec3(1f, 0f, 0f)
    }

    fun update() {
        if (mode == CameraMode.ORBIT) {
            val px = target.x + orbitDistance * cos(orbitPitch) * sin(orbitYaw)
            val py = target.y + orbitDistance * sin(orbitPitch)
            val pz = target.z + orbitDistance * cos(orbitPitch) * cos(orbitYaw)
            position = Vec3(px, py, pz)
        }
        viewMatrix = Mat4().lookAt(position, target, up)
        projectionMatrix = Mat4().perspective(fov * PI.toFloat() / 180f, aspect, near, far)
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
}
