package com.remotte.app.control;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.remotte.app.control.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Oscar on 1/31/2015.
 */

public class ConnectService extends Service {

    private boolean connected = false;
    private boolean scanning = false;

    private BluetoothAdapter mBluetoothAdapter;
    public List<BluetoothGattCharacteristic> chars = null;
    private BluetoothGatt gaatt;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(MainActivity.TAG, "Received start id " + startId + ": " + intent);
        initializeBluetooth();

        if(!scanning) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            scanning = true;
            Log.d(MainActivity.TAG, "Begin scan Remotte device");
        } else { Log.d(MainActivity.TAG, "Scan is active"); }

        return START_REDELIVER_INTENT;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if(device.getName().equalsIgnoreCase("HID Remotte")) {
                        Log.d(MainActivity.TAG, "Remotte found");
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                        scanning = false;
                        connect(device.getAddress());
                    }
                }
            };

    private boolean initializeBluetooth() {
        BluetoothManager mBluetoothManager =
                (BluetoothManager) getBaseContext().getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) { return false; }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if(mBluetoothAdapter == null) { return false; }
        return true;
    }

    public boolean connect(final String address) {
        if(mBluetoothAdapter == null || address == null) { return false; }
        mBluetoothAdapter.getRemoteDevice(address)
                .connectGatt(getBaseContext(), false, mGattCallback);
        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connected = true;
                sendBroadcast(new Intent("com.remotte.REMOTTE_CONNECTED"));

                gaatt = gatt;

                if(!gatt.discoverServices()) {
                    gatt.connect();
                    gatt.discoverServices();
                }
            } else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                connected = false;
                sendBroadcast(new Intent("com.remotte.REMOTTE_DISCONNECTED"));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(MainActivity.TAG, "onServiceDiscovered");
            if(status == BluetoothGatt.GATT_SUCCESS) {
                displayGattServices(gatt.getServices());

                for(int i=0; i<chars.size(); i++) {
                    if(chars.get(i).getUuid().toString().contains("ffe1")) {
                        Log.d(MainActivity.TAG, "Button found");
                        gatt.setCharacteristicNotification(chars.get(i), true);
                        BluetoothGattDescriptor descriptor = chars.get(i).getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    } else if(chars.get(i).getUuid().toString().contains("2a19")) {
                        gatt.readCharacteristic(chars.get(i));
                        Log.d(MainActivity.TAG, "Battery found");
                    }
                }
            } else {
                Log.d(MainActivity.TAG, "Problem listing Remotte services, try again");
                gatt.discoverServices();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if(characteristic.getUuid().toString().contains("2a19")) {
                Log.d(MainActivity.TAG, "BATTERY LEVEL: " + Integer.parseInt(bytesToHex(characteristic.getValue()), 16)+"%");
            }
        }

        // FUNCTION CALLED WHEN THE APP RECEIVES A NOTIFICATION FROM THE REMOTTE.
        // IT SENDS BROADCAST TO SENSORS ACTIVITY TO INFORM ABOUT THE CHANGE AND BRING THE NEW VALUE.
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            Log.d(MainActivity.TAG, "Cosas cambiando");
            if(characteristic.getUuid().toString().contains("2a19")) {  //ON BATTERY CHANGE NOTIFICATION
                Intent intent = new Intent();
                intent.setAction("com.remotte.BATTERY_CHANGED");
                intent.putExtra("batt_value", bytesToHex(characteristic.getValue()));
                sendBroadcast(intent);
            } else if(characteristic.getUuid().toString().contains("ffe1")) {   //ON REMOTTE BUTTON PRESSED NOTIFICATION
                Log.d(MainActivity.TAG, "KeyPressed");
                Intent intent = new Intent("com.remotte.KEY_PRESSED");
                intent.putExtra("key_value", bytesToHex(characteristic.getValue()));
                sendBroadcast(intent);
            } else if(characteristic.getUuid().toString().contains("aa11")) {   //ON ACCELEROMETER VALUE CHANGED NOTIFICATION
                Intent intent = new Intent();
                intent.setAction("com.remotte.ACCELEROMETER");
                intent.putExtra("accel_value", bytesToHex(characteristic.getValue()));
                sendBroadcast(intent);
            } else if(characteristic.getUuid().toString().contains("aa51")) {   //ON GYROSCOPE VALUE CHANGED NOTIFICATION
                Intent intent = new Intent();
                intent.setAction("com.remotte.GYROSCOPE");
                intent.putExtra("gyro_value", bytesToHex(characteristic.getValue()));
                sendBroadcast(intent);
            } else if(characteristic.getUuid().toString().contains("aa41")) {   //ON ALTIMETER VALUE CHANGED NOTIFICATION
                Intent intent = new Intent();
                intent.setAction("com.remotte.ALTIMETER");
                intent.putExtra("alt_value", bytesToHex(characteristic.getValue()));
                sendBroadcast(intent);
            } else if(characteristic.getUuid().toString().contains("aa01")) {   //ON THERMOMETER VALUE CHANGED NOTIFICATION
                Intent intent = new Intent();
                intent.setAction("com.remotte.THERMOMETER");
                intent.putExtra("temp_value", bytesToHex(characteristic.getValue()));
                sendBroadcast(intent);
            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null){
            Log.d(MainActivity.TAG, "Not found services on Remotte");
            return;
        }
        chars = new ArrayList<>();
        for(BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                Log.d(MainActivity.TAG, "displayGattServices --> Characteristic: " + gattCharacteristic.getUuid().toString());
                chars.add(gattCharacteristic);
            }
        }
    }

    public byte[] hexStringToByteArray(String s) {
        try {
            int len = s.length();
            byte[] data = new byte[len/2];
            for(int i = 0; i < len; i+=2){
                data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
            }
            return data;
        } catch (Exception e) {
        }
        return new byte[]{00};
    }

    public String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = null;
        try {
            hexChars = new char[bytes.length * 2];
        } catch(Exception e){}
        if(hexChars != null) {
            for(int j = 0; j < bytes.length; j++ ) {
                int v = bytes[j] & 0xFF;
                hexChars[j * 2] = hexArray[v >>> 4];
                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
            }
            return new String(hexChars);
        } else { return "--"; }
    }

    public void writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        try {
            gatt.writeCharacteristic(characteristic);
        } catch (Exception e) { }
    }

    public void readCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        try {
            gatt.readCharacteristic(characteristic);
        } catch (Exception e) {
            Log.d(MainActivity.TAG, "Cannot read the characteristic: " + characteristic);
        }
    }

    @Override
    public void onDestroy() {
        if(gaatt != null) gaatt.disconnect();
        super.onDestroy();
    }
}
