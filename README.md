# Morse Code Communicator
Goal: Morse code input from screen taps is sent over SMS. Made for accessibility for people with
visual impairments.

## Functionality
Short and long taps are displayed on the screen as dots and dashes. Pause to translate a letter.
Swipe to the right to enter a space, and left to backspace. Cover the proximity sensor and enter
the dot-dash sequence for a number to get the corresponding symbol. Phone vibrations and text-to-speech
readback provide feedback to user. Shake the phone twice in a row to store the message, and be
prompted to enter a recipient phone number. Shake twice again to send the message to the phone
number entered. The screen is also cleared to start over with another message.

## Requirements
* Android Studio
* Android phone with SIM card and android 5.1.1 or later
* Allow SMS permission when the app requests it, or in your phone settings.
* Note: vibration feedback only possible on android 8 or later.
* No external hardware, sensors, software libraries, etc. are used. Everything should be available via
Android Studio and the Android phone.

## Download and Run
1. Download and install the latest version of Android Studio from https://developer.android.com/studio/
2. Download/clone this repository
3. Open Android Studio and select the MorseCodeCommunicator folder/project
4. Run the app via Android Studio. You must allow the app SMS permissions.
Instructions for Android Studio USB debugging can be found on on 
https://developer.android.com/training/basics/firstapp/running-app.
You may need to install USB driver, see same link.

## Testing
Tested on Android Studio version 2021.1.1.22, via USB Connection to a Samsung Galaxy S10e running
Android 9.0, a Samsung Galaxy S6 running Android 5.1.1, and a OneNote Plus running Android 9.0.
