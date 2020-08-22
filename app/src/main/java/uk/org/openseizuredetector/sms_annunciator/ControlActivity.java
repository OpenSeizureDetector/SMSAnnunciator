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

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class ControlActivity extends AppCompatActivity {
    private String TAG = "ControlActivity";
    private Timer mUiTimer;
    private ServiceUtils mUtil;
    private ServiceUtils.SmsAnnunciatorServiceConnection mServiceConn;
    private Activity mActivity;
    private Handler mHandler = new Handler();   // used to update ui from mUiTimer
    private Button mStartBtn;
    private Button mStopBtn;
    private Button mAcceptBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

    }

    @Override
    protected void onResume() {
        Log.v(TAG,"onResume()");
        super.onResume();

        mActivity = this;
        mUtil = new ServiceUtils(mActivity);
        mServiceConn = new ServiceUtils.SmsAnnunciatorServiceConnection(mActivity);
        mUtil.bindToServer(mActivity,mServiceConn);

        mStartBtn = (Button)findViewById(R.id.startServiceButton);
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG,"Start Button Clicked");
                mUtil.startServer();
                mUtil.bindToServer(mActivity,mServiceConn);
            }
        });
        mStopBtn = (Button)findViewById(R.id.stopServiceButton);
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG,"Stop Button Clicked");
                mServiceConn.mService.acceptAlarm();
                mUtil.unbindFromServer(mActivity, mServiceConn);
                mUtil.stopServer();
            }
        });
        mAcceptBtn = (Button)findViewById(R.id.acceptAlarmButton2);
        mAcceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.v(TAG,"Accept Alarm Button Clicked");
                mServiceConn.mService.acceptAlarm();
            }
        });



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
        mUtil.unbindFromServer(this,mServiceConn);
        mUiTimer.cancel();
        mUiTimer = null;
    }


    final Runnable updateUi = new Runnable() {
        public void run() {

            TextView tv;
            tv = (TextView) findViewById(R.id.service_status_tv);
            if (!mServiceConn.mBound) {
                tv.setText("Not Bound to Service");
                mStartBtn.setEnabled(true);
                mStopBtn.setEnabled(false);
                mAcceptBtn.setBackgroundColor(Color.LTGRAY);
                mAcceptBtn.setTextColor(Color.BLACK);
                tv = (TextView) findViewById(R.id.alarmStandingTv);
                tv.setText("---");
            } else {
                if (mServiceConn.mService == null) {
                    tv.setText("Service Stopped");
                    mStartBtn.setEnabled(true);
                    mStopBtn.setEnabled(false);
                    mAcceptBtn.setBackgroundColor(Color.LTGRAY);
                    mAcceptBtn.setTextColor(Color.BLACK);
                    tv = (TextView) findViewById(R.id.alarmStandingTv);
                    tv.setText("---");

                } else {
                    tv.setText("Service Running");
                    mStartBtn.setEnabled(false);
                    mStopBtn.setEnabled(true);
                    if (mServiceConn.mService.mAlarmStanding) {
                        mAcceptBtn.setBackgroundColor(Color.RED);
                        mAcceptBtn.setTextColor(Color.WHITE);
                    } else {
                        mAcceptBtn.setBackgroundColor(Color.LTGRAY);
                        mAcceptBtn.setTextColor(Color.BLACK);
                    }
                    tv = (TextView) findViewById(R.id.alarmStandingTv);
                    if (mServiceConn.mService.mAlarmStanding) {
                        tv.setText("ALARM STANDING");
                    } else {
                        tv.setText("Alarm Status OK");
                    }
                    tv = (TextView) findViewById(R.id.phoneNoTv);
                    tv.setText(mServiceConn.mService.mAlarmPhoneNo);
                    tv = (TextView) findViewById(R.id.messageTv);
                    tv.setText(mServiceConn.mService.mAlarmMsg);

                }
            }


        }
    };

}