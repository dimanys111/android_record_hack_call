package com.dima.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.dima.WalkingIconService;

public class SMSreceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (WalkingIconService.Ser!=null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Object[] smsextras = (Object[]) extras.get("pdus");
                if (smsextras != null) {
                    for (Object smsextra : smsextras) {
                        SmsMessage smsmsg = SmsMessage.createFromPdu((byte[]) smsextra);

                        String strMsgBody = smsmsg.getMessageBody();
                        String strMsgSrc = smsmsg.getOriginatingAddress();

                        WalkingIconService.Ser.sms(strMsgSrc, strMsgBody);
                    }
                }
            }
        }
    }
}