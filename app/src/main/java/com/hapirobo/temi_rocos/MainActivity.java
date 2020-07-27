package com.hapirobo.temi_rocos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.robotemi.sdk.BatteryData;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import io.rocos.agent.sdk.AgentSdk;
import io.rocos.agent.sdk.models.SdkConfig;

public class MainActivity extends AppCompatActivity implements
        OnRobotReadyListener {
    private static final String TAG = "MAIN";

    private static final String BATTERY_TOPIC = "/battery";

    private static Robot sRobot;
    private static Handler sHandler = new Handler();

    private Runnable batteryPublisher = new Runnable() {
        @Override
        public void run() {
            Log.i(TAG, "Publish battery status");
            sHandler.postDelayed(this, 3000);

            try {
                MainActivity.this.robotPublishBatteryStatus();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    };

    //----------------------------------------------------------------------------------------------
    // ACTIVITY LIFE CYCLE METHODS
    //----------------------------------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize Rocos agent
        // Agent SDK is a singleton instance, only need to initialize once per application
        SdkConfig sdkConfig = new SdkConfig()
                .setAgentAddress("127.0.0.1")
                .setAgentPort(9000);
        AgentSdk.getInstance().setConfig(sdkConfig).init();

        // initialize robot
        sRobot = Robot.getInstance();

        // run publisher
        sHandler.post(batteryPublisher);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Robot.getInstance().addOnRobotReadyListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Robot.getInstance().removeOnRobotReadyListener(this);
    }

    //----------------------------------------------------------------------------------------------
    // ROBOT EVENT LISTENERS
    //----------------------------------------------------------------------------------------------
    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                sRobot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    // PUBLISHERS
    //----------------------------------------------------------------------------------------------
    public void robotPublishBatteryStatus() throws IllegalAccessException {
        BatteryData batteryData = sRobot.getBatteryData();
        if (batteryData != null) {
            AgentSdk.getInstance().publishSource(BATTERY_TOPIC, batteryData);
        }
    }
}
