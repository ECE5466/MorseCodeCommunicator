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
    private SensorManager sensorManager;            // Global sensor manager for various methods
    private Boolean isPressed;                      // Keeps track of is finger is pressed down
    private String messageText = "";                // Text on screen with message
    private String morseLetterText = "";            // Text on screen with current dot-dash entries
    private Map<String, Character> morseChars;      // Alphanumeric chars to dot-dash strings
    private final Handler handler = new Handler();  // Handler for runnables
    private int singleSpace = 0, singleLetter = 0;
    private long lastTimeOfShake = 0;               // Time of last phone shake
    private Context context;
    public TextToSpeech t1;
    private AudioManager am;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        /* Create sudio manager */
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        /* Set volume to max, since we are using volume buttons for other commands for now */
        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);

        /* Create sensor manager */
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        /* Initialize morse code characters */
        morseChars = new HashMap<String, Character>();
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
    }

    /* Initialize global mapping of alphanumeric characters to their morse code translations */
    private void initMorseChars() {
        /* Make lists of letters/numbers and their translations */
        Character alphanumeric[] = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
                'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
                'y', 'z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

        String morseAlphanumeric[] = {".-", "-...", "-.-.", "-..", ".", "..-.", "--.", "....",
                "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.",
                "--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-",
                "-.--", "--..", ".----", "..---", "...--", "....-", ".....",
                "-....", "--...", "---..", "----.", "-----"};

        /* Put pairs into the map */
        int i;
        for (i = 0; i < 36; i++) {
            morseChars.put(morseAlphanumeric[i], alphanumeric[i]);
        }
    }

    /** Runnable thread for when user does a short press (dot) */
    private final Runnable dotRunnable = new Runnable() {
        public void run() {
            TextView morseTextView = findViewById(R.id.morseTextView);
            morseLetterText = morseLetterText.concat(".");
            morseTextView.setText(morseLetterText);
            Log.i(MORSETAG, "dot");
        }
    };

    /** Runnable thread for when user does along press (dash) */
    private final Runnable dashRunnable = new Runnable() {
        public void run() {
            TextView morseTextView = findViewById(R.id.morseTextView);
            /* Delete the extra dot that was just added from dot runnable */
            morseLetterText = morseLetterText.substring(0, morseLetterText.length() - 1);
            morseLetterText = morseLetterText.concat("-");
            morseTextView.setText(morseLetterText);
            Log.i(MORSETAG, "dash");
        }
    };

    /** Runnable thread for when user pauses (space) */
    private final Runnable spaceRunnable = new Runnable() {
        public void run() {
            TextView msgTextView = findViewById(R.id.msgTextView);
            /* Speak word from last space on before adding the next space char */
            t1.speak(messageText.substring(messageText.lastIndexOf(' ') + 1, messageText.length()),TextToSpeech.QUEUE_FLUSH,null);

            messageText = messageText.concat(" ");
            msgTextView.setText(messageText);

            Log.i(MORSETAG, "space");
        }
    };

    /** When user touches screen, determine if it is a short or long click (dot or dash) */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /* Get variables needed for vibration */
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        final VibrationEffect vibrationEffect;
        /* Putting finger down */
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            /* Start vibration and keep going until finger comes up, but max 2 seconds */
            /* Vibration requires min API 26, so only perform if running 26 or higher */
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrationEffect = VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE);
                vibrator.vibrate(vibrationEffect);
            }

            /* Execute short click (dot) runnable after 10 ms, long click (dash) after 240 ms */
            handler.postDelayed(dotRunnable, 10);
            handler.postDelayed(dashRunnable, 200);
            isPressed = true;
        }

        /* Taking finger up/off */
        if(event.getAction() == MotionEvent.ACTION_UP) {
            if(isPressed) {
                isPressed = false;
                /* Vibration requires min API 26, so only need to cancel if running 26 or higher */
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.cancel();
                }
                /* Now remove any runnable callbacks that might be in the queue, to refresh */
                handler.removeCallbacks(dotRunnable);
                handler.removeCallbacks(dashRunnable);
                /* TODO If for a certain amount of time, register it as a "space"? */
                // TODO can't do this here, because it waits 1200 ms before moving on/getting next touch
                //handler.postDelayed(spaceRunnable, 1200);
            }
        }

        return true;
    }

    /** Called when accelerometer reading changes */
    public void onSensorChanged(SensorEvent event) {
        /* Turn x, y, z acceleration readings into a single metric */
        double x = event.values[0];
        double y = event.values[1];
        double z = event.values[2];
        double accelSample = Math.sqrt((x*x + y*y + z*z));

        /* If metric is above threshold and is second shake, send message and clear screen */
        if (accelSample > 25) {
            Log.i(SENSORTAG, Double.toString(accelSample));
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
                t1.speak(messageText,TextToSpeech.QUEUE_FLUSH,null);
                if (sendSms() < 0) {
                    Log.e(SMSTAG, "Error sending sms after shake; requesting permission now");
                    requestSmsPermission();
                }
            }
            lastTimeOfShake = currentTimeOfShake;
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
            Log.i(SMSTAG, "requested permission");
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
                sendSms();
            } else {
                /* Permission was denied; print error message for now TODO any other handling? */
                Log.i(SMSTAG, "SMS permission to send messages was denied");
            }
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        TextView msgTextView = findViewById(R.id.msgTextView);
        TextView morseTextView2 = findViewById(R.id.morseTextView);

        switch (keyCode) {
            /* On volume up press, translate letter */
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if(singleLetter == 0 && morseLetterText.length() > 0){
                        if(morseChars.containsKey(morseLetterText)) {
                            messageText = messageText + (morseChars.get(morseLetterText));
                            t1.speak(morseChars.get(morseLetterText) + "",TextToSpeech.QUEUE_FLUSH,null);
                            morseLetterText = "";

                        } else{
                            Toast toast = Toast.makeText(context,"Not a letter",Toast.LENGTH_SHORT);
                            toast.show();
                            t1.speak("Not a letter",TextToSpeech.QUEUE_FLUSH,null);
                            morseLetterText = "";
                        }
                    }
                    singleLetter = 1;
                }
                if(action == KeyEvent.ACTION_UP){
                    singleLetter = 0;
                }
                msgTextView.setText(messageText);
                morseTextView2.setText(morseLetterText);
                morseLetterText = "";
                return true;
            /* On volume down press, add a space */
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if(singleSpace == 0) {
                        handler.postDelayed(spaceRunnable, 0);
                    }
                    singleSpace = 1;
                }
                if(action == KeyEvent.ACTION_UP) {
                    handler.removeCallbacks(spaceRunnable);
                    singleSpace = 0;
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }


    /** Called when user shakes phone, signalling they want to send the message */
    private int sendSms() {
        // TODO Eventually want to pass phone number as parameter
        String phoneNum = "8583360273";

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNum,null , messageText,null,null);
        } catch (Exception e) {
            Log.e(SMSTAG, "Error sending SMS message");
            e.printStackTrace();
            return -1;
        }

        /* Reset screen text to empty */
        TextView msgTextView = findViewById(R.id.msgTextView);
        messageText = "";
        msgTextView.setText(messageText);

        Log.i(SMSTAG, "Successfully sent message over SMS");
        return 0;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Do nothing if sensor accuracy changes */
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
            if(t1 !=null){
                t1.stop();
                t1.shutdown();
            }
    }

}
