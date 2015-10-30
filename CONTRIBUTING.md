# Contribution Guide

## Development Dependencies

- [Android Studio 1.3.x](http://developer.android.com/tools/studio/index.html)
- Java 7 [(OS X download here)](https://support.apple.com/kb/DL1572?locale=en_US)
- The Android support Maven repository

## Submitting a Change

0. Fork and clone the repo

1. Open the `android-buruberi` project directory in Android Studio

2. Add a JUnit test target for the `buruberi-core` module

3. Synchronize gradle and make sure the tests pass locally

4. Make your changes, adding tests where necessary

5. Ensure all tests pass

6. Commit your changes, writing a [good commit message](http://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html)

7. Push to your fork and submit a pull request

Please be sure to use the `@NonNull`/`@Nullable` annotations on all new code written,
and try to follow existing coding conventions in the project. When in doubt, run the Android Studio
code formatting tool.

## Useful References

- [Project Javadocs](#)
- [Bluetooth Low Energy introduction](http://developer.android.com/guide/topics/connectivity/bluetooth-le.html)
- [`BluetoothManager` reference](http://developer.android.com/reference/android/bluetooth/BluetoothManager.html)
- [`BluetoothAdapter` reference](http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html)
- [`BluetoothLeScanner` reference](http://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner.html)
- [`BluetoothDevice` reference](http://developer.android.com/reference/android/bluetooth/BluetoothDevice.html)
- [`BluetoothGatt` reference](http://developer.android.com/reference/android/bluetooth/BluetoothGatt.html)
- [The Scantastic example app](tree/master/buruberi-example)
