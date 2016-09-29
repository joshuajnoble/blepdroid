package blepdroid;

import processing.core.PApplet;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class BlepGattCallback extends BluetoothGattCallback {
	
	private Blepdroid blepdroid;
	
	private static BlepGattCallback _instance;
	public static BlepGattCallback getInstance()
	{
		if(_instance == null)
		{
			_instance = new BlepGattCallback();
		}
		
		return _instance;
	}
	
	public void setBlepdroidInstance(Blepdroid _blepdroid)
	{
		blepdroid = _blepdroid;
	}
	
	
	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		blepdroid.characteristicChangedCallback(gatt, characteristic);
	}
	
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		
		blepdroid.characteristicReadCallback(gatt, characteristic, status);
	}
	
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		
		blepdroid.characteristicWriteCallback(gatt, characteristic, status);
	}
	
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		blepdroid.connectionStateChangeCallback(gatt, status, newState);
	}
	
	@Override
	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{
		
		blepdroid.descriptorReadCallback( gatt, descriptor, status );
	}
	
	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{

		blepdroid.descriptorWriteCallback(gatt, descriptor, status);
		
	}
	
	@Override
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
	{
		//blepdroid.onBluetoothRSSIMethod.invoke( blepdroid.parent, gatt.getClass().getName(), rssi );
		blepdroid.rssiCallback(gatt, rssi, status);
	}
	
	@Override
	public void onReliableWriteCompleted(BluetoothGatt gatt, int status)
	{
		
	}

	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status)
	{
		PApplet.println( "BlepGattCallback::onServicesDiscovered " + Integer.toString(status));
		
		if(status == BluetoothGatt.GATT_SUCCESS)
		{
			blepdroid.servicesDiscoveredCallback(gatt, status);
		}
		else
		{
			PApplet.println( " status != BluetoothGatt.GATT_SUCCESS ");
		}
	}
}
