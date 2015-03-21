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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.csr.btsmart.BtSmartService;
import com.csr.view.DataView;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class DemoActivity extends Activity {

    private BluetoothDevice mDeviceToConnect = null;
    private BtSmartService mService = null;
    private boolean mConnected = false;

    // For connect timeout.
    private static Handler mHandler = new Handler();

    private TextView mStatusText = null;

    // Data Views to update on the display.
    DataView distanceData = null;
    DataView speedData    = null;
    DataView cadenceData  = null;
    DataView strideData   = null;
    DataView locationData = null;

    ImageButton buttonReset = null;

    private String mManufacturer;
    private String mHardwareRev;
    private String mFwRev;
    private String mSwRev;
    private String mSerialNo;
    private String mBatteryPercent;

    private static final int REQUEST_MANUFACTURER    = 0;
    private static final int REQUEST_BATTERY         = 1;
    //private static final int REQUEST_RSC_MEASUREMENT = 2;
    //private static final int REQUEST_LOCATION        = 3;
    private static final int REQUEST_HARDWARE_REV    = 4;
    private static final int REQUEST_FW_REV          = 5;
    private static final int REQUEST_SW_REV          = 6;
    private static final int REQUEST_SERIAL_NO       = 7;

    private static final int CONNECT_TIMEOUT_MILLIS = 5000;

    public static final String EXTRA_MANUFACTURER = "MANUF";
    public static final String EXTRA_HARDWARE_REV = "HWREV";
    public static final String EXTRA_FW_REV       = "FWREV";
    public static final String EXTRA_SW_REV       = "SWREV";
    public static final String EXTRA_SERIAL       = "SERIALNO";
    public static final String EXTRA_BATTERY      = "BATTERY";

    private static final int INFO_ACTIVITY_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Prevent screen rotation.
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        // Display back button in action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.activity_demo);

        mStatusText = (TextView) findViewById(R.id.statusText);
        distanceData = (DataView) findViewById(R.id.distanceData);
        speedData = (DataView) findViewById(R.id.speedData);
        cadenceData = (DataView) findViewById(R.id.cadenceData);
        strideData = (DataView) findViewById(R.id.strideData);
        locationData = (DataView) findViewById(R.id.locationData);

        // Get the device to connect to that was passed to us by the scan results Activity.
        Intent intent = getIntent();
        mDeviceToConnect = intent.getExtras().getParcelable(BluetoothDevice.EXTRA_DEVICE);

        // Make a connection to BtSmartService to enable us to use its services.
        Intent bindIntent = new Intent(this, BtSmartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy()
    {
        // Disconnect Bluetooth connection.
        if (mService != null)
        {
            mService.disconnect();
        }
        unbindService(mServiceConnection);
        Toast.makeText(this, "Disconnected from device", Toast.LENGTH_SHORT).show();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.demo, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle presses on the action bar items
        switch (item.getItemId())
        {
            case R.id.action_info:
                Intent intent = new Intent(this, DeviceInfoActivity.class);
                intent.putExtra(EXTRA_MANUFACTURER, mManufacturer);
                intent.putExtra(EXTRA_HARDWARE_REV, mHardwareRev);
                intent.putExtra(EXTRA_FW_REV, mFwRev);
                intent.putExtra(EXTRA_SW_REV, mSwRev);
                intent.putExtra(EXTRA_SERIAL, mSerialNo);
                intent.putExtra(EXTRA_BATTERY, mBatteryPercent);
                // Start with startActivityForResult so that we can kill it using the request id if Bluetooth disconnects.
                this.startActivityForResult(intent, INFO_ACTIVITY_REQUEST);
            default:
                return super.onOptionsItemSelected(item);
        }
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

                        // Request characteristic values for device information Activity to display.
                        smartService.requestCharacteristicValue(REQUEST_MANUFACTURER,
                                BtSmartService.BtSmartUuid.DEVICE_INFORMATION_SERVICE.getUuid(),
                                BtSmartService.BtSmartUuid.MANUFACTURER_NAME.getUuid(), parentActivity.mValueHandler);

                        smartService.requestCharacteristicValue(REQUEST_HARDWARE_REV,
                                BtSmartService.BtSmartUuid.DEVICE_INFORMATION_SERVICE.getUuid(),
                                BtSmartService.BtSmartUuid.HARDWARE_REVISION.getUuid(), parentActivity.mValueHandler);

                        smartService.requestCharacteristicValue(REQUEST_FW_REV,
                                BtSmartService.BtSmartUuid.DEVICE_INFORMATION_SERVICE.getUuid(),
                                BtSmartService.BtSmartUuid.FIRMWARE_REVISION.getUuid(), parentActivity.mValueHandler);

                        smartService.requestCharacteristicValue(REQUEST_SW_REV,
                                BtSmartService.BtSmartUuid.DEVICE_INFORMATION_SERVICE.getUuid(),
                                BtSmartService.BtSmartUuid.SOFTWARE_REVISION.getUuid(), parentActivity.mValueHandler);

                        smartService.requestCharacteristicValue(REQUEST_SERIAL_NO,
                                BtSmartService.BtSmartUuid.DEVICE_INFORMATION_SERVICE.getUuid(), BtSmartService.BtSmartUuid.SERIAL_NUMBER.getUuid(),
                                parentActivity.mValueHandler);

                        /*
                        // Get sensor location.
                        smartService.requestCharacteristicValue(REQUEST_LOCATION, BtSmartService.BtSmartUuid.RSC_SERVICE.getUuid(),
                                BtSmartService.BtSmartUuid.SENSOR_LOCATION.getUuid(), parentActivity.mValueHandler);

                        // Register to be told about RSC values.
                        smartService.requestCharacteristicNotification(REQUEST_RSC_MEASUREMENT,
                                BtSmartService.BtSmartUuid.RSC_SERVICE.getUuid(), BtSmartService.BtSmartUuid.RSC_MEASUREMENT.getUuid(),
                                parentActivity.mValueHandler);
                        */

                        // Register to be told about battery level.
                        smartService.requestCharacteristicNotification(REQUEST_BATTERY,
                                BtSmartService.BtSmartUuid.BATTERY_SERVICE.getUuid(), BtSmartService.BtSmartUuid.BATTERY_LEVEL.getUuid(),
                                parentActivity.mValueHandler);

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
                    /*
                    switch (requestId) {
                        case REQUEST_RSC_MEASUREMENT:
                            Toast.makeText(parentActivity, "Failed to register for RSC notifications.", Toast.LENGTH_SHORT)
                                    .show();
                            parentActivity.finish();
                            break;
                        default:
                            break;
                    }
                    */

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

                    /*
                    // RSC notification.
                    if (serviceUuid.compareTo(BtSmartService.BtSmartUuid.RSC_SERVICE.getUuid()) == 0
                        && characteristicUuid.compareTo(BtSmartService.BtSmartUuid.RSC_MEASUREMENT.getUuid()) == 0) {
                        parentActivity.rscMeasurementHandler(msgExtra.getByteArray(BtSmartService.EXTRA_VALUE));
                    }
                    // Device information
                    else */ if (serviceUuid.compareTo(BtSmartService.BtSmartUuid.DEVICE_INFORMATION_SERVICE.getUuid()) == 0) {
                        String value;
                        try {
                            value = new String(msgExtra.getByteArray(BtSmartService.EXTRA_VALUE), "UTF-8");
                        }
                        catch (UnsupportedEncodingException e) {
                            value = "--";
                        }
                        if (characteristicUuid.compareTo(BtSmartService.BtSmartUuid.MANUFACTURER_NAME.getUuid()) == 0) {
                            parentActivity.mManufacturer = value;
                        }
                        else if (characteristicUuid.compareTo(BtSmartService.BtSmartUuid.HARDWARE_REVISION.getUuid()) == 0) {
                            parentActivity.mHardwareRev = value;
                        }
                        else if (characteristicUuid.compareTo(BtSmartService.BtSmartUuid.FIRMWARE_REVISION.getUuid()) == 0) {
                            parentActivity.mFwRev = value;
                        }
                        else if (characteristicUuid.compareTo(BtSmartService.BtSmartUuid.SOFTWARE_REVISION.getUuid()) == 0) {
                            parentActivity.mSwRev = value;
                        }
                        else if (characteristicUuid.compareTo(BtSmartService.BtSmartUuid.SERIAL_NUMBER.getUuid()) == 0) {
                            parentActivity.mSerialNo = value;
                        }
                    }
                    // Battery level notification.
                    else if (serviceUuid.compareTo(BtSmartService.BtSmartUuid.BATTERY_SERVICE.getUuid()) == 0
                             && characteristicUuid.compareTo(BtSmartService.BtSmartUuid.BATTERY_LEVEL.getUuid()) == 0) {
                        parentActivity.batteryNotificationHandler(msgExtra.getByteArray(BtSmartService.EXTRA_VALUE)[0]);
                    }
                    /*
                    // Sensor location
                    else if (serviceUuid.compareTo(BtSmartService.BtSmartUuid.RSC_SERVICE.getUuid()) == 0
                             && characteristicUuid.compareTo(BtSmartService.BtSmartUuid.SENSOR_LOCATION.getUuid()) == 0) {
                        parentActivity.sensorLocationHandler(msgExtra.getByteArray(BtSmartService.EXTRA_VALUE)[0]);
                    }
                    */
                    break;
                }
            }
        }
    }

    /**
     * Do something with the battery level received in a notification.
     *
     * @param value
     *            The battery percentage value.
     */
    private void batteryNotificationHandler(byte value) {
        mBatteryPercent = String.valueOf(value + "%");
    }

    /**
     * Process the sensor location value received from the remote device.
     *
     * @param locationIndex
     *            Index into the list of possible valid locations.
     */
    /*
    private void sensorLocationHandler(int locationIndex) {
        final String[] locations =
                { "Other", "Top of shoe", "In shoe", "Hip", "Front wheel", "Left crank", "Right crank", "Left pedal",
                  "Right pedal", "Front hub", "Rear dropout", "Chainstay", "Rear wheel", "Rear hub" };

        String location = "Not recognised";
        if (locationIndex > 0 && locationIndex < locations.length) {
            location = locations[locationIndex];
        }
        locationData.setValueText(location);
    }
    */

    /**
     * Calculate running speed and cadence values form data recieved in the characteristic notification and display in
     * the UI.
     *
     * @param value
     *            Value received in the characteristic notification.
     */
    /*
    private void rscMeasurementHandler(byte[] value) {
        final byte INDEX_FLAGS = 0;
        final byte INDEX_SPEED_VALUE = 1;
        final byte INDEX_CADENCE_VALUE = 3;
        final byte INDEX_STRIDE_VALUE = 4;
        final byte INDEX_DISTANCE_VALUE = 6;

        BluetoothGattCharacteristic rscMeasurement =
                new BluetoothGattCharacteristic(BtSmartService.BtSmartUuid.RSC_MEASUREMENT.getUuid(), 0, 0);
        rscMeasurement.setValue(value);

        final byte FLAG_STRIDE_PRESENT = 0x01;
        final byte FLAG_DISTANCE_PRESENT = (0x01 << 1);

        byte flags = value[INDEX_FLAGS];

        // Extract each of the values from the characteristic and upadte the display.
        // We have to & the values with all ones to make them unsigned (Java does not support an unsigned type).

        if ((flags & FLAG_STRIDE_PRESENT) != 0) {
            int stride =
                    (rscMeasurement.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, INDEX_STRIDE_VALUE) & 0xffff);
            strideData.setValueText(String.valueOf(stride));
        }

        if ((flags & FLAG_DISTANCE_PRESENT) != 0) {
            long distance =
                    (rscMeasurement.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, INDEX_DISTANCE_VALUE)
                     & 0x00000000ffffffffL) / 10;
            distanceData.setValueText(String.valueOf(distance));
        }

        int cadence =
                (rscMeasurement.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, INDEX_CADENCE_VALUE) & 0xff);
        cadenceData.setValueText(String.valueOf(cadence));

        int speed =
                (rscMeasurement.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, INDEX_SPEED_VALUE) & 0xffff) / 256;
        speedData.setValueText(String.valueOf(speed));
    }
    */
}
