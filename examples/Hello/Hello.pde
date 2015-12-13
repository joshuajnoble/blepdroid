import blepdroid.*;
import blepdroid.BlepdroidDevice;
import com.lannbox.rfduinotest.*;
import android.os.Bundle;
import android.content.Context;
import java.util.UUID;


public static UUID RFDUINO_UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
public static UUID RFDUINO_UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
public static UUID RFDUINO_UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
public static UUID RFDUINO_UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
public static UUID RFDUINO_UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

// just some example objects
public static UUID BLENANO_UUID_SERVICE = UUID.fromString( "713D0000-503E-4C75-BA94-3148F18D941E");
public static UUID BLENANO_UUID_RECEIVE = UUID.fromString("713D0002-503E-4C75-BA94-3148F18D941E");
public static UUID BLENANO_UUID_SEND = UUID.fromString("713D0003-503E-4C75-BA94-3148F18D941E");


BlepdroidDevice felix1;
BlepdroidDevice felix2;

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
    Blepdroid.getInstance().writeCharacteristic(felix1, RFDUINO_UUID_SEND, hi.getBytes());
    delay(400);
    Blepdroid.getInstance().writeCharacteristic(felix2, RFDUINO_UUID_SEND, hi.getBytes());
  } else
  {
    println(" scan !");
    Blepdroid.getInstance().scanDevices();
  }
}

void onDeviceDiscovered(BlepdroidDevice device)
{
  println("discovered device " + device.name + " address: " + device.address + " rssi: " + device.rssi );

  if (device.name != null && device.name.equals("felix1"))
  {
    if (Blepdroid.getInstance().connectDevice(device))
    {
      println(" connected felix 1 ");
      
      felix1 = device;
      
    } else
    {
      println(" couldn't connect ");
    }
  }
  
  if (device.name != null && device.name.equals("felix2"))
  {
    if (Blepdroid.getInstance().connectDevice(device))
    {
      println(" connected felix 2 ");
      
      felix2 = device;
      
    } else
    {
      println(" couldn't connect ");
    }
  }
  
  
}

void onServicesDiscovered(BlepdroidDevice device, int status)
{
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
  println(" ============================================================ ");
  println(" HELLO SKETCH ");
  println(" onBluetoothConnection " + device.address + " " + state);
  println(" ============================================================ ");
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