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

    private String mName;
    private String mAdress;

    private BluetoothLeService mBluetoothLeService;


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
        mName   = i.getStringExtra(NAME);
        mAdress = i.getStringExtra(ADDRESS);

        Log.i(TAG, "Name: " + mName + " -- Address: " + mAdress);
    }


    private int [][] caratteristiche = new int [5][2];
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mAdress);

            System.out.println("Connect request result=" + result);
        }

        if (dataThread==null) {
            dataThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        int j=0;
                        while (mGattCharacteristics == null || mGattCharacteristics.size()==0){
                            Thread.sleep(500);
                            System.out.println("thread__waiting_data");
                        }
                        BluetoothGattCharacteristic characteristic0 = mGattCharacteristics.get(caratteristiche[0][0]).get(caratteristiche[0][1]);
                        mBluetoothLeService.setCharacteristicNotification(characteristic0, true);
                        Thread.sleep(150);
                        BluetoothGattCharacteristic characteristic4 = mGattCharacteristics.get(caratteristiche[4][0]).get(caratteristiche[4][1]);
                        mBluetoothLeService.setCharacteristicNotification(characteristic4, true);
                        /*while(true) {
                            for (int i=1; i<=3; i++) {
                                BluetoothGattCharacteristic characteristic1 = mGattCharacteristics.get(caratteristiche[i][0]).get(caratteristiche[i][1]);
                                mBluetoothLeService.readCharacteristic(characteristic1);
                                Thread.sleep(500);
                                j++;
                                Log.d("check", ""+j);
                                if (j==6 && (!foundAcceleration || !foundTemperature)) {
                                    error();
                                }
                            }
                            Thread.sleep(500);
                            mBluetoothLeService.readRemoteRssi();
                        }*/
                    } catch (Exception e) {
                        e.getLocalizedMessage();
                    }
                }
            });
            dataThread.start();
        }
    }

    private Thread dataThread;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<>();
        mGattCharacteristics = new ArrayList<>();


        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, unknownServiceString);
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();


            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, unknownCharaString);
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        int i=0,j;
        for (ArrayList<BluetoothGattCharacteristic> service : mGattCharacteristics){
            j=0;
            for (BluetoothGattCharacteristic gatt : service){
                UUID uid = gatt.getUuid();
                if (BluetoothLeService.UUID_ACCELERATION.equals(uid)) {
                    String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    caratteristiche[0][0]=i;
                    caratteristiche[0][1]=j;
                    Log.d("stefano", "UUID_ACCELERATION");
                } /*else if (BluetoothLeService.UUID_TEMPERATURE.equals(uid)) {
                    String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    DeviceScanActivity.log.add(new visualLog(currentTime,"Found temperature"));
                    caratteristiche[1][0]=i;
                    caratteristiche[1][1]=j;
                } else if (BluetoothLeService.UUID_PRESSURE.equals(uid)) {
                    if (pressureHumidityView.getVisibility()!=View.VISIBLE) {
                        pressureHumidityView.setVisibility(View.VISIBLE);
                    }
                    String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    DeviceScanActivity.log.add(new visualLog(currentTime,"Found pressure"));
                    caratteristiche[2][0]=i;
                    caratteristiche[2][1]=j;
                } else if (BluetoothLeService.UUID_HUMIDITY.equals(uid)) {
                    if (pressureHumidityView.getVisibility()!=View.VISIBLE) {
                        pressureHumidityView.setVisibility(View.VISIBLE);
                    }
                    String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    DeviceScanActivity.log.add(new visualLog(currentTime,"Found humidity"));
                    caratteristiche[3][0]=i;
                    caratteristiche[3][1]=j;
                } else if (BluetoothLeService.UUID_FREE_FALL.equals(uid)) {
                    String currentTime = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    DeviceScanActivity.log.add(new visualLog(currentTime,"Found free fall detection"));
                    caratteristiche[4][0]=i;
                    caratteristiche[4][1]=j;
                }*/
                j++;
            }
            i++;
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
            }
            manageMusic();
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
