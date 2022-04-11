package com.example.morsecodecommunicator;

import static com.example.morsecodecommunicator.MainActivity.ttsSwitch;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ttsSwitch = (Switch) findViewById(R.id.ttsSwitch);
        ttsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(ttsSwitch.isChecked()) {
                    MainActivity.am.setStreamVolume(AudioManager.STREAM_MUSIC,MainActivity.am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
                    Toast toast = Toast.makeText(MainActivity.context,"TTS Enabled",Toast.LENGTH_LONG);
                    toast.show();
                } else {
                    MainActivity.am.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
                    Toast toast = Toast.makeText(MainActivity.context,"TTS Disabled",Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        /* Restore values of switch from saved state */
        boolean bool_tts_switch = preferences.getBoolean("ttsSwitch", true);

        /* Set  the fetched data into the switch */
        ttsSwitch = (Switch) findViewById(R.id.ttsSwitch);
        ttsSwitch.setChecked(bool_tts_switch);
    }

    @Override
    protected void onPause() {
        super.onPause();

        /* Store values between instances in a SharedPreferences data type */
        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();  // Put the values from the UI

        /* Get current state of switch */
        Switch tts_switch = (Switch) findViewById(R.id.ttsSwitch);
        boolean bool_tts_switch = tts_switch.isChecked();

        /* Store the state value in the editor */
        editor.putBoolean("ttsSwitch", bool_tts_switch);

        /* Commit states to SharedPreferences storage */
        editor.commit();
    }


}