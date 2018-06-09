package com.example.cb.testwifiscan.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.example.cb.testwifiscan.connect.ConnectUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by cb on 2018/6/8.
 */

public class WifiControlManager implements WifiScanResultReceiver.WifiScanResultCallback,
        WifiConnectionReceiver.WifiConnectionCallback {

    private static final int CONNECT_TIMEOUT_TIME = 15000;

    private WifiScanResultReceiver wifiScanResultReceiver;
    private WifiConnectionReceiver wifiConnectionReceiver;

    private WifiManager wifiManager;
    private Context context;

    private WifiScannerResult wifiScannerResult;
    private ConnectWifiResult connectWifiResult;

    public WifiControlManager(@NonNull Context context) {
        this.context = context;
    }

    public void setWifiScannerResult(WifiScannerResult wifiScannerResult) {
        this.wifiScannerResult = wifiScannerResult;
    }

    public void setConnectWifiResult(ConnectWifiResult connectWifiResult) {
        this.connectWifiResult = connectWifiResult;
    }

    /**
     * getWifiManager
     * @return
     */
    public WifiManager getWifiManager() {
        if (wifiManager == null) {
            wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        return wifiManager;
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
        unregisterReceiver(context, wifiScanResultReceiver);
        if (context != null) {
            wifiScanResultReceiver = new WifiScanResultReceiver(this);
            context.registerReceiver(wifiScanResultReceiver,
                    new IntentFilter(WifiScanResultReceiver.ACTION_SCAN_WIFI_BROADCAST));
            getWifiManager().startScan();
        } else {
            if (wifiScannerResult != null) {
                wifiScannerResult.onResult(null);
            }
        }
    }

    /**
     * 扫描wifi结束
     */
    @Override
    public void onReceiveScanResult() {
        unregisterReceiver(context, wifiScanResultReceiver);
        if (wifiScannerResult == null) {
            return;
        }
        try {
            List<ScanResult> scanResults = getWifiManager().getScanResults();
            if (scanResults != null && scanResults.size() > 0) {
                wifiScannerResult.onResult(filterScanResults(scanResults));
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
    private List<ScanResult> filterScanResults(List<ScanResult> scanResults) {
        if (scanResults == null) {
            return null;
        }
        List<ScanResult> wifiList = new ArrayList<>();
        for (ScanResult result : scanResults) {
            if (TextUtils.isEmpty(result.SSID) || TextUtils.isEmpty(result.SSID.replaceAll(" ", ""))) {
                continue;
            }
            wifiList.add(result);

//            boolean found = false;
//            for (ScanResult item : wifiList) {
//                if (item.SSID.equals(result.SSID) && item.capabilities.equals(result.capabilities)) {
//                    found = true;
//                    break;
//                }
//            }
//            if (!found) {
//                wifiList.add(result);
//            }
        }
        return wifiList;
    }

    /**
     * 连接wifi
     * @param scanResult
     * @param password
     */
    public void connect(@NonNull ScanResult scanResult, @NonNull String password) {
        unregisterReceiver(context, wifiConnectionReceiver);
        if (context != null) {
            wifiConnectionReceiver = new WifiConnectionReceiver(this, getWifiManager(), scanResult, CONNECT_TIMEOUT_TIME);
            context.registerReceiver(wifiConnectionReceiver,
                    new IntentFilter(WifiConnectionReceiver.ACTION_NETWORK_STATE_CHANGED));
            context.registerReceiver(wifiConnectionReceiver,
                    new IntentFilter(WifiConnectionReceiver.ACTION_SUPPLICANT_STATE_CHANGED));
            if (!ConnectUtils.connectToWifi(context, getWifiManager(), scanResult, password)) {
                errorConnect();
            }
        } else {
            errorConnect();
        }
    }

    @Override
    public void successfulConnect() {
        unregisterReceiver(context, wifiConnectionReceiver);
        if (connectWifiResult != null) {
            connectWifiResult.connectSuccess();
        }
    }

    @Override
    public void errorConnect() {
        unregisterReceiver(context, wifiConnectionReceiver);
        if (connectWifiResult != null) {
            connectWifiResult.connectFail(ConnectWifiResult.type_connect_fail);
        }
    }

    private void unregisterReceiver(Context context, BroadcastReceiver receiver) {
        if (receiver != null && context != null) {
            try {
                context.unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
            receiver = null;
        }
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
        int type_connect_fail = 0;
        int type_get_net_id_fail = 1;
        int type_wifi_no_connet = 2;
        int type_ssid_is_empty = 3;
        int type_connect_fail_create_wifi_config_fail = 4;
        /**
         * the result
         * @param code
         */
        void connectFail(int code);

        /**
         * connectSuccess
         */
        void connectSuccess();
    }
}
