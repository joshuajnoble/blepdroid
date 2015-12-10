package blepdroid;

import java.util.UUID;


public class BlepdroidDevice {
	public String name;
	public String address;
	public UUID id;
	public int rssi;
	public byte[] scanRecord;
	
	BlepdroidDevice( String name, String address, UUID id, int rssi, byte[] record ) {
		
		this.address = address;
		this.name = name;
		this.id = id;
		this.rssi = rssi;
		this.scanRecord = record;
	}
	
};