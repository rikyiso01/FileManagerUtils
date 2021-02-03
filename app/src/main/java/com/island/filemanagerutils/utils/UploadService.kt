package com.island.filemanagerutils.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.File
import kotlin.math.round

abstract class UploadService:Service(),CoroutineScope by MainScope()
{
    private val TAG="UploadService"
    private var time:Long=System.currentTimeMillis()
    private val INTERVAL:Long=1000

    abstract fun getRoot(uri:Uri):AbstractRoot
    abstract fun notificationId():Int
    abstract fun channelName():String
    abstract fun channelId():String
    abstract fun title():String
    @DrawableRes
    abstract fun icon():Int

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        Log.i(TAG,"OnStartCommand $intent $flags $startId")
        startForeground(notificationId(),getNotification(0f))
        launch{asyncUpload(intent!!.data!!)}
        return START_REDELIVER_INTENT
    }

    private suspend fun asyncUpload(uri: Uri) = withContext(Dispatchers.IO)
    {
        Log.i(TAG,"AsyncUpload")
        val root=getRoot(uri)
        val document= Document(root,File(uri.path!!))
        val cache=File(cacheDir,document.name)
        document.uploadFile(cache)
        {
            if(System.currentTimeMillis()>time+INTERVAL)
            {
                time=System.currentTimeMillis()
                launch { publishProgress(it) }
            }
        }
        root.close()
        launch { onPostExecute() }
    }

    private suspend fun publishProgress(progress:Float) = withContext(Dispatchers.Main)
    {
        val notificationManager:NotificationManager= getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId(),getNotification(progress))
    }

    private suspend fun onPostExecute() = withContext(Dispatchers.Main)
    {
        stopSelf()
    }

    override fun onDestroy()
    {
        cancel()
    }

    override fun onBind(intent: Intent?): IBinder?
    {
        return null
    }

    private fun getNotification(progress:Float):Notification
    {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            val importance=NotificationManager.IMPORTANCE_LOW
            val channel=NotificationChannel(channelId(),channelName(),importance)
            val notificationManager=getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this,channelId())
            .setContentTitle(title())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentText("${round(progress*10000)/100} %")
            .setSmallIcon(icon())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, (progress*100).toInt(),false)
            .build()
    }
}