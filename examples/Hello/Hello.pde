import blepdroid.*;
import blepdroid.BlepdroidDevice;
import com.lannbox.rfduinotest.*;
import android.os.Bundle;
import android.content.Context;
import java.util.UUID;

// here's some RFDuino values
public static UUID RFDUINO_UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
public static UUID RFDUINO_UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
public static UUID RFDUINO_UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
public static UUID RFDUINO_UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
public static UUID RFDUINO_UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

// here's some BLE values
public static UUID BLENANO_UUID_SERVICE = UUID.fromString( "713D0000-503E-4C75-BA94-3148F18D941E");
public static UUID BLENANO_UUID_RECEIVE = UUID.fromString("713D0002-503E-4C75-BA94-3148F18D941E");
public static UUID BLENANO_UUID_SEND = UUID.fromString("713D0003-503E-4C75-BA94-3148F18D941E");


BlepdroidDevice device1;
BlepdroidDevice device2;

boolean allSetUp = false;

void setup() {
  size(400, 400);
  smooth();
  println(" OK ");

  Blepdroid.initialize(this);
}

void draw() {

  background(20);
  fill(255);
}

void mousePressed()
{ 

  if (mouseY < 100)

  {
    println(" saying hi!");
    String hi = new String("hi");
    Blepdroid.getInstance().writeCharacteristic(device1, RFDUINO_UUID_SEND, hi.getBytes());
    delay(400);
    Blepdroid.getInstance().writeCharacteristic(device2, RFDUINO_UUID_SEND, hi.getBytes());
  } else
  {
    println(" scan !");
    Blepdroid.getInstance().scanDevices();
  }
}

void onDeviceDiscovered(BlepdroidDevice device)
{
  println("discovered device " + device.name + " address: " + device.address + " rssi: " + device.rssi );

  if (device.name != null && device.name.equals("device1"))
  {
    if (Blepdroid.getInstance().connectDevice(device))
    {
      println(" connected device 1 ");
      
      device1 = device;
      
    } else
    {
      println(" couldn't connect device 1 ");
    }
  }
  
  if (device.name != null && device.name.equals("device2"))
  {
    if (Blepdroid.getInstance().connectDevice(device))
    {
      println(" connected device 2 ");
      
      device2 = device;
      
    } else
    {
      println(" couldn't connect device 2 ");
    }
  }
  
  
}

void onServicesDiscovered(BlepdroidDevice device, int status)
{
  
  HashMap<String, ArrayList<String>> servicesAndCharas = Blepdroid.getInstance().findAllServicesCharacteristics(device);
  
  for( String service : servicesAndCharas.keySet())
  {
    print( service + " has " );
    
    // this will list the UUIDs of each service, in the future we're going to make
    // this tell you more about each characteristic, e.g. whether it's readable or writable
    println( servicesAndCharas.get(service));
    
  }
  
  // we want to set this for whatever device we just connected to
  Blepdroid.getInstance().setCharacteristicToListen(device, RFDUINO_UUID_RECEIVE);

  allSetUp = true;
}

// these are all the BLE callbacks
void onBluetoothRSSI(BlepdroidDevice device, int rssi)
{
  println(" onBluetoothRSSI " + device.address + " " + Integer.toString(rssi));
}

void onBluetoothConnection( BlepdroidDevice device, int state)
{
  Blepdroid.getInstance().discoverServices(device);
}

void onCharacteristicChanged(BlepdroidDevice device, String characteristic, byte[] data)
{
  String dataString = new String(data);
  println(" onCharacteristicChanged " + characteristic + " " + dataString  );
}

void onDescriptorWrite(BlepdroidDevice device, String characteristic, String data)
{
  println(" onDescriptorWrite " + characteristic + " " + data);
}

void onDescriptorRead(BlepdroidDevice device, String characteristic, String data)
{
  println(" onDescriptorRead " + characteristic + " " + data);
}

void onCharacteristicRead(BlepdroidDevice device, String characteristic, byte[] data)
{
  println(" onCharacteristicRead " + characteristic + " " + data);
}

void onCharacteristicWrite(BlepdroidDevice device, String characteristic, byte[] data)
{
  println(" onCharacteristicWrite " + characteristic + " " + data);
}