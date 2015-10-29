# ブルーベリー (burūberī)

Less flaky Bluetooth Low Energy for Android.

# Introduction

Working with Bluetooth Low Energy on Android can be a real pain. Major quality
differences between manufacturers, mysterious undocumented error codes, bugs
introduced between OS releases... These are not the things dreams are made of.
Buruberi is a small library that wraps the Android Bluetooth Low Energy APIs,
and tries to insulate you from as many of these problems as it can. When it
can't insulate you from a problem, it does its best to provide mitigation tools.

# TODO

- Clean up implementation of characteristic writes.
- Clean up implementation of descriptor notification control.
- Sort out generating jars for distribution.
- Sort out maven hosting.

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
