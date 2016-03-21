package com.bixi.bleapp;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeviceActivity extends Activity {

    private static final String TAG = DeviceActivity.class.getSimpleName();

    public static final String NAME    = "name";
    public static final String ADDRESS = "address";

    private String mAdress;
    private Thread dataThread;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;


    public static final String CMDNEXT     = "next";
    public static final String CMDSTOP     = "stop";
    public static final String CMDPLAY     = "play";
    public static final String CMDNAME     = "command";
    public static final String CMDPAUSE    = "pause";
    public static final String CMDPREVIOUS = "previous";
    public static final String SERVICECMD  = "com.android.music.musicservicecommand";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "Service connected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if(!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            mBluetoothLeService.connect(mAdress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(TAG, "Service disconnected");
            mBluetoothLeService = null;
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
        mAdress  = i.getStringExtra(ADDRESS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mAdress);
            Log.i(TAG, "Connect request result = " + result);
        }

        if (dataThread==null) {
            dataThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int j=0;
                        while (mBluetoothGattCharacteristic == null) {
                            Thread.sleep(500);
                            System.out.println("thread__waiting_data");
                        }
                        BluetoothGattCharacteristic characteristic0 = mBluetoothGattCharacteristic;
                        mBluetoothLeService.setCharacteristicNotification(characteristic0, true);
                        Thread.sleep(150);
                    } catch (Exception e) {
                        e.getLocalizedMessage();
                    }
                }
            });
            dataThread.start();
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                if (BluetoothLeService.UUID_ACCELERATION.equals(gattCharacteristic.getUuid())) {
                    mBluetoothGattCharacteristic = gattCharacteristic;
                }
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "On Received");

            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            }else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                int value =  intent.getIntExtra(BluetoothLeService.EXTRA_DATA, -1);
                switch (value) {
                    case BluetoothLeService.EXTRA_BUTTON_CLICK:
                        manageMusic();
                        break;
                }
            }
        }
    };

    private void manageMusic() {
        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if(mAudioManager.isMusicActive()) {
            Intent i = new Intent(SERVICECMD);
            i.putExtra(CMDNAME , CMDPAUSE );
            DeviceActivity.this.sendBroadcast(i);
            Log.d(TAG, "PAUSE");
        } else {
            Intent i = new Intent(SERVICECMD);
            i.putExtra(CMDNAME , CMDPLAY );
            DeviceActivity.this.sendBroadcast(i);
            Log.d(TAG, "START");
        }
    }
}
