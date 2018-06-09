package com.example.cb.testwifiscan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.format.Formatter;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * Wifi工具类
 * Created by luojinjin on 2016/12/14.
 */
public class WifiScanManager {
    private static volatile WifiScanManager mInstance;
    private volatile WifiScannerResult mWifiScannerResult;
    private List<ScanResult> mWifiList = new ArrayList<>();
    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                if (mWifiManager != null && mWifiManager.getScanResults() != null) {
                    mWifiList.clear();
                    mWifiList.addAll(mWifiManager.getScanResults());
                    if (mWifiScannerResult != null) {
                        synchronized (WifiScanManager.class) {
                            if (mWifiScanReceiver != null) {
                                mWifiScannerResult.onResult();
                            }
                        }
                    }

                    if (mIsStartScanning) {
                        mWifiManager.startScan();
                    }
                }
            }
        }
    };
    private Context mContext;
    private boolean mIsStartScanning = false;

    private WifiScanManager() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static WifiScanManager getInstance() {
        if (mInstance == null) {
            synchronized (WifiScanManager.class) {
                if (mInstance == null) {
                    mInstance = new WifiScanManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 开始扫描wifi
     *
     * @param context           the context
     * @param wifiScannerResult the wifi scanner result
     */
    public void startScan(Context context, WifiScannerResult wifiScannerResult) {
        if (context != null && wifiScannerResult != null) {
            mContext = context;
            mWifiScannerResult = wifiScannerResult;
            mWifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            mContext.registerReceiver(mWifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mIsStartScanning = true;
            mWifiManager.startScan();
        }
    }

    /**
     * 停止扫描wifi
     */
    public void stopScan() {
        mIsStartScanning = false;
        if (mContext != null) {
            try {
                mContext.unregisterReceiver(mWifiScanReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
            mContext = null;
        }
        mWifiManager = null;
        mWifiScannerResult = null;
    }

    /**
     * 获取当前网关ip
     *
     * @param context
     *
     * @return
     */
    public String getCurrentGateway(Context context) {
        WifiManager wifiService = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String gateway = "0.0.0.0";
        if (wifiService != null) {
            DhcpInfo dhcpInfo = wifiService.getDhcpInfo();
            if (dhcpInfo != null) {
                gateway = intToIp(dhcpInfo.gateway);
            }
        }
        return gateway;
    }

    /**
     * 获取当前WifiInfo
     *
     * @param context the context
     *
     * @return current wifi
     */
    public WifiInfo getCurrentWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager.getConnectionInfo();
    }

    /**
     * 获取当前wifi的ssid名称
     *
     * @param context the context
     *
     * @return current wifi ssid
     */
    public String getCurrentWifiSsid(Context context) {
        WifiInfo wifiInfo = getCurrentWifi(context);
        if (wifiInfo == null || wifiInfo.getNetworkId() == -1) {
            return null;
        } else {
            return wifiInfo.getSSID().replace("\"", "");
        }
    }

    /**
     * 获取当前网络连接wifi的子网掩码
     *
     * @param context
     *
     * @return
     */
    public String getCurrentWifiNetMask(Context context) {
        String maskIp = "255.255.255.0";
        WifiManager wifiService = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiService.getDhcpInfo();
        //DhcpInfo中的ipAddress是一个int型的变量，通过Formatter将其转化为字符串IP地址
        if (dhcpInfo != null) {
            int netMask = dhcpInfo.netmask;
            if (netMask != 0) {
                maskIp = Formatter.formatIpAddress(dhcpInfo.netmask);
                return maskIp;
            }
        }
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (Network network : cm.getAllNetworks()) {
                NetworkInfo networkInfo = cm.getNetworkInfo(network);
                if (networkInfo.getType() == activeNetworkInfo.getType()) {
                    LinkProperties lp = cm.getLinkProperties(network);
                    Iterator localIterator1 = lp.getLinkAddresses().iterator();
                    if (localIterator1.hasNext()) {
                        LinkAddress localLinkAddress = (LinkAddress) localIterator1.next();
                        //获得网络的前缀长度，然后根据前缀长度来获取掩码
                        int i = localLinkAddress.getPrefixLength();
                        if (i > 0 && i <= 32) {
                            maskIp = calcMaskByPrefixLength(i);
                        }
                    }
                }
            }
        }
        return maskIp;
    }

    /**
     * 获取广播地址
     *
     * @return
     */
    public static String getBroadcastAddress() {
        try {
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() != null) {
                            String ip = interfaceAddress.getBroadcast().toString().substring(1);
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private String calcMaskByPrefixLength(int length) {
        int mask = -1 << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int[] maskParts = new int[partsNum];
        int selector = 0x000000ff;

        for (int i = 0; i < maskParts.length; i++) {
            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
        }

        StringBuilder result = new StringBuilder();
        result.append(maskParts[0]);
        for (int i = 1; i < maskParts.length; i++) {
            result.append(".").append(maskParts[i]);
        }
        return result.toString();
    }

    /**
     * 将int数值转换为ip
     *
     * @param ip
     *
     * @return
     */
    private String intToIp(int ip) {
        return (ip & 0xFF) + "." + (0xFF & ip >> 8) + "." + (0xFF & ip >> 16) + "." + (0xFF & ip >> 24);
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
     * The interface Wifi scanner result.
     */
    public interface WifiScannerResult {
        /**
         * On result.
         */
        void onResult();
    }

}
