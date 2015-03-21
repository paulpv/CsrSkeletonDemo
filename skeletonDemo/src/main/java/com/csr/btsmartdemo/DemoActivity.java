/******************************************************************************
 *  Copyright (C) Cambridge Silicon Radio Limited 2014
 *
 *  This software is provided to the customer for evaluation
 *  purposes only and, as such early feedback on performance and operation
 *  is anticipated. The software source code is subject to change and
 *  not intended for production. Use of developmental release software is
 *  at the user's own risk. This software is provided "as is," and CSR
 *  cautions users to determine for themselves the suitability of using the
 *  beta release version of this software. CSR makes no warranty or
 *  representation whatsoever of merchantability or fitness of the product
 *  for any particular purpose or use. In no event shall CSR be liable for
 *  any consequential, incidental or special damages whatsoever arising out
 *  of the use of or inability to use this software, even if the user has
 *  advised CSR of the possibility of such damages.
 *
 ******************************************************************************/

package com.csr.btsmartdemo;

import java.lang.ref.WeakReference;
import java.util.UUID;
import com.csr.btsmart.BtSmartService;
import com.csr.btsmartdemo.R;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.widget.TextView;
import android.widget.Toast;

public class DemoActivity extends Activity {

    private BluetoothDevice mDeviceToConnect = null;
    private BtSmartService mService = null;
    private boolean mConnected = false;
    private TextView mStatusText = null;

    // For connect timeout.
    private static Handler mHandler = new Handler();
    
    private static final int CONNECT_TIMEOUT_MILLIS = 5000;
   
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Prevent screen rotation.
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        // Display back button in action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_demo);
    
        mStatusText = (TextView)findViewById(R.id.statusText);
        
        // Get the device to connect to that was passed to us by the scan results Activity.
        Intent intent = getIntent();
        mDeviceToConnect = intent.getExtras().getParcelable(BluetoothDevice.EXTRA_DEVICE);

        // Make a connection to BtSmartService to enable us to use its services.
        Intent bindIntent = new Intent(this, BtSmartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }    

    @Override
    public void onDestroy() {
        // Disconnect Bluetooth connection.
        if (mService != null) {
            mService.disconnect();
        }
        unbindService(mServiceConnection);
        Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    /**
     * Callbacks for changes to the state of the connection to BtSmartService.
     */
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((BtSmartService.LocalBinder) rawBinder).getService();
            if (mService != null) {
                // We have a connection to BtSmartService so now we can connect
                // and register the device handler.
                mService.connectAsClient(mDeviceToConnect, mDeviceHandler);
                startConnectTimer();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    /**
     * Start a timer to close the Activity after a fixed length of time. Used to prevent waiting for the connection to
     * happen forever.
     */
    private void startConnectTimer() {
        mHandler.postDelayed(onConnectTimeout, CONNECT_TIMEOUT_MILLIS);        
    }

    private Runnable onConnectTimeout = new Runnable() {
        @Override
        public void run() {
            if (!mConnected) finish();            
        }
    };
    
    /**
     * This is the handler for general messages about the connection.
     */
    private final DeviceHandler mDeviceHandler = new DeviceHandler(this);

    private static class DeviceHandler extends Handler {
        private final WeakReference<DemoActivity> mActivity;

        public DeviceHandler(DemoActivity activity) {
            mActivity = new WeakReference<DemoActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            DemoActivity parentActivity = mActivity.get();
            if (parentActivity != null) {
                BtSmartService smartService = parentActivity.mService;
                switch (msg.what) {                
                case BtSmartService.MESSAGE_CONNECTED: {

                    if (parentActivity != null) {
                        parentActivity.mConnected = true;
                        
                        // Cancel the connect timer.
                        mHandler.removeCallbacks(parentActivity.onConnectTimeout);
                        
                        parentActivity.mStatusText.setText("Connected.");
                        
                        // TODO: Request characteristic values to get notifications for.                        

                    }
                    break;
                }
                case BtSmartService.MESSAGE_DISCONNECTED: {
                    // End this activity and go back to scan results view.
                    mActivity.get().finish();
                    break;
                }
                }
            }
        }
    };

    
    /**
     * This is the handler for characteristic value updates.
     */
    private final Handler mValueHandler = new ValueHandler(this);

    private static class ValueHandler extends Handler {
        private final WeakReference<DemoActivity> mActivity;

        public ValueHandler(DemoActivity activity) {
            mActivity = new WeakReference<DemoActivity>(activity);
        }

        public void handleMessage(Message msg) {
            DemoActivity parentActivity = mActivity.get();
            if (parentActivity != null) {
                switch (msg.what) {
                case BtSmartService.MESSAGE_REQUEST_FAILED:
                    // The request id tells us what failed.
                    int requestId = msg.getData().getInt(BtSmartService.EXTRA_CLIENT_REQUEST_ID);
                    
                    // TODO: React to failure.
                    
                    break;
                case BtSmartService.MESSAGE_CHARACTERISTIC_VALUE:
                    // This code is executed when a value is received in response to a direct  
                    // get or a notification.

                    Bundle msgExtra = msg.getData();
                    UUID serviceUuid =
                            ((ParcelUuid) msgExtra.getParcelable(BtSmartService.EXTRA_SERVICE_UUID)).getUuid();
                    UUID characteristicUuid =
                            ((ParcelUuid) msgExtra.getParcelable(BtSmartService.EXTRA_CHARACTERISTIC_UUID)).getUuid();
                   
                    // TODO: Do something with the value. The serviceUuid and  
                    // characteristicUuid tell you which characteristic the value belongs to.

                }
            }
        }
    };

}
