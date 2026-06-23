package com.threedstudio.render

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * OpenGL 渲染服务 - 后台视频导出等长任务使用
 * 在 AndroidManifest.xml 中注册为 foregroundService
 */
class GLRenderService : Service() {

    companion object {
        private const val TAG = "GLRenderService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "GLRenderService created")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "GLRenderService started")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "GLRenderService destroyed")
    }
}
