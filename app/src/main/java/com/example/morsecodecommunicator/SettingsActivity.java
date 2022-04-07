package com.example.morsecodecommunicator;

import static com.example.morsecodecommunicator.MainActivity.ttsSwitch;

import androidx.appcompat.app.AppCompatActivity;

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
         ttsSwitch = (Switch)findViewById(R.id.switch1);
        ttsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(ttsSwitch.isChecked()){

                    MainActivity.am.setStreamVolume(AudioManager.STREAM_MUSIC,MainActivity.am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
                    Toast toast = Toast.makeText(MainActivity.context,"TTS Enabled",Toast.LENGTH_LONG);
                    toast.show();
                }else{
                    MainActivity.am.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
                    Toast toast = Toast.makeText(MainActivity.context,"TTS Disabled",Toast.LENGTH_LONG);
                    toast.show();
                }
            }
        });
    }


}