/*
  OpenSeiuzureDetector SMS Annunciator - An Android App to Generate Audible Alarms if an
  OpenSeizureDetector SMS Text Message Alert is Received.

  See http://openseizuredetector.org.uk for more information.

  Copyright Graham Jones, 2020.

  This file is part of OpenSeiuzureDetector SMS Annunciator.

  OpenSeiuzureDetector SMS Annunciator is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  OpenSeiuzureDetector SMS Annunciator is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with OpenSeiuzureDetector SMS Annunciator.  If not, see <http://www.gnu.org/licenses/>.

*/

package uk.org.openseizuredetector.sms_annunciator;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class SmsAnnunciatorService extends Service {
    private String TAG = "SmsAnnunciatorService";
    private String mUuidStr = "0f675b21-5a36-4fe7-9761-fd0c691651f3";
    private Context mContext;
    private Handler mHandler;
    private ToneGenerator mToneGenerator;
    public boolean mAlarmStanding = false;
    public String mAlarmMsg = "";
    public String mAlarmPhoneNo = "";

    private final IBinder mBinder = new SdBinder();

    private int NOTIFICATION_ID = 1;
    private String mNotChId = "OSD Notification Channel";
    private CharSequence mNotChName = "OSD Notification Chennel";
    private String mNotChDesc = "OSD Notification Channel Description";

    private NotificationManager mNM;
    private NotificationCompat.Builder mNotificationBuilder;
    private Notification mNotification;


    private SmsBroadcastReceiver mSmsReceiver;
    private CountDownTimer mBeepTimer;

    /**
     * class to handle binding the MainApp activity to this service
     * so it can access mSdData.
     */
    public class SdBinder extends Binder {
        SmsAnnunciatorService getService() {
            return SmsAnnunciatorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate()");
        mToneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        mHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand() - SdServer service starting");
        mContext = this;

        // register sms receiver
        mSmsReceiver = new SmsBroadcastReceiver();
        IntentFilter filter = new IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        registerReceiver(mSmsReceiver, filter);


        // Initialise Notification channel for API level 26 and over
        // from https://stackoverflow.com/questions/44443690/notificationcompat-with-api-26
        mNM = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(this, mNotChId);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(mNotChId,
                    mNotChName,
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(mNotChDesc);
            mNM.createNotificationChannel(channel);
        }

        // Display a notification icon in the status bar of the phone to
        // show the service is running.
        if (Build.VERSION.SDK_INT >= 26) {
            Log.v(TAG, "showing Notification and calling startForeground (Android 8 and higher)");
            showNotification();
            startForeground(NOTIFICATION_ID, mNotification);
        } else {
            Log.v(TAG, "showing Notification");
            showNotification();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy(): SmsAnnunciator Service stopping");
        unregisterReceiver(mSmsReceiver);
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        Log.v(TAG, "showNotification()");
        int iconId;
        String titleStr;
        iconId = R.drawable.icon_24x24_green;

        Intent i = new Intent(getApplicationContext(), ControlActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent contentIntent =
                PendingIntent.getActivity(this,
                        0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        if (mNotificationBuilder != null) {
            mNotification = mNotificationBuilder.setContentIntent(contentIntent)
                    .setSmallIcon(iconId)
                    .setColor(0x00ffffff)
                    .setAutoCancel(false)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_text))
                    .setOnlyAlertOnce(true)
                    .build();
            mNM.notify(NOTIFICATION_ID, mNotification);
        } else {
            Log.i(TAG, "showNotification() - notification builder is null, so not showing notification.");
        }
    }

    public void acceptAlarm() {
        Log.v(TAG,"acceptAlarm()");
        mAlarmStanding = false;
        mAlarmMsg = "";
        mAlarmPhoneNo = "";
    }

    private void beep(int duration) {
        if (mToneGenerator != null) {
            mToneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, duration);
            Log.v(TAG, "beep()");
        } else {
            showToast("Warning mToneGenerator is null - not beeping!!!");
            Log.v(TAG, "beep() - Warming mToneGenerator is null - not beeping!!!");
        }
    }

    private void startBeepTimer() {
        Log.v(TAG,"startBeepTimer()");
        beep(3000);
        mBeepTimer = new CountDownTimer(1000,1000) {
            @Override
            public void onTick(long l) {
                // Do nothing
            }

            @Override
            public void onFinish() {
                Log.v(TAG,"mBeepTimer.onFinish()");
                if (mAlarmStanding) {
                    Log.v(TAG,"mBeepTimer - Alarm still standing - beeping and re-starting");
                    beep(3000);
                    startBeepTimer();
                }
            }
        };
        mBeepTimer.start();
    }

    /**
     * used to make sure timers etc. run on UI thread
     */
    public void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(mContext, msg,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private class SmsBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "SmsBroadcastReceiver.onReceive");
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            if (bundle == null) {
                Toast.makeText(context,
                        "Empty SMS Message Received - Ignoring",
                        Toast.LENGTH_SHORT).show();
            } else {
                //---retrieve the SMS message received---
                Object[] pdus = (Object[]) bundle.get("pdus");
                msgs = new SmsMessage[pdus.length];
                for (int i = 0; i < msgs.length; i++) {
                    String str = "";
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    str += "SMS from " + msgs[i].getOriginatingAddress();
                    str += " :";
                    str += msgs[i].getMessageBody();
                    str += "\n";
                    Toast.makeText(context,
                            str,
                            Toast.LENGTH_LONG).show();
                }
                String msg0 = msgs[0].getMessageBody();
                String shortUuidStr = mUuidStr.substring(mUuidStr.length()-6);
                Log.v(TAG,"shortUuidStr="+shortUuidStr);
                if (msg0.toLowerCase().contains(shortUuidStr)) {
                    try {
                        Intent i = new Intent(
                                getApplicationContext(),
                                ControlActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception ex) {
                        Log.e(TAG, "exception starting alarm activity " + ex.toString());
                    }
                    mAlarmMsg = msgs[0].getMessageBody();
                    mAlarmPhoneNo = msgs[0].getOriginatingAddress();
                    mAlarmStanding = true;
                    startBeepTimer();
                    Toast.makeText(context,
                            "Found OpenSeizureDetector UUID - Generating Alarm",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context,
                            "Message does not contain "+shortUuidStr+" - ignoring",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}