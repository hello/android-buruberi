# ブルーベリー (burūberī)

Less flaky Bluetooth Low Energy for Android.

# Introduction

Working with Bluetooth Low Energy on Android can be a real pain. Major quality
differences between manufacturers, mysterious undocumented error codes, bugs
introduced between OS releases... These are not the things dreams are made of.
Buruberi is a small library that wraps the Android Bluetooth Low Energy APIs,
and tries to insulate you from as many of these problems as it can.

# Using AnimeAndroidGo99

## Scanning for Peripherals

## Connecting and Bonding

## Using Services

## Writing to Characteristics

## Processing Packets

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

Get it on the [releases](https://github.com/hello/anime-android-go-99/releases) page.

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
