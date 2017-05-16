package com.github.yamill.orientation;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.provider.Settings;
import android.view.Display;
import android.view.OrientationEventListener;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import android.content.res.Configuration;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.util.Log;

public class OrientationModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    final OrientationEventListener mOrientationEventListener;
    private String mOrientation;
    private String mSpecificOrientation;
    private boolean mLocked = false;

    final private String[] mOrientations;

    private boolean mHostActive = false;

    public static final String LANDSCAPE = "LANDSCAPE";
    public static final String LANDSCAPE_LEFT = "LANDSCAPE-LEFT";
    public static final String LANDSCAPE_RIGHT = "LANDSCAPE-RIGHT";
    public static final String PORTRAIT = "PORTRAIT";
    public static final String PORTRAIT_UPSIDEDOWN = "PORTRAITUPSIDEDOWN";
    public static final String ORIENTATION_UNKNOWN = "UNKNOWN";

    private static final int ACTIVE_SECTOR_SIZE = 45;
    private final String[] ORIENTATIONS_PORTRAIT_DEVICE = {PORTRAIT, LANDSCAPE_RIGHT, PORTRAIT_UPSIDEDOWN, LANDSCAPE_LEFT};
    private final String[] ORIENTATIONS_LANDSCAPE_DEVICE = {LANDSCAPE_LEFT, PORTRAIT, LANDSCAPE_RIGHT, PORTRAIT_UPSIDEDOWN};

    public OrientationModule(ReactApplicationContext reactContext) {
        super(reactContext);
        final ReactApplicationContext ctx = reactContext;

        mOrientations = isLandscapeDevice() ? ORIENTATIONS_LANDSCAPE_DEVICE : ORIENTATIONS_PORTRAIT_DEVICE;

        if((ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)) {
            mOrientation = PORTRAIT;
            mSpecificOrientation = PORTRAIT;
        }else if(ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mOrientation = LANDSCAPE;
            mSpecificOrientation = LANDSCAPE;
        }
        mOrientationEventListener = new OrientationEventListener(reactContext,
                SensorManager.SENSOR_DELAY_UI) {
            @Override
            public void onOrientationChanged(int orientationValue) {
                if (isDeviceOrientationLocked() || !ctx.hasActiveCatalystInstance())
                    return;
                
                if (mOrientation != null && mSpecificOrientation != null) {
                    final int halfSector = ACTIVE_SECTOR_SIZE / 2;
                    if ((orientationValue % 90) > halfSector
                            && (orientationValue % 90) < (90 - halfSector)) {
                        return;
                    }
                }

                final String orientation = getOrientationString(orientationValue);
                final String specificOrientation = getSpecificOrientationString(orientationValue);

                final DeviceEventManagerModule.RCTDeviceEventEmitter deviceEventEmitter = ctx.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
                if(!mLocked) {
                    if (!orientation.equals(mOrientation)) {
                        mOrientation = orientation;
                        WritableMap params = Arguments.createMap();
                        params.putString("orientation", orientation);
                        deviceEventEmitter.emit("orientationDidChange", params);
                    }

                    if (!specificOrientation.equals(mSpecificOrientation)) {
                        mSpecificOrientation = specificOrientation;
                        WritableMap params = Arguments.createMap();
                        params.putString("specificOrientation", specificOrientation);
                        deviceEventEmitter.emit("specificOrientationDidChange", params);
                    }
                } else {
                    
                    if (mOrientation != null && mSpecificOrientation != null) {
                        final int halfSector = ACTIVE_SECTOR_SIZE / 2;
                        if ((orientationValue % 90) > halfSector && (orientationValue % 90) < (90 - halfSector)) {
                            return;
                        }
                    }
                    final String lockedOrientation = getOrientationString(orientationValue);
                    
                    if(lockedOrientation == LANDSCAPE && specificOrientation.equals(mSpecificOrientation)){
                        unlockAllOrientations();
                        mOrientation = orientation;
                        mSpecificOrientation = specificOrientation;
                    }
                }

            }
        };
        ctx.addLifecycleEventListener(this);

        if (mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
    }

    @Override
    public String getName() {
        return "Orientation";
    }

    @ReactMethod
    public void getOrientation(Callback callback) {
        callback.invoke(null, mOrientation);
    }

    @ReactMethod
    public void getSpecificOrientation(Callback callback) {
        callback.invoke(null, mSpecificOrientation);
    }


    @ReactMethod
    public void lockToPortrait() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        mLocked = true;
        mSpecificOrientation = PORTRAIT;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @ReactMethod
    public void lockToLandscape() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        mLocked = true;
        mSpecificOrientation = LANDSCAPE;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeLeft() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        mLocked = true;
        mSpecificOrientation = LANDSCAPE_LEFT;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @ReactMethod
    public void lockToLandscapeRight() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        mLocked = true;
        mSpecificOrientation = LANDSCAPE_RIGHT;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
    }

    @ReactMethod
    public void unlockAllOrientations() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }
        mLocked = false;
        mSpecificOrientation = null;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    @Override
    public
    @Nullable
    Map<String, Object> getConstants() {
        HashMap<String, Object> constants = new HashMap<String, Object>();
        constants.put("initialOrientation", mOrientation);
        return constants;
    }

    private boolean isDeviceOrientationLocked() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return false;
        }
        return Settings.System.getInt(
                activity.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0
        ) == 0;
    }

    private boolean isLandscapeDevice() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return false;
        }

        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x > size.y;
    }

    private String getSpecificOrientationString(int orientationValue) {



        if (orientationValue < 0) {
            return PORTRAIT; // maybe need ORIENTATION_UNKNOWN
        }

        final int index = (int) ((float) orientationValue / 90.0 + 0.5) % 4;
        return mOrientations[index];
    }

    private String getOrientationString(int orientationValue) {
        final String specificOrientation = getSpecificOrientationString(orientationValue);
        switch (specificOrientation) {
            case LANDSCAPE_LEFT:
            case LANDSCAPE_RIGHT:
                return LANDSCAPE;
            case PORTRAIT:
            case PORTRAIT_UPSIDEDOWN:
                return PORTRAIT;
            default:
                return ORIENTATION_UNKNOWN;
        }
    }

    @Override
    public void onHostResume() {
        mHostActive = true;
    }

    @Override
    public void onHostPause() {
        mHostActive = false;
    }

    @Override
    public void onHostDestroy() {
        mHostActive = false;
    }
}
