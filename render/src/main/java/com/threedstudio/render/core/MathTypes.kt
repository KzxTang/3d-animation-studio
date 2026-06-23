package com.threedstudio.render.core

import kotlin.math.*

data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)
    
    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )
    
    fun length(): Float = sqrt(x * x + y * y + z * z)
    
    fun normalize(): Vec3 {
        val len = length()
        return if (len > 1e-8f) this / len else Vec3()
    }
    
    fun lerp(target: Vec3, t: Float): Vec3 = Vec3(
        x + (target.x - x) * t,
        y + (target.y - y) * t,
        z + (target.z - z) * t
    )
    
    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val FORWARD = Vec3(0f, 0f, -1f)
    }
}

data class Vec4(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 0f) {
    fun toVec3(): Vec3 = Vec3(x, y, z)
    
    companion object {
        val ZERO = Vec4(0f, 0f, 0f, 0f)
    }
}

data class Color(var r: Float = 1f, var g: Float = 1f, var b: Float = 1f, var a: Float = 1f) {
    
    fun toFloatArray(): FloatArray = floatArrayOf(r, g, b, a)
    
    companion object {
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
        val RED = Color(1f, 0f, 0f, 1f)
        val GREEN = Color(0f, 1f, 0f, 1f)
        val BLUE = Color(0f, 0f, 1f, 1f)
    }
}

data class Quaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    
    fun normalize(): Quaternion {
        val mag = sqrt(w * w + x * x + y * y + z * z)
        return if (mag > 1e-8f) Quaternion(x / mag, y / mag, z / mag, w / mag) else IDENTITY
    }
    
    fun slerp(target: Quaternion, t: Float): Quaternion {
        var cosOmega = w * target.w + x * target.x + y * target.y + z * target.z
        var t0 = target
        if (cosOmega < 0f) {
            cosOmega = -cosOmega
            t0 = Quaternion(-target.x, -target.y, -target.z, -target.w)
        }
        val k0: Float
        val k1: Float
        if (cosOmega > 0.9999f) {
            k0 = 1f - t
            k1 = t
        } else {
            val sinOmega = sqrt(1f - cosOmega * cosOmega)
            val omega = atan2(sinOmega, cosOmega)
            val oneOverSinOmega = 1f / sinOmega
            k0 = sin((1f - t) * omega) * oneOverSinOmega
            k1 = sin(t * omega) * oneOverSinOmega
        }
        return Quaternion(
            k0 * x + k1 * t0.x,
            k0 * y + k1 * t0.y,
            k0 * z + k1 * t0.z,
            k0 * w + k1 * t0.w
        )
    }
    
    fun toEulerAngles(): Vec3 {
        val sinrCosp = 2f * (w * x + y * z)
        val cosrCosp = 1f - 2f * (x * x + y * y)
        val roll = atan2(sinrCosp, cosrCosp)
        
        val sinp = 2f * (w * y - z * x)
        val pitch = if (abs(sinp) >= 1f) Math.copySign(PI.toFloat() / 2f, sinp) else asin(sinp)
        
        val sinyCosp = 2f * (w * z + x * y)
        val cosyCosp = 1f - 2f * (y * y + z * z)
        val yaw = atan2(sinyCosp, cosyCosp)
        
        return Vec3(roll, pitch, yaw)
    }
    
    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
        
        fun fromEulerAngles(euler: Vec3): Quaternion {
            val cy = cos(euler.z * 0.5f)
            val sy = sin(euler.z * 0.5f)
            val cp = cos(euler.y * 0.5f)
            val sp = sin(euler.y * 0.5f)
            val cr = cos(euler.x * 0.5f)
            val sr = sin(euler.x * 0.5f)
            
            return Quaternion(
                sr * cp * cy - cr * sp * sy,
                cr * sp * cy + sr * cp * sy,
                cr * cp * sy - sr * sp * cy,
                cr * cp * cy + sr * sp * sy
            )
        }
    }
}

data class Mat4 {
    val elements = FloatArray(16)
    
    init {
        setIdentity()
    }
    
    fun setIdentity(): Mat4 {
        elements.fill(0f)
        elements[0] = 1f
        elements[5] = 1f
        elements[10] = 1f
        elements[15] = 1f
        return this
    }
    
    fun translate(translation: Vec3): Mat4 {
        elements[12] += translation.x
        elements[13] += translation.y
        elements[14] += translation.z
        return this
    }
    
    fun scale(scale: Vec3): Mat4 {
        elements[0] *= scale.x
        elements[5] *= scale.y
        elements[10] *= scale.z
        return this
    }
    
    fun rotate(rotation: Quaternion): Mat4 {
        val x = rotation.x
        val y = rotation.y
        val z = rotation.z
        val w = rotation.w
        
        val xx = x * x; val yy = y * y; val zz = z * z
        val xy = x * y; val xz = x * z; val yz = y * z
        val wx = w * x; val wy = w * y; val wz = w * z
        
        val rotMat = Mat4()
        rotMat.elements[0] = 1f - 2f * (yy + zz)
        rotMat.elements[1] = 2f * (xy + wz)
        rotMat.elements[2] = 2f * (xz - wy)
        rotMat.elements[4] = 2f * (xy - wz)
        rotMat.elements[5] = 1f - 2f * (xx + zz)
        rotMat.elements[6] = 2f * (yz + wx)
        rotMat.elements[8] = 2f * (xz + wy)
        rotMat.elements[9] = 2f * (yz - wx)
        rotMat.elements[10] = 1f - 2f * (xx + yy)
        
        val result = Mat4()
        multiply(this, rotMat, result)
        elements.copyFrom(result.elements)
        return this
    }
    
    fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): Mat4 {
        val tanHalfFovy = tan(fovY / 2f)
        elements.fill(0f)
        elements[0] = 1f / (aspect * tanHalfFovy)
        elements[5] = 1f / tanHalfFovy
        elements[10] = -(far + near) / (far - near)
        elements[11] = -1f
        elements[14] = -(2f * far * near) / (far - near)
        return this
    }
    
    fun lookAt(eye: Vec3, center: Vec3, up: Vec3): Mat4 {
        val f = (center - eye).normalize()
        val s = f.cross(up).normalize()
        val u = s.cross(f)
        
        elements[0] = s.x; elements[4] = s.y; elements[8] = s.z; elements[12] = -s.dot(eye)
        elements[1] = u.x; elements[5] = u.y; elements[9] = u.z; elements[13] = -u.dot(eye)
        elements[2] = -f.x; elements[6] = -f.y; elements[10] = -f.z; elements[14] = f.dot(eye)
        elements[3] = 0f; elements[7] = 0f; elements[11] = 0f; elements[15] = 1f
        return this
    }
    
    fun toFloatArray(): FloatArray = elements.copyOf()
    
    companion object {
        fun multiply(a: Mat4, b: Mat4, out: Mat4) {
            for (i in 0..3) {
                for (j in 0..3) {
                    var sum = 0f
                    for (k in 0..3) {
                        sum += a.elements[i + k * 4] * b.elements[k + j * 4]
                    }
                    out.elements[i + j * 4] = sum
                }
            }
        }
    }
}

private fun FloatArray.copyFrom(source: FloatArray) {
    for (i in indices) {
        this[i] = source[i]
    }
}
