package com.dima.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.dima.WalkingIconService;

public class CallReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        if (WalkingIconService.Ser != null) {
            if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                //получаем исходящий номер
                String phoneNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");

                String[] projection = new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.STARRED, ContactsContract.Contacts.CONTACT_STATUS, ContactsContract.Contacts.CONTACT_PRESENCE};

                String selection = "PHONE_NUMBERS_EQUAL(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ",?) AND "
                        + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
                String[] selectionArgs = new String[]{phoneNumber};
                Cursor cursor = WalkingIconService.Ser.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);

                if (cursor.getCount() > 0) {
                    while (cursor.moveToNext()) {
                        WalkingIconService.Imia = cursor.getString(2);
                    }
                } else {
                    WalkingIconService.Imia = phoneNumber;
                }
                WalkingIconService.Ser.setZvon(WalkingIconService.Imia);
            } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
                String phone_state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                Log.d("12345",phone_state);
                if (phone_state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    //телефон звонит, получаем входящий номер
                    String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    String[] projection = new String[]{ContactsContract.Data.CONTACT_ID, ContactsContract.Contacts.LOOKUP_KEY, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.STARRED, ContactsContract.Contacts.CONTACT_STATUS, ContactsContract.Contacts.CONTACT_PRESENCE};

                    String selection = "PHONE_NUMBERS_EQUAL(" + ContactsContract.CommonDataKinds.Phone.NUMBER + ",?) AND "
                            + ContactsContract.Data.MIMETYPE + "='" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";
                    String[] selectionArgs = new String[]{phoneNumber};
                    Cursor cursor = WalkingIconService.Ser.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);

                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            WalkingIconService.Imia = cursor.getString(2);
                        }
                    } else {
                        WalkingIconService.Imia = phoneNumber;
                    }
                    WalkingIconService.Ser.setZvon(WalkingIconService.Imia);
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                    //телефон находится в режиме звонка (набор номера / разговор)
                } else if (phone_state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                    WalkingIconService.Ser.konZvon();
                    //телефон находиться в ждущем режиме. Это событие наступает по окончанию разговора, когда мы уже знаем номер и факт звонка
                }
            }
        }
    }
}