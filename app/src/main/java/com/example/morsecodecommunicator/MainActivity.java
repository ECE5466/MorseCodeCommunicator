package com.example.morsecodecommunicator;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private final String MORSETAG = "MorseCode";    // Tag relating to morse code communication
    private final String SENSORTAG = "ACCEL";       // Tag relating to accelerometer readings
    private SensorManager sensorManager;            // Global sensor manager for various methods
    private Boolean isPressed;                      // Keeps track of is finger is pressed down
    private String displayText = "";                // dots-dash text to display on screen
    private Map<Character, String> morseChars;      // Alphanumeric chars to dot-dash strings
    private final Handler handler = new Handler();  // Handler for runnables

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Create sensor manager */
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        /* Initialize morse code characters */
        morseChars = new HashMap<Character, String>();
        initMorseChars();
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    /* Initialize global mapping of alphanumeric characters to their morse code translations */
    // TODO for now, just use strings of . and -, can change this later if needed
    // TODO this function is not used yet because I haven't written any translation code
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
            morseChars.put(alphanumeric[i], morseAlphanumeric[i]);
        }
    }

    /** Runnable thread for when user does a short press (dot) */
    private final Runnable dotRunnable = new Runnable() {
        public void run() {
            TextView mainTextView = findViewById(R.id.mainTextView);
            displayText = displayText.concat(".");
            mainTextView.setText(displayText);
            Log.i(MORSETAG, "dot");
        }
    };

    /** Runnable thread for when user does along press (dash) */
    private final Runnable dashRunnable = new Runnable() {
        public void run() {
            TextView mainTextView = findViewById(R.id.mainTextView);
            // TODO For now, just delete the extra dot that is added before the dash
            displayText = displayText.substring(0, displayText.length() - 1);
            displayText = displayText.concat("-");
            mainTextView.setText(displayText);
            Log.i(MORSETAG, "dash");
        }
    };

    /** Runnable thread for when user pauses (space) */
    private final Runnable spaceRunnable = new Runnable() {
        public void run() {
            TextView mainTextView = findViewById(R.id.mainTextView);
            displayText = displayText.concat(" ");
            mainTextView.setText(displayText);
            Log.i(MORSETAG, "space");
        }
    };

    /** When user touches screen, determine if it is a short or long click (dot or dash) */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /* Putting finger down */
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            /* Execute short click (dot) runnable after 100 ms */
            // TODO this makes there always be a dot, even when keep holding down for a dash
            // TODO for now, just do correction to delete the extra dot in the dash function
            handler.postDelayed(dotRunnable, 50);
            /* Execute long click (dash) runnable after 1000 ms = 1 second */
            handler.postDelayed(dashRunnable, 260);
            isPressed = true;
        }

        /* Taking finger up/off */
        if(event.getAction() == MotionEvent.ACTION_UP) {
            if(isPressed) {
                isPressed = false;
                handler.removeCallbacks(dotRunnable);
                handler.removeCallbacks(dashRunnable);
                /* If for a certain amount of time, register it as a "space" */
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


        /* If metric is above threshold, clear message */
        // TODO in the future, send the message, don't just clear it
        // TODO can change this number later if needed
        if (accelSample > 35) {
            Log.i(SENSORTAG, Double.toString(accelSample));
            if (sendMessage() < 0)
                Log.e(MORSETAG, "Error sending message after shake");
        }
    }

    /** Called when user shakes phone, signalling they want to send the message */
    private int sendMessage() {
        // TODO for now, just reset the text to empty
        TextView mainTextView = findViewById(R.id.mainTextView);
        displayText = "";
        mainTextView.setText(displayText);
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
    }

}
