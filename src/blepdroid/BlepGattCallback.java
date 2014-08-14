package blepdroid;

import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BlepGattCallback extends BluetoothGattCallback {
	
	public void deviceDiscovered(BlepdroidDevice d)
	{
		try {
			//PApplet.println( "in device discovered " + Blepdroid.getInstance().onDeviceDiscoveredMethod );
			Blepdroid.getInstance().onDeviceDiscoveredMethod.invoke(Blepdroid.getInstance().parent, d.name, d.address, d.id, d.rssi, d.scanRecord);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void servicesDiscovered(BluetoothGatt gatt, int status)
	{
		try {
			Blepdroid.getInstance().onServicesDiscoveredMethod.invoke(gatt.getDevice().getName(), status);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		try {
			Blepdroid.getInstance().onCharacteristicChangedMethod.invoke( characteristic.getClass().getName(), characteristic.getValue() );
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		try {
			Blepdroid.getInstance().onCharacteristicReadMethod.invoke( characteristic.getClass().getName(), characteristic.getValue() );
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		try {
			Blepdroid.getInstance().onCharacteristicWriteMethod.invoke( characteristic.getClass().getName(), characteristic.getValue() );
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		try {
			Blepdroid.getInstance().onBluetoothConnectionMethod.invoke( gatt.getDevice().getName(), newState );
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{
		try {
			Blepdroid.getInstance().onDescriptorReadMethod.invoke( descriptor.getClass().getName(), "READ" );
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{
		try {
			Blepdroid.getInstance().onDescriptorWriteMethod.invoke( descriptor.getClass().getName(), "WRITE");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
	}
	
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
	{
		try {
			Blepdroid.getInstance().onBluetoothRSSIMethod.invoke( gatt.getClass().getName(), rssi );
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
	{
		
	}

	public void onServicesDiscovered(BluetoothGatt gatt, int status)
	{
		try {
			Blepdroid.getInstance().onServicesDiscoveredMethod.invoke( gatt.getClass().getName(), status );
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
