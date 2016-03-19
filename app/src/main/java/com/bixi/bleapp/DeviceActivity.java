package com.bixi.bleapp;

import android.app.Activity;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class DeviceActivity extends Activity {

    private static final String TAG = DeviceActivity.class.getSimpleName();

    public static final String NAME    = "name";
    public static final String ADDRESS = "address";

    private String mName;
    private String mAdress;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        getExtrasValues();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private void getExtrasValues() {
        Intent i = getIntent();
        mName   = i.getStringExtra(NAME);
        mAdress = i.getStringExtra(ADDRESS);

        Log.i(TAG, "Name: " + mName + " -- Address: " + mAdress);
    }

    @Override
    protected void onResume() {
        super.onResume();


    }
}
