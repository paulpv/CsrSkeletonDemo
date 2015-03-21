package com.csr.btsmartdemo;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;

import com.csr.view.DataView;

/**
 * Activity to display information about the connected device such as battery level and serial number.
 */
public class DeviceInfoActivity
        extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_info);

        // Display back button in action bar.
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Prevent screen rotation.
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        final DataView batteryData = (DataView) findViewById(R.id.batteryData);
        final DataView manufacturerData = (DataView) findViewById(R.id.manufacturerData);
        final DataView hwRevData = (DataView) findViewById(R.id.hardwareRevData);
        final DataView swRevData = (DataView) findViewById(R.id.swRevData);
        final DataView fwRevData = (DataView) findViewById(R.id.fwRevData);
        final DataView serialNoData = (DataView) findViewById(R.id.serialNoData);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        batteryData.setValueText(extras.getString(DemoActivity.EXTRA_BATTERY));
        manufacturerData.setValueText(extras.getString(DemoActivity.EXTRA_MANUFACTURER));
        hwRevData.setValueText(extras.getString(DemoActivity.EXTRA_HARDWARE_REV));
        swRevData.setValueText(extras.getString(DemoActivity.EXTRA_SW_REV));
        fwRevData.setValueText(extras.getString(DemoActivity.EXTRA_FW_REV));
        serialNoData.setValueText(extras.getString(DemoActivity.EXTRA_SERIAL));
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_device_info, menu);
        return true;
    }
    */

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                // Back button in action bar should have the same behaviour as the phone back button.
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
