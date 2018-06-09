package com.example.cb.testwifiscan.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;

/**
 * 扫描wifi结果 BroadcastReceiver
 * Created by cb on 2018/6/7.
 */

public class WifiScanResultReceiver extends BroadcastReceiver {
    public static final String ACTION_SCAN_WIFI_BROADCAST = WifiManager.SCAN_RESULTS_AVAILABLE_ACTION;

    private WifiScanResultCallback wifiScanResultCallback;

    public WifiScanResultReceiver(WifiScanResultCallback wifiScanResultCallback) {
        this.wifiScanResultCallback = wifiScanResultCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_SCAN_WIFI_BROADCAST.equalsIgnoreCase(intent.getAction()) && wifiScanResultCallback != null) {
            wifiScanResultCallback.onReceiveScanResult();
        }
    }

    /**
     * 收到扫描结果
     */
    public interface WifiScanResultCallback {
        /**
         * wifiScanResultCallback
         */
        void onReceiveScanResult();
    }
}
