package com.threedstudio.animation

import com.threedstudio.render.core.Vec3
import com.threedstudio.render.core.Quaternion
import com.threedstudio.render.core.Color
import kotlin.math.*

/**
 * 关键帧插值类型枚举
 */
enum class InterpolationType {
    LINEAR,      // 线性插值
    BEZIER,      // 贝塞尔曲线插值
    STEP,        // 阶跃插值（无过渡）
    EASE_IN,     // 缓入
    EASE_OUT,    // 缓出
    EASE_IN_OUT  // 缓入缓出
}

/**
 * 关键帧数据基类 - 支持多种数值类型
 */
sealed class KeyframeValue {
    data class FloatValue(val value: Float) : KeyframeValue()
    data class Vec3Value(val value: Vec3) : KeyframeValue()
    data class QuatValue(val value: Quaternion) : KeyframeValue()
    data class ColorValue(val value: Color) : KeyframeValue()
}

/**
 * 单个关键帧定义
 */
data class Keyframe(
    val time: Float,             // 时间戳（秒）
    val value: KeyframeValue,    // 关键帧值
    val interpolation: InterpolationType = InterpolationType.LINEAR,
    val bezierIn: Vec3 = Vec3(0.25f, 0.1f, 0.25f),   // 贝塞尔入控制点
    val bezierOut: Vec3 = Vec3(0.75f, 0.9f, 0.75f)    // 贝塞尔出控制点
) {
    companion object {
        const val TIME_PRECISION = 1f / 60f  // 默认60fps精度
    }
}

/**
 * 动画轨道基类
 */
abstract class AnimationTrack(
    val name: String,
    val type: TrackType
) {
    protected val keyframes = mutableListOf<Keyframe>()
    
    fun addKeyframe(keyframe: Keyframe) {
        val insertIndex = keyframes.binarySearch { it.time.compareTo(keyframe.time) }
        if (insertIndex < 0) {
            keyframes.add(-insertIndex - 1, keyframe)
        } else {
            keyframes[insertIndex] = keyframe
        }
    }
    
    fun removeKeyframe(time: Float): Boolean {
        return keyframes.removeAll { abs(it.time - time) < Keyframe.TIME_PRECISION }
    }
    
    fun removeKeyframes(times: List<Float>) {
        keyframes.removeAll { kf -> times.any { abs(kf.time - it) < Keyframe.TIME_PRECISION } }
    }
    
    fun getKeyframeAtTime(time: Float): Keyframe? {
        return keyframes.find { abs(it.time - time) < Keyframe.TIME_PRECISION }
    }
    
    fun getAllKeyframes(): List<Keyframe> = keyframes.toList()
    
    fun getKeyframeCount(): Int = keyframes.size
    
    fun getTimeRange(): Pair<Float, Float> {
        if (keyframes.isEmpty()) return Pair(0f, 0f)
        return Pair(keyframes.first().time, keyframes.last().time)
    }
    
    /**
     * 在指定时间评估轨道值（自动插值）
     */
    abstract fun evaluate(time: Float): KeyframeValue
    
    /**
     * 线性插值实现
     */
    protected fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    
    protected fun lerpVec3(a: Vec3, b: Vec3, t: Float): Vec3 {
        return Vec3(lerp(a.x, b.x, t), lerp(a.y, b.y, t), lerp(a.z, b.z, t))
    }
    
    protected fun lerpColor(a: Color, b: Color, t: Float): Color {
        return Color(
            lerp(a.r, b.r, t), lerp(a.g, b.g, t),
            lerp(a.b, b.b, t), lerp(a.a, b.a, t)
        )
    }
    
    /**
     * 贝塞尔插值
     */
    protected fun bezierInterpolate(a: Float, b: Float, t: Float, cp1: Float, cp2: Float): Float {
        val t2 = t * t
        val t3 = t2 * t
        val mt = 1f - t
        val mt2 = mt * mt
        val mt3 = mt2 * mt
        return mt3 * a + 3f * mt2 * t * cp1 + 3f * mt * t2 * cp2 + t3 * b
    }
    
    /**
     * 缓动函数
     */
    protected fun easeInOut(t: Float): Float {
        return if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
    }
    
    fun clearAllKeyframes() {
        keyframes.clear()
    }
    
    fun copyKeyframes(fromTime: Float, toTime: Float) {
        val kf = getKeyframeAtTime(fromTime) ?: return
        addKeyframe(kf.copy(time = toTime))
    }
}

/**
 * 模型变换轨道 - 位移/旋转/缩放
 */
class TransformTrack(name: String) : AnimationTrack(name, TrackType.TRANSFORM) {
    
    override fun evaluate(time: Float): KeyframeValue {
        if (keyframes.isEmpty()) return KeyframeValue.Vec3Value(Vec3.ZERO)
        if (keyframes.size == 1) return keyframes[0].value
        
        // 查找包围当前时间的两个关键帧
        var leftIdx = 0
        var rightIdx = keyframes.size - 1
        
        for (i in 0 until keyframes.size) {
            if (keyframes[i].time <= time) leftIdx = i
            if (keyframes[i].time >= time) {
                rightIdx = i
                break
            }
        }
        
        val leftKf = keyframes[leftIdx]
        val rightKf = keyframes[rightIdx]
        
        if (leftIdx == rightIdx || rightKf.time - leftKf.time < 1e-6f) {
            return rightKf.value
        }
        
        val t = ((time - leftKf.time) / (rightKf.time - leftKf.time)).coerceIn(0f, 1f)
        
        return when (leftKf.value) {
            is KeyframeValue.Vec3Value -> {
                val a = leftKf.value as KeyframeValue.Vec3Value
                val b = rightKf.value as KeyframeValue.Vec3Value
                val adjustedT = applyInterpolation(t, leftKf.interpolation)
                KeyframeValue.Vec3Value(lerpVec3(a.value, b.value, adjustedT))
            }
            is KeyframeValue.QuatValue -> {
                val a = leftKf.value as KeyframeValue.QuatValue
                val b = rightKf.value as KeyframeValue.QuatValue
                val adjustedT = applyInterpolation(t, leftKf.interpolation)
                KeyframeValue.QuatValue(a.value.slerp(b.value, adjustedT))
            }
            else -> rightKf.value
        }
    }
    
    private fun applyInterpolation(t: Float, type: InterpolationType): Float {
        return when (type) {
            InterpolationType.LINEAR -> t
            InterpolationType.STEP -> if (t < 1f) 0f else 1f
            InterpolationType.EASE_IN -> t * t
            InterpolationType.EASE_OUT -> 1f - (1f - t) * (1f - t)
            InterpolationType.EASE_IN_OUT -> easeInOut(t)
            InterpolationType.BEZIER -> t // 贝塞尔在外部处理
        }
    }
}

/**
 * 表情轨道 - BlendShape权重
 */
class MorphTrack(name: String) : AnimationTrack(name, TrackType.MORPH) {
    
    override fun evaluate(time: Float): KeyframeValue {
        if (keyframes.isEmpty()) return KeyframeValue.FloatValue(0f)
        if (keyframes.size == 1) return keyframes[0].value
        
        var leftIdx = 0
        var rightIdx = keyframes.size - 1
        for (i in 0 until keyframes.size) {
            if (keyframes[i].time <= time) leftIdx = i
            if (keyframes[i].time >= time) {
                rightIdx = i
                break
            }
        }
        
        val leftKf = keyframes[leftIdx]
        val rightKf = keyframes[rightIdx]
        if (leftIdx == rightIdx || rightKf.time - leftKf.time < 1e-6f) return rightKf.value
        
        val t = ((time - leftKf.time) / (rightKf.time - leftKf.time)).coerceIn(0f, 1f)
        val a = leftKf.value as KeyframeValue.FloatValue
        val b = rightKf.value as KeyframeValue.FloatValue
        return KeyframeValue.FloatValue(lerp(a.value, b.value, t))
    }
}

/**
 * 相机轨道
 */
class CameraTrack(name: String) : AnimationTrack(name, TrackType.CAMERA) {
    
    override fun evaluate(time: Float): KeyframeValue {
        if (keyframes.isEmpty()) return KeyframeValue.Vec3Value(Vec3.ZERO)
        if (keyframes.size == 1) return keyframes[0].value
        
        var leftIdx = 0
        var rightIdx = keyframes.size - 1
        for (i in 0 until keyframes.size) {
            if (keyframes[i].time <= time) leftIdx = i
            if (keyframes[i].time >= time) {
                rightIdx = i
                break
            }
        }
        
        val leftKf = keyframes[leftIdx]
        val rightKf = keyframes[rightIdx]
        if (leftIdx == rightIdx || rightKf.time - leftKf.time < 1e-6f) return rightKf.value
        
        val t = ((time - leftKf.time) / (rightKf.time - leftKf.time)).coerceIn(0f, 1f)
        val a = leftKf.value as KeyframeValue.Vec3Value
        val b = rightKf.value as KeyframeValue.Vec3Value
        return KeyframeValue.Vec3Value(lerpVec3(a.value, b.value, t))
    }
}

/**
 * 灯光轨道
 */
class LightTrack(name: String) : AnimationTrack(name, TrackType.LIGHT) {
    
    override fun evaluate(time: Float): KeyframeValue {
        if (keyframes.isEmpty()) return KeyframeValue.ColorValue(Color.WHITE)
        if (keyframes.size == 1) return keyframes[0].value
        
        var leftIdx = 0
        var rightIdx = keyframes.size - 1
        for (i in 0 until keyframes.size) {
            if (keyframes[i].time <= time) leftIdx = i
            if (keyframes[i].time >= time) {
                rightIdx = i
                break
            }
        }
        
        val leftKf = keyframes[leftIdx]
        val rightKf = keyframes[rightIdx]
        if (leftIdx == rightIdx || rightKf.time - leftKf.time < 1e-6f) return rightKf.value
        
        val t = ((time - leftKf.time) / (rightKf.time - leftKf.time)).coerceIn(0f, 1f)
        
        return when (leftKf.value) {
            is KeyframeValue.ColorValue -> {
                val a = leftKf.value as KeyframeValue.ColorValue
                val b = rightKf.value as KeyframeValue.ColorValue
                KeyframeValue.ColorValue(lerpColor(a.value, b.value, t))
            }
            is KeyframeValue.FloatValue -> {
                val a = leftKf.value as KeyframeValue.FloatValue
                val b = rightKf.value as KeyframeValue.FloatValue
                KeyframeValue.FloatValue(lerp(a.value, b.value, t))
            }
            else -> rightKf.value
        }
    }
}

enum class TrackType {
    TRANSFORM, MORPH, CAMERA, LIGHT
}

/**
 * 动画控制主类 - 时间轴与播放管理
 */
class AnimationController {
    private val tracks = mutableMapOf<String, AnimationTrack>()
    private var currentTime = 0f
    private var frameRate = 30f
    private var isPlaying = false
    private var loopEnabled = false
    private var startTime = 0f
    private var endTime = 10f
    
    fun createTrack(name: String, type: TrackType): AnimationTrack {
        val track = when (type) {
            TrackType.TRANSFORM -> TransformTrack(name)
            TrackType.MORPH -> MorphTrack(name)
            TrackType.CAMERA -> CameraTrack(name)
            TrackType.LIGHT -> LightTrack(name)
        }
        tracks[name] = track
        return track
    }
    
    fun removeTrack(name: String) {
        tracks.remove(name)
    }
    
    fun getTrack(name: String): AnimationTrack? = tracks[name]
    
    fun getAllTracks(): List<AnimationTrack> = tracks.values.toList()
    
    fun setCurrentTime(time: Float) {
        currentTime = time.coerceIn(0f, getDuration())
    }
    
    fun getCurrentTime(): Float = currentTime
    
    fun setFrameRate(fps: Float) {
        frameRate = fps.coerceIn(1f, 120f)
    }
    
    fun getFrameRate(): Float = frameRate
    
    fun play() { isPlaying = true }
    fun pause() { isPlaying = false }
    fun stop() { isPlaying = false; currentTime = 0f }
    fun isPlaying(): Boolean = isPlaying
    
    fun setLoop(enabled: Boolean) { loopEnabled = enabled }
    fun isLoopEnabled(): Boolean = loopEnabled
    
    fun setTimeRange(start: Float, end: Float) {
        startTime = start.coerceAtLeast(0f)
        this.endTime = end.coerceAtLeast(startTime + 0.1f)
    }
    
    fun getDuration(): Float = endTime - startTime
    
    /**
     * 更新动画状态（每帧调用）
     */
    fun update(deltaTime: Float) {
        if (!isPlaying) return
        currentTime += deltaTime
        if (currentTime >= endTime) {
            if (loopEnabled) {
                currentTime = startTime
            } else {
                currentTime = endTime
                isPlaying = false
            }
        }
    }
    
    /**
     * 评估所有轨道在当前时间点的值
     */
    fun evaluateAll(): Map<String, KeyframeValue> {
        val result = mutableMapOf<String, KeyframeValue>()
        for ((name, track) in tracks) {
            result[name] = track.evaluate(currentTime)
        }
        return result
    }
    
    /**
     * 批量复制关键帧
     */
    fun copyKeyframes(sourceTime: Float, targetTime: Float) {
        for (track in tracks.values) {
            track.copyKeyframes(sourceTime, targetTime)
        }
    }
    
    /**
     * 批量删除指定时间的关键帧
     */
    fun deleteKeyframesAtTime(time: Float) {
        for (track in tracks.values) {
            track.removeKeyframe(time)
        }
    }
    
    /**
     * 批量移动关键帧
     */
    fun moveKeyframes(fromTime: Float, toTime: Float) {
        for (track in tracks.values) {
            val kf = track.getKeyframeAtTime(fromTime) ?: continue
            track.removeKeyframe(fromTime)
            track.addKeyframe(kf.copy(time = toTime))
        }
    }
    
    fun getTotalKeyframeCount(): Int {
        return tracks.values.sumOf { it.getKeyframeCount() }
    }
    
    fun clearAllTracks() {
        tracks.clear()
    }
}

/**
 * 曲线编辑器数据 - 用于UI可视化
 */
class CurveEditorData(
    val trackName: String,
    val keyframes: List<Keyframe>
) {
    fun getEvaluatedPoints(samples: Int = 100): List<Pair<Float, Float>> {
        if (keyframes.isEmpty()) return emptyList()
        val range = Pair(
            keyframes.minOf { it.time },
            keyframes.maxOf { it.time }
        )
        val points = mutableListOf<Pair<Float, Float>>()
        val step = (range.second - range.first) / samples.toFloat()
        for (i in 0..samples) {
            val t = range.first + i * step
            points.add(Pair(t, 0f))
        }
        return points
    }
}
