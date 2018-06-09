package com.example.cb.testwifiscan;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.example.cb.testwifiscan.wifi.WifiUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by cb on 2018/6/7.
 */

public class WifiConnectManager {

    private static WifiConnectManager instance;
    private WifiManager mWifiManager;
    private List<ScanResult> mWifiList;

    private Context applicationContext;

    public void setApplicationContext(Context applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * getInstance
     * @return
     */
    public static WifiConnectManager getInstance() {
        if (instance == null) {
            synchronized (WifiConnectManager.class) {
                if (instance == null) {
                    instance = new WifiConnectManager();
                }
            }
        }
        return instance;
    }

    public WifiConnectManager(){

    }

    /**
     * getWifiManager
     * @return
     */
    public WifiManager getWifiManager() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) applicationContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        return mWifiManager;
    }

    /**
     * 获取当前wifi列表
     *
     * @return wifi list
     */
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    /**
     * wifi是否打开
     * @return
     */
    public boolean isWifiEnable() {
        return getWifiManager().isWifiEnabled();
    }

    /**
     * 打开wifi
     */
    public void openWifi(final OpenWifiResult openWifiResult) {
        if (!isWifiEnable()) {
            getWifiManager().setWifiEnabled(true);
            //小米4（6.0.1）系统，会有打开wifi的弹窗，不管拒绝或者同意，都会往下执行
            //开启wifi功能需要一段时间(我在手机上测试一般需要1-3秒左右)，所以，需等待3s遍历，是否打开wifi成功
            Observable.just("")
                    .map(new Function<String, Boolean>() {
                        @Override
                        public Boolean apply(String s) throws Exception {
                            int count = 0;
                            boolean result = isWifiEnable();
                            while (!result && count < 30) {
                                Thread.sleep(100);
                                count++;
                                result = isWifiEnable();
                            }
                            return result;
                        }
                    })
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean aBoolean) throws Exception {
                            if (openWifiResult != null) {
                                openWifiResult.onResult(isWifiEnable());
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            if (openWifiResult != null) {
                                openWifiResult.onResult(isWifiEnable());
                            }
                        }
                    });
        }
    }

    /**
     * 关闭wifi
     */
    public void closeWifi() {
        if (isWifiEnable()) {
            getWifiManager().setWifiEnabled(false);
        }
    }

    /**
     * 开始扫描wifi
     */
    public void startScan() {
        getWifiManager().startScan();
    }

    /**
     * 处理扫描wifi BroadCast 扫描结果
     * @param context BroadCast applicationContext
     * @param intent BroadCast onReceive intent
     */
    public void handleBroadCastScanResult(Context context, Intent intent, @NonNull WifiScannerResult wifiScannerResult) {
        try {
            List<ScanResult> scanResults = getWifiManager().getScanResults();
            if (scanResults != null && scanResults.size() > 0) {
                filterScanResults(scanResults);
                wifiScannerResult.onResult(mWifiList);
            } else {
                wifiScannerResult.onResult(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            wifiScannerResult.onResult(null);
        }
    }

    /**
     * 过滤移除没有SSID和重复的Wifi
     * @param scanResults
     */
    private void filterScanResults(List<ScanResult> scanResults) {
        if (scanResults == null) {
            return;
        }

        if (mWifiList == null) {
            mWifiList = new ArrayList<>();
        } else {
            mWifiList.clear();
        }
        for (ScanResult result : scanResults) {
            if (TextUtils.isEmpty(result.SSID) || TextUtils.isEmpty(result.SSID.replaceAll(" ", ""))) {
                continue;
            }
            boolean found = false;
            for (ScanResult item : mWifiList) {
                if (item.SSID.equals(result.SSID) && WifiUtils.equals(item.BSSID, result.BSSID) &&
                        item.capabilities.equals(result.capabilities)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                mWifiList.add(result);
            }
        }
    }

    /**
     * 连接wifi
     * @param scanResult
     * @param password
     * @param connectWifiResult
     */
    public void connect(@NonNull ScanResult scanResult, String password, @NonNull ConnectWifiResult connectWifiResult) {
        if (TextUtils.isEmpty(scanResult.SSID)) {
            connectWifiResult.connectResult(ConnectWifiResult.type_ssid_is_empty);
            return;
        }

//        ConnectUtils.connectToWifi(applicationContext, getWifiManager(), scanResult, password);

        connect(scanResult.SSID, password, getCipherType(scanResult), connectWifiResult);
    }

    /**
     * 连接wifi
     * @param ssid
     * @param password
     * @param type
     * @param connectWifiResult
     */
    public void connect(final String ssid, final String password, final WifiCipherType type, @NonNull final ConnectWifiResult connectWifiResult) {
        if (isWifiEnable()) {
            startConnect(ssid, password, type,connectWifiResult);
        } else {
            openWifi(new OpenWifiResult() {
                @Override
                public void onResult(boolean success) {
                    if (success) {
                        startConnect(ssid, password, type,connectWifiResult);
                    } else {
                        connectWifiResult.connectResult(ConnectWifiResult.type_wifi_no_connet);
                    }
                }
            });
        }
    }

    private void startConnect(final String ssid, final String password, final WifiCipherType type, @NonNull final ConnectWifiResult connectWifiResult) {
        Observable.just("")
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        WifiConfiguration configuration = isWifiExsits(ssid);
                        int netID;
                        if (configuration != null) {
                            netID = configuration.networkId;
                        } else {
                            WifiConfiguration wifiConfig = createWifiInfo(ssid, password, type);
                            if (wifiConfig == null) {
                                return "create_wifi_config_fail";
                            }
                            netID = getWifiManager().addNetwork(wifiConfig);
                        }
                        if (netID == -1) {
                            return "get_net_id_fail";
                        }

                        boolean enableNetWorkResult = getWifiManager().enableNetwork(netID, true);
                        if (!enableNetWorkResult) {
                            getWifiManager().enableNetwork(netID, true);
                        }
                        boolean reconnectResult = getWifiManager().reconnect();
                        if (!reconnectResult) {
                            getWifiManager().reconnect();
                        }
                        return "wait_for_result";
                    }
                })
                .map(new Function<String, Integer>() {
                    @Override
                    public Integer apply(String s) throws Exception {
                        if ("create_wifi_config_fail".equals(s)) {
                            return ConnectWifiResult.type_connect_fail_create_wifi_config_fail;
                        }
                        if ("get_net_id_fail".equals(s)) {
                            return ConnectWifiResult.type_get_net_id_fail;
                        }
                        int count = 0;
                        String connectWifiSSID = null;
                        while (count < 75 && TextUtils.isEmpty(connectWifiSSID)) {
                            Thread.sleep(200);
                            connectWifiSSID = getCurrentWifiSSID(applicationContext);
                            count++;
                        }

                        if (!TextUtils.isEmpty(connectWifiSSID) && connectWifiSSID.equals(ssid)) {
                            return ConnectWifiResult.type_connect_success;
                        }

                        return ConnectWifiResult.type_connect_fail;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer result) throws Exception {
                        connectWifiResult.connectResult(result);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        connectWifiResult.connectResult(ConnectWifiResult.type_connect_fail);
                    }
                });
    }

    public static int getMaxPriority(WifiManager wifiManager, String ssid) {
        List<WifiConfiguration> allConfig = wifiManager.getConfiguredNetworks();
        int max = 0;
        if (allConfig != null) {
            for (WifiConfiguration wifiConfig : allConfig) {
                // Shift the status to make enableNetworks() more efficient.
                if (wifiConfig.status == WifiConfiguration.Status.CURRENT) {
                    wifiConfig.status = WifiConfiguration.Status.ENABLED;
                } else if (wifiConfig.status == WifiConfiguration.Status.DISABLED) {
                    wifiConfig.status = WifiConfiguration.Status.CURRENT;
                }

                if (ssid != null && ssid.equals(wifiConfig.SSID)) {
                    continue;
                }
                if (max < wifiConfig.priority) {
                    max = wifiConfig.priority;
                }
            }
        }
        return max;
    }

    /**
     * 之前是否配置（储存）过该wifi信息
     * @param ssid
     * @return
     */
    private WifiConfiguration isWifiExsits(String ssid) {
        List<WifiConfiguration> existingConfigs = getWifiManager().getConfiguredNetworks();
        if (existingConfigs == null) {
            return null;
        }
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID != null && existingConfig.SSID.equals("\"" + ssid + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    private WifiConfiguration createWifiInfo(String ssid, String password, WifiCipherType type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";

        if (type == WifiCipherType.WIFICIPHER_NOPASS) {
            // config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
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
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        // wpa
        if (type == WifiCipherType.WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms
                    .set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
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

    private boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();
        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }
        return isHex(wepKey);
    }

    private boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a'
                    && c <= 'f')) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets cipher type.
     *
     * @param scanResult    the scanResult
     * @return the cipher type
     */
    public static WifiCipherType getCipherType(@NonNull ScanResult scanResult) {
        if (!TextUtils.isEmpty(scanResult.SSID)) {
            String capabilities = scanResult.capabilities;
            if (!TextUtils.isEmpty(capabilities)) {
                if (capabilities.toLowerCase().contains("wpa")) {
                    return WifiCipherType.WIFICIPHER_WPA;
                } else if (capabilities.toLowerCase().contains("wep")) {
                    return WifiCipherType.WIFICIPHER_WEP;
                } else {
                    return WifiCipherType.WIFICIPHER_NOPASS;
                }
            }
        }
        return WifiCipherType.WIFICIPHER_INVALID;
    }

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
     * The interface Wifi scanner result.
     */
    public interface WifiScannerResult {

        /**
         * On result.
         * @param list
         */
        void onResult(List<ScanResult> list);
    }

    /**
     * 打开wifi结果
     */
    public interface OpenWifiResult{
        /**
         * the onResult
         * @param success
         */
        void onResult(boolean success);
    }

    /**
     * 连接wifi结果
     */
    public interface ConnectWifiResult {
        int type_wifi_no_connet = 1;
        int type_ssid_is_empty = 2;
        int type_connect_fail_create_wifi_config_fail = 3;
        int type_connect_success = 4;
        int type_connect_fail = 5;
        int type_get_net_id_fail = 6;
        /**
         * the result
         * @param result
         */
        void connectResult(int result);
    }

    /**
     * 加密方式：
     */
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

}
