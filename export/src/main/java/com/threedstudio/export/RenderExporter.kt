package com.threedstudio.export

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES30
import android.view.Surface
import com.threedstudio.render.GLRenderEngine
import kotlinx.coroutines.*
import java.io.File

/**
 * 视频渲染导出引擎
 * 使用MediaCodec + MediaMuxer硬件编码输出MP4/MOV
 * 支持H.264/H.265编码，可调分辨率、帧率、采样质量
 */
class RenderExporter(
    private val renderEngine: GLRenderEngine
) {
    private var exportJob: Job? = null
    
    data class ExportSettings(
        var outputPath: String = "",
        var format: ExportFormat = ExportFormat.MP4,
        var width: Int = 1920,
        var height: Int = 1080,
        var frameRate: Int = 30,
        var bitRate: Int = 8000000,
        var sampleQuality: SampleQuality = SampleQuality.MEDIUM,
        var durationSeconds: Float = 10f,
        var useHardwareEncoder: Boolean = true
    )
    
    enum class ExportFormat {
        MP4, MOV
    }
    
    enum class SampleQuality(val msaaLevel: Int) {
        LOW(1),
        MEDIUM(2),
        HIGH(4),
        ULTRA(8)
    }
    
    private var currentSettings = ExportSettings()
    private var isExporting = false
    private var exportProgress = 0f
    
    fun configure(settings: ExportSettings.() -> Unit) {
        currentSettings.apply(settings)
    }
    
    suspend fun startExport(
        onProgress: (Float) -> Unit = {},
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (isExporting) return
        isExporting = true
        exportProgress = 0f
        
        exportJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = currentSettings
                val totalFrames = (settings.durationSeconds * settings.frameRate).toInt()
                
                // 配置MediaCodec编码器
                val mimeType = when (settings.format) {
                    ExportFormat.MP4 -> MediaFormat.MIME_TYPE_VIDEO_AVC // H.264
                    ExportFormat.MOV -> MediaFormat.MIME_TYPE_VIDEO_HEVC // H.265
                }
                
                val mediaFormat = MediaFormat.createVideoFormat(mimeType, settings.width, settings.height).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, settings.bitRate)
                    setInteger(MediaFormat.KEY_FRAME_RATE, settings.frameRate)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, 
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                }
                
                val codec = if (settings.useHardwareEncoder) {
                    MediaCodec.createEncoderByType(mimeType)
                } else {
                    MediaCodec.createByCodecName("OMX.google.h264.encoder")
                }
                
                codec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                val inputSurface = codec.createInputSurface()
                codec.start()
                
                // 配置MediaMuxer
                val outputFile = File(settings.outputPath)
                outputFile.parentFile?.mkdirs()
                
                val muxerFormat = when (settings.format) {
                    ExportFormat.MP4 -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                    ExportFormat.MOV -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                }
                val muxer = MediaMuxer(settings.outputPath, muxerFormat)
                
                // 创建离屏渲染Surface
                val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                val version = IntArray(2)
                EGL14.eglInitialize(eglDisplay, version, 0, version, 1)
                
                val attribList = intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_DEPTH_SIZE, 16,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
                )
                
                val eglConfigs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                EGL14.eglChooseConfig(eglDisplay, attribList, 0, eglConfigs, 0, 1, numConfigs, 0)
                
                val eglContext = EGL14.eglCreateContext(eglDisplay, eglConfigs[0], 
                    EGL14.EGL_NO_CONTEXT, intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0)
                
                val surfaceAttribs = intArrayOf(
                    EGL14.EGL_WIDTH, settings.width,
                    EGL14.EGL_HEIGHT, settings.height,
                    EGL14.EGL_NONE
                )
                val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfigs[0], surfaceAttribs, 0)
                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                
                // 设置渲染视口
                renderEngine.setViewport(settings.width, settings.height)
                
                var videoTrackIndex = -1
                val bufferInfo = MediaCodec.BufferInfo()
                var frameIndex = 0
                
                // 逐帧渲染
                while (frameIndex < totalFrames && isActive) {
                    val frameTime = frameIndex.toFloat() / settings.frameRate
                    
                    // 渲染当前帧
                    renderEngine.renderFrame()
                    
                    // 交换缓冲区（如果有双缓冲）
                    EGL14.eglSwapBuffers(eglDisplay, eglSurface)
                    
                    // 从编码器获取输出
                    val outputBufferId = codec.dequeueOutputBuffer(bufferInfo, 10000)
                    when {
                        outputBufferId >= 0 -> {
                            val outputBuffer = codec.getOutputBuffer(outputBufferId)
                            if (outputBuffer != null && bufferInfo.size > 0) {
                                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                    codec.releaseOutputBuffer(outputBufferId, false)
                                } else {
                                    if (videoTrackIndex == -1) {
                                        videoTrackIndex = muxer.addTrack(codec.outputFormat)
                                        muxer.start()
                                    }
                                    outputBuffer.position(bufferInfo.offset)
                                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                    muxer.writeSampleData(videoTrackIndex, outputBuffer, bufferInfo)
                                    codec.releaseOutputBuffer(outputBufferId, false)
                                }
                            }
                        }
                        outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            // 格式变化处理
                        }
                    }
                    
                    // 更新进度
                    frameIndex++
                    exportProgress = frameIndex.toFloat() / totalFrames
                    withContext(Dispatchers.Main) {
                        onProgress(exportProgress)
                    }
                }
                
                // 清理资源
                codec.stop()
                codec.release()
                muxer.stop()
                muxer.release()
                
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglTerminate(eglDisplay)
                
                withContext(Dispatchers.Main) {
                    isExporting = false
                    onComplete(true, settings.outputPath)
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isExporting = false
                    onComplete(false, "导出失败: ${e.message}")
                }
            }
        }
    }
    
    fun cancelExport() {
        exportJob?.cancel()
        isExporting = false
    }
    
    fun getProgress(): Float = exportProgress
    fun isExporting(): Boolean = isExporting
}

/**
 * 性能优化管理器 - 适配高中低端安卓设备
 */
object PerformanceManager {
    enum class DeviceTier {
        LOW, MEDIUM, HIGH
    }
    
    var currentTier: DeviceTier = DeviceTier.MEDIUM
        private set
    
    fun detectDeviceTier(totalMemoryMB: Long, glRenderer: String): DeviceTier {
        currentTier = when {
            totalMemoryMB < 4096 -> DeviceTier.LOW
            totalMemoryMB < 8192 -> DeviceTier.MEDIUM
            else -> DeviceTier.HIGH
        }
        
        // 根据GPU型号进一步调整
        if (glRenderer.contains("Adreno", true)) {
            if (glRenderer.contains("6") || glRenderer.contains("7")) {
                currentTier = DeviceTier.HIGH
            }
        } else if (glRenderer.contains("Mali", true)) {
            if (glRenderer.contains("G7") || glRenderer.contains("G8")) {
                currentTier = DeviceTier.HIGH
            }
        }
        
        return currentTier
    }
    
    fun getRecommendedSettings(): RenderSettings {
        return when (currentTier) {
            DeviceTier.LOW -> RenderSettings(
                msaaLevel = 1,
                shadowMapSize = 512,
                maxTextureSize = 1024,
                enableShadows = false,
                enablePostProcessing = false,
                lodDistance = 30f,
                maxBoneCount = 64
            )
            DeviceTier.MEDIUM -> RenderSettings(
                msaaLevel = 2,
                shadowMapSize = 1024,
                maxTextureSize = 2048,
                enableShadows = true,
                enablePostProcessing = false,
                lodDistance = 50f,
                maxBoneCount = 128
            )
            DeviceTier.HIGH -> RenderSettings(
                msaaLevel = 4,
                shadowMapSize = 2048,
                maxTextureSize = 4096,
                enableShadows = true,
                enablePostProcessing = true,
                lodDistance = 100f,
                maxBoneCount = 256
            )
        }
    }
    
    data class RenderSettings(
        val msaaLevel: Int,
        val shadowMapSize: Int,
        val maxTextureSize: Int,
        val enableShadows: Boolean,
        val enablePostProcessing: Boolean,
        val lodDistance: Float,
        val maxBoneCount: Int
    )
}
