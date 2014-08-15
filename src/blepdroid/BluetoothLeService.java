/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package blepdroid;

import java.util.List;
import java.util.UUID;

import processing.core.PApplet;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String mBluetoothDeviceAddress;
	private BluetoothGatt mBluetoothGatt;
	private BluetoothGattService mBluetoothGattService;
	private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;

	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

	public class LocalBinder extends Binder {
		BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// After using a given device, you should make sure that
		// BluetoothGatt.close() is called
		// such that resources are cleaned up properly. In this particular
		// example, close() is
		// invoked when the UI is disconnected from the Service.
		close();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize( PApplet parent ) {
		// For API level 18 and above, get a reference to BluetoothAdapter through BluetoothManager.
		
		PApplet.println(" Initializing BluetoothManager.");
		
		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) parent.getSystemService(Context.BLUETOOTH_SERVICE);
			if (mBluetoothManager == null) {
				PApplet.println(TAG + "Unable to initialize BluetoothManager.");
				return false;
			}
		}

		mBluetoothAdapter = mBluetoothManager.getAdapter();
		if (mBluetoothAdapter == null) {
			PApplet.println(TAG + "Unable to obtain a BluetoothAdapter.");
			return false;
		}

		return true;
	}

	/**
	 * Connects to the GATT server hosted on the Bluetooth LE device.
	 * 
	 * @param address
	 *            The device address of the destination device.
	 * 
	 * @return Return true if the connection is initiated successfully. The
	 *         connection result is reported asynchronously through the
	 *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 *         callback.
	 */
	public boolean connect(final String address) {
		
		PApplet.println(" BluetoothLEService connectDevice ");
		
		if (mBluetoothAdapter == null || address == null) {
			PApplet.println(TAG + "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// Previously connected device. Try to reconnect.
		if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
			
			PApplet.println(TAG + "Trying to use an existing mBluetoothGatt for connection.");
			
			if (mBluetoothGatt.connect()) {
				mConnectionState = STATE_CONNECTING;
				return true;
			} else {
				return false;
			}
		}

		final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			PApplet.println(TAG + "Device not found.  Unable to connect.");
			return false;
		}
		
		// We want to directly connect to the device, so we are setting the autoConnect parameter to false.
		mBluetoothGatt = device.connectGatt(this, false, Blepdroid.getInstance().gattCallback);
		PApplet.println(TAG + "Trying to create a new connection.");
		mBluetoothDeviceAddress = address;
		mConnectionState = STATE_CONNECTING;
		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			//Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.disconnect();
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void close() {
		if (mBluetoothGatt == null) {
			return;
		}
		mBluetoothGatt.close();
		mBluetoothGatt = null;
	}

	/**
	 * Write to {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void writeCharacteristic(UUID characteristicID, byte[] value) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		
		BluetoothGattCharacteristic characteristic = mBluetoothGattService.getCharacteristic(characteristicID);
		characteristic.setValue(value);
		mBluetoothGatt.writeCharacteristic(characteristic);
	}

	/**
	 * Request a read on a given {@code BluetoothGattCharacteristic}. The read
	 * result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void readCharacteristic(UUID characteristicID) {
		
		BluetoothGattCharacteristic characteristic = mBluetoothGattService.getCharacteristic(characteristicID);
		
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			PApplet.println(TAG + "BluetoothAdapter not initialized");
			return;
		}
		
		mBluetoothGatt.readCharacteristic(characteristic);
	}
	
	public void readRSSI() {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			PApplet.println(TAG + "BluetoothAdapter not initialized");
			return;
		}
		mBluetoothGatt.readRemoteRssi();
	}

	public void setCharacteristicNotification(UUID uuid, UUID clientConfig, boolean enabled) {
		if (mBluetoothAdapter == null || mBluetoothGatt == null) {
			PApplet.println(TAG + "BluetoothAdapter not initialized");
			return;
		}

		
      BluetoothGattCharacteristic receiveCharacteristic = mBluetoothGattService.getCharacteristic(uuid);
      if (receiveCharacteristic != null) {
          BluetoothGattDescriptor receiveConfigDescriptor = receiveCharacteristic.getDescriptor(clientConfig);
          if (receiveConfigDescriptor != null) {
              mBluetoothGatt.setCharacteristicNotification(receiveCharacteristic, true);

              receiveConfigDescriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
              mBluetoothGatt.writeDescriptor(receiveConfigDescriptor);
          } else {
             PApplet.println( "RFduino receive config descriptor not found!");
          }

      } else {
    	  PApplet.println( "RFduino receive characteristic not found!");
      }

		
	}
	
	public boolean createService(UUID service)
	{
        mBluetoothGattService = mBluetoothGatt.getService(service);
        if (mBluetoothGattService == null) {
            Log.e(TAG, "RFduino GATT service not found!");
            return false;
        }
        
        return true;
	}

//            BluetoothGattCharacteristic receiveCharacteristic = mBluetoothGattService.getCharacteristic(UUID_RECEIVE);
//            if (receiveCharacteristic != null) {
//                BluetoothGattDescriptor receiveConfigDescriptor = receiveCharacteristic.getDescriptor(UUID_CLIENT_CONFIGURATION);
//                if (receiveConfigDescriptor != null) {
//                    gatt.setCharacteristicNotification(receiveCharacteristic, true);
//
//                    receiveConfigDescriptor.setValue( BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    gatt.writeDescriptor(receiveConfigDescriptor);
//                } else {
//                    Log.e(TAG, "RFduino receive config descriptor not found!");
//                }
//
//            } else {
//                Log.e(TAG, "RFduino receive characteristic not found!");
//            }
//        }
//		
//		return mBluetoothGatt;
//	}
	
	public boolean discoverServices()
	{
		return mBluetoothGatt.discoverServices();
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBluetoothGatt == null) {
			PApplet.println(" getSupportedGattServices no gatt object ");
			return null;
		}

		return mBluetoothGatt.getServices();
	}
}
