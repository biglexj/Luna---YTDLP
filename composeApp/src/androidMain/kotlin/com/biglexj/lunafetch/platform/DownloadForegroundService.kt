package com.biglexj.lunafetch.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.biglexj.lunafetch.MainActivity

class DownloadForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(ChannelId, "Descargas", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionStop -> stopSelf()
            else -> {
                val progress = intent?.getIntExtra(ProgressKey, 0) ?: 0
                startForeground(NotificationId, notification(progress))
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(progress: Int): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, ChannelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Luna Fetch")
            .setContentText(if (progress > 0) "Descargando… $progress %" else "Preparando descarga…")
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
            .build()
    }

    companion object {
        private const val ChannelId = "lunafetch-downloads"
        private const val NotificationId = 4101
        private const val ProgressKey = "progress"
        private const val ActionStop = "com.biglexj.lunafetch.STOP_DOWNLOAD_SERVICE"

        fun start(context: Context) = send(context, Intent(context, DownloadForegroundService::class.java))

        fun update(context: Context, progress: Int) = send(
            context,
            Intent(context, DownloadForegroundService::class.java).putExtra(ProgressKey, progress),
        )

        fun stop(context: Context) {
            context.stopService(Intent(context, DownloadForegroundService::class.java).setAction(ActionStop))
        }

        private fun send(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}
