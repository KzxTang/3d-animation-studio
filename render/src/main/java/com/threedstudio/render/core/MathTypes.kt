package com.threedstudio.render.core

import kotlin.math.*

class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    operator fun div(s: Float) = Vec3(x / s, y / s, z / s)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun len() = sqrt(x * x + y * y + z * z)
    fun norm(): Vec3 { val l = len(); return if (l > 1e-8f) this / l else Vec3(0f, 0f, 0f) }
    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
        val UP = Vec3(0f, 1f, 0f)
        val FORWARD = Vec3(0f, 0f, -1f)
    }
}

class Color(var r: Float = 1f, var g: Float = 1f, var b: Float = 1f, var a: Float = 1f) {
    companion object {
        val WHITE = Color(1f, 1f, 1f, 1f)
        val BLACK = Color(0f, 0f, 0f, 1f)
    }
}

class Quaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun slerp(target: Quaternion, t: Float): Quaternion {
        var cosO = w * target.w + x * target.x + y * target.y + z * target.z
        var t0 = target
        if (cosO < 0f) { cosO = -cosO; t0 = Quaternion(-target.x, -target.y, -target.z, -target.w) }
        val k0: Float; val k1: Float
        if (cosO > 0.9999f) { k0 = 1f - t; k1 = t }
        else {
            val sinO = sqrt(1f - cosO * cosO); val om = atan2(sinO, cosO); val inv = 1f / sinO
            k0 = sin((1f - t) * om) * inv; k1 = sin(t * om) * inv
        }
        return Quaternion(k0 * x + k1 * t0.x, k0 * y + k1 * t0.y, k0 * z + k1 * t0.z, k0 * w + k1 * t0.w)
    }
    companion object { val IDENTITY = Quaternion() }
}

class Mat4 {
    val m = FloatArray(16) { i -> if (i % 5 == 0) 1f else 0f }

    fun tr(v: Vec3) = clone().also { it.m[12] += v.x; it.m[13] += v.y; it.m[14] += v.z }
    fun sc(s: Vec3) = clone().also { it.m[0] *= s.x; it.m[5] *= s.y; it.m[10] *= s.z }
    fun rot(q: Quaternion): Mat4 {
        val xx = q.x * q.x; val yy = q.y * q.y; val zz = q.z * q.z
        val xy = q.x * q.y; val xz = q.x * q.z; val yz = q.y * q.z
        val wx = q.w * q.x; val wy = q.w * q.y; val wz = q.w * q.z
        val r = Mat4()
        r.m[0] = 1f - 2f * (yy + zz); r.m[1] = 2f * (xy + wz);  r.m[2] = 2f * (xz - wy)
        r.m[4] = 2f * (xy - wz);   r.m[5] = 1f - 2f * (xx + zz); r.m[6] = 2f * (yz + wx)
        r.m[8] = 2f * (xz + wy);   r.m[9] = 2f * (yz - wx);   r.m[10] = 1f - 2f * (xx + yy)
        return Companion.mul(this, r)
    }
    fun persp(fovY: Float, aspect: Float, near: Float, far: Float): Mat4 {
        val t = tan(fovY / 2f); val o = Mat4()
        o.m.fill(0f); o.m[0] = 1f / (aspect * t); o.m[5] = 1f / t
        o.m[10] = -(far + near) / (far - near); o.m[11] = -1f; o.m[14] = -(2f * far * near) / (far - near)
        return o
    }
    fun lookAt(eye: Vec3, center: Vec3, up: Vec3): Mat4 {
        val f = (center - eye).norm(); val s = f.cross(up).norm(); val u = s.cross(f)
        val o = Mat4()
        o.m[0]=s.x; o.m[4]=s.y; o.m[8]=s.z; o.m[12]=-s.dot(eye)
        o.m[1]=u.x; o.m[5]=u.y; o.m[9]=u.z; o.m[13]=-u.dot(eye)
        o.m[2]=-f.x;o.m[6]=-f.y;o.m[10]=-f.z;o.m[14]=f.dot(eye)
        o.m[3]=0f;o.m[7]=0f;o.m[11]=0f;o.m[15]=1f
        return o
    }
    fun arr(): FloatArray = m.copyOf()
    private fun clone(): Mat4 { val c = Mat4(); for (i in 0..15) c.m[i] = m[i]; return c }
    companion object {
        fun mul(a: Mat4, b: Mat4): Mat4 {
            val o = Mat4(); for (i in 0..3) for (j in 0..3) { var s = 0f; for (k in 0..3) s += a.m[i + k * 4] * b.m[k + j * 4]; o.m[i + j * 4] = s }; return o
        }
        fun mul(a: Mat4, b: Mat4, out: Mat4) { for (i in 0..3) for (j in 0..3) { var s = 0f; for (k in 0..3) s += a.m[i + k * 4] * b.m[k + j * 4]; out.m[i + j * 4] = s } }
    }
}
