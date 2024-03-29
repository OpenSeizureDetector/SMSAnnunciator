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
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class ControlActivity extends AppCompatActivity {
    private String TAG = "ControlActivity";
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
    };

    private Timer mUiTimer;
    private ServiceUtils mUtil;
    private ServiceUtils.SmsAnnunciatorServiceConnection mServiceConn;
    private Activity mActivity;
    private Handler mHandler = new Handler();   // used to update ui from mUiTimer
    private Button mStartBtn;
    private Button mStopBtn;
    private Button mAcceptBtn;
    private Switch mEnableSwitch;
    private MenuItem mEnableSwitchItem;
    private boolean mPermissionsRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        mActivity = this;
        mUtil = new ServiceUtils(mActivity);
        mServiceConn = new ServiceUtils.SmsAnnunciatorServiceConnection(mActivity);
    }

    /**
     * Create Action Bar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu()");
        getMenuInflater().inflate(R.menu.control_activity_actions, menu);

        mEnableSwitchItem = menu.findItem(R.id.enable_switch_item);
        mEnableSwitch = mEnableSwitchItem.getActionView().findViewById(R.id.enable_switch);
        mEnableSwitch.setOnCheckedChangeListener( new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean enabled) {
                if (enabled) {
                    if (mUtil.isServerRunning()) {
                        Log.v(TAG,"onCheckedChanged - Server already running, not starting");
                    } else {
                        Log.v(TAG, "Server Enabled from Switch");
                        mUtil.startServer();
                        mUtil.bindToServer(mActivity, mServiceConn);
                    }
                } else {
                    Log.v(TAG,"Server Disabled from Switch");
                    if (mServiceConn != null) {
                        if (mServiceConn.mService != null) {
                            mServiceConn.mService.acceptAlarm();
                        } else {
                            Log.w(TAG,"mServiceConn.mService is null, so not accepting alarm");
                        }
                        mUtil.unbindFromServer(mActivity, mServiceConn);
                    } else {
                        Log.w(TAG,"mServiceConn.is null, so not unbinding from server");
                    }
                    mUtil.stopServer();
                }
            }
        });

        menu.findItem(R.id.about_menuitem).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                showAbout();
                return true;
            }
        });
        return true;
    }

    @Override
    protected void onResume() {
        Log.v(TAG,"onResume()");
        super.onResume();

        requestPermissions();

        if (mUtil.isServerRunning()) {
            mUtil.bindToServer(mActivity, mServiceConn);
        }

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
                tv.setText(R.string.ServiceNotRunningMsg);
                tv.setBackgroundColor(Color.YELLOW);
                if (mEnableSwitch != null)
                    mEnableSwitch.setChecked(false);
                //mStartBtn.setEnabled(true);
                //mStopBtn.setEnabled(false);
                if (mAcceptBtn != null) {
                    mAcceptBtn.setBackgroundColor(Color.LTGRAY);
                    mAcceptBtn.setTextColor(Color.BLACK);
                    mAcceptBtn.setEnabled(false);
                }
                tv = (TextView) findViewById(R.id.alarmStandingTv);
                tv.setText("---");
            } else {
                if (mServiceConn.mService == null) {
                    tv.setText(R.string.ServiceStoppedMsg);
                    tv.setBackgroundColor(Color.YELLOW);
                    if (mEnableSwitch != null)
                        mEnableSwitch.setChecked(false);
                    //mStartBtn.setEnabled(true);
                    //mStopBtn.setEnabled(false);
                    if (mAcceptBtn != null) {
                        mAcceptBtn.setBackgroundColor(Color.LTGRAY);
                        mAcceptBtn.setTextColor(Color.BLACK);
                        mAcceptBtn.setEnabled(false);
                    }
                    tv = (TextView) findViewById(R.id.alarmStandingTv);
                    tv.setText("---");

                } else {
                    tv.setText(R.string.ServiceRunningMsg);
                    tv.setBackgroundColor(Color.WHITE);
                    if (mEnableSwitch != null)
                        mEnableSwitch.setChecked(true);
                    //mStartBtn.setEnabled(false);
                    //mStopBtn.setEnabled(true);
                    if (mServiceConn.mService.mAlarmStanding) {
                        if (mAcceptBtn != null) {
                            mAcceptBtn.setBackgroundColor(Color.RED);
                            mAcceptBtn.setTextColor(Color.WHITE);
                            mAcceptBtn.setEnabled(true);
                        }
                    } else {
                        if (mAcceptBtn != null) {
                            mAcceptBtn.setBackgroundColor(Color.LTGRAY);
                            mAcceptBtn.setTextColor(Color.BLACK);
                            mAcceptBtn.setEnabled(true);
                        }
                    }
                    tv = (TextView) findViewById(R.id.alarmStandingTv);
                    if (mServiceConn.mService.mAlarmStanding) {
                        tv.setText(R.string.AlarmStandingMsg);
                    } else {
                        tv.setText(R.string.AlarmOKMsg);
                    }
                    tv = (TextView) findViewById(R.id.phoneNoTv);
                    tv.setText(mServiceConn.mService.mAlarmPhoneNo);
                    tv = (TextView) findViewById(R.id.messageTv);
                    tv.setText(mServiceConn.mService.mAlarmMsg);

                }
            }
        }
    };


    public void requestPermissions() {
        if (mPermissionsRequested) {
            Log.i(TAG,"requestPermissions() - request already sent - not doing anything");
        } else {
            Log.i(TAG, "requestPermissions() - requesting permissions");
            for (int i = 0; i < REQUIRED_PERMISSIONS.length; i++) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        REQUIRED_PERMISSIONS[i])) {
                    Log.i(TAG, "shouldShowRationale for permission" + REQUIRED_PERMISSIONS[i]);
                }
            }
            ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS,
                    42);
            mPermissionsRequested = true;
        }
    }

    private void showAbout() {
        View aboutView = getLayoutInflater().inflate(R.layout.about_layout, null, false);
        String versionName = getAppVersionName();
        Log.i(TAG, "showAbout() - version name = " + versionName);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.icon_24x24_green);
        builder.setTitle(getString(R.string.SMSAnnunciatorTitle)+" V" + versionName);
        builder.setView(aboutView);
        builder.create();
        builder.show();
    }

    private String getAppVersionName() {
        String versionName = "unknown";
        // From http://stackoverflow.com/questions/4471025/
        //         how-can-you-get-the-manifest-version-number-
        //         from-the-apps-layout-xml-variable
        final PackageManager packageManager = this.getPackageManager();
        if (packageManager != null) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(this.getPackageName(), 0);
                versionName = packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.v(TAG, "failed to find versionName");
                versionName = null;
            }
        }
        return versionName;
    }

}