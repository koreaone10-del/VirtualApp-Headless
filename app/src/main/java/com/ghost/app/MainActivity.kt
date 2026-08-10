package com.ghost.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            finish()
            return
        }
        val serviceIntent = Intent(this, GhostService::class.java).apply {
            putExtra("TARGET_PACKAGE", "com.example.yourapp") // غيره إلى حزمة تطبيقك
        }
        startForegroundService(serviceIntent)
        finish()
    }
}
