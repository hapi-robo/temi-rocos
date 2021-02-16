package com.hapirobo.temi_rocos;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.robotemi.sdk.BatteryData;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.UserInfo;
import com.robotemi.sdk.listeners.OnRobotReadyListener;

import java.util.List;

import io.rocos.agent.sdk.AgentSdk;
import io.rocos.agent.sdk.models.SdkConfig;

public class MainActivity extends AppCompatActivity implements
        OnRobotReadyListener {
    private static final String TAG = "MAIN";

    private static final String TOPIC_INFO = "/info";
    private static final String TOPIC_BATTERY = "/battery";
    private static final String TOPIC_TELEMETRY = "/telemetry";

    private static Robot sRobot;
    private static Handler sHandler = new Handler();

    private Runnable dataPublisher = new Runnable() {
        @Override
        public void run() {
            sHandler.postDelayed(this, 1000);

            try {
                MainActivity.this.robotPublishData();
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
        sHandler.post(dataPublisher);
    }

    public static class RobotInfo {
        String serialNumber;
        String launcherVersion;
        String roboxVersion;
        List<String> waypointList;
    }

    public static class RobotTelemetry {
        int sensor1;
        int sensor2;
        int sensor3;
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

            // get all contacts
            UserInfo admin = sRobot.getAdminInfo();
            Log.i(TAG, admin.getName());
            Log.i(TAG, admin.getUserId());
            Log.i(TAG, admin.getPicUrl());
            Log.i(TAG, String.valueOf(admin.getRole()));
        }
    }

    //----------------------------------------------------------------------------------------------
    // PUBLISHERS
    //----------------------------------------------------------------------------------------------
    public void robotPublishData() throws IllegalAccessException {
        Log.i(TAG, "Publish robot data");

        RobotInfo info = new RobotInfo();
        info.serialNumber = sRobot.getSerialNumber();
        info.launcherVersion = sRobot.getLauncherVersion();
        info.roboxVersion = sRobot.getRoboxVersion();
        info.waypointList = sRobot.getLocations();
        AgentSdk.getInstance().publishSource(TOPIC_INFO, info);

        BatteryData battery = sRobot.getBatteryData();
        if (battery != null) {
            AgentSdk.getInstance().publishSource(TOPIC_BATTERY, battery);
        }

        RobotTelemetry telem = new RobotTelemetry();
        telem.sensor1 = 1;
        telem.sensor2 = 2;
//        AgentSdk.getInstance().publishSource(TOPIC_TELEMETRY, telem);
    }
}
