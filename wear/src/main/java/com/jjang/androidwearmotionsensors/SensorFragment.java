package com.jjang.androidwearmotionsensors;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.*;
import java.io.PrintWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class SensorFragment extends Fragment implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private static final String COUNT_PATH = "/count";
    private static final String COUNT_KEY = "count";
    private DataClient mDataClient;

    private View mView;
    private TextView mTextTitle;
    private TextView mTextValues;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;
    private long mShakeTime = 0;
    private long mRotationTime = 0;
    private int count = 0;

    private int slTime = 0;
    private int mvTime = 0;

    private double avgAccInSecond = 0;
    private int currSecond = -1;
    private float runningSum = 0;
    private float numOfSensorEvents = 0;

    public static SensorFragment newInstance(int sensorType) {
        SensorFragment f = new SensorFragment();

        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);

        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if(args != null) {
            mSensorType = args.getInt("sensorType");
        }

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(mSensorType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.sensor, container, false);

        mTextTitle = (TextView) mView.findViewById(R.id.text_title);
        mTextTitle.setText("Data Harmony");
        mTextValues = (TextView) mView.findViewById(R.id.text_values);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
        {
            return;
        }

//        getPhysicalActivityLevel(event.values[0], event.values[1], event.values[2]);
        int slHours = this.slTime / 3600;
        int slMinutes = this.slTime / 60 - 60*slHours;
        int slSeconds = this.slTime % 60;

        int mvHours = this.mvTime / 3600;
        int mvMinutes = this.mvTime / 60 - 60*mvHours;
        int mvSeconds = this.mvTime % 60;

        mTextValues.setText(
                "sedentary: " + Integer.toString(slHours) + ":" + Integer.toString(slMinutes) + ":" + Integer.toString(slSeconds) + "\n" +
                "mod-vig: " + Integer.toString(mvHours) + ":" + Integer.toString(mvMinutes) + ":" + Integer.toString(mvSeconds) + "\n" +
                "x = " + Float.toString(event.values[0]) + "\n" +
                "y = " + Float.toString(event.values[1]) + "\n" +
                "z = " + Float.toString(event.values[2]) + "\n"
        );

        java.util.Date date = new java.util.Date();


        updateSecondAvgAcceleration(event);
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);

        }
        else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            detectRotation(event);
        }
    }

    public void getPhysicalActivityLevel(Float avgAcceleration) {
        float THRESHOLD_VALUE = 3.0f;

        float MET = -0.0433743911409f*avgAcceleration*avgAcceleration + 2.44176517041f * avgAcceleration - 17.30153958f;

        if (MET > THRESHOLD_VALUE) {
            this.mvTime += 1;
        } else {
            this.slTime += 1;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            float gForce = FloatMath.sqrt(gX*gX + gY*gY + gZ*gZ);

            if(gForce > SHAKE_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(66, 69, 244));
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();

        if((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            if(Math.abs(event.values[0]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[1]) > ROTATION_THRESHOLD ||
               Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
            }
            else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void updateSecondAvgAcceleration(SensorEvent event) {
        Float x = event.values[0];
        Float y = event.values[1];
        Float z = event.values[2];

        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        Date today = Calendar.getInstance().getTime();
        String reportDate = df.format(today);

        String seconds = reportDate.split(" ")[1].split(":")[2];
        int secondsNum = Integer.parseInt(seconds);

        if (this.currSecond == secondsNum) {
            this.runningSum += this.getManitude(x, y, z);
            this.numOfSensorEvents += 1;
        } else {
            // update time
            Float avgAcceleration = this.runningSum / this.numOfSensorEvents;
            getPhysicalActivityLevel(avgAcceleration);

            // reset counts
            this.currSecond = secondsNum;
            this.runningSum = this.getManitude(x, y, z);
            this.numOfSensorEvents = 1;
        }

    }

    public Float getManitude(Float x, Float y, Float z) {
        return FloatMath.sqrt(x*x + y*y + z*z);
    }
}
