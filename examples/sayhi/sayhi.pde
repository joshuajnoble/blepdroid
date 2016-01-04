import blepdroid.*;
import com.lannbox.rfduinotest.*;
import android.os.Bundle;
import android.content.Context;
import java.util.UUID;

public static UUID RFDUINO_UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
public static UUID RFDUINO_UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
public static UUID RFDUINO_UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
public static UUID RFDUINO_UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
public static UUID RFDUINO_UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

boolean allSetUp = false;


void setup() {
  size(400,400);
  smooth();
  Blepdroid.initialize( this);
}

void draw() {
  background(0);
  fill(255);
}

void mousePressed()
{ 
  if(mouseY < 200)
  {
    println(" saying hi!");
    String hi = new String("hi");
    Blepdroid.getInstance().writeCharacteristic(RFDUINO_UUID_SEND, hi.getBytes());
  }
}

void onDeviceDiscovered(BlepdroidDevice device)
{
  println("discovered device " + device.name + " address: " + device.address + " rssi: " + device.rssi );
  if(device.name.equals("RFduino"))
  {
    Blepdroid.getInstance().connectDevice(device.address);
  }
}

void onServicesDiscovered(ArrayList<UUID> ids, int status)
{
  println(" onServicesDiscovered " + ids );
  println(" 0 means ok, anything else means bad " + status);
  
  HashMap<String, ArrayList<String>> servicesAndCharas = Blepdroid.getInstance().findAllServicesCharacteristics();
    println( servicesAndCharas.size() );
    for( String service : servicesAndCharas.keySet())
    {
      print( service + " has " );
      println( servicesAndCharas.get(service));
    }
    Blepdroid.getInstance().connectToService(RFDUINO_UUID_SERVICE); 
    Blepdroid.getInstance().setCharacteristicToListen(RFDUINO_UUID_RECEIVE);
    
   allSetUp = true;
}

void onBluetoothRSSI(String device, int rssi)
{
  println(" onBluetoothRSSI " + device + " " + Integer.toString(rssi));
}

void onBluetoothConnection( String device, int state)
{
  println(" onBluetoothConnection " + device + " " + state);
  if(state == 2)
  {
    Blepdroid.getInstance().discoverServices();
  }
}

void onCharacteristicChanged(String characteristic, byte[] data)
{
  String dataString = new String(data);
  println(" onCharacteristicChanged " + characteristic + " " + dataString  );
}

void onDescriptorWrite(String characteristic, String data)
{
  println(" onDescriptorWrite " + characteristic + " " + data);
}

void onDescriptorRead(String characteristic, String data)
{
  println(" onDescriptorRead " + characteristic + " " + data);
}

void onCharacteristicRead(String characteristic, byte[] data)
{
  println(" onCharacteristicRead " + characteristic + " " + data);
}

void onCharacteristicWrite(String characteristic, byte[] data)
{
  println(" onCharacteristicWrite " + characteristic + " " + data);
}
