# Morse Code Communicator
Goal: Morse code input from screen taps is sent over SMS. Made for accessibility for people with
visual impairments.

## Current status
Short and long taps are displayed on the screen as dots and dashes. Pause to translate a letter.
Swipe to the right to enter a space, and left to backspace. Phone vibrations and text-to-speech
readback provide feedback to user. Shake the phone twice in a row to store the message, and be
prompted to enter a recipient phone number. Shake twice again to send the message to the phone
number entered. The screen is also cleared to start over with another message.

## Requirements
* Android Studio
* Android device with android 5.1.1 or later
* Note: vibration feedback only possible on android 8 or later.

## Download and Run
1. Download and install the latest version of Android Studio from https://developer.android.com/studio/
2. Download and install git
3. Clone this git repository to your computer
4. Open Android Studio and select the MorseCodeCommunicator folder/project
5. Run the app according to the instructions on https://developer.android.com/training/basics/firstapp/running-app.
You may need to install USB driver, see same link.

## Testing
Tested on Android Studio version 2021.1.1.22, via USB Connection to a Samsung Galaxy S10e running
Android 9.0, and a Samsung Galaxy S6 running Android 5.1.1.
