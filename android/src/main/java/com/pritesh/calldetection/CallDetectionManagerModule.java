package com.pritesh.calldetection;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.util.HashMap;
import java.util.Map;

public class CallDetectionManagerModule
        extends ReactContextBaseJavaModule
        implements Application.ActivityLifecycleCallbacks,
        CallDetectionPhoneStateListener.PhoneCallStateUpdate {

    private boolean wasAppInOffHook = false;
    private boolean wasAppInRinging = false;
    private ReactApplicationContext reactContext;
    private TelephonyManager telephonyManager;
    private CallStateUpdateActionModule jsModule = null;
    private CallDetectionPhoneStateListener callDetectionPhoneStateListener;

    public CallDetectionManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "CallDetectionManagerAndroid";
    }

    @ReactMethod
    public void startListener() {
        telephonyManager = (TelephonyManager) this.reactContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        callDetectionPhoneStateListener = new CallDetectionPhoneStateListener(this);
        telephonyManager.listen(callDetectionPhoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);

    }

    @ReactMethod
    public void stopListener() {
        telephonyManager.listen(callDetectionPhoneStateListener,
                PhoneStateListener.LISTEN_NONE);
        telephonyManager = null;
        callDetectionPhoneStateListener = null;
    }

    @ReactMethod
    public void currentCallStatus(Callback statusCallback) {
      int state = telephonyManager.getCallState();
      String stateLabel = "";
      switch (state) {
          //Hangup
          case TelephonyManager.CALL_STATE_IDLE:
            // Device call state: No activity.
            stateLabel = "Idle";
            break;
          //Outgoing
          case TelephonyManager.CALL_STATE_OFFHOOK:
            //Device call state: Off-hook. At least one call exists that is dialing, active, or on hold, and no calls are ringing or waiting.
            stateLabel = "Offhook";
            break;
          //Incoming
          case TelephonyManager.CALL_STATE_RINGING:
            stateLabel = "Ringing";
            break;
      }
      WritableMap stateMap = new WritableNativeMap();
      stateMap.putString("callState", stateLabel);
      WritableArray stateArray = new WritableNativeArray();
      stateArray.pushMap(stateMap);

      statusCallback.invoke(null, stateArray);
    }

    /**
     * @return a map of constants this module exports to JS. Supports JSON types.
     */
    public
    @Nullable
    Map<String, Object> getConstants() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("Incoming", "Incoming");
        map.put("Offhook", "Offhook");
        map.put("Disconnected", "Disconnected");
        map.put("Missed", "Missed");
        return map;
    }

    // Activity Lifecycle Methods
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceType) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
      if (wasAppInOffHook && jsModule != null) {
        TelephonyManager tManager =
          (TelephonyManager) this.reactContext.getSystemService(Context.TELEPHONY_SERVICE);
        int state = tManager.getCallState();
        Log.d("CallDetectionManager", "Current State is " + state);
        if (state == TelephonyManager.CALL_STATE_IDLE) {
          wasAppInOffHook = false;
          jsModule.callStateUpdated("Disconnected", null);
        }
      }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    public void phoneCallStateUpdated(int state, String phoneNumber) {
        jsModule = this.reactContext.getJSModule(CallStateUpdateActionModule.class);
        Log.d("CallDetectionManager", "The state is " + state);
        switch (state) {
            //Hangup
            case TelephonyManager.CALL_STATE_IDLE:
                if(wasAppInOffHook == true) {
                  jsModule.callStateUpdated("Disconnected", phoneNumber);
                } else if(wasAppInRinging == true) {
                  jsModule.callStateUpdated("Missed", phoneNumber);
                }
                //reset device state
                wasAppInRinging = false;
                wasAppInOffHook = false;
                break;
            //Outgoing
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Device call state: Off-hook. At least one call exists that is dialing, active, or on hold, and no calls are ringing or waiting.
                wasAppInOffHook = true;
                jsModule.callStateUpdated("Offhook", phoneNumber);
                break;
            //Incoming
            case TelephonyManager.CALL_STATE_RINGING:
                // Device call state: Ringing. A new call arrived and is ringing or waiting. In the latter case, another call is already active.
                wasAppInRinging = true;
                jsModule.callStateUpdated("Incoming", phoneNumber);
                break;
            default:
              Log.d("CallDetectionManager", "Unknown State " + Integer.toString(state));
              break;
        }
    }
}
