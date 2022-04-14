package com.example.morsecodecommunicator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final String MORSETAG = "MORSECODE";    // Tag relating to morse code communication
    private final String SENSORTAG = "SENSOR";      // Tag relating to sensor readings
    private final String SMSTAG = "SMS";            // Tag relating to SMS messaging
    private final String SWIPETAG = "SWIPE";        // Tag relating to swipes
    private SensorManager sensorManager;            // Sensor manager for various methods
    private Boolean isPressed;                      // Keeps track of is finger is pressed down
    private Boolean isSymbol;                       // Whether current entry is a symbol
    private StringBuilder displayText = new StringBuilder();      // Text on screen with message
    private StringBuilder morseLetterText = new StringBuilder();  // Text on screen with current dot-dash entries
    private String message;                         // Store the message to send
    private Map<String, Character> morseChars;      // Dot-dash strings to alphanumeric chars
    private Map<String, Character> morseSpecialChars;  // Dot-dash strings to special chars
    private final Handler handler = new Handler();  // Handler for runnables
    private int singleSpace = 0, singleLetter = 0;
    private long lastTimeOfShake = 0;               // Time of last phone shake
    private float x1, x2, xLength;                  // x position values to determine swipes
    private boolean isPhoneEntry = false;           // Whether or not entry is for phone number
    private Context context;
    public TextToSpeech t1;
    private AudioManager am;

    /* Change this to true if you want to use the volume up button to translate letters
     * Leave as false to use a pause to confirm letters */
    private boolean useVolume = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        /* Create sudio manager */
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (useVolume) {
            /* Set volume to max, since we are using volume buttons for other commands for now */
            am.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                    0);
        }

        /* Create sensor manager */
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        /* Initialize morse code characters */
        morseChars = new HashMap<String, Character>();
        morseSpecialChars = new HashMap<String, Character>();
        initMorseChars();

        /* Initialize text to speech */
        t1 = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                sensorManager.SENSOR_DELAY_NORMAL);
    }

    /* Initialize global mapping of alphanumeric characters to their morse code translations */
    private void initMorseChars() {
        /* Make lists of letters/numbers and their translations */
        String morseAlphanumeric[] = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....",
                "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.",
                "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-",
                "-.--", "--..", ".----", "..---", "...--", "....-", ".....",
                "-....", "--...", "---..", "----.", "-----"};

        Character alphanumeric[] = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
                'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
                'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

        String morseSpecialSyms[] = {".----", "..---", "...--", "....-", ".....",
                "-....", "--...", "---..", "----.", "-----", ".", "-", "-."};

        Character specialSyms[] = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '.', ',', '?'};

        /* Put pairs into the map */
        int i;
        for (i = 0; i < morseAlphanumeric.length; i++) {
            morseChars.put(morseAlphanumeric[i], alphanumeric[i]);
        }

        for (i = 0; i < morseSpecialSyms.length; i++) {
            morseSpecialChars.put(morseSpecialSyms[i], specialSyms[i]);
        }
    }

    /** Runnable thread for when user does a short press (dot) */
    private final Runnable dotRunnable = new Runnable() {
        public void run() {
            TextView morseTextView = findViewById(R.id.morseTextView);
            morseLetterText.append('.');
            morseTextView.setText(morseLetterText);
            Log.i(MORSETAG, "dot");
        }
    };

    /** Runnable thread for when user does a long press (dash) */
    private final Runnable dashRunnable = new Runnable() {
        public void run() {
            TextView morseTextView = findViewById(R.id.morseTextView);
            /* Delete the extra dot that was just added from dot runnable */
            morseLetterText.deleteCharAt(morseLetterText.length()-1);
            morseLetterText.append('-');
            morseTextView.setText(morseLetterText);
            Log.i(MORSETAG, "dash");
        }
    };

    /** Runnable thread for when user pauses with finger up (translate letter) */
    private final Runnable translateLetterRunnable = new Runnable() {
        public void run() {
            /* This method is basically the same as dispatchKeyEvent */
            TextView msgTextView = findViewById(R.id.msgTextView);
            TextView morseTextView = findViewById(R.id.morseTextView);

            if (morseLetterText.length() > 0) {
                if (isSymbol) {   // Symbol
                    if (morseSpecialChars.containsKey(morseLetterText.toString())) {
                        char nextSym = morseSpecialChars.get(morseLetterText.toString());
                        addEntry(nextSym);
                    } else {
                        wrongEntry("Not a valid symbol");
                    }
                } else if (isPhoneEntry) {   // Number (phone number)
                    if (morseChars.containsKey(morseLetterText.toString())) {
                        char nextNum = morseChars.get(morseLetterText.toString());
                        if (nextNum >= '0' && nextNum <= '9') {
                            addEntry(nextNum);
                        } else {
                            wrongEntry("Not a number");
                        }
                    } else {
                        wrongEntry("Not a number");
                    }
                } else {   // Letter
                    if (morseChars.containsKey(morseLetterText.toString())) {
                        char nextLetter = morseChars.get(morseLetterText.toString());
                        addEntry(nextLetter);
                    } else {
                        wrongEntry("Not a letter");
                    }
                }

                msgTextView.setText(displayText);
                morseTextView.setText(morseLetterText);
                morseLetterText.delete(0, morseLetterText.length());
            }
        }
    };

    /** Add a space character to message text */
    private void addSpaceChar() {
            TextView displayTextView = findViewById(R.id.msgTextView);
            TextView morseTextView = findViewById(R.id.morseTextView);
            /* Speak word from last space on before adding the next space char */
            t1.speak(displayText.substring(displayText.toString().lastIndexOf(' ') + 1, displayText.length()),TextToSpeech.QUEUE_FLUSH,null);
            displayText.append(" ");
            // TODO do we need to update display if space? doesnt look any different
            displayTextView.setText(displayText);
            Log.i(MORSETAG, "space");
            /* Also clear morse letter text */
            morseLetterText.delete(0,morseLetterText.length());
            morseTextView.setText(morseLetterText);
    }

    /** Delete latest character from message text */
    private void backspace() {
        if (displayText.length() == 0)
            return;
        TextView displayTextView = findViewById(R.id.msgTextView);
        TextView morseTextView = findViewById(R.id.morseTextView);
        displayText.delete((displayText.length() - 1),displayText.length());
        displayTextView.setText(displayText);
        /* Speak "backspace" */
        t1.speak("backspace",TextToSpeech.QUEUE_FLUSH,null);
        Log.i(MORSETAG, "backspace");
        /* Also clear morse letter text */
        morseLetterText.delete(0,morseLetterText.length());
        morseTextView.setText(morseLetterText);
    }

    /** When user touches screen, determine if it is a short/long click, or left/right swipe */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /* Get variables needed for vibration */
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        final VibrationEffect vibrationEffect;

        switch (event.getAction()) {
            /* Putting finger down */
            case MotionEvent.ACTION_DOWN:
                /* Remove any runnable tranlation callbacks that might be in the queue */
                handler.removeCallbacks(translateLetterRunnable);

                /* Get x position of tap */
                x1 = event.getX();

                /* Start vibration and keep going until finger comes up, but max 2 seconds */
                /* Vibration requires min API 26, so only perform if running 26 or higher */
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrationEffect = VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE);
                    vibrator.vibrate(vibrationEffect);
                }

                /* Execute short click (dot) runnable after 10 ms, long click (dash) after 200 ms */
                handler.postDelayed(dotRunnable, 10);
                handler.postDelayed(dashRunnable, 200);
                isPressed = true;
                break;

            /* Taking finger up/off */
            case MotionEvent.ACTION_UP:
                /* Only if finger was just pressed down */
                if(isPressed) {
                    isPressed = false;

                    /* Get x position of release */
                    x2 = event.getX();

                    /* Vibration requires min API 26, so only need to cancel if running 26 or higher */
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.cancel();
                    }

                    /* Check for swipe right or left */
                    xLength = x2 - x1;
                    if (Math.abs(xLength) > 150) {
                        /* Delete the extra dot or dash that was just added */
                        morseLetterText.append(morseLetterText.substring(0, morseLetterText.length() - 1));
                        Log.i(SWIPETAG, "xLength = " + Float.toString(xLength));
                        /* Left to right swipe */
                        if (x2 > x1) {
                            Log.i(SWIPETAG, "right swipe");
                            addSpaceChar();
                        } else {
                            Log.i(SWIPETAG, "left swipe");
                            backspace();
                        }
                    }

                    /* Remove any runnable dot/dash callbacks that might be in the queue */
                    handler.removeCallbacks(dotRunnable);
                    handler.removeCallbacks(dashRunnable);

                    /* If finger is up for a certain amount of time translate the letter */
                    if (!useVolume) {
                        handler.postDelayed(translateLetterRunnable, 300);
                    }
                }
                break;

        }

        return super.onTouchEvent(event);
    }

    /** Called when accelerometer or proximity reading changes */
    public void onSensorChanged(SensorEvent event) {
        Sensor source = event.sensor;
        if (source.equals(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER))) {
            /* Turn x, y, z acceleration readings into a single metric */
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            double accelSample = Math.sqrt((x * x + y * y + z * z));
            if (accelSample > 25) {
                Log.i(SENSORTAG, "Accel sample = " + Double.toString(accelSample));
                accelerometerRoutine(x, y, z);
            }
        } else if (source.equals(sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY))) {
            proximityRoutine(event.values[0]);
        }
    }

    /** Called when an accelerometer reading changes */
    private void accelerometerRoutine(double x, double y, double z) {
        long currentTimeOfShake = System.currentTimeMillis();
        /* Sleep without accelerometer running so it doesn't send multiple messages */
        sensorManager.unregisterListener(this);
        try {
            Thread.sleep(250);
        } catch (Exception e){
            e.printStackTrace();
        }
        /* Turn accelerometer back on */
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);

        //Log.i(SMSTAG, Long.toString(currentTimeOfShake - lastTimeOfShake));
        if (currentTimeOfShake - lastTimeOfShake < 1000) {
            /* Check if user just entered message, or phone number */
            if (!isPhoneEntry) {
                message = displayText.toString();
                displayText.delete(0,displayText.length());
                t1.speak(message + "... Now, enter recipient phone number",TextToSpeech.QUEUE_FLUSH,null);
                TextView msgTextView = findViewById(R.id.msgTextView);
                msgTextView.setText(displayText);
                isPhoneEntry = true;
            } else {
                StringBuilder speakText = new StringBuilder();
                for (int i = 0; i < displayText.length(); i++) {
                    speakText.append(displayText.charAt(i) + " ");
                }
                t1.speak(speakText.toString(),TextToSpeech.QUEUE_FLUSH,null);
                if (sendSms(displayText.toString()) < 0) {
                    Log.e(SMSTAG, "Error sending sms; requesting permission now");
                    requestSmsPermission();
                }
                isPhoneEntry = false;
            }
        }
        lastTimeOfShake = currentTimeOfShake;
    }

    /** Called when a proximity reading is made */
    private void proximityRoutine(double x) {
        Log.i(SENSORTAG, "Proximity reading = " + Double.toString(x));
        if (morseLetterText.length() == 0 && x < 1) {
            /* Assume that user is entering a special character, for now */
            isSymbol = true;
        } else if (x > 0) {
            /* If proximity reading ever is high (far), it is not a symbol */
            isSymbol = false;
        }
    }

    /** Check if app has SMS sending permissions; if not, request permission
     ** Returns 0 if SMS permission already enabled, -1 if request had to be made */
    private int requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.SEND_SMS},
                    123);
            Log.i(SMSTAG, "Requested SMS permission");
            return -1;
        }
        return 0;
    }

    /** When SMS permission is requested, if it is granted, immediately send the SMS */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 123) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                /* Permission was granted, send SMS message */
                sendSms(message);
            } else {
                /* Permission was denied; print error message for now TODO any other handling? */
                Log.i(SMSTAG, "SMS permission to send messages was denied");
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (useVolume) {
            int action = event.getAction();
            int keyCode = event.getKeyCode();
            TextView msgTextView = findViewById(R.id.msgTextView);
            TextView morseTextView = findViewById(R.id.morseTextView);

            switch (keyCode) {
                /* On volume up press, translate dot-dash to letter/symbol/number */
                case KeyEvent.KEYCODE_VOLUME_UP:
                    if (action == KeyEvent.ACTION_DOWN) {
                        if (singleLetter == 0 && morseLetterText.length() > 0) {
                            if (isSymbol) {   // Symbol
                                if (morseSpecialChars.containsKey(morseLetterText.toString())) {
                                    char nextSym = morseSpecialChars.get(morseLetterText.toString());
                                    addEntry(nextSym);
                                } else {
                                    wrongEntry("Not a valid symbol");
                                }
                            } else if (isPhoneEntry) {   // Number (phone number)
                                if (morseChars.containsKey(morseLetterText.toString())) {
                                    char nextNum = morseChars.get(morseLetterText.toString());
                                    if (nextNum >= '0' && nextNum <= '9') {
                                        addEntry(nextNum);
                                    } else {
                                        wrongEntry("Not a number");
                                    }
                                } else {
                                    wrongEntry("Not a number");
                                }
                            } else {   // Letter
                                if (morseChars.containsKey(morseLetterText.toString())) {
                                    char nextLetter = morseChars.get(morseLetterText.toString());
                                    addEntry(nextLetter);
                                } else {
                                    wrongEntry("Not a letter");
                                }
                            }
                        }
                        singleLetter = 1;
                    }
                    if (action == KeyEvent.ACTION_UP) {
                        singleLetter = 0;
                    }
                    msgTextView.setText(displayText);
                    morseTextView.setText(morseLetterText);
                    morseLetterText.delete(0, morseLetterText.length());
                    return true;
                // TODO delete this commented out code: space now implemented with swipe right
                /* On volume down press, add a space */
                //case KeyEvent.KEYCODE_VOLUME_DOWN:
                //    if (action == KeyEvent.ACTION_DOWN) {
                //        if(singleSpace == 0) {
                //            addSpaceChar();
                //        }
                //        singleSpace = 1;
                //    }
                //    if(action == KeyEvent.ACTION_UP) {
                //        singleSpace = 0;
                //    }
                //    return true;
                default:
                    return super.dispatchKeyEvent(event);
            }
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    /** Add character to translated string and speak it */
    void addEntry(Character c) {
        displayText.append(c);
        t1.speak(c + "", TextToSpeech.QUEUE_FLUSH, null);
        morseLetterText.delete(0, morseLetterText.length());
    }

    /** Notifies user via Toast and TTS of improper entry message, and clears dots/dashes */
    void wrongEntry(String msg) {
        Toast toast = Toast.makeText(context,msg,Toast.LENGTH_SHORT);
        toast.show();
        t1.speak(msg,TextToSpeech.QUEUE_FLUSH,null);
        morseLetterText.delete(0,morseLetterText.length());
    }

    /** Called when user shakes phone, signalling they want to send the message */
    private int sendSms(String phoneNum) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            Log.d("SMSTAG", message);
            smsManager.sendTextMessage(phoneNum,null , message,null,null);
        } catch (Exception e) {
            Log.e(SMSTAG, "Error sending SMS message");
            e.printStackTrace();
            return -1;
        }

        /* Reset strings to empty */
        message = "";
        displayText.delete(0,displayText.length());
        TextView msgTextView = findViewById(R.id.msgTextView);
        msgTextView.setText(displayText.toString());

        Log.i(SMSTAG, "Successfully sent message over SMS");
        return 0;
    }

    /** Required for GestureDetector.OnGestureListener; leave as default */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Do nothing if sensor accuracy changes */
    }

    @Override
    protected void onPause() {
        super.onPause();

        /* Unregister sensor listener */
        sensorManager.unregisterListener(this);
    }

}
