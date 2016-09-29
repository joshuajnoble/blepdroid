/*
 * 
 */
package blepdroid;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import processing.core.PApplet;
import android.Manifest;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import com.lannbox.rfduinotest.BluetoothHelper;

/**
 * The Class Blepdroid manages the bluetooth connections and service on the
 * android device. This class has been tested and can manage multiple
 * simultaneous bluetooth connections. The maximum number of connections varied
 * by device limitations but 3 simultaneous connections were typical.
 * 
 * To receive data from bluetooth connections a sketch should define the
 * following method:<br />
 * 
 * void onBluetoothDataEvent(String who, byte[] data)<br />
 * 
 * who - the name of the device sending the data<br />
 * data - byte array of the data received<br />
 */

public class Blepdroid extends Fragment {

	enum BLE_MODE {
		CLIENT, PERIPHERAL
	};
	
	
	private BLE_MODE mode;
	
	BluetoothDevice chosenBluetoothDevice;

	String unknownServiceString; // getResources().getString(R.string.unknown_service);
	String unknownCharaString; // getResources().getString(R.string.unknown_characteristic);

	private BluetoothLeService mBluetoothLeService;

	private HashMap<String, BluetoothGattCharacteristic> selectedServiceCharacteristics = new HashMap<String, BluetoothGattCharacteristic>();
	//
	private HashMap<String, ArrayList<BluetoothGattCharacteristic>> availableServicesAndCharacteristics = new HashMap<String, ArrayList<BluetoothGattCharacteristic>>();
	//

//	private BluetoothAdapter mBluetoothAdapter;
	private Handler mHandler;

	public HashMap<String, String> gattAttributes;
	public HashMap<String, UUID> characteristicUUIDs;

	private HashMap<String, BlepdroidDevice> discoveredDevices;
	BlepdroidAdapter blepdroidAdapter;

	protected Method onBluetoothRSSIMethod;
	protected Method onServicesDiscoveredMethod;
	protected Method onBluetoothConnectionMethod;
	protected Method onCharacteristicChangedMethod;
	protected Method onDescriptorWriteMethod;
	protected Method onDescriptorReadMethod;
	protected Method onCharacteristicReadMethod;
	protected Method onCharacteristicWriteMethod;
	protected Method onDeviceDiscoveredMethod;

	// these are for the RFDduino
	public static UUID UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
	public static UUID UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
	public static UUID UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
	public static UUID UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
	public static UUID UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

	// Stops scanning after 10 seconds.
	private static final long SCAN_PERIOD = 1000;

	public static Blepdroid blepdroidSingleton;

	public String hwAddressToConnect;
	public BlepGattCallback gattCallback;
	
	boolean hasScanPermission;

	HashMap<String, BluetoothGatt> discoveredGatts;

	// new
	// private int mConnectionState = STATE_DISCONNECTED;

	private static final int STATE_DISCONNECTED = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;

	public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
	public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";
	
	BlepPeripheral blepPeripheral;

	private PApplet parent;
//
//	// public static void initialize(Context parent)
//	public static void initialize(PApplet _parent) {
//		_parent.getFragmentManager().beginTransaction().add(android.R.id.content, new Blepdroid(_parent)).commit();
//
//	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// required
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override
	public void onCreate(Bundle savedInstanceState) {

		PApplet.println(" BlepDroid on create ");

		super.onCreate(savedInstanceState);
		
		blepdroidAdapter = new BlepdroidAdapter(parent, this);
		
		if(mode == BLE_MODE.CLIENT)
		{
		
	        Intent gattServiceIntent = new Intent(parent.getActivity(), BluetoothLeService.class);
	        parent.getActivity().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
	        parent.getActivity().startService(gattServiceIntent);
		}
		else
		{
			blepPeripheral = new BlepPeripheral();
			blepPeripheral.init(parent, this);
		}
        
        PApplet.println( "service started ??? ");
	}
	
	@Override
	public void onStart() {
		if(Build.VERSION.SDK_INT > 22)
		{
			if (ContextCompat.checkSelfPermission(parent.getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				// No explanation needed, we can request the permission.
				hasScanPermission = false;
				ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
			} 
			else
			{
				hasScanPermission = true;
			}
		} else {
			hasScanPermission = true;
		}
		super.onStart();   
	};

	@Override
	public void onResume() {

		PApplet.println(" BlepDroid on onResume ");

		super.onResume();
		if (parent != null) {
			parent.getActivity().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

			// won't need this once we're done, hopefully
			if (onDeviceDiscoveredMethod == null) {
				findParentIntention();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		PApplet.println(" on pause ");

		if (mBluetoothLeService != null) {
			mBluetoothLeService.disconnectAll();
		}
		parent.getActivity().unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		PApplet.println(" on destroy ");

		parent.getActivity().unbindService(mServiceConnection);
		mBluetoothLeService.closeAll();
		mBluetoothLeService = null;
	}

	// Code to manage Service lifecycle
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {

			PApplet.println(" ServiceConnection onServiceConnected ");
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

			if (!mBluetoothLeService.initialize(parent)) {
				PApplet.println(" ServiceConnection Unable to initialize Bluetooth ");
			} else {
				PApplet.println(" ServiceConnection able to initialize Bluetooth ");
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
	    switch (requestCode) {
	        case 1: {
	            // If request is cancelled, the result arrays are empty.
	            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
	            	hasScanPermission = true;
	            } else {
	            	hasScanPermission = false;
	            }
	            return;
	        }
	    }
	}
	
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {

			PApplet.println(" BroadcastReceiver onReceive ");

			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				// mConnected = true;
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				// mConnected = false;
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics
				// parseGattServices(mBluetoothLeService.getSupportedGattServices());
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {

			}
		}
	};

	public Blepdroid(PApplet _parent) {

		PApplet.println(" Blepdroid starting Blepdroid(PApplet _parent) ");

		parent = _parent;
		discoveredDevices = new HashMap<String, BlepdroidDevice>();
		findParentIntention();
		BlepGattCallback.getInstance().setBlepdroidInstance(this);
		
		parent.getFragmentManager().beginTransaction().add(android.R.id.content, this).commit();

		mode = BLE_MODE.CLIENT;
	}
	
	public Blepdroid(PApplet _parent, boolean isPeripheral) {

		PApplet.println(" Blepdroid starting Blepdroid(PApplet _parent) ");

		parent = _parent;
		discoveredDevices = new HashMap<String, BlepdroidDevice>();
		findParentIntention();
		
		if(isPeripheral)
		{
			mode = BLE_MODE.PERIPHERAL;
		}
		else
		{
			mode = BLE_MODE.CLIENT;
		}
		
		BlepGattCallback.getInstance().setBlepdroidInstance(this);
		
		parent.getFragmentManager().beginTransaction().add(android.R.id.content, this).commit();

	}


	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// begin public API
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public BluetoothAdapter getBluetoothAdapater() {
		return BluetoothAdapter.getDefaultAdapter();
	}

	public String getAddress() {

		BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();

		if (bluetoothAdapter != null) {
			return bluetoothAdapter.getAddress();
		} else {
			return "NO ADDRESS";
		}
	}

	public boolean connectToDeviceByName(String _name) {
		return mBluetoothLeService.connect(discoveredDevices.get(_name));
	}

	public boolean connectDevice(BlepdroidDevice device) {
		PApplet.println(" connectDevice ");
		if (mBluetoothLeService != null) {
			PApplet.println(" connectDevice::connected device ");
			// return
			// mBluetoothLeService.connect(blepdroid.hwAddressToConnect);
			return mBluetoothLeService.connect(device);
		} else {
			PApplet.println(" connectDevice::can't connect device ");
			return false;
		}
	}

	public void scanDevices() {

		if(!hasScanPermission)
		{
			PApplet.println(" don't have permission to scan for devices ");
			return;
		}
		
		PApplet.println(" scanDevices ");

		discoveredDevices.clear();

		if (mHandler == null) {
			mHandler = parent.getActivity().getWindow().getDecorView().getHandler();
		}

		PApplet.println(" check functionality ");

		if (!parent.getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			Toast.makeText(parent.getActivity(), "BLE Not Supported", Toast.LENGTH_SHORT).show();
			PApplet.println(" BLE Not Supported ");
			parent.exit();
		}

		scanLeDevice(true);
	}

	public void writeCharacteristic(BlepdroidDevice device, UUID characteristic, byte[] data) {
		try {
			// put the data in, then send it off to be written
			mBluetoothLeService.writeCharacteristic(device, characteristic, data);
		} catch (NullPointerException e) {
			PApplet.println(" passed a null BlepdroidDevice");
		}
	}

	public void readCharacteristic(BlepdroidDevice device, UUID characteristic) {
		try {
			// put the data in, then send it off to be written
			mBluetoothLeService.readCharacteristic(device, characteristic);
		} catch (NullPointerException e) {
			PApplet.println(" passed a null BlepdroidDevice");
		}

	}

	public void readRSSI(BlepdroidDevice device) {
		try {
			mBluetoothLeService.readRSSI(device);
		} catch (NullPointerException e) {
			PApplet.println(" passed a null BlepdroidDevice");
		}
	}

	public void setCharacteristicToListen(BlepdroidDevice device,
			String characteristic) {
		try {
			// mNotifyCharacteristic =
			// selectedServiceCharacteristics.get(characteristic);
			mBluetoothLeService.setCharacteristicNotification(device, selectedServiceCharacteristics.get(characteristic).getUuid(), UUID_CLIENT_CONFIGURATION, true);
		} catch (NullPointerException e) {
			PApplet.println(" passed a null BlepdroidDevice");
		}
	}

	public void setCharacteristicToListen(BlepdroidDevice device,
			UUID characteristic) {
		try {
			mBluetoothLeService.setCharacteristicNotification(device, characteristic, UUID_CLIENT_CONFIGURATION, true);
		} catch (NullPointerException e) {
			PApplet.println(" passed a null BlepdroidDevice");
		}
	}

	public void discoverServices(BlepdroidDevice device) {

		try {
			mBluetoothLeService.discoverServices(device);
		} catch (NullPointerException e) {
			PApplet.println(" passed a null BlepdroidDevice");
		}
	}

	public boolean queryService(BlepdroidDevice device, UUID serviceID) {
		try {
			return mBluetoothLeService.queryService(device, serviceID);
		} catch (NullPointerException e) {
			PApplet.println(" passed a null BlepdroidDevice");
			return false;
		}
	}

	public HashMap<String, ArrayList<String>> findAllServicesCharacteristics(BlepdroidDevice device) {
		// get them
		parseGattServices(mBluetoothLeService.getSupportedGattServices(device.address));

		// now make them friendly
		HashMap<String, ArrayList<String>> allCharasForService = new HashMap<String, ArrayList<String>>();
		for (String service : availableServicesAndCharacteristics.keySet()) {
			ArrayList<String> chars = new ArrayList<String>();
			for (BluetoothGattCharacteristic chara : availableServicesAndCharacteristics.get(service)) {
				chars.add(chara.getUuid().toString());
			}
			allCharasForService.put(service, chars);
		}
		return allCharasForService;
	}
	
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// peripheral
	
	public void setDeviceName( String name )
	{
		
	}
	
	public void addService( String name )
	{
		
	}
	
	public void addCharacteristic( String characteristicName, String serviceName, byte[] value )
	{
		
	}

	

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// end public API
	// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	// peripheral
	public void onPeripheralWrite( byte[] data )
	{
		
	}
	
	// peripheral
	public void onPeripheralRead( byte[] data )
	{
		
	}
	
	
	
	public void servicesDiscovered(BluetoothGatt gatt, int status) {
		discoveredGatts.put(Integer.toString(gatt.hashCode()), gatt);
	}

	// iterate through the supported GATT Services/Characteristics.
	private void parseGattServices(List<BluetoothGattService> gattServices) {

		if (gattServices == null) {
			return;
		}

		String uuid = "";
		availableServicesAndCharacteristics = new HashMap<String, ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			uuid = gattService.getUuid().toString();

			List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				uuid = gattCharacteristic.getUuid().toString();
			}
			availableServicesAndCharacteristics.put(gattService.getClass().getName(), charas);
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

	private void findParentIntention() {
		try {
			onServicesDiscoveredMethod = parent.getClass().getMethod("onServicesDiscovered", new Class[] { BlepdroidDevice.class, int.class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onServicesDiscoveredMethods ");
		}

		try {

			onBluetoothRSSIMethod = parent.getClass().getMethod("onBluetoothRSSI", new Class[] { BlepdroidDevice.class, int.class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onBluetoothRSSIMethod ");
		}

		try {

			onBluetoothConnectionMethod = parent.getClass().getMethod("onBluetoothConnection", new Class[] { BlepdroidDevice.class, int.class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onBluetoothConnectionMethod ");
		}
		try {
			onCharacteristicChangedMethod = parent.getClass().getMethod( "onCharacteristicChanged",new Class[] { BlepdroidDevice.class, String.class, byte[].class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onCharacteristicChangedMethod ");
		}
		try {

			onDescriptorWriteMethod = parent.getClass().getMethod( "onDescriptorWrite", new Class[] { BlepdroidDevice.class, String.class, String.class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onDescriptorWriteMethod ");
		}
		try {
			onDescriptorReadMethod = parent.getClass().getMethod("onDescriptorRead", new Class[] { BlepdroidDevice.class, String.class, String.class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onDescriptorReadMethod ");
		}

		try {

			onCharacteristicReadMethod = parent.getClass().getMethod( "onCharacteristicRead", new Class[] { BlepdroidDevice.class, String.class, byte[].class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onCharacteristicReadMethod ");
		}
		try {

			onCharacteristicWriteMethod = parent.getClass().getMethod( "onCharacteristicWrite", new Class[] { BlepdroidDevice.class, String.class, byte[].class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onCharacteristicWriteMethod ");
		}
		try {
			// onDeviceDiscoveredMethod = parent.getClass().getMethod(
			// "onDeviceDiscovered", new Class[] { String.class, String.class,
			// UUID.class, int.class, byte[].class} );
			onDeviceDiscoveredMethod = parent.getClass().getMethod("onDeviceDiscovered", new Class[] { BlepdroidDevice.class });
		} catch (NoSuchMethodException e) {
			// PApplet.println("Did not find onDeviceDiscoveredMethod ");
		}
		try {
			// do we have service UUIDs?
			Field serviceUUID = parent.getClass().getDeclaredField( "UUID_SERVICE");
			UUID_SERVICE = (UUID) serviceUUID.get(parent);
		} catch (Exception e) {

		}

	}

	public void addDevice(final BluetoothDevice device, int rssi, byte[] scanRecord) {

		if (!discoveredDevices.containsKey(device.getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			BlepdroidDevice d = new BlepdroidDevice(device.getName(), device.getAddress(), rssi, scanRecord);

			discoveredDevices.put(device.getAddress(), d);

			try {
				PApplet.println("device discovered ");
				onDeviceDiscoveredMethod.invoke( parent, discoveredDevices.get(device.getAddress()));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} else {
			// already have it
			discoveredDevices.get(device.getAddress()).rssi = rssi;
			discoveredDevices.get(device.getAddress()).scanRecord = scanRecord;
		}

	}

	private void scanLeDevice(final boolean enable) {
		PApplet.println(" scanLeDevice ");
		if (enable) {
			parent.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					PApplet.println("running scan");
					// Stops scanning after a pre-defined scan period.
					mHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							//mBluetoothAdapter.stopLeScan(mLeScanCallback);
							blepdroidAdapter.stopScan();
						}
					}, SCAN_PERIOD);

					PApplet.println("calling startLeScan");
					blepdroidAdapter.startScan(mHandler);
//					if (mBluetoothAdapter.startLeScan(mLeScanCallback)) {
//						PApplet.println("called startLeScan successfully");
//					} else {
//						PApplet.println("didn't call startLeScan successfully");
//					}
				}
			});
		} else {
			PApplet.println("stopping scan");
//			mBluetoothAdapter.stopLeScan(mLeScanCallback);
			blepdroidAdapter.stopScan();
		}
	}
	
	// TODO this:
	
	public void beginAdvertisingWithName(String advertisementName)
	{
		
	}
	
	public void acceptConnection(BlepdroidDevice device)
	{
		
	}

	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// callbacks to manage the records of the discovered devices //
	// //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void servicesDiscoveredCallback(BluetoothGatt gatt, int status) {

		PApplet.println(" servicesDiscoveredCallback getting invoked ");

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {

			PApplet.println(" !!!! discoveredDevices.containsKey ");

			ArrayList<UUID> uuids = new ArrayList<UUID>();

			for (int i = 0; i < gatt.getServices().size(); i++) {
				uuids.add(gatt.getServices().get(i).getUuid());
			}

			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			byte[] emptyRecord = null;
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress(), uuids, 0, emptyRecord);

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {

			for (int i = 0; i < gatt.getServices().size(); i++) {
				discoveredDevices.get(gatt.getDevice().getAddress()).serviceIDs.add(gatt.getServices().get(i).getUuid());
			}

			PApplet.println(gatt.getDevice().getUuids());
		}

		PApplet.println(" getting ready to onServicesDiscoveredMethod invoke ");
		PApplet.println(onServicesDiscoveredMethod);

		try {

			PApplet.println(" onServicesDiscoveredMethod invoke ");
			onServicesDiscoveredMethod.invoke(
					parent,
					discoveredDevices.get(gatt.getDevice().getAddress()),
					status);

		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		PApplet.println(" servicesDiscoveredCallback exiting ");
	}

	public void rssiCallback(BluetoothGatt gatt, int rssi, int status) {

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			byte[] emptyRecord = null;
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress(), null, rssi, emptyRecord);

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {
			// already have it
			discoveredDevices.get(gatt.getDevice().getName()).rssi = rssi;
		}

		try {
			onBluetoothRSSIMethod.invoke(parent,discoveredDevices.get(gatt.getDevice().getAddress()), rssi);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	public void descriptorWriteCallback(BluetoothGatt gatt,
			BluetoothGattDescriptor descriptor, int status) {

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(),gatt.getDevice().getAddress());

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {
			// already have it
		}

		try {
			onDescriptorWriteMethod.invoke(parent, discoveredDevices.get(gatt.getDevice().getAddress()), descriptor.getClass().getName(), "WRITE");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void descriptorReadCallback(BluetoothGatt gatt,
			BluetoothGattDescriptor descriptor, int status) {

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress());

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {
			// already have it
		}

		try {
			onDescriptorReadMethod.invoke( parent, discoveredDevices.get(gatt.getDevice().getAddress()), descriptor.getValue(), "READ");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

	}

	public void connectionStateChangeCallback(BluetoothGatt gatt, int status, int newState) {

		PApplet.println(" connectionStateChangeCallback ");

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress());

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {
			// already have it
		}

		try {
			PApplet.println(" onBluetoothConnectionMethod invoke ");
			onBluetoothConnectionMethod.invoke( parent, discoveredDevices.get(gatt.getDevice().getAddress()), newState);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void characteristicWriteCallback(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress());

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {
			// already have it
		}

		try {
			onCharacteristicWriteMethod.invoke(parent,discoveredDevices.get(gatt.getDevice().getAddress()),characteristic.getClass().getName(),characteristic.getValue());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void characteristicReadCallback(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic, int status) {

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(),
					gatt.getDevice().getAddress());

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {
			// already have it
		}

		try {
			onCharacteristicReadMethod.invoke( parent,discoveredDevices.get(gatt.getDevice().getAddress()), characteristic.getUuid().toString(), characteristic.getValue());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void characteristicChangedCallback(BluetoothGatt gatt,
			BluetoothGattCharacteristic characteristic) {

		// this shouldn't happen but you never know
		if (!discoveredDevices.containsKey(gatt.getDevice().getAddress())) {
			// BlepdroidDevice( String name, String address, UUID[]
			// serviceUUIDs, int rssi, byte[] record )
			BlepdroidDevice d = new BlepdroidDevice(gatt.getDevice().getName(), gatt.getDevice().getAddress());

			discoveredDevices.put(gatt.getDevice().getAddress(), d);

		} else {
			// already have it
		}

		try {
			onCharacteristicChangedMethod.invoke(parent,discoveredDevices.get(gatt.getDevice().getAddress()),characteristic.getUuid().toString(),characteristic.getValue());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
