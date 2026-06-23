package com.threedstudio.render.core

import kotlin.math.*

data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vec3(x / scalar, y / scalar, z / scalar)
    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z
    fun cross(other: Vec3) = Vec3(y * other.z - z * other.y, z * other.x - x * other.z, x * other.y - y * other.x)
    fun length() = sqrt(x * x + y * y + z * z)
    fun normalize(): Vec3 { val len = length(); return if (len > 1e-8f) this / len else ZERO }
    fun lerp(target: Vec3, t: Float) = Vec3(x + (target.x - x) * t, y + (target.y - y) * t, z + (target.z - z) * t)
    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val FORWARD = Vec3(0f, 0f, -1f)
    }
}

data class Color(var r: Float = 1f, var g: Float = 1f, var b: Float = 1f, var a: Float = 1f) {
    companion object {
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
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
        if (cosOmega < 0f) { cosOmega = -cosOmega; t0 = Quaternion(-target.x, -target.y, -target.z, -target.w) }
        val k0: Float; val k1: Float
        if (cosOmega > 0.9999f) { k0 = 1f - t; k1 = t }
        else {
            val sinOmega = sqrt(1f - cosOmega * cosOmega)
            val omega = atan2(sinOmega, cosOmega)
            val oneOverSinOmega = 1f / sinOmega
            k0 = sin((1f - t) * omega) * oneOverSinOmega
            k1 = sin(t * omega) * oneOverSinOmega
        }
        return Quaternion(k0 * x + k1 * t0.x, k0 * y + k1 * t0.y, k0 * z + k1 * t0.z, k0 * w + k1 * t0.w)
    }
    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)
        fun fromEulerAngles(euler: Vec3): Quaternion {
            val cy = cos(euler.z * 0.5f); val sy = sin(euler.z * 0.5f)
            val cp = cos(euler.y * 0.5f); val sp = sin(euler.y * 0.5f)
            val cr = cos(euler.x * 0.5f); val sr = sin(euler.x * 0.5f)
            return Quaternion(
                sr * cp * cy - cr * sp * sy, cr * sp * cy + sr * cp * sy,
                cr * cp * sy - sr * sp * cy, cr * cp * cy + sr * sp * sy
            )
        }
    }
}

class Mat4 {
    val elements = FloatArray(16) { i -> if (i % 5 == 0) 1f else 0f }

    fun translate(v: Vec3): Mat4 {
        val m = copyElements()
        m.elements[12] += v.x; m.elements[13] += v.y; m.elements[14] += v.z
        return m
    }
    fun scale(s: Vec3): Mat4 {
        val m = copyElements()
        m.elements[0] *= s.x; m.elements[5] *= s.y; m.elements[10] *= s.z
        return m
    }
    fun rotate(q: Quaternion): Mat4 {
        val xx = q.x * q.x; val yy = q.y * q.y; val zz = q.z * q.z
        val xy = q.x * q.y; val xz = q.x * q.z; val yz = q.y * q.z
        val wx = q.w * q.x; val wy = q.w * q.y; val wz = q.w * q.z
        val r = Mat4()
        r.elements[0] = 1f - 2f * (yy + zz); r.elements[1] = 2f * (xy + wz);  r.elements[2] = 2f * (xz - wy)
        r.elements[4] = 2f * (xy - wz);   r.elements[5] = 1f - 2f * (xx + zz); r.elements[6] = 2f * (yz + wx)
        r.elements[8] = 2f * (xz + wy);   r.elements[9] = 2f * (yz - wx);   r.elements[10] = 1f - 2f * (xx + yy)
        return multiply(this, r)
    }

    fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): Mat4 {
        val m = Mat4()
        val tanHalfFovy = tan(fovY / 2f)
        m.elements.fill(0f)
        m.elements[0] = 1f / (aspect * tanHalfFovy)
        m.elements[5] = 1f / tanHalfFovy
        m.elements[10] = -(far + near) / (far - near)
        m.elements[11] = -1f
        m.elements[14] = -(2f * far * near) / (far - near)
        return m
    }

    fun lookAt(eye: Vec3, center: Vec3, up: Vec3): Mat4 {
        val f = (center - eye).normalize()
        val s = f.cross(up).normalize()
        val u = s.cross(f)
        val m = Mat4()
        m.elements[0] = s.x; m.elements[4] = s.y; m.elements[8] = s.z; m.elements[12] = -s.dot(eye)
        m.elements[1] = u.x; m.elements[5] = u.y; m.elements[9] = u.z; m.elements[13] = -u.dot(eye)
        m.elements[2] = -f.x; m.elements[6] = -f.y; m.elements[10] = -f.z; m.elements[14] = f.dot(eye)
        m.elements[3] = 0f; m.elements[7] = 0f; m.elements[11] = 0f; m.elements[15] = 1f
        return m
    }

    fun toFloatArray(): FloatArray = elements.copyOf()

    private fun copyElements(): Mat4 {
        val m = Mat4()
        for (i in 0..15) m.elements[i] = elements[i]
        return m
    }

    companion object {
        fun multiply(a: Mat4, b: Mat4): Mat4 {
            val out = Mat4()
            for (i in 0..3) for (j in 0..3) {
                var sum = 0f
                for (k in 0..3) sum += a.elements[i + k * 4] * b.elements[k + j * 4]
                out.elements[i + j * 4] = sum
            }
            return out
        }
        fun multiply(a: Mat4, b: Mat4, out: Mat4) {
            for (i in 0..3) for (j in 0..3) {
                var sum = 0f
                for (k in 0..3) sum += a.elements[i + k * 4] * b.elements[k + j * 4]
                out.elements[i + j * 4] = sum
            }
        }
    }
}
