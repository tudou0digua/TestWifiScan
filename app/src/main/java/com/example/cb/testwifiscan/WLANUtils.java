package com.example.cb.testwifiscan;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.chinamobile.cmccsdk.bean.MScanResultModule;

import java.util.List;

/**
 * Created by CCrz on 2018/6/4.
 */

public class WLANUtils {
    public static final String PSK = "PSK";
    public static final String WEP = "WEP";
    public static final String EAP = "EAP";
    public static final String OPEN = "Open";
    public static final int WEP_PASSWORD_AUTO = 0;
    public static final int WEP_PASSWORD_ASCII = 1;

    public static WifiConfiguration getConfigBySsidAndCapability(WifiManager wifiManager, String ssid,
                                                                 String capability) {
        List<WifiConfiguration> allConfig = wifiManager.getConfiguredNetworks();
        if (allConfig != null) {
            for (WifiConfiguration wifiConfig : allConfig) {
                String aSsid = getHumanReadableSsid(wifiConfig.SSID);
                if (ssid.equals(aSsid) && matchWifiConfiguration(wifiConfig, capability)) {
                    return wifiConfig;
                }
            }
        }
        return null;
    }

    public static String getHumanReadableSsid(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return "";
        }
        final int lastPos = ssid.length() - 1;
        if (ssid.charAt(0) == '"' && ssid.charAt(lastPos) == '"') {
            return ssid.substring(1, lastPos);
        }
        return ssid;
    }

    public static boolean matchWifiConfiguration(WifiConfiguration config, String cap) {
        String security = WLANUtils.getScanResultSecurity(cap);
        return matchWifiConfigurationBySecurity(config, security);
    }
    public static String getScanResultSecurity(String cap) {
        final String[] securityModes = {WEP, PSK, EAP};
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        return OPEN;
    }

    public static boolean matchWifiConfigurationBySecurity(WifiConfiguration config, String security) {
        if (OPEN.equals(security)) {
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)
                    && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)
                    && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
                // OPEN为KeyMgmt.NONE,{0}
                return true;
            }
        } else if (WEP.equals(security)) {
            if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)
                    && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)
                    && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
                // WEP也为KeyMgmt.NONE,{0}
                if (config.wepKeys != null && config.wepKeys.length > 0 && config.wepKeys[0] != null
                        && config.wepKeys[0].length() > 0) {
                    // 有输入WEP密码
                    return true;
                }
            }
        } else if (PSK.equals(security)) {
            if (!config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) && config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)
                    && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
                // PSK为KeyMgmt.WPA_PSK,{1} or {1,2}
                if (config.preSharedKey != null && config.preSharedKey.length() > 0) {
                    // 有输入PSK密码
                    return true;
                }
            }
        } else if (EAP.equals(security)) {
            if (!config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE) && !config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)
                    && config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)
                    && config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
                // EAP为KeyMgmt.WPA_PSK + KeyMgmt.IEEE8021X,{2,3}
                return true;
            }
        }
        return false;
    }

    public static void reconnect(WifiManager wifiManager, WifiConfiguration targetConfig) {
        int max = WLANUtils.getMaxPriority(wifiManager, targetConfig.SSID);
        // Reset the priority of each network if it goes too high.
        if (max > 1000000) {
            List<WifiConfiguration> allConfig = wifiManager.getConfiguredNetworks();
            if (allConfig != null) {
                for (WifiConfiguration wifiConfig : allConfig) {
                    if (wifiConfig.networkId != -1) {
                        WifiConfiguration config = new WifiConfiguration();
                        config.networkId = wifiConfig.networkId;
                        config.priority = 0;
                        wifiManager.updateNetwork(config);
                    }
                }
            }
            max = 0;
        }

        WifiConfiguration config = new WifiConfiguration();
        config.networkId = targetConfig.networkId;
        if (targetConfig.priority <= max) {
            config.priority = 1 + max;
        } else {
            config.priority = targetConfig.priority;
        }

        wifiManager.updateNetwork(config);
        saveNetworks(wifiManager);
        boolean isOperateOk = wifiManager.enableNetwork(config.networkId, true);
        if (!isOperateOk) { //如果操作失败，重试一次
            wifiManager.enableNetwork(config.networkId, true);
        }

        isOperateOk = wifiManager.reconnect();
        if (!isOperateOk) { //如果操作失败，重试一次
            wifiManager.reconnect();
        }
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
    public static void saveNetworks(WifiManager wifiManager) {
        // Always save the configuration with all networks enabled.
        wifiManager.saveConfiguration();
        updateWifiConfigStatus(wifiManager);
    }

    public static void updateWifiConfigStatus(WifiManager wifiManager) {
        List<WifiConfiguration> allConfig = wifiManager.getConfiguredNetworks();
        if (allConfig != null) {
            for (WifiConfiguration wifiConfig : allConfig) {
                // Shift the status to make enableNetwork more efficient.
                if (wifiConfig.status == WifiConfiguration.Status.CURRENT) {
                    wifiConfig.status = WifiConfiguration.Status.ENABLED;
                } else if (wifiConfig.status == WifiConfiguration.Status.DISABLED) {
                    wifiConfig.status = WifiConfiguration.Status.CURRENT;
                }
            }
        }
    }

    public static WifiConfiguration transScanResult2WifiConfig(MScanResultModule scanResult, String pwd) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = convertToQuotedString(scanResult.SSID);
        setupSecurity(config, getScanResultSecurity(scanResult), WEP_PASSWORD_AUTO, pwd);
        return config;
    }

    private static String convertToQuotedString(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }

        final int lastPos = string.length() - 1;
        if (lastPos < 0 || (string.charAt(0) == '"' && string.charAt(lastPos) == '"')) {
            return string;
        }

        return "\"" + string + "\"";
    }

    private static void setupSecurity(
            WifiConfiguration config, String security,
            int mWepPasswordType, String mPassword) {
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        if (TextUtils.isEmpty(security)) {
            security = OPEN;
        }

        switch (security) {
            case WEP:
                // If password is empty, it should be left untouched
                if (!TextUtils.isEmpty(mPassword)) {
                    if (mWepPasswordType == WEP_PASSWORD_AUTO) {
                        if (isHexWepKey(mPassword)) {
                            config.wepKeys[0] = mPassword;
                        } else {
                            config.wepKeys[0] = convertToQuotedString(mPassword);
                        }
                    } else {
                        config.wepKeys[0] = mWepPasswordType == WEP_PASSWORD_ASCII ? convertToQuotedString(mPassword)
                                : mPassword;
                    }
                }
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                break;
            case PSK:
                // If password is empty, it should be left untouched
                if (!TextUtils.isEmpty(mPassword)) {
                    if (mPassword.length() == 64 && isHex(mPassword)) {
                        // Goes unquoted as hex
                        config.preSharedKey = mPassword;
                    } else {
                        // Goes quoted as ASCII
                        config.preSharedKey = convertToQuotedString(mPassword);
                    }
                }
                break;
            case EAP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                break;
            case OPEN:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
        }
    }
    public static String getScanResultSecurity(MScanResultModule scanResult) {
        final String cap = scanResult.capabilities;
        final String[] securityModes = {WEP, PSK, EAP};
        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        return OPEN;
    }
    private static boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        return !(len != 10 && len != 26 && len != 58) && isHex(wepKey);

    }
    private static boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }

        return true;
    }
}
