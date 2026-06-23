package com.threedstudio.render

import android.app.Service
import android.content.Intent
import android.os.IBinder

class GLRenderService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}
