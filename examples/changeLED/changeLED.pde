import blepdroid.*;
import com.lannbox.rfduinotest.*;
import android.os.Bundle;
import android.content.Context;
import java.util.UUID;
import android.view.MotionEvent;

public static UUID RFDUINO_UUID_SERVICE = BluetoothHelper.sixteenBitUuid(0x2220);
public static UUID RFDUINO_UUID_RECEIVE = BluetoothHelper.sixteenBitUuid(0x2221);
public static UUID RFDUINO_UUID_SEND = BluetoothHelper.sixteenBitUuid(0x2222);
public static UUID RFDUINO_UUID_DISCONNECT = BluetoothHelper.sixteenBitUuid(0x2223);
public static UUID RFDUINO_UUID_CLIENT_CONFIGURATION = BluetoothHelper.sixteenBitUuid(0x2902);

BlepdroidDevice bdDevice;
Blepdroid blepdroid;

boolean allSetUp = false;
byte[] rgb = {0, 0, 0};

void setup() {
  size(400,400);
  smooth();
  blepdroid = new Blepdroid(this);
}

void draw() {
  background(0);
  fill(255);
}

public boolean surfaceTouchEvent(MotionEvent me) {
  // Number of places on the screen being touched:
  int numPointers = me.getPointerCount();
  
  if(numPointers > 0)
  {
    int y = (int) me.getY(0);
    rgb[0] = (byte) y;
    blepdroid.writeCharacteristic(bdDevice, RFDUINO_UUID_SEND, rgb);
  }
  
  if(numPointers > 1)
  {
    int y = (int) me.getY(1);
    rgb[1] = (byte) y;
    blepdroid.writeCharacteristic(bdDevice, RFDUINO_UUID_SEND, rgb);
  }
  
  if(numPointers > 2)
  {
    int y = (int) me.getY(2);
    rgb[2] = (byte) y;
    blepdroid.writeCharacteristic(bdDevice, RFDUINO_UUID_SEND, rgb);
  }
  
  // If you want the variables for motionX/motionY, mouseX/mouseY etc.
  // to work properly, you'll need to call super.surfaceTouchEvent().
  return super.surfaceTouchEvent(me);
}

void mousePressed()
{ 
  String hi = new String("ffffff");
  blepdroid.writeCharacteristic(bdDevice, RFDUINO_UUID_SEND, hi.getBytes());
}

void onDeviceDiscovered(BlepdroidDevice device)
{
  println("discovered device " + device.name + " address: " + device.address + " rssi: " + device.rssi );
  if(device.name.equals("my device")) // here's where you want to put your own device name  
  {
    if(blepdroid.connectDevice(device))
    {
      bdDevice = device; // now store it for later use
    }
  }
}

void onServicesDiscovered(ArrayList<UUID> ids, int status)
{
  println(" onServicesDiscovered " + ids );
  println(" 0 means ok, anything else means bad " + status);
  
  HashMap<String, ArrayList<String>> servicesAndCharas = blepdroid.findAllServicesCharacteristics(bdDevice);
    println( servicesAndCharas.size() );
    for( String service : servicesAndCharas.keySet())
    {
      print( service + " has " );
      println( servicesAndCharas.get(service));
    }
    blepdroid.setCharacteristicToListen(bdDevice, RFDUINO_UUID_RECEIVE);
    
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
    blepdroid.discoverServices(bdDevice);
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
