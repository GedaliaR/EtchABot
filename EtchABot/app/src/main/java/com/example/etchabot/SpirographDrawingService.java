package com.example.etchabot;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import io.github.giuseppebrb.ardutooth.Ardutooth;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Thread.sleep;

public class SpirographDrawingService extends Service {

    private static final String TAG = "SpirographService";

    private static final int MAX_Y = 12_000;
    private static final int HALF_MAX_Y = MAX_Y / 2;
    private static final int MAX_X = 16_000;
    private static final int HALF_MAX_X = MAX_X / 2;

    private static int angle;
    private static double kParam; //to be derived from user input
    private static double lParam; //to be derived from user input
    private long x;
    private long y;

    @SuppressLint("StaticFieldLeak")
    private static Ardutooth mArdutooth;
    private Thread drawingThread;
    private static boolean stopped;


    public void setAngle(int angle){
        SpirographDrawingService.angle = angle;
    }

    public void setKParam(double kParam) {
        SpirographDrawingService.kParam = kParam;
    }

    public void setLParam(double lParam) {
        SpirographDrawingService.lParam = lParam;
    }

    public void setArdutooth(Ardutooth mArdutooth) {
        SpirographDrawingService.mArdutooth = mArdutooth;
    }

    /**
     * Plots the next coordinate in tracing the shape of the spirograph.
     * I copied and pasted the math here - its a bit over my head.
     */
    private void calculatePoint() {
        double t = Math.toRadians(angle);
        double fac = (1.0 - kParam) / kParam;
        x = Math.round((HALF_MAX_Y - 20) * ((1 - kParam) * cos(t) + lParam * kParam * cos(fac * t)) + HALF_MAX_X);
        y = Math.round((HALF_MAX_Y - 20) * ((1 - kParam) * sin(t) - lParam * kParam * sin(fac * t)) + HALF_MAX_Y);
        // Increment angle
        angle = angle + 2;
    }

    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void onCreate(){

        drawingThread = new Thread(new Runnable() {

            private boolean secondCommand = true;
            private boolean firstCommand = true;

            long currentX;
            long currentY;

            @Override
            public void run() {
                while (!stopped){

                    currentX = x;
                    currentY = y;

                    calculatePoint(); //update x and y

                    Log.i(TAG, "In Drawing Thread. X = " + x +", Y = " + y);

                    String command = "L " + x + " " + y +";";

                    Log.i(TAG, "Sending Command: " + command);
                    mArdutooth.sendString(command);

                    //need to pause after each instruction to allow motors to run
                    try {
                        if (firstCommand || secondCommand){
                            if (!firstCommand){
                                secondCommand = false;
                            }
                            firstCommand = false;
                            sleep(15000); //the first two commands always take a longer time to execute
                        }
                        sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            });

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        drawingThread.start();

        Log.i(TAG, "Params: k = " + kParam + ", l = " + lParam);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        stopped = true;
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
