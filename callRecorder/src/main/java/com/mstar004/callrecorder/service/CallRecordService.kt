package com.mstar004.callrecorder.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.location.Location
import android.media.AudioAttributes
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.mstar004.callrecorder.CallRecord
import com.mstar004.callrecorder.R
import com.mstar004.callrecorder.helper.PrefsHelper


open class CallRecordService : Service() {

    private lateinit var mCallRecord: CallRecord

    private val CHANNEL_WHATEVER = "channel_whatever"
    private var attr: AudioAttributes? = null
    private val NOTIFY_ID = 2992
    private var contentIntent: PendingIntent? = null

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun buildPendingIntent(): PendingIntent? {
        val i = Intent(this, javaClass)
        i.action = Intent.ACTION_SHUTDOWN
        return PendingIntent.getService(this, 0, i, 0)
    }

    @SuppressLint("NewApi", "UnspecifiedImmutableFlag")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        val mgr = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            mgr.getNotificationChannel(CHANNEL_WHATEVER) == null
        ) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_WHATEVER,
                    "Whatever", NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val b = NotificationCompat.Builder(this, CHANNEL_WHATEVER)
        b.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setSmallIcon(R.drawable.ic_baseline_mic_24)
            .addAction(
                R.drawable.drawble,
                getString(R.string.text_stop),
                buildPendingIntent()
            )

        b.setContentTitle(getString(R.string.text_status))
        attr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val intent = Intent(this, CallRecordService::class.java)
        contentIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            0
        )
        startForeground(NOTIFY_ID, b.build(), FOREGROUND_SERVICE_TYPE_MICROPHONE)

        Log.e("EEEE", "onCreate: Service")

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.e("EEEE", "onStartCommand: DONE")
        val fileName = PrefsHelper.readPrefString(this, CallRecord.PREF_FILE_NAME)
        val dirPath = PrefsHelper.readPrefString(this, CallRecord.PREF_DIR_PATH)
        val dirName = PrefsHelper.readPrefString(this, CallRecord.PREF_DIR_NAME)
        val showSeed = PrefsHelper.readPrefBool(this, CallRecord.PREF_SHOW_SEED)
        val showPhoneNumber = PrefsHelper.readPrefBool(this, CallRecord.PREF_SHOW_PHONE_NUMBER)
        val outputFormat = PrefsHelper.readPrefInt(this, CallRecord.PREF_OUTPUT_FORMAT)
        val audioSource = PrefsHelper.readPrefInt(this, CallRecord.PREF_AUDIO_SOURCE)
        val audioEncoder = PrefsHelper.readPrefInt(this, CallRecord.PREF_AUDIO_ENCODER)

        mCallRecord = CallRecord.Builder(this).setRecordFileName(fileName).setRecordDirName(dirName)
            .setRecordDirPath(dirPath).setAudioEncoder(audioEncoder).setAudioSource(audioSource)
            .setOutputFormat(outputFormat).setShowSeed(showSeed).setShowPhoneNumber(showPhoneNumber)
            .build()

        mCallRecord.startCallReceiver()

        return START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        mCallRecord.stopCallReceiver()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {
        private val TAG = CallRecordService::class.java.simpleName
    }
}