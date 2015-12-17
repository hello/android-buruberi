# ブルーベリー (burūberī)

Less flaky Bluetooth Low Energy for Android.

# Introduction

Working with Bluetooth Low Energy on Android can be a real pain. Major quality
differences between manufacturers, mysterious undocumented error codes, bugs
introduced between OS releases... These are not the things dreams are made of.
Buruberi is a small library that wraps the Android Bluetooth Low Energy APIs,
and tries to insulate you from as many of these problems as it can.

# Using Burūberī

## Creating a Stack

All interaction with Bluetooth through burūberī happens through an instance of the `BluetoothStack`
class. `BluetoothStack`s are created through the `Buruberi` builder class.

```java
final BluetoothStack bluetoothStack = new Buruberi()
        .setApplicationContext(context)
        .build();
```

The `Buruberi` class will choose an appropriate implementation of `BluetoothStack` for the
current runtime environment. The returned `BluetoothStack` should be stored as a singleton value in
your application, and shared between all Bluetooth code. Creating multiple `BluetoothStack` objects
may result in unexpected behavior.

## Scanning for Peripherals

Bluetooth Low Energy peripherals are discovered by passing a `PeripheralCriteria` object to a
`BluetoothStack`. `PeripheralCriteria` allows you to scan for peripherals by MAC address, by
advertising data, or a combination of both.

```java
final PeripheralCriteria criteria = new PeripheralCriteria();
criteria.setLimit(5);
criteria.setDuration(15 * 1000L);

final Observable<List<GattPeripheral>> discover = bluetoothStack.discoverPeripherals(criteria);
discover.subscribe(peripherals -> {
   Log.i("Discover", "Found peripherals " + peripherals);
},
error -> {
   Log.e("Discover", "Could not scan for peripherals.", error);
});
```

## Connecting and Bonding

After you‘ve completed a scan for your intended Bluetooth Low Energy peripheral, you can connect to
it, and manage bonds. _Note: on many phones running Lollipop, you cannot create a new bond once the
phone has created a gatt connection to the peripheral._

```java

final OperationTimeout timeout = peripheral.createOperationTimeout("Connect", 30, TimeUnit.SECONDS);
final Observable<GattPeripheral> connect = peripheral.connect(timeout);
connect.subscribe(p -> {
    Log.i("Connect", "Connected to peripheral " + p);
}, error -> {
    Log.e("Connect", "Could not connect to peripheral.", error);
});

final Observable<GattPeripheral> bond = peripheral.createBond();
bond.subscribe(p -> {
    Log.i("Bond", "Created bond with peripheral " + p);
}, error -> {
    Log.e("Bond", "Could not create bond with peripheral.", error);
});
```

## Using Services

Once you‘ve connected to a peripheral, you can perform service discovery on it.

```java
final OperationTimeout timeout = peripheral.createOperationTimeout("Services", 30, TimeUnit.SECONDS);
final Observable<Map<UUID, PeripheralService>> services = peripheral.discoverServices(timeout);
services.subscribe(allServices -> {
    Log.i("Services", "Discovered services " + allServices);
}, error -> {
    Log.e("Services", "Could not discover services", error);
});
```

## Writing to Characteristics

After you‘ve connected to a peripheral, and performed service discovery on it, you can write to
characteristics.

```java
final GattCharacteristic characteristic = service.getCharacteristic(MY_CHARACTERISTIC);
final OperationTimeout timeout = peripheral.createOperationTimeout("Write", 30, TimeUnit.SECONDS);
final byte[] payload = { 0x4, 0x2 };
final Observable<Void> write = characteristic.write(GattPeripheral.WriteType.DEFAULT, payload, timeout);
write.subscribe(ignored -> {
    Log.i("Write", "Wrote to characteristic " + characteristic.getUuid());
}, error -> {
    Log.e("Write", "Could not write to characteristic " + characteristic.getUuid(), error);
});
```

## Processing Packets

Incoming packets from characteristics are routed through a user-defined `PacketHandler` object. From
the packet handler, your client code can parse and route the packets as appropriate for your
peripheral.

```java
final PacketHandler printingHandler = new PacketHandler() {
    @Override
    public boolean processIncomingPacket(@NonNull UUID characteristicIdentifier, 
                                         @NonNull byte[] payload) {
        Log.i("Packets", "Got payload " + Bytes.toString(payload));
        return true;
    }

    @Override
    public void transportDisconnected() {
        Log.i("Packets", "Peripheral disconnected");
    }
};

peripheral.setPacketHandler(printingHandler);

final GattCharacteristic characteristic = service.getCharacteristic(MY_CHARACTERISTIC);
final OperationTimeout timeout = peripheral.createOperationTimeout("Enable", 30, TimeUnit.SECONDS);
final Observable<UUID> notify = characteristic.enableNotification(MY_DATA_DESCRIPTOR, timeout);
notify.subscribe(descriptorId -> {
    Log.i("Write", "Enabled notification for descriptor " + descriptorId);
}, error -> {
    Log.e("Write", "Could not enable notifications for descriptor", error);
});
```

# Download

## Gradle

```groovy
dependencies {
    compile 'is.hello:buruberi-core:0.9.0'
    testCompile 'is.hello:buruberi-testing:0.9.0'
}
```

Make sure that your project's root `build.gradle` file contains the `jcenter()` repository.

## Jar

Get it on the [releases](https://github.com/hello/android-buruberi/releases) page.

_Does not include convenience resources._

# Contributing

If you'd like to contribute to `android-buruberi`, fork the project on GitHub, and submit a pull
request with your changes. Please be sure to include unit tests for any changes you make, and
follow the coding style of the project as closely as possible. The full contribution guidelines
can be found [here](https://github.com/hello/android-buruberi/blob/master/CONTRIBUTING.md).

# License

	Copyright 2015 Hello Inc.
	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
