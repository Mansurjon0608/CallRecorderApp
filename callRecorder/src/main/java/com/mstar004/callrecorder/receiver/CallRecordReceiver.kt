package com.mstar004.callrecorder.receiver

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.mstar004.callrecorder.CallRecord
import com.mstar004.callrecorder.helper.PrefsHelper
import java.io.File
import java.io.IOException
import java.util.Date

open class CallRecordReceiver(private var callRecord: CallRecord) : PhoneCallReceiver() {

    companion object {

        private val TAG = CallRecordReceiver::class.java.simpleName

        const val ACTION_IN = "android.intent.action.PHONE_STATE"
        const val ACTION_OUT = "android.intent.action.NEW_OUTGOING_CALL"
        const val EXTRA_PHONE_NUMBER = "android.intent.extra.PHONE_NUMBER"
        private var recorder: MediaRecorder? = null
    }

    private var audioFile: File? = null
    private var isRecordStarted = false

    override fun onIncomingCallReceived(context: Context, number: String?, start: Date) {
    }

    override fun onIncomingCallAnswered(context: Context, number: String?, start: Date) {
        startRecord(context, "incoming", number)
    }

    override fun onIncomingCallEnded(context: Context, number: String?, start: Date, end: Date) {
        stopRecord(context)
    }

    override fun onOutgoingCallStarted(context: Context, number: String?, start: Date) {
        startRecord(context, "outgoing", number)
    }

    override fun onOutgoingCallEnded(context: Context, number: String?, start: Date, end: Date) {
        stopRecord(context)
    }

    override fun onMissedCall(context: Context, number: String?, start: Date) {
    }

    // Derived classes could override these to respond to specific events of interest
    protected open fun onRecordingStarted(context: Context, callRecord: CallRecord, audioFile: File?) {}

    protected open fun onRecordingFinished(context: Context, callRecord: CallRecord, audioFile: File?) {}

    private fun startRecord(context: Context, seed: String, phoneNumber: String?) {

        Log.e("EEEE", "startRecord: " )
        try {
            val isSaveFile = PrefsHelper.readPrefBool(context, CallRecord.PREF_SAVE_FILE)

            // is save file?
            if (!isSaveFile) {
                return
            }

            if (isRecordStarted) {
                try {
                    recorder?.stop()  // stop the recording
                } catch (e: RuntimeException) {
                    // RuntimeException is thrown when stop() is called immediately after start().
                    // In this case the output file is not properly constructed ans should be deleted.
                    audioFile?.delete()
                }

                releaseMediaRecorder()
                isRecordStarted = false
            } else {
                if (prepareAudioRecorder(context, seed, phoneNumber)) {
                    recorder!!.start()
                    isRecordStarted = true
                    onRecordingStarted(context, callRecord, audioFile)
                } else {
                    releaseMediaRecorder()
                }
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            releaseMediaRecorder()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            releaseMediaRecorder()
        } catch (e: Exception) {
            e.printStackTrace()
            releaseMediaRecorder()
        }
    }

    private fun prepareAudioRecorder(
        context: Context, seed: String, phoneNumber: String?
    ): Boolean {
        Log.e("EEEE", "prepareAudioRecorder: DONE", )
        try {
            var fileName = PrefsHelper.readPrefString(context, CallRecord.PREF_FILE_NAME)
            val dirPath = PrefsHelper.readPrefString(context, CallRecord.PREF_DIR_PATH)
            val dirName = PrefsHelper.readPrefString(context, CallRecord.PREF_DIR_NAME)
            val showSeed = PrefsHelper.readPrefBool(context, CallRecord.PREF_SHOW_SEED)
            val showPhoneNumber =
                PrefsHelper.readPrefBool(context, CallRecord.PREF_SHOW_PHONE_NUMBER)
            val outputFormat = PrefsHelper.readPrefInt(context, CallRecord.PREF_OUTPUT_FORMAT)
            val audioSource = PrefsHelper.readPrefInt(context, CallRecord.PREF_AUDIO_SOURCE)
            val audioEncoder = PrefsHelper.readPrefInt(context, CallRecord.PREF_AUDIO_ENCODER)

            val sampleDir = File("$dirPath/$dirName")

            if (!sampleDir.exists()) {
                sampleDir.mkdirs()
            }

            val fileNameBuilder = StringBuilder()
            fileNameBuilder.append(fileName)
            fileNameBuilder.append("_")

            if (showSeed) {
                fileNameBuilder.append(seed)
                fileNameBuilder.append("_")
            }

            if (showPhoneNumber && phoneNumber != null) {
                fileNameBuilder.append(phoneNumber)
                fileNameBuilder.append("_")
            }

            fileName = fileNameBuilder.toString()

            val suffix: String
            when (outputFormat) {
                MediaRecorder.OutputFormat.AMR_NB -> {
                    suffix = ".amr"
                }
                MediaRecorder.OutputFormat.AMR_WB -> {
                    suffix = ".amr"
                }
                MediaRecorder.OutputFormat.MPEG_4 -> {
                    suffix = ".mp4"
                }
                MediaRecorder.OutputFormat.THREE_GPP -> {
                    suffix = ".3gp"
                }
                else -> {
                    suffix = ".amr"
                }
            }

            audioFile = File.createTempFile(fileName, suffix, sampleDir)

            recorder = MediaRecorder()
            recorder?.apply {
                setAudioSource(audioSource)
                setOutputFormat(outputFormat)
                setAudioEncoder(audioEncoder)
                setOutputFile(audioFile!!.absolutePath)
                setOnErrorListener { _, _, _ -> }
            }

            try {
                recorder?.prepare()
            } catch (e: IllegalStateException) {
                releaseMediaRecorder()
                return false
            } catch (e: IOException) {
                releaseMediaRecorder()
                return false
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun stopRecord(context: Context) {
        Log.e("EEEE", "stopRecord:" )
        try {
            if (recorder != null && isRecordStarted) {
                releaseMediaRecorder()
                isRecordStarted = false
                onRecordingFinished(context, callRecord, audioFile)
            }
        } catch (e: Exception) {
            releaseMediaRecorder()
            e.printStackTrace()
        }
    }



    private fun releaseMediaRecorder() {
        recorder?.apply {
            reset()
            release()
        }
        recorder = null
    }
}