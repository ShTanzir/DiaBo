package com.tanzir.diabo.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val CHANNEL_ID = "diabo_build_status"
        private const val CHANNEL_NAME = "Build status"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when a DiaBo cloud build finishes" }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    fun notifyBuildSuccess(projectId: String, buildId: String) {
        notify(
            id = buildId.hashCode(),
            title = "Build complete ✅",
            text = "Your Real Build finished — tap to view the preview and download the APK."
        )
    }

    fun notifyBuildFailed(projectId: String, buildId: String, reason: String?) {
        notify(
            id = buildId.hashCode(),
            title = "Build failed ❌",
            text = reason ?: "The cloud build didn't complete. Tap to see details."
        )
    }

    private fun notify(id: Int, title: String, text: String) {
        // POST_NOTIFICATIONS is a runtime permission on API 33+; if it hasn't been
        // granted yet, silently skip rather than crash — the in-app build status
        // (Room-backed) is always the source of truth regardless.
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasPermission) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
