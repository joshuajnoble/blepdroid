package blepdroid;

import processing.core.PApplet;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class BlepGattCallback extends BluetoothGattCallback {
	
	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
		Blepdroid.getInstance().characteristicChangedCallback(gatt, characteristic);
	}
	
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		
		Blepdroid.getInstance().characteristicReadCallback(gatt, characteristic, status);
	}
	
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
		
		Blepdroid.getInstance().characteristicWriteCallback(gatt, characteristic, status);
	}
	
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		Blepdroid.getInstance().connectionStateChangeCallback(gatt, status, newState);
	}
	
	@Override
	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{
		
		Blepdroid.getInstance().descriptorReadCallback( gatt, descriptor, status );
	}
	
	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{

		Blepdroid.getInstance().descriptorWriteCallback(gatt, descriptor, status);
		
	}
	
	@Override
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status)
	{
		//Blepdroid.getInstance().onBluetoothRSSIMethod.invoke( Blepdroid.getInstance().parent, gatt.getClass().getName(), rssi );
		Blepdroid.getInstance().rssiCallback(gatt, rssi, status);
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
			Blepdroid.getInstance().servicesDiscoveredCallback(gatt, status);
		}
		else
		{
			PApplet.println( " status != BluetoothGatt.GATT_SUCCESS ");
		}
	}
}
