package com.example.etchabot;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import io.github.giuseppebrb.ardutooth.Ardutooth;

public class MicrostepActivity extends AppCompatActivity {

    Ardutooth mArdutooth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_microstep);

        mArdutooth = Ardutooth.getInstance(this);

    }

    public void microstepUp(View view) {
        String cmd = "M " + 0 + " " + 5 + ";";
        mArdutooth.sendString(cmd);
    }

    public void microstepDown(View view) {
        String cmd = "M " + 0 + " " + -5 + ";";
        mArdutooth.sendString(cmd);
    }

    public void microstepRight(View view) {
        String cmd = "M " + 5 + " " + 0 + ";";
        mArdutooth.sendString(cmd);
    }

    public void microstepLeft(View view) {
        String cmd = "M " + -5 + " " + 0 + ";";
        mArdutooth.sendString(cmd);
    }
}
