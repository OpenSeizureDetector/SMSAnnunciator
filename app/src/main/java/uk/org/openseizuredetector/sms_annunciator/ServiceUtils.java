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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class ServiceUtils {
    private String TAG = "ServiceUtils";
    private int mNbound = 0;
    private Context mContext;

    public ServiceUtils(Context c) {
        mContext = c;
    }

    /**
     * Start the SdServer service
     */
    public void startServer() {
        // Start the server
        Log.d(TAG,"startServer()");
        if (isServerRunning()) {
            Log.v(TAG,"Server Already Running, not starting it again");
            return;
        }
        Intent sdServerIntent;
        sdServerIntent = new Intent(mContext, SmsAnnunciatorService.class);
        sdServerIntent.setData(Uri.parse("Start"));
        if (Build.VERSION.SDK_INT >= 26) {
            Log.i(TAG,"Starting Foreground Service (Android 8 and above)");
            mContext.startForegroundService(sdServerIntent);
        } else {
            Log.i(TAG,"Starting Normal Service (Pre-Android 8)");
            mContext.startService(sdServerIntent);
        }
    }

    /**
     * Stop the SdServer service
     */
    public void stopServer() {
        // Stop the server
        Log.d(TAG,"stopServer()");
        // then send an Intent to stop the service.
        Intent sdServerIntent;
        sdServerIntent = new Intent(mContext, SmsAnnunciatorService.class);
        sdServerIntent.setData(Uri.parse("Stop"));
        mContext.stopService(sdServerIntent);

    }

    public boolean isServerRunning() {
        int nServers = 0;
        /* Log.v(TAG,"isServerRunning()...."); */
        ActivityManager manager =
                (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            //Log.v(TAG,"Service: "+service.service.getClassName());
            if ("uk.org.openseizuredetector.sms_annunciator.SmsAnnunciatorService"
                    .equals(service.service.getClassName())) {
                nServers = nServers + 1;
            }
        }
        if (nServers != 0) {
            Log.v(TAG, "isServerRunning() - " + nServers + " instances are running");
            return true;
        }
        else
            return false;
    }




    public void bindToServer(Activity activity, SmsAnnunciatorServiceConnection smsAnnunciatorServiceConnection) {
            Log.i(TAG, "ServiceUtils.bindToServer() - binding to Server");
            Intent intent = new Intent(smsAnnunciatorServiceConnection.mContext, SmsAnnunciatorService.class);
            activity.bindService(intent, smsAnnunciatorServiceConnection, Context.BIND_AUTO_CREATE);
            mNbound = mNbound + 1;
            Log.i(TAG,"ServiceUtils.bindToServer() - mNbound = "+mNbound);
        }

        /**
         * unbind an activity from server
         */
        public void unbindFromServer(Activity activity, SmsAnnunciatorServiceConnection smsAnnunciatorServiceConnection) {
            // unbind this activity from the service if it is bound.
            if (smsAnnunciatorServiceConnection.mBound) {
                Log.i(TAG, "unbindFromServer() - unbinding");
                try {
                    activity.unbindService(smsAnnunciatorServiceConnection);
                    smsAnnunciatorServiceConnection.mBound = false;
                    mNbound = mNbound - 1;
                } catch (Exception ex) {
                    Log.e(TAG, "unbindFromServer() - error unbinding service - " + ex.toString());
                }
            } else {
                Log.i(TAG, "unbindFromServer() - not bound to server - ignoring");
            }
            Log.i(TAG,"ServiceUtils.unBindFromServer() - mNbound = "+mNbound);
        }
    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    public static class SmsAnnunciatorServiceConnection implements ServiceConnection {
        private String TAG = "SmsAnnunciatorServiceConnection";
        public SmsAnnunciatorService mService = null;
        public boolean mBound = false;
        public Context mContext;

        public SmsAnnunciatorServiceConnection(Context context) {
            mContext = context;
        }

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SmsAnnunciatorService.SdBinder binder = (SmsAnnunciatorService.SdBinder) service;
            mService = binder.getService();
            mBound = true;
            if (mService != null) {
                Log.v(TAG, "onServiceConnected() - connected ok");
            } else {
                Log.v(TAG, "onServiceConnected() - mService is null - this is wrong!");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.v(TAG, "onServiceDisonnected()");
            mBound = false;
        }

    }

}
