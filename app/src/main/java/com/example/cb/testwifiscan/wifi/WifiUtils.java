package com.example.cb.testwifiscan.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by cb on 2018/6/8.
 */

public class WifiUtils {

    /**
     * 获取当前wifi的SSID名称
     *
     * @param context the applicationContext
     *
     * @return current wifi ssid
     */
    public static String getCurrentWifiSSID(Context context) {
        WifiInfo wifiInfo = getCurrentWifi(context);
        if (wifiInfo == null || wifiInfo.getNetworkId() == -1 || wifiInfo.getSSID() == null) {
            return null;
        } else {
            return wifiInfo.getSSID().replace("\"", "");
        }
    }

    /**
     * 获取当前WifiInfo
     *
     * @param context the applicationContext
     *
     * @return current wifi
     */
    public static WifiInfo getCurrentWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager.getConnectionInfo();
    }

    /**
     * wifi是否已成功连接
     * @param wifiManager
     * @param bssid
     * @return
     */
    public static boolean isAlreadyConnected(WifiManager wifiManager, String bssid) {
        if (bssid != null && wifiManager != null) {
            if (wifiManager.getConnectionInfo() != null && wifiManager.getConnectionInfo().getBSSID() != null &&
                    wifiManager.getConnectionInfo().getIpAddress() != 0 &&
                    WifiUtils.equals(bssid, wifiManager.getConnectionInfo().getBSSID())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 尝试重新连接指定wifi
     * @param wifiManager
     * @param scanResult
     * @return
     */
    public static boolean reEnableNetworkIfPossible(@NonNull final WifiManager wifiManager, @Nullable final ScanResult scanResult) {
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || scanResult == null || configurations.isEmpty()) {
            return false;
        }
        boolean result = false;
        for (WifiConfiguration wifiConfig : configurations) {
            if (equals(scanResult.BSSID, wifiConfig.BSSID) && equals(scanResult.SSID, trimQuotes(wifiConfig.SSID))) {
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
                break;
            }
        }
        return result;
    }

    @Nullable
    private static String trimQuotes(@Nullable String str) {
        if (str != null && !str.isEmpty()) {
            return str.replaceAll("^\"*", "").replaceAll("\"*$", "");
        }
        return str;
    }

    /**
     * equals
     * @param a
     * @param b
     * @return
     */
    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }
}
