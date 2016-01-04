blepdroid
=========

**B**​luetooth **L**​ow **E**​nergy for **P**​rocessing An​**droid**

Right now optimized for RFDuino but potentially possible for any BLE + GATT device. We've tested this library with the following devices: Red Bear BLE Nano, RFDuino, nRF8001 and it should work with the Bluefruit LE UART Friend as well as anything else BLE compatible. You'll want to get service/characteristic IDs to make connecting to a preconfigured device and service easier, or you could always just code up your own service/characteristic device set.

For instance, if you needed to connect to a BLE device that you knew basically nothing about, you'd power up your device, and in your Processing app scan for devices, connect to the device that you're interested in and scan for services and then scan for characteristics.

##Glossary##

Ok, first let's talk terminology.

Device: a device that has a bluetooth transceiver. You can see what a device is called, what its hardware address is, and how strong the signal is.

RSSI: Received signal strength indication, i.e. how strong the signal is

Service: A single device can have multiple services. You can ask a device what services it has or you can just know ahead of time what service you're looking for. These don't send/recieve data, but they encapsulate characteristics, which do send/receive data.

Characteristic: A service has multiple characteristics that can be things like "heartrate" or "voice data" or "RFDUINO_SEND" or whatever else. These are the actual data channels. Most of them are read or write but not both. You hear data received on them by subscribing to the characterstic using setCharacteristicToListen() and then receiving information in the onCharacteristicChanged() callback. Speaking of callbacks:

##Callbacks##

The basic structure is as follows: *pretty much everything happens via calls + callbacks*. That's not my idea, that's Androids idea. It's not a bad idea though, even though it doesn't really mesh with Processing all that well. So, lets list them out:

```java
void onDeviceDiscovered(BlepdroidDevice device)
```

We have run a scan and found a device. BlepdroidDevice looks like this (pretty simple)

```java
	public String name;
	public String address;
	public UUID id;
	public int rssi;
	public byte[] scanRecord;
```

Name is usually something friendly, like "RFDuino" or "Wireless Mouse". Address is the device address that you'll use to connect to it. ID is the ID that the manufacturer put on it, rssi is signal strength (yes, you can use this for trilateration). And scanrecord is...something.

```java
void onServicesDiscovered(ArrayList<String> ids, int status)
```

Every device has a bunch of services. When you connect to a device, you can say "hey, what services do you have?" and the device will tell you. By "tell you" I mean "give you a bunch of 128bit hex values" but that's usually good enough to know whether it supports the thing you're looking for or not.

```java
void onBluetoothRSSI(String device, int rssi)
```

I don't have blepdroid set up yet to do constant RSSI scanning, but when I do, this is where it'll go.


```java
void onBluetoothConnection( String device, int state)
```

When you've actually connected to a device. For state 0x0 is good, anything else is bad.

```java
void onCharacteristicChanged(String characteristic, byte[] data)
```

You subscribed to a characteristic (i.e. a service (confusing, I know) that the device supports) and it has changed. This is usually a sign that some data has been written to your device.

```java
void onDescriptorWrite(String characteristic, String data)
```

You've successfully written to a descriptor

```java
void onDescriptorRead(String characteristic, String data)
```
You've sucessfully read from a descriptor

```java
void onCharacteristicRead(String characteristic, byte[] data)
```

You've successfully read from a characteristic (and got the following data).

```java
void onCharacteristicWrite(String characteristic, byte[] data)
```

You've successfully written `data` to a characteristic.

##Non-callbacks##

To kick all these events off we've got a few other methods:

```java
void scanDevices()
```

This does what it says, but you'll notice that it returns `void` because, again, everything is callbacks. This triggers onDeviceDiscovered()

```java
void connectDevice(String _hwAddress)
```

This is what helps you connect to a device. You can call it with the address of the BluetoothDevice object.

```java
void discoverServices()
```

You've connected to a device and now you want to see what services it has. This triggers onServicesDiscovered.

```java
void connectToService(UUID serviceID )
```

You've found a service that you like and you want to connect to it!

```java
void setCharacteristicToListen(String characteristic);
void setCharacteristicToListen(UUID characteristic);
```

Once you've connected to a service, then you can say that you want to be updated whenever it broadcasts a characteristic notification. This triggers onCharacteristicChanged()

```java
void writeCharacteristic(UUID characteristic, byte[] data);
```

Finally, this sends data to a characteristic that the device+service has.


## Building this ##

You'll need to edit your classpath in the project settings like so:

![alt tag](build_path.png)

After that you can then select the project and then do Export, Jar, and set it to export to your Processing libraries library folder
