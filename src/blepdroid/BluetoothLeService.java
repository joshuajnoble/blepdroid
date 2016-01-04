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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	// private String mBluetoothDeviceAddress;
	// private BluetoothGatt mBluetoothGatt;
	// private BluetoothGattService mBluetoothGattService;

	// private ConcurrentLinkedQueue<BluetoothGattService> mQueue;
	private ConcurrentHashMap<String, BluetoothGatt> mGatts;

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
		closeAll();
		return super.onUnbind(intent);
	}

	private final IBinder mBinder = new LocalBinder();

	/**
	 * Initializes a reference to the local Bluetooth adapter.
	 * 
	 * @return Return true if the initialization is successful.
	 */
	public boolean initialize(PApplet parent) {
		// For API level 18 and above, get a reference to BluetoothAdapter
		// through BluetoothManager.

		PApplet.println(" Initializing BluetoothManager.");

		if (mBluetoothManager == null) {
			mBluetoothManager = (BluetoothManager) parent.getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
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
		
		mGatts = new ConcurrentHashMap<String, BluetoothGatt>();

		PApplet.println(" Have a bluetooth manager ");
		
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
	public boolean connect( BlepdroidDevice device ) {

		PApplet.println(" BluetoothLEService connectDevice ");

		if (mBluetoothAdapter == null || device.address == null) {
			PApplet.println(device.address + "BluetoothAdapter not initialized or unspecified address.");
			return false;
		}

		// if (mBluetoothDeviceAddress != null &&
		// address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
		//
		// PApplet.println(TAG +
		// "Trying to use an existing mBluetoothGatt for connection.");
		//
		// if (mBluetoothGatt.connect()) {
		// mConnectionState = STATE_CONNECTING;
		// return true;
		// } else {
		// return false;
		// }
		// }

		// Previously connected device. Try to reconnect.
		for (String s : mGatts.keySet()) {
			if (s.equals(device.address)) {
				if (mGatts.get(s).connect()) {
					return true;
				} else {
					return false;
				}
			}
		}
		
		PApplet.println(" BluetoothLeService::connect GattDevice not found, making a new one ");

		//
		// final BluetoothDevice device =
		// mBluetoothAdapter.getRemoteDevice(address);
		// if (device == null) {
		// PApplet.println(TAG + "Device not found.  Unable to connect.");
		// return false;
		// }
		//
		// // We want to directly connect to the device, so we are setting the
		// autoConnect parameter to false.
		// mBluetoothGatt = device.connectGatt(this, false,
		// Blepdroid.getInstance().gattCallback);
		//
		// if(mBluetoothGatt == null )
		// {
		// PApplet.println(" BluetoothLeService::connect NO mBluetoothGatt created ");
		// }
		// PApplet.println(TAG + "Trying to create a new connection.");
		// mBluetoothDeviceAddress = address;
		// mConnectionState = STATE_CONNECTING;
		// return true;

		// it's not an existing device?
		final BluetoothDevice bleDevice = mBluetoothAdapter.getRemoteDevice(device.address);
		
		if (bleDevice == null) {
			PApplet.println(TAG + "Device not found.  Unable to connect.");
			return false;
		}

		PApplet.println(" BluetoothLeService::connect connectGatt ");
		
		BluetoothGatt gatt = bleDevice.connectGatt(this, false, Blepdroid.getInstance().gattCallback);
		mGatts.put(device.address, gatt);

		return true;
	}

	/**
	 * Disconnects an existing connection or cancel a pending connection. The
	 * disconnection result is reported asynchronously through the
	 * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
	 * callback.
	 */
	public void disconnect(BlepdroidDevice device) {
		// if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		// //Log.w(TAG, "BluetoothAdapter not initialized");
		// return;
		// }
		// mBluetoothGatt.disconnect();

		for (String s : mGatts.keySet()) {
			if (s.equals(device.address)) {
				mGatts.get(s).disconnect();
			}
		}
	}

	public void disconnectAll() {
		// if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		// //Log.w(TAG, "BluetoothAdapter not initialized");
		// return;
		// }
		// mBluetoothGatt.disconnect();

		for (String s : mGatts.keySet()) {
			mGatts.get(s).disconnect();
		}
	}

	public void close(BlepdroidDevice device) {
		// if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		// //Log.w(TAG, "BluetoothAdapter not initialized");
		// return;
		// }
		// mBluetoothGatt.disconnect();

		for (String s : mGatts.keySet()) {
			if (s.equals(device.address)) {
				mGatts.get(s).close();
				mGatts.remove(s);
			}
		}
	}

	/**
	 * After using a given BLE device, the app must call this method to ensure
	 * resources are released properly.
	 */
	public void closeAll() {
		for (String s : mGatts.keySet()) {
			mGatts.get(s).close();
		}
		mGatts.clear();
		
	}

	/**
	 * Write to {@code BluetoothGattCharacteristic}. The read result is reported
	 * asynchronously through the
	 * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
	 * callback.
	 * 
	 * @param characteristic
	 *            The characteristic to read from.
	 */
	public void writeCharacteristic(BlepdroidDevice device, UUID characteristicID, byte[] value) {
		// if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		// Log.w(TAG, "BluetoothAdapter not initialized");
		// return;
		// }
//		PApplet.println( " writing char " + device.address + " " + characteristicID );
		
		if(device == null)
		{
			PApplet.println( " no device? ");
		}
		
		for (String s : mGatts.keySet()) {

			if (s.equals(device.address)) {
				
				for (BluetoothGattService service : mGatts.get(s).getServices()) {
					// if(service.getCharacteristics().contains(o))
					List<BluetoothGattCharacteristic> charList = service.getCharacteristics();
					
					for (BluetoothGattCharacteristic chart : charList) {

						if (chart.getUuid().equals(characteristicID)) {
							chart.setValue(value);
							PApplet.println(" successfully wrote char ");
							mGatts.get(s).writeCharacteristic(chart);
						}
					}
				}
			}
		}
		//
		// BluetoothGattCharacteristic characteristic =
		// mBluetoothGattService.getCharacteristic(characteristicID);
		// characteristic.setValue(value);
		// mBluetoothGatt.writeCharacteristic(characteristic);
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
	public void readCharacteristic(BlepdroidDevice device, UUID characteristicID) {

		// BluetoothGattCharacteristic characteristic =
		// mBluetoothGattService.getCharacteristic(characteristicID);
		//
		// if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		// PApplet.println(TAG + "BluetoothAdapter not initialized");
		// return;
		// }
		//
		// mBluetoothGatt.readCharacteristic(characteristic);

		for (String s : mGatts.keySet()) {
			if (s.equals(device.address)) {
				for (BluetoothGattService service : mGatts.get(s).getServices()) {
					// if(service.getCharacteristics().contains(o))
					List<BluetoothGattCharacteristic> charList = service
							.getCharacteristics();
					for (BluetoothGattCharacteristic chart : charList) {
						if (chart.getUuid().equals(characteristicID)) {
							mGatts.get(s).readCharacteristic(chart);
						}
					}
				}
			}
		}

	}

	public void readRSSI(BlepdroidDevice device) {
		// if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		// PApplet.println(TAG + "BluetoothAdapter not initialized");
		// return;
		// }
		// mBluetoothGatt.readRemoteRssi();

		for (String s : mGatts.keySet()) {
			if (s.equals(device.address)) {
				mGatts.get(s).readRemoteRssi();
			}
		}

	}

	public void setCharacteristicNotification(BlepdroidDevice device, UUID uuid,
			UUID clientConfig, boolean enabled) {
		// if (mBluetoothAdapter == null || mBluetoothGatt == null) {
		// PApplet.println(TAG + "BluetoothAdapter not initialized");
		// return;
		// }
		//
		// if(mBluetoothGattService == null) {
		// PApplet.println("WTF no mBluetoothGattService");
		// return;
		// }
		// BluetoothGattCharacteristic receiveCharacteristic =
		// mBluetoothGattService.getCharacteristic(uuid);
		// if (receiveCharacteristic != null) {
		// BluetoothGattDescriptor receiveConfigDescriptor =
		// receiveCharacteristic.getDescriptor(clientConfig);
		// if (receiveConfigDescriptor != null) {
		// mBluetoothGatt.setCharacteristicNotification(receiveCharacteristic,
		// true);
		//
		// receiveConfigDescriptor.setValue(
		// BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		// mBluetoothGatt.writeDescriptor(receiveConfigDescriptor);
		// } else {
		// PApplet.println( "RFduino receive config descriptor not found!");
		// }
		//
		// } else {
		// PApplet.println( "RFduino receive characteristic not found!");
		// }

		for (String s : mGatts.keySet()) {
			if (s.equals(device.address)) {
				for (BluetoothGattService service : mGatts.get(s).getServices()) {
					// if(service.getCharacteristics().contains(o))
					List<BluetoothGattCharacteristic> charList = service
							.getCharacteristics();
					for (BluetoothGattCharacteristic chart : charList) {
						if (chart.getUuid().equals(uuid)) {
							BluetoothGattDescriptor receiveConfigDescriptor = chart
									.getDescriptor(clientConfig);
							if (receiveConfigDescriptor != null) {
								mGatts.get(s).setCharacteristicNotification(
										chart, true);

								receiveConfigDescriptor
										.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
								mGatts.get(s).writeDescriptor(
										receiveConfigDescriptor);
							} else {
								PApplet.println("RFduino receive config descriptor not found!");
							}
						}
					}
				}
			}
		}

	}
//
	public boolean queryService(BlepdroidDevice device, UUID serviceId) {

		for (String s : mGatts.keySet()) 
		{
			if (s.equals(device.address)) 
			{
				for (BluetoothGattService service : mGatts.get(s).getServices()) 
				{
					if(service.getUuid().equals(serviceId))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}

	public boolean discoverServices(BlepdroidDevice device) {

//		return mBluetoothGatt.discoverServices();
		
		for (String s : mGatts.keySet()) {
			if(s.equals(device.address)) {
				return mGatts.get(s).discoverServices();
			}
		}
		return false;
	}

	/**
	 * Retrieves a list of supported GATT services on the connected device. This
	 * should be invoked only after {@code BluetoothGatt#discoverServices()}
	 * completes successfully.
	 * 
	 * @return A {@code List} of supported services.
	 */
	public List<BluetoothGattService> getSupportedGattServices( String address ) {
//		if (mBluetoothGatt == null) {
//			PApplet.println(" getSupportedGattServices no gatt object ");
//			return null;
//		}
//
//		return mBluetoothGatt.getServices();
		
		for (String s : mGatts.keySet()) {
			if (s.equals(address)) {
				return mGatts.get(s).getServices();
			}
		}
		
		ArrayList<BluetoothGattService> empty = new ArrayList<BluetoothGattService>(); // can't do this?
		return empty;
	}
}
