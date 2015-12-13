package blepdroid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothProfile;
import android.os.ParcelUuid;

// simple data object representing a hardware device
public class BlepdroidDevice 
{
	
	public String name;
	public String address;
	public ArrayList<UUID> serviceIDs;
	public int rssi;
	public byte[] scanRecord;
	public int connectionState;
	
	BlepdroidDevice( String name, String address, int rssi ) 
	{
		this(name, address, null, rssi, null, BluetoothProfile.STATE_DISCONNECTED);
	}
	
	BlepdroidDevice( String name, String address ) 
	{
		this(name, address, null, 0, null, BluetoothProfile.STATE_DISCONNECTED);
	}
	
	BlepdroidDevice( String name, String address, int rssi, byte[] scanRecord ) 
	{
		this(name, address, null, rssi, scanRecord, BluetoothProfile.STATE_DISCONNECTED);
	}
	
	BlepdroidDevice( String name, String address, ArrayList<UUID> serviceUuids, int rssi, byte[] record ) 
	{

		this(name, address, serviceUuids, rssi, record, BluetoothProfile.STATE_DISCONNECTED);
	}
	
	BlepdroidDevice( String name, String address, ArrayList<UUID> serviceUuids, int rssi, byte[] record, int connectState ) {
		
		this.address = address;
		this.name = name;
		if(serviceUuids == null)
		{
			this.serviceIDs = new ArrayList<UUID>(); 
		}
		else
		{
			this.serviceIDs = serviceUuids;
		}
		this.rssi = rssi;
		this.scanRecord = record;
		this.connectionState = connectState;
	}
	
};