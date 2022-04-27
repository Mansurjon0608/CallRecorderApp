package com.mstar004.callrecorder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import java.util.Date

abstract class PhoneCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.e("EEEE", "onReceive: Active", )

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        if (intent.action == CallRecordReceiver.ACTION_OUT) {
            savedNumber = intent.extras!!.getString(CallRecordReceiver.EXTRA_PHONE_NUMBER)?.toInt()
        } else {
            val stateStr = intent.extras!!.getString(TelephonyManager.EXTRA_STATE)
            val number = intent.extras!!.getString(TelephonyManager.EXTRA_INCOMING_NUMBER)
            savedNumber = number?.toInt()
            var state = 0

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_IDLE -> state = TelephonyManager.CALL_STATE_IDLE
                TelephonyManager.EXTRA_STATE_OFFHOOK -> state = TelephonyManager.CALL_STATE_OFFHOOK
                TelephonyManager.EXTRA_STATE_RINGING -> state = TelephonyManager.CALL_STATE_RINGING
            }
            onCallStateChanged(context, state, number)
        }
    }

    //Derived classes should override these to respond to specific events of interest
    protected abstract fun onIncomingCallReceived(context: Context, number: String?, start: Date)

    protected abstract fun onIncomingCallAnswered(context: Context, number: String?, start: Date)

    protected abstract fun onIncomingCallEnded(
        context: Context, number: String?, start: Date, end: Date
    )

    protected abstract fun onOutgoingCallStarted(context: Context, number: String?, start: Date)

    protected abstract fun onOutgoingCallEnded(
        context: Context, number: String?, start: Date, end: Date
    )

    protected abstract fun onMissedCall(context: Context, number: String?, start: Date)

    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) {
            //No change, debounce extras
            return
        }

        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                isIncoming = true
                callStartTime = Date()
                savedNumber = number?.toInt()

                onIncomingCallReceived(context, number, callStartTime)
            }
            TelephonyManager.CALL_STATE_OFFHOOK ->
                //Transition of ringing->offhoo k are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false
                    callStartTime = Date()

                    onOutgoingCallStarted(context, savedNumber?.toString(), callStartTime)
                } else {
                    isIncoming = true
                    callStartTime = Date()

                    onIncomingCallAnswered(context, savedNumber?.toString(), callStartTime)
                }
            TelephonyManager.CALL_STATE_IDLE ->
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber?.toString(), callStartTime)
                } else if (isIncoming) {
                    onIncomingCallEnded(context, savedNumber?.toString(), callStartTime, Date())
                } else {
                    onOutgoingCallEnded(context, savedNumber?.toString(), callStartTime, Date())
                }
        }
        lastState = state
    }

    companion object {
        //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var callStartTime: Date = Date()
        private var isIncoming: Boolean = false
        private var savedNumber: Int? =
            null  //because the passed incoming is only valid in ringing
    }
}