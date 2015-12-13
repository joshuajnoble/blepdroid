package blepdroid;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import processing.core.PApplet;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

public class BlepGattCallback extends BluetoothGattCallback {
	
	@Override
	public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
	{
//		try {
//			Blepdroid.getInstance().onCharacteristicChangedMethod.invoke( Blepdroid.getInstance().parent, characteristic.getUuid().toString(), characteristic.getValue());
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
		
		Blepdroid.getInstance().characteristicChangedCallback(gatt, characteristic);
	}
	
	@Override
	public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
//		try {
//			Blepdroid.getInstance().onCharacteristicReadMethod.invoke( Blepdroid.getInstance().parent, characteristic.getUuid().toString(), characteristic.getValue() );
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
		
		Blepdroid.getInstance().characteristicReadCallback(gatt, characteristic, status);
	}
	
	@Override
	public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
	{
//		try {
//			Blepdroid.getInstance().onCharacteristicWriteMethod.invoke( Blepdroid.getInstance().parent, characteristic.getClass().getName(), characteristic.getValue() );
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
		
		Blepdroid.getInstance().characteristicWriteCallback(gatt, characteristic, status);
	}
	
	@Override
	public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
	{
		
		PApplet.println(" onConnectionStateChange " + Integer.toString(status) +  " " + Integer.toString(newState) ); 
//		
//		try {
//			Blepdroid.getInstance().onBluetoothConnectionMethod.invoke( Blepdroid.getInstance().parent, gatt.getDevice().getName(), newState );
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			
//			// try without parent
//			try{
//				Blepdroid.getInstance().onBluetoothConnectionMethod.invoke( gatt.getDevice().getName(), newState );
//			}
//			catch (IllegalArgumentException e2) {
//			} 
//			catch (IllegalAccessException e2) {
//			} 
//			catch (InvocationTargetException e2) {
//			}
//		
//		}
		
		Blepdroid.getInstance().connectionStateChangeCallback(gatt, status, newState);
	}
	
	@Override
	public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{
//		try {
//			Blepdroid.getInstance().onDescriptorReadMethod.invoke( Blepdroid.getInstance().parent, descriptor.getValue(), "READ" );
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
		
		Blepdroid.getInstance().descriptorReadCallback( gatt, descriptor, status );
	}
	
	@Override
	public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
	{
//		try {
//			Blepdroid.getInstance().onDescriptorWriteMethod.invoke( Blepdroid.getInstance().parent, descriptor.getClass().getName(), "WRITE");
//		} catch (IllegalArgumentException e) {
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			e.printStackTrace();
//		} catch (InvocationTargetException e) {
//			e.printStackTrace();
//		}
		
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
		
//			Blepdroid.getInstance().servicesDiscovered(gatt, status);
//			
//			ArrayList<UUID> serviceStrings = new ArrayList<UUID>();
//			List<BluetoothGattService> services = gatt.getServices();
//			
//			for( int i = 0; i < services.size(); i++ )
//			{
//				serviceStrings.add( services.get(i).getUuid() );
//			}
//			
				Blepdroid.getInstance().servicesDiscoveredCallback(gatt, status);
				//Blepdroid.getInstance().onServicesDiscoveredMethod.invoke( Blepdroid.getInstance().parent, serviceStrings, status );
			}
		else
		{
			PApplet.println( " status != BluetoothGatt.GATT_SUCCESS ");
		}
	}
}
