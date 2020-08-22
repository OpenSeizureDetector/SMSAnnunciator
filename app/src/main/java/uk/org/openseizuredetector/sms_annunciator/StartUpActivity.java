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
import android.os.Bundle;
import android.util.Log;


public class StartUpActivity extends AppCompatActivity {
    private String TAG = "StartUpActivity";
    private boolean mPermissionsRequested = false;
    ServiceUtils mUtil;
    private final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECEIVE_SMS,
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mUtil = new ServiceUtils(this);
        mUtil.startServer();
        requestPermissions(this);
        this.finish();
    }


    public void requestPermissions(Activity activity) {
        if (mPermissionsRequested) {
            Log.i(TAG,"requestPermissions() - request already sent - not doing anything");
        } else {
            Log.i(TAG, "requestPermissions() - requesting permissions");
            for (int i = 0; i < REQUIRED_PERMISSIONS.length; i++) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        REQUIRED_PERMISSIONS[i])) {
                    Log.i(TAG, "shouldShowRationale for permission" + REQUIRED_PERMISSIONS[i]);
                }
            }
            ActivityCompat.requestPermissions(activity,
                    REQUIRED_PERMISSIONS,
                    42);
            mPermissionsRequested = true;
        }
    }
}