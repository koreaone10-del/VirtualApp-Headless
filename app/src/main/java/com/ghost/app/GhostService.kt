```kotlin
package com.ghost.app

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat

class GhostService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private var windowManager: WindowManager? = null
    private var dummyView: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, buildNotification())

        // WakeLock جزئي يحافظ على المعالج نشطاً
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AppGhost::WakeLock")
        wakeLock.acquire()

        // كتم كل أنواع الصوت
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        for (stream in arrayOf(
            AudioManager.STREAM_MUSIC, AudioManager.STREAM_RING,
            AudioManager.STREAM_ALARM, AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM
        )) {
            audioManager.setStreamVolume(stream, 0, 0)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra("TARGET_PACKAGE") ?: return START_NOT_STICKY
        showInvisibleWindow(packageName)
        return START_STICKY
    }

    private fun showInvisibleWindow(targetPackage: String) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // نافذة 1×1 بكسل
        val layout = LinearLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }

        val params = WindowManager.LayoutParams(
            1, 1,  // الحجم
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        windowManager?.addView(layout, params)
        dummyView = layout

        // بدء تشغيل التطبيق المستهدف (سيظهر في الخلفية)
        val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        }
    }

    override fun onDestroy() {
        dummyView?.let { windowManager?.removeView(it) }
        wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "ghost_channel",
                "AppGhost",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, "ghost_channel")
            .setContentTitle("AppGhost نشط")
            .setContentText("التطبيق يعمل في الخلفية...")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build()
    }
}
```
