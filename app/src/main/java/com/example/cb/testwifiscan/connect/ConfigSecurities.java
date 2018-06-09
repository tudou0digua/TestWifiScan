package com.example.cb.testwifiscan.connect;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ConfigSecurities {
    public static final String SECURITY_NONE = "OPEN";
    public static final String SECURITY_WEP = "WEP";
    public static final String SECURITY_PSK = "PSK";
    public static final String SECURITY_EAP = "EAP";

    /**
     * Fill in the security fields of WifiConfiguration config.
     *
     * @param config   The object to fill.
     * @param security If is OPEN, password is ignored.
     * @param password Password of the network if security is not OPEN.
     */
    public static void setupSecurity(@NonNull WifiConfiguration config, String security, @NonNull final String password) {
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        switch (security) {
            case SECURITY_NONE:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                break;
            case SECURITY_WEP:
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                if (isHexWepKey(password)) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = convertToQuotedString(password);
                }
                break;
            case SECURITY_PSK:
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                if (password.matches("[0-9A-Fa-f]{64}")) {
                    config.preSharedKey = password;
                } else {
                    config.preSharedKey = convertToQuotedString(password);
                }
                break;
            case SECURITY_EAP:
                config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
                config.preSharedKey = convertToQuotedString(password);
                break;
            default:
                break;
        }
    }

    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final WifiConfiguration configToFind) {
        final String ssid = configToFind.SSID;
        if (ssid == null || ssid.isEmpty()) {
            return null;
        }

        final String bssid = configToFind.BSSID;
        final String security = getSecurity(configToFind);

        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null) {
            return null;
        }

        for (final WifiConfiguration config : configurations) {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID)) {
                final String configSecurity = getSecurity(config);
                if (ConfigSecurities.equals(security, configSecurity)) {
                    return config;
                }
            }
        }
        return null;
    }

    @Nullable
    static WifiConfiguration getWifiConfiguration(@NonNull final WifiManager wifiMgr, @NonNull final ScanResult scanResult) {
        if (scanResult.BSSID == null || scanResult.SSID == null || scanResult.SSID.isEmpty() || scanResult.BSSID.isEmpty()) {
            return null;
        }
        final String ssid = convertToQuotedString(scanResult.SSID);
        final String bssid = scanResult.BSSID;
        final String security = getSecurity(scanResult);

        final List<WifiConfiguration> configurations = wifiMgr.getConfiguredNetworks();
        if (configurations == null) {
            return null;
        }

        for (final WifiConfiguration config : configurations) {
            if (bssid.equals(config.BSSID) || ssid.equals(config.SSID)) {
                final String configSecurity = getSecurity(config);
                if (ConfigSecurities.equals(security, configSecurity)) {
                    return config;
                }
            }
        }
        return null;
    }

    static String getSecurity(@NonNull WifiConfiguration config) {
        String security = SECURITY_NONE;
        final Collection<String> securities = new ArrayList<>();
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
            // If we never set group ciphers, wpa_supplicant puts all of them.
            // For open, we don't set group ciphers.
            // For WEP, we specifically only set WEP40 and WEP104, so CCMP
            // and TKIP should not be there.
            if (config.wepKeys[0] != null) {
                security = SECURITY_WEP;
            } else {
                security = SECURITY_NONE;
            }
            securities.add(security);
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            security = SECURITY_EAP;
            securities.add(security);
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            security = SECURITY_PSK;
            securities.add(security);
        }

        return security;
    }

    public static String getSecurity(@NonNull ScanResult result) {
        String security = SECURITY_NONE;
        if (result.capabilities.contains(SECURITY_WEP)) {
            security = SECURITY_WEP;
        }
        if (result.capabilities.contains(SECURITY_PSK)) {
            security = SECURITY_PSK;
        }
        if (result.capabilities.contains(SECURITY_EAP)) {
            security = SECURITY_EAP;
        }

        return security;
    }

    /**
     * @return The security of a given {@link ScanResult}.
     */
    public static String getSecurityPrettyPlusWps(@Nullable ScanResult scanResult) {
        if (scanResult == null) {
            return "";
        }
        String result = getSecurity(scanResult);

        if (scanResult.capabilities.contains("WPS")) {
            result = result + ", WPS";
        }
        return result;
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

    public static boolean isHexWepKey(@Nullable String wepKey) {
        final int passwordLen = wepKey == null ? 0 : wepKey.length();
        return passwordLen != 0 && (passwordLen == 10 || passwordLen == 26 || passwordLen == 58) && wepKey.matches("[0-9A-Fa-f]*");
    }

    private static boolean equals(@Nullable Object a, @Nullable Object b) {
        return a == b || a != null && a.equals(b);
    }
}
