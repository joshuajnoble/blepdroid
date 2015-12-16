import blepdroid.*;
import com.lannbox.rfduinotest.*;
import android.os.Bundle;
import android.content.Context;
import java.util.UUID;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public static UUID RFDUINO_UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
public static UUID RFDUINO_UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
public static UUID RFDUINO_UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
public static UUID RFDUINO_UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
public static UUID RFDUINO_UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

boolean allSetUp = false;

PVector accelerator;

void setup() {
  size(600, 800, P3D);
  
  Blepdroid.initialize(  this);
  
  accelerator = new PVector();
  accelerator.x = width/2;
  accelerator.y = height/2;
}

void draw() {
  background(0);
  fill(255);

  noStroke();
  translate(accelerator.x, accelerator.y, accelerator.z);
  //pointLight(255, 255, 0, 50, 200, 40);
  //pointLight(255, 0, 255, 500, 700, 40);
  sphere(100);
}

void mousePressed()
{
}

void onDeviceDiscovered(BlepdroidDevice device)
{
  println("discovered device " + device.name + " address: " + device.address + " rssi: " + device.rssi );
  if (device.name.equals("Distinct Name"))
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
  for ( String service : servicesAndCharas.keySet ())
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
  if (state == 2)
  {
    Blepdroid.getInstance().discoverServices();
  }
}

void onCharacteristicChanged(String characteristic, byte[] data)
{
  
  println( data );
  
  String dataString = new String(data);
  // get each set of two bytes
  byte[] xb = new byte[2];
  byte[] yb = new byte[2];
  byte[] zb = new byte[2];

  System.arraycopy( data, 0, xb, 0, 2 );
  System.arraycopy( data, 2, yb, 0, 2 );
  System.arraycopy( data, 4, zb, 0, 2 );

  short x = ByteBuffer.wrap(xb).order(ByteOrder.LITTLE_ENDIAN).getShort();
  short y = ByteBuffer.wrap(yb).order(ByteOrder.LITTLE_ENDIAN).getShort();
  short z = ByteBuffer.wrap(zb).order(ByteOrder.LITTLE_ENDIAN).getShort();
  
  accelerator.x += (x/4);
  accelerator.y += (y/4);
  accelerator.z += (z/4);
 
  accelerator.x = constrain(accelerator.x, 0, 600);
  accelerator.y = constrain(accelerator.y, 0, 800);
  accelerator.z = constrain(accelerator.z, -100, 300);
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

