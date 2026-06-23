package com.threedstudio.app

import android.app.Application
import android.util.Log
import com.threedstudio.render.GLRenderEngine

/**
 * 3D动画工作室应用主Application
 * 负责全局初始化：渲染引擎、资源缓存、性能管理器
 */
class ThreeDStudioApp : Application() {
    
    companion object {
        private const val TAG = "ThreeDStudio"
        lateinit var instance: ThreeDStudioApp
            private set
    }
    
    var renderEngine: GLRenderEngine? = null
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "3D Animation Studio initializing...")
        
        // 初始化性能管理器
        initPerformanceManager()
        
        Log.i(TAG, "3D Animation Studio initialized successfully")
    }
    
    private fun initPerformanceManager() {
        val maxMemory = Runtime.getRuntime().maxMemory()
        val totalMemory = Runtime.getRuntime().totalMemory()
        Log.i(TAG, "Memory: max=${maxMemory / 1024 / 1024}MB, " +
                "total=${totalMemory / 1024 / 1024}MB")
    }
    
    fun initRenderEngine() {
        if (renderEngine == null) {
            renderEngine = GLRenderEngine()
            Log.i(TAG, "Render engine initialized")
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        renderEngine?.release()
        Log.i(TAG, "3D Animation Studio terminated")
    }
}
