package com.mstar004.demorecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.mstar004.callrecorder.CallRecord
import com.mstar004.callrecorder.service.CallRecordService

class MainActivity : AppCompatActivity() {
    companion object {
        private val TAG = MainActivity::class.java.simpleName

    }

    private lateinit var callRecord: CallRecord

    var builder = NotificationCompat.Builder(this, "CHANNEL_WHATEVER")
        .setSmallIcon(R.drawable.ic_baseline_mic_24)
        .setContentTitle("AdminRecorder")
        .setContentText("Microphone is active")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getPermission()

        callRecord = CallRecord.Builder(this)
            .setLogEnable(true)
            .setRecordFileName("record")
            .setRecordDirName("OperatorCallRecorder")
            .setAudioSource(MediaRecorder.AudioSource.MIC)
            .setShowSeed(true)
            .build()
    }

    fun startCallRecordClick(view: View) {
        Toast.makeText(this, "Listener is ON", Toast.LENGTH_SHORT).show()
        callRecord.startCallReceiver()

        //callRecord.enableSaveFile();
        //callRecord.changeRecordDirName("NewDirName");
    }

    fun stopCallRecordClick(view: View) {
        Toast.makeText(this, "Listener is OFF", Toast.LENGTH_SHORT).show()
        callRecord.stopCallReceiver()

        //callRecord.disableSaveFile();
        //callRecord.changeRecordFileName("NewFileName");
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun getPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Dexter.withContext(this@MainActivity)
                .withPermissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.SYSTEM_ALERT_WINDOW,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.BIND_ACCESSIBILITY_SERVICE
                )
                .withListener(object : MultiplePermissionsListener {

                    @RequiresApi(Build.VERSION_CODES.R)
                    @SuppressLint("MissingPermission")
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if (!isMyServiceRunning(CallRecordService::class.java)) {
                                startForegroundService(Intent(this@MainActivity,
                                    CallRecordService::class.java))
                            }
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?,
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
                .withErrorListener {
                    getPermission()
                }
                .check()
        } else {
            Dexter.withContext(this@MainActivity)
                .withPermissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.SYSTEM_ALERT_WINDOW,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.BIND_ACCESSIBILITY_SERVICE
                )
                .withListener(object : MultiplePermissionsListener {
                    @RequiresApi(Build.VERSION_CODES.Q)
                    @SuppressLint("MissingPermission")
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.let {
                            if (!isMyServiceRunning(CallRecordService::class.java)) {
                                startForegroundService(
                                    Intent(
                                        this@MainActivity,
                                        CallRecordService::class.java
                                    )
                                )

                            } else getPermission()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?,
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
                .withErrorListener {
                    getPermission()
                }
                .check()
        }
    }

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}