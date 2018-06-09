package com.example.cb.testwifiscan;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import java.util.List;

/**
 * Copy from http://club.gizwits.com/thread-3685-1-1.html
 * Created by 小铲子 at 2016-10-10
 */
public class WifiAutoConnectManager {

    private static final String TAG = WifiAutoConnectManager.class
            .getSimpleName();

    private WifiManager wifiManager;

    /**
     * The enum Wifi cipher type.
     */
// 定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public enum WifiCipherType {
        /**
         * Wificipher wep wifi cipher type.
         */
        WIFICIPHER_WEP, /**
         * Wificipher wpa wifi cipher type.
         */
        WIFICIPHER_WPA, /**
         * Wificipher nopass wifi cipher type.
         */
        WIFICIPHER_NOPASS, /**
         * Wificipher invalid wifi cipher type.
         */
        WIFICIPHER_INVALID
    }

    /**
     * Instantiates a new Wifi auto connect manager.
     *
     * @param wifiManager the wifi manager
     */
// 构造函数
    public WifiAutoConnectManager(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }

    /**
     * 切换wifi
     *
     * @param ssid
     * @param password
     */
    private void connectNetwork(Context context, String ssid, String password) {
        connect(ssid, password, WifiAutoConnectManager.getCipherType(context, ssid));
    }

    /**
     * Connect.
     *
     * @param ssid           the ssid
     * @param password       the password
     * @param type           the type
     * @param resultListener the result listener
     */
    public void connect(String ssid, String password, WifiCipherType type, ConnectResultListener resultListener) {
        Thread thread = new Thread(new ConnectRunnable(ssid, password, type, resultListener));
        thread.start();
    }

    /**
     * Connect.
     *
     * @param ssid     the ssid
     * @param password the password
     * @param type     the type
     */
// 提供一个外部接口，传入要连接的无线网
    public void connect(String ssid, String password, WifiCipherType type) {
        Thread thread = new Thread(new ConnectRunnable(ssid, password, type, null));
        thread.start();
    }

    // 查看以前是否也配置过这个网络
    private WifiConfiguration isExsits(String ssid) {
        List<WifiConfiguration> existingConfigs = wifiManager
                .getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + ssid + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private WifiConfiguration createWifiInfo(String ssid, String password,
                                             WifiCipherType type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";

        if (type == WifiCipherType.WIFICIPHER_NOPASS) {
            // config.wepKeys[0] = "";
            config.allowedKeyManagement.set(KeyMgmt.NONE);
            // config.wepTxKeyIndex = 0;
        }
        // wep
        if (type == WifiCipherType.WIFICIPHER_WEP) {
            if (!TextUtils.isEmpty(password)) {
                if (isHexWepKey(password)) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = "\"" + password + "\"";
                }
            }
            config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        // wpa
        if (type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.TKIP);
            // 此处需要修改否则不能自动重联
            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers
                    .set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;

        }
        return config;
    }

    // 打开wifi功能
    public boolean openWifi() {
        boolean bRet = true;
        if (!wifiManager.isWifiEnabled()) {
            bRet = wifiManager.setWifiEnabled(true);
        }
        return bRet;
    }

    // 关闭WIFI
    private void closeWifi() {
        if (wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    /**
     * The type Connect runnable.
     */
    class ConnectRunnable implements Runnable {
        private String ssid;
        private String password;
        private WifiCipherType type;
        private ConnectResultListener mListener;

        /**
         * Instantiates a new Connect runnable.
         *
         * @param ssid     the ssid
         * @param password the password
         * @param type     the type
         * @param listener the listener
         */
        public ConnectRunnable(String ssid, String password, WifiCipherType type, ConnectResultListener listener) {
            this.ssid = ssid;
            this.password = password;
            this.type = type;
            mListener = listener;
        }

        @Override
        public void run() {
            // 打开wifi
            openWifi();
            // 开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以要等到wifi
            // 状态变成WIFI_STATE_ENABLED的时候才能执行下面的语句
            while (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING) {
                try {
                    // 为了避免程序一直while循环，让它睡个100毫秒检测……
                    Thread.sleep(100);

                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }

            WifiConfiguration tempConfig = isExsits(ssid);

            if (tempConfig != null) {
                // wifiManager.removeNetwork(tempConfig.networkId);

                boolean b = wifiManager.enableNetwork(tempConfig.networkId,
                        true);
                if (mListener != null) {
                    mListener.onConnectResult(b);
                }
            } else {
                WifiConfiguration wifiConfig = createWifiInfo(ssid, password,
                        type);
                //
                if (wifiConfig == null) {
                    Log.d(TAG, "wifiConfig is null!");
                    return;
                }

                int netID = wifiManager.addNetwork(wifiConfig);
                boolean enabled = wifiManager.enableNetwork(netID, true);
                Log.d(TAG, "enableNetwork status enable=" + enabled);
                boolean connected = wifiManager.reconnect();
                Log.d(TAG, "enableNetwork connected=" + connected);
                if (mListener != null) {
                    mListener.onConnectResult(enabled && connected);
                }
            }

        }
    }

    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }

        return isHex(wepKey);
    }

    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f')) {
                return false;
            }
        }

        return true;
    }

    // 获取ssid的加密方式

    /**
     * Gets cipher type.
     *
     * @param context the context
     * @param ssid    the ssid
     * @return the cipher type
     */
    public static WifiCipherType getCipherType(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);

        List<ScanResult> list = wifiManager.getScanResults();

        for (ScanResult scResult : list) {

            if (!TextUtils.isEmpty(scResult.SSID) && scResult.SSID.equals(ssid)) {
                String capabilities = scResult.capabilities;
                // Log.i("hefeng","capabilities=" + capabilities);

                if (!TextUtils.isEmpty(capabilities)) {

                    if (capabilities.contains("WPA")
                            || capabilities.contains("wpa")) {
                        Log.i("hefeng", "wpa");
                        return WifiCipherType.WIFICIPHER_WPA;
                    } else if (capabilities.contains("WEP")
                            || capabilities.contains("wep")) {
                        Log.i("hefeng", "wep");
                        return WifiCipherType.WIFICIPHER_WEP;
                    } else {
                        Log.i("hefeng", "no");
                        return WifiCipherType.WIFICIPHER_NOPASS;
                    }
                }
            }
        }
        return WifiCipherType.WIFICIPHER_INVALID;
    }

    /**
     * The interface Connect result listener.
     */
    public interface ConnectResultListener {
        /**
         * 返回连接wifi是否成功
         *
         * @param isConnect the is connect
         */
        void onConnectResult(boolean isConnect);
    }
}
