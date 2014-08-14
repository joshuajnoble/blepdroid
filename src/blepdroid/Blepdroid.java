/*
 * 
 */
package blepdroid;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import processing.core.PApplet;
import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * The Class Blepdroid manages the bluetooth connections and service on the android device.
 * This class has been tested and can manage multiple simultaneous bluetooth connections.  The maximum
 *  number of connections varied by device limitations but 3 simultaneous connections were typical.
 * 
 * To receive data from bluetooth connections a sketch should define the following method:<br />
 * 
 * void onBluetoothDataEvent(String who, byte[] data)<br />
 * 
 * who - the name of the device sending the data<br />
 * data - byte array of the data received<br />
 */

public class Blepdroid extends Fragment {
	
	protected PApplet parent;
	protected Context parentContext;

	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	BluetoothDevice chosenBluetoothDevice;
	
	private final static String TAG = Blepdroid.class.getSimpleName();

	private String mDeviceName;
	private String mDeviceAddress;
	  
	String unknownServiceString; //getResources().getString(R.string.unknown_service);
	String unknownCharaString; //getResources().getString(R.string.unknown_characteristic);
	  
	private BluetoothLeService mBluetoothLeService;
	private HashMap<String, BluetoothGattCharacteristic> selectedServiceCharacteristics 
		= new HashMap<String, BluetoothGattCharacteristic>();
	  
	private HashMap<String, ArrayList<BluetoothGattCharacteristic>> availableServicesAndCharacteristics 
		= new HashMap<String, ArrayList<BluetoothGattCharacteristic>>();
	  
	private boolean mConnected = false;
	private BluetoothGattCharacteristic mNotifyCharacteristic;
	
	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";	
		  
	private BluetoothAdapter mBluetoothAdapter;
	private boolean mScanning;
	private Handler mHandler;
  
	public static HashMap<String, String> gattAttributes;
	public static HashMap<String, UUID> characteristicUUIDs;
	
	private HashMap<String, BlepdroidDevice> discoveredDevices;
	
	protected Method onBluetoothRSSIMethod;
	protected Method onServicesDiscoveredMethod;
	protected Method onBluetoothConnectionMethod;
	protected Method onCharacteristicChangedMethod;
	protected Method onDescriptorWriteMethod;
	protected Method onDescriptorReadMethod;
	protected Method onCharacteristicReadMethod;
	protected Method onCharacteristicWriteMethod;
	protected Method onDeviceDiscoveredMethod;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

	protected static Blepdroid blepdroidSingleton;
	
	public String hwAddressToConnect;
	
	public BlepGattCallback gattCallback;
	
	// new
	private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// required
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
       // final Intent intent = getIntent();
        Intent gattServiceIntent = new Intent(parent.getApplicationContext(), BluetoothLeService.class);
        parent.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
	public void onResume() {
        super.onResume();
        parent.getApplicationContext().registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
	public void onPause() {
        super.onPause();
        parent.getApplicationContext().unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
	public void onDestroy() {
        super.onDestroy();
        parent.getApplicationContext().unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }
    
    
	// Code to manage Service lifecycle
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
        	
        	PApplet.println( " ServiceConnection onServiceConnected ");
        	
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize(parent)) {
                PApplet.println(TAG + " ServiceConnection Unable to initialize Bluetooth ");
                //finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            // mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	
        	PApplet.println( " BroadcastReceiver onReceive ");
        	
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                //updateConnectionState(R.string.connected);
                //invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                //updateConnectionState(R.string.disconnected);
//                invalidateOptionsMenu();
//                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                parseGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };
	
	public Blepdroid(Context _parent) {
		
		PApplet.println(" Blepdroid starting Blepdroid(PApplet _parent) ");
		
		if(blepdroidSingleton != null)
			return;
		
		this.parentContext = _parent;
		this.parent = (PApplet) _parent;
		
		discoveredDevices = new HashMap<String, BlepdroidDevice>();
		findParentIntention();		
		gattCallback = new BlepGattCallback();

		blepdroidSingleton = this;
		
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// begin public API
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	
	public static String lookup(String uuid, String defaultName) {
        String name = gattAttributes.get(uuid);
        return name == null ? defaultName : name;
    }
	
	public static Blepdroid getInstance()
	{
		return blepdroidSingleton;
	}

	public BluetoothAdapter getBluetoothAdapater() {
		return BluetoothAdapter.getDefaultAdapter();
	}

	public String getAddress() {
		
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		
		if (bluetoothAdapter != null) {
			return bluetoothAdapter.getAddress();
		} else {
			return "NO ADDRESS";
		}
	}


	public ArrayList<String> getConnectedDeviceNames() {
		ArrayList<String> devices = new ArrayList<String>();
		return devices;
	}

	public boolean connectToDeviceByName(String _name) {
		return mBluetoothLeService.connect( discoveredDevices.get(_name).address);
	}

	public void connectDevice(String _hwAddress) {
		
		hwAddressToConnect = _hwAddress;
		
		PApplet.println(" connectDevice ");
		parent.runOnUiThread( new Runnable()
		{
			public void run()
			{
				Blepdroid.getInstance().mBluetoothLeService.connect(Blepdroid.getInstance().hwAddressToConnect);
			}
		});
	}
	
	public boolean connectDevice(BlepdroidDevice device) {
		return mBluetoothLeService.connect(device.address);
	}

	public void scanDevices() {
	
		discoveredDevices.clear();
		
		parent.runOnUiThread( new Runnable()
		{
			public void run()
			{
				mHandler = new Handler();
				
				PApplet.println(" check functionality ");
		
		        // Use this check to determine whether BLE is supported on the device.  Then you can
		        // selectively disable BLE-related features.
		        if (!parent.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
		        	Toast.makeText(Blepdroid.getInstance().parent, "BLE Not Supported", Toast.LENGTH_SHORT).show();
		        	PApplet.println(" BLE Not Supported ");
		            //finish();
		        }
		
		        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
		        // BluetoothAdapter through BluetoothManager.
		        final BluetoothManager bluetoothManager = (BluetoothManager) parent.getSystemService(Context.BLUETOOTH_SERVICE);
		        mBluetoothAdapter = bluetoothManager.getAdapter();
		
		        // Checks if Bluetooth is supported on the device.
		        if (mBluetoothAdapter == null) {
		        	Toast.makeText(Blepdroid.getInstance().parent, "BLE Not Supported", Toast.LENGTH_SHORT).show();
		        	PApplet.println(" BLE Not Supported ");
		            return;
		        }
		        
		        scanLeDevice(true);
			}
		});
	}
	
	public void addDevice( final BluetoothDevice device, int rssi, byte[] scanRecord )
	{

		UUID uuid = new UUID(0,0);
		if(device.getUuids() != null && device.getUuids().length > 0)
		{
			uuid = device.getUuids()[0].getUuid();
		}
		
		BlepdroidDevice d = new BlepdroidDevice(device.getAddress(), device.getName(), uuid, rssi, scanRecord);
		if(!discoveredDevices.containsKey(device.getName()))
		{
			//PApplet.println( device.getName() + " " + device.getAddress() + " " + rssi + " " + scanRecord);
			discoveredDevices.put( device.getName(), d );
			
			try {
				Blepdroid.getInstance().onDeviceDiscoveredMethod.invoke(Blepdroid.getInstance().parent, d.name, d.address, d.id, d.rssi, d.scanRecord);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	public void writeCharacteristic(String characteristic, byte[] data) {
		// put the data in, then send it off to be written
		selectedServiceCharacteristics.get(characteristic).setValue(data);
		mBluetoothLeService.writeCharacteristic(selectedServiceCharacteristics.get(characteristic));

	}
	
	public void readRSSI()
	{
		mBluetoothLeService.readRSSI();
	}

	public void setCharacteristicToListen(String characteristic) {	
		
		final int charaProp = selectedServiceCharacteristics.get(characteristic).getProperties();
		
		if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristic != null) {
                mBluetoothLeService.setCharacteristicNotification(mNotifyCharacteristic, false);
                mNotifyCharacteristic = null;
            }
            mBluetoothLeService.readCharacteristic(selectedServiceCharacteristics.get(characteristic));
        }
        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = selectedServiceCharacteristics.get(characteristic);
            mBluetoothLeService.setCharacteristicNotification(selectedServiceCharacteristics.get(characteristic), true);
        }
		
		mBluetoothLeService.setCharacteristicNotification(selectedServiceCharacteristics.get(characteristic), true);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// end public API
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	

    // iterate through the supported GATT Services/Characteristics.
    private void parseGattServices(List<BluetoothGattService> gattServices) {
        
    	if (gattServices == null) 
        	return;
        
        String uuid = null;
        
//        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
//        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        availableServicesAndCharacteristics = new HashMap<String, ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(  LIST_NAME, Blepdroid.lookup(uuid, unknownServiceString));
            currentServiceData.put( LIST_UUID, uuid );
//            gattServiceData.add(currentServiceData);

//            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =  new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
//                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
//                currentCharaData.put(LIST_NAME, Blepdroid.lookup(uuid, unknownCharaString));
//                currentCharaData.put(LIST_UUID, uuid);
//                gattCharacteristicGroupData.add(currentCharaData);
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
		try 
		{
			onServicesDiscoveredMethod = parent.getClass().getMethod( "onServicesDiscovered", new Class[] { String.class, int.class });
			onBluetoothRSSIMethod = parent.getClass().getMethod( "onBluetoothRSSI", new Class[] { String.class, int.class });
			onBluetoothConnectionMethod = parent.getClass().getMethod( "onBluetoothConnection", new Class[] { String.class, int.class });
			onCharacteristicChangedMethod = parent.getClass().getMethod( "onCharacteristicChanged", new Class[] { String.class, byte[].class }); 
			onDescriptorWriteMethod = parent.getClass().getMethod( "onDescriptorWrite", new Class[] { String.class, String.class });
			onDescriptorReadMethod = parent.getClass().getMethod( "onDescriptorRead", new Class[] { String.class, String.class });
			onCharacteristicReadMethod = parent.getClass().getMethod( "onCharacteristicRead", new Class[] { String.class, String.class });
			onCharacteristicWriteMethod = parent.getClass().getMethod( "onCharacteristicWrite", new Class[] { String.class, String.class });
			
			onDeviceDiscoveredMethod = parent.getClass().getMethod( "onDeviceDiscovered", new Class[] { String.class, String.class, UUID.class, int.class, byte[].class} );
		} 
		catch (NoSuchMethodException e) 
		{
			PApplet.println("Did not find all callback methods ");
		}

	}
	

    private void scanLeDevice(final boolean enable) {
    	PApplet.println(" scanLeDevice ");
        if (enable) {
    		parent.runOnUiThread( new Runnable()
    		{
    			public void run()
    			{
	        	
		            // Stops scanning after a pre-defined scan period.
		            mHandler.postDelayed(new Runnable() {
		                @Override
		                public void run() {
		                    mScanning = false;
		                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
		                    //invalidateOptionsMenu();
		                }
		            }, SCAN_PERIOD);
		
		            mScanning = true;
		            mBluetoothAdapter.startLeScan(mLeScanCallback);
    			}
    		});
	        } else {
	            mScanning = false;
	            mBluetoothAdapter.stopLeScan(mLeScanCallback);
	        }
        //invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
            parent.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Blepdroid.getInstance().addDevice(device, rssi, scanRecord);
                }
            });
        }
    };

}