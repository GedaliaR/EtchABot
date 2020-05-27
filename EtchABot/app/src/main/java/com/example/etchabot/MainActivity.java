package com.example.etchabot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import io.github.controlwear.virtual.joystick.android.JoystickView;
import io.github.giuseppebrb.ardutooth.Ardutooth;

import static com.example.etchabot.Utils.showInfoDialog;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int SPEED_COEFFICIENT = 5;

    private TextView mAngleTextView;
    private TextView mStrengthTextView;
    private Ardutooth mArdutooth;
    private boolean threadIsRunning;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupToolbar();
        setupFAB();

        mArdutooth = Ardutooth.getInstance(this);

        mAngleTextView = (TextView) findViewById(R.id.textView_angle);
        mStrengthTextView = (TextView) findViewById(R.id.textView_strength);

        mArdutooth.setConnection();
        setupJoystick();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }


    private void setupJoystick() {
        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {

                //Make strength a scale of 10 - 100 by tens only
//                strength /= 10;
//                strength *= 10;

                mAngleTextView.setText(angle + "°");
                mStrengthTextView.setText(strength + "%");

                double angleRadians = Math.toRadians(angle);

                int xSpeed = (int) (Math.cos(angleRadians) * strength * SPEED_COEFFICIENT);
                int ySpeed = (int) (Math.sin(angleRadians) * strength * SPEED_COEFFICIENT);

                String command = ("M " + xSpeed + " " + ySpeed + ";");

                Log.i(TAG,command);
                mArdutooth.sendString(command);

                if (!threadIsRunning)
                    startReaderThread();
            }
        }, 500);
    }

    private void startReaderThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                threadIsRunning = true;
                Log.i(TAG, "Reader Thread was created");
                while (mArdutooth.isConnected()) {
                    if (mArdutooth.isReady()) {
                        Log.i(TAG, "Arduino says" + mArdutooth.receiveLine());
                    }
                }
            }
        });
        thread.start();
    }


    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Auto Homing", Snackbar.LENGTH_LONG).show();
                mArdutooth.sendString("L 0 0;");

                if (!threadIsRunning)
                    startReaderThread();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case (R.id.action_microstep): {
                //stuf
                break;
            }
            case (R.id.action_spirograph): {
                Intent intent = new Intent(getApplicationContext(), SpirographActivity.class);
                startActivityForResult(intent, 0);
                return true;
            }
            case (R.id.action_about): {
                showAbout();
                return true;
            }
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAbout() {
        showInfoDialog(this, R.string.app_name, R.string.about_message);
    }

}
