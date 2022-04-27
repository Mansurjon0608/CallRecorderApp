package com.mstar004.demorecorder

import android.content.Context
import com.mstar004.callrecorder.CallRecord
import com.mstar004.callrecorder.receiver.CallRecordReceiver


import java.util.Date

class MyCallRecordReceiver(callRecord: CallRecord) : CallRecordReceiver(callRecord) {

    override fun onIncomingCallReceived(context: Context, number: String?, start: Date) {
        super.onIncomingCallReceived(context, number, start)
    }

    override fun onOutgoingCallStarted(context: Context, number: String?, start: Date) {
        super.onOutgoingCallStarted(context, number, start)
    }
}