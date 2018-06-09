package com.example.cb.testwifiscan.connect;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.example.cb.testwifiscan.wifi.WifiUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by cb on 2018/6/8.
 */

public class ConnectUtils {

    private static final String TAG = "ConnectUtils";

    private static final int MAX_PRIORITY = 99999;

    /**
     * 连接wifi
     * @param context
     * @param wifiManager
     * @param scanResult
     * @param password
     * @return
     */
    public static boolean connectToWifi(@NonNull Context context, @NonNull WifiManager wifiManager,
                                        @NonNull ScanResult scanResult, @NonNull String password) {
        WifiConfiguration config = ConfigSecurities.getWifiConfiguration(wifiManager, scanResult);
        if (config != null && password.isEmpty()) {
            wifiLog("PASSWORD WAS EMPTY. TRYING TO CONNECT TO EXISTING NETWORK CONFIGURATION");
            return connectToConfiguredNetwork(wifiManager, config, true);
        }

        if (!cleanPreviousConfiguration(wifiManager, config)) {
            wifiLog("COULDN'T REMOVE PREVIOUS CONFIG, CONNECTING TO EXISTING ONE");
            return connectToConfiguredNetwork(wifiManager, config, true);
        }

        final String security = ConfigSecurities.getSecurity(scanResult);

        if (WifiUtils.equals(ConfigSecurities.SECURITY_NONE, security)) {
            checkForExcessOpenNetworkAndSave(context.getContentResolver(), wifiManager);
        }

        config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        config.BSSID = scanResult.BSSID;
        ConfigSecurities.setupSecurity(config, security, password);

        int id = wifiManager.addNetwork(config);
        wifiLog("Network ID: " + id);
        if (id == -1) {
            return false;
        }

        if (!wifiManager.saveConfiguration()) {
            wifiLog("Couldn't save wifi config");
            return false;
        }
        // We have to retrieve the WifiConfiguration after save
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        if (config == null) {
            wifiLog("Error getting wifi config after save. (config == null)");
            return false;
        }
        return connectToConfiguredNetwork(wifiManager, config, true);
    }

    private static boolean connectToConfiguredNetwork(@NonNull WifiManager wifiManager, @Nullable WifiConfiguration config, boolean reassociate) {
        if (config == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= 23) {
            return disableAllButOne(wifiManager, config) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());
        }

        int oldPri = config.priority;
        // Make it the highest priority.
        int newPri = getMaxPriority(wifiManager) + 1;
        if (newPri > MAX_PRIORITY) {
            newPri = shiftPriorityAndSave(wifiManager);
            config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
            if (config == null) {
                return false;
            }
        }

        // Set highest priority to this configured network
        config.priority = newPri;
        int networkId = wifiManager.updateNetwork(config);
        if (networkId == -1) {
            return false;
        }

        // Do not disable others
        if (!wifiManager.enableNetwork(networkId, false)) {
            config.priority = oldPri;
            return false;
        }

        if (!wifiManager.saveConfiguration()) {
            config.priority = oldPri;
            return false;
        }

        // We have to retrieve the WifiConfiguration after save.
        config = ConfigSecurities.getWifiConfiguration(wifiManager, config);
        return config != null && disableAllButOne(wifiManager, config) && (reassociate ? wifiManager.reassociate() : wifiManager.reconnect());
    }

    /**
     * disableAllButOne
     * @param wifiManager
     * @param config
     * @return
     */
    public static boolean disableAllButOne(@NonNull final WifiManager wifiManager, @Nullable final WifiConfiguration config) {
        @Nullable final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        if (configurations == null || config == null || configurations.isEmpty()) {
            return false;
        }
        boolean result = false;

        for (WifiConfiguration wifiConfig : configurations) {
            if (wifiConfig.networkId == config.networkId) {
                result = wifiManager.enableNetwork(wifiConfig.networkId, true);
            } else {
                wifiManager.disableNetwork(wifiConfig.networkId);
            }
        }
        wifiLog("disableAllButOne " + result);
        return result;
    }

    /**
     * checkForExcessOpenNetworkAndSave
     * @param resolver
     * @param wifiMgr
     * @return
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean checkForExcessOpenNetworkAndSave(@NonNull final ContentResolver resolver, @NonNull final WifiManager wifiMgr) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);

        boolean modified = false;
        int tempCount = 0;
        final int numOpenNetworksKept = Build.VERSION.SDK_INT >= 17
                ? Settings.Secure.getInt(resolver, Settings.Global.WIFI_NUM_OPEN_NETWORKS_KEPT, 10)
                : Settings.Secure.getInt(resolver, Settings.Secure.WIFI_NUM_OPEN_NETWORKS_KEPT, 10);

        for (int i = configurations.size() - 1; i >= 0; i--) {
            final WifiConfiguration config = configurations.get(i);
            if (WifiUtils.equals(ConfigSecurities.SECURITY_NONE, ConfigSecurities.getSecurity(config))) {
                tempCount++;
                if (tempCount >= numOpenNetworksKept) {
                    modified = true;
                    wifiMgr.removeNetwork(config.networkId);
                }
            }
        }
        return !modified || wifiMgr.saveConfiguration();

    }

    private static int getMaxPriority(@NonNull final WifiManager wifiManager) {
        final List<WifiConfiguration> configurations = wifiManager.getConfiguredNetworks();
        int pri = 0;
        for (final WifiConfiguration config : configurations) {
            if (config.priority > pri) {
                pri = config.priority;
            }
        }
        return pri;
    }

    private static int shiftPriorityAndSave(@NonNull final WifiManager wifiMgr) {
        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        sortByPriority(configurations);
        final int size = configurations.size();
        for (int i = 0; i < size; i++) {
            final WifiConfiguration config = configurations.get(i);
            config.priority = i;
            wifiMgr.updateNetwork(config);
        }
        wifiMgr.saveConfiguration();
        return size;
    }

    /**
     * getPowerPercentage
     * @param power
     * @return
     */
    @SuppressWarnings("unused")
    public static int getPowerPercentage(int power) {
        int i;
        if (power <= -93) {
            i = 0;
        } else {
            if (-25 <= power && power <= 0) {
                i = 100;
            } else {
                i = 125 + power;
            }
        }
        return i;
    }

    /**
     * convertToQuotedString
     * @param string
     * @return
     */
    @NonNull
    public static String convertToQuotedString(@NonNull String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        final int lastPos = string.length() - 1;
        if (lastPos < 0 || (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }

        return "\"" + string + "\"";
    }

    private static boolean isHexWepKey(@Nullable String wepKey) {
        final int passwordLen = wepKey == null ? 0 : wepKey.length();
        return passwordLen != 0 && (passwordLen == 10 || passwordLen == 26 || passwordLen == 58) && wepKey.matches("[0-9A-Fa-f]*");
    }


    private static void sortByPriority(@NonNull final List<WifiConfiguration> configurations) {
        Collections.sort(configurations, new Comparator<WifiConfiguration>() {
            @Override
            public int compare(WifiConfiguration o1, WifiConfiguration o2) {
                return o1.priority - o2.priority;
            }
        });
    }

    private static boolean cleanPreviousConfiguration(@NonNull final WifiManager wifiManager, @NonNull final ScanResult scanResult) {
        //On Android 6.0 (API level 23) and above if my app did not create the configuration in the first place, it can not remove it either.
        final WifiConfiguration config = ConfigSecurities.getWifiConfiguration(wifiManager, scanResult);
        wifiLog("Attempting to remove previous network config...");
        if (config == null) {
            return true;
        }

        if (wifiManager.removeNetwork(config.networkId)) {
            wifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    private static boolean cleanPreviousConfiguration(@NonNull final WifiManager wifiManager, @Nullable final WifiConfiguration config) {
        //On Android 6.0 (API level 23) and above if my app did not create the configuration in the first place, it can not remove it either.

        wifiLog("Attempting to remove previous network config...");
        if (config == null) {
            return true;
        }

        if (wifiManager.removeNetwork(config.networkId)) {
            wifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    /**
     * wifiLog
     * @param text
     */
    public static void wifiLog(final String text) {
        Log.d(TAG, "WifiUtils: " + text);
    }
}
