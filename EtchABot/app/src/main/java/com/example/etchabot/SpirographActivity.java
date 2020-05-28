package com.example.etchabot;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Random;

import io.github.giuseppebrb.ardutooth.Ardutooth;

import static java.lang.Thread.sleep;

public class SpirographActivity extends AppCompatActivity {

    private static final String TAG = "SpirographActivity";

    private EditText mKParamEditText;
    private EditText mLParamEditText;
    private Button mStartButton;
    private Button mStopButton;

    Random random;

    private Ardutooth mArdutooth;
    private boolean readerThreadIsRunning;

    SpirographDrawingService mDrawingService;
    private boolean isSpirographServiceRunning;

//    @Override
//    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spirograph);

        setupFAB();

        initializeFields();

    }

    private void initializeFields() {
        mArdutooth = Ardutooth.getInstance(this);

        mDrawingService = new SpirographDrawingService();
        mDrawingService.setArdutooth(mArdutooth);

        random = new Random();

        mKParamEditText = findViewById(R.id.k_param);
        mLParamEditText = findViewById(R.id.l_param);
        mStartButton = findViewById(R.id.start_button);
        mStopButton = findViewById(R.id.stop_button);

        getServiceStatus();
    }

    private void getServiceStatus() {
        isSpirographServiceRunning = !mDrawingService.isStopped();

        if (isSpirographServiceRunning){
            mStopButton.setEnabled(true);
            mStartButton.setEnabled(false);
        }else {
            mStopButton.setEnabled(false);
            mStartButton.setEnabled(true);
        }
    }

    public void stopSpirograph(View view) {
        Log.i(TAG, "Stop Button Hit");

        Intent intent = new Intent(this, SpirographDrawingService.class);
        getApplicationContext().stopService(intent);

        isSpirographServiceRunning = false;

        mStartButton.setEnabled(true);
        mStopButton.setEnabled(false);

        mDrawingService.setAngle(0);
    }

    public void startSpirograph(View view) {
        Log.i(TAG, "Start Button Hit");

        mStartButton.setEnabled(false);
        mStopButton.setEnabled(true);

        setParamsForSpirograph();

        mArdutooth.sendString("L 0 0"); //Auto-Home first

        if (!readerThreadIsRunning)
            startReaderThread();

        Intent intent = new Intent(this, SpirographDrawingService.class);
        getApplicationContext().startService(intent);
        
        isSpirographServiceRunning = true;

    }

    private void setParamsForSpirograph() {
        double kParam;
        double lParam;

        if (mKParamEditText.getText().toString().equals("")){
            kParam = random.nextInt(70) + 15;
        } else{
            kParam = Double.parseDouble(mKParamEditText.getText().toString());
        }

        if (mLParamEditText.getText().toString().equals("")){
            lParam = random.nextInt(70) + 15;
        } else{
            lParam = Double.parseDouble(mLParamEditText.getText().toString());
        }

        kParam /= 100;
        lParam /= 100;

        Log.i(TAG, "kParam = " + kParam + ", lParam = " + lParam);

        mDrawingService.setKParam(kParam);
        mDrawingService.setLParam(lParam);
    }

    private void setupFAB() {
        FloatingActionButton fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Auto Homing", Snackbar.LENGTH_LONG).show();
                mArdutooth.sendString("L 0 0;");

                if (!readerThreadIsRunning)
                    startReaderThread();
            }
        });
    }

    private void startReaderThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                readerThreadIsRunning = true;
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

}
