package com.pritesh.calldetection;

import com.facebook.react.bridge.JavaScriptModule;

import com.facebook.react.bridge.WritableMap;

public interface CallStateUpdateActionModule extends JavaScriptModule {
    void callStateUpdated(String state, WritableMap phoneNumber);
}
