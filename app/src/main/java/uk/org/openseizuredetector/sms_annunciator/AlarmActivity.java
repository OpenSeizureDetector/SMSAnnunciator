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

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class AlarmActivity extends AppCompatActivity {
    private String TAG = "AlarmActivity";
    private Timer mUiTimer;
    private ServiceUtils mUtil;
    private ServiceUtils.SmsAnnunciatorServiceConnection mServiceConn;
    private Handler mHandler = new Handler();   // used to update ui from mUiTimer
    private Button mAcceptBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG,"onCreate()");
        setContentView(R.layout.activity_alarm);
        mUtil = new ServiceUtils(this);
        mServiceConn = new ServiceUtils.SmsAnnunciatorServiceConnection(this);
        mUtil.bindToServer(this,mServiceConn);
        mAcceptBtn = (Button)findViewById(R.id.acceptAlarmButton);
        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG,"Accept Alarm Button Clicked");
                mServiceConn.mService.acceptAlarm();
            }
        });

    }

    @Override
    protected void onResume() {
        Log.v(TAG,"onResume()");
        super.onResume();
        // start timer to refresh user interface every second.
        mUiTimer = new Timer();
        mUiTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.post(updateUi);
            }
        }, 0, 1000);
    }

    @Override
    protected void onPause() {
        Log.v(TAG,"onPause()");
        super.onPause();
        mUiTimer.cancel();
        mUiTimer = null;
    }

    final Runnable updateUi = new Runnable() {
        public void run() {

            TextView tv;
            tv = (TextView) findViewById(R.id.alarmStandingTv);
            if (mServiceConn.mService == null) {
                tv.setText("mService is NULL???");
            } else {
                if (mServiceConn.mService.mAlarmStanding) {
                    tv.setText("ALARM STANDING");
                } else {
                    tv.setText("Alarm Accepted");
                }
                tv = (TextView) findViewById(R.id.phoneNoTv);
                tv.setText(mServiceConn.mService.mAlarmPhoneNo);
                tv = (TextView) findViewById(R.id.messageTv);
                tv.setText(mServiceConn.mService.mAlarmMsg);
                if (!mServiceConn.mService.mAlarmStanding) {
                    Log.v(TAG,"Finishing Activity");
                    finish();
                }
            }

        }
    };
}