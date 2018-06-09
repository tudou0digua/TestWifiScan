package com.example.cb.testwifiscan;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.chinamobile.cmccsdk.bean.MScanResultModule;
import com.example.cb.testwifiscan.wifi.WifiControlManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    RecyclerView rv;
    WifiAdapter adapter;
    WifiManager mWifiManager;
    WifiAutoConnectManager wifiAutoConnectManager;

//    WifiScanResultReceiver wifiScanResultReceiver;

    WifiControlManager wifiControlManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rv = findViewById(R.id.rv);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiAutoConnectManager = new WifiAutoConnectManager(mWifiManager);

        initData();

        wifiControlManager = new WifiControlManager(MainActivity.this);

        wifiControlManager.setWifiScannerResult(new WifiControlManager.WifiScannerResult() {
            @Override
            public void onResult(List<ScanResult> list) {
                if (list != null) {
                    adapter.setList(list);
                }
            }
        });

        wifiControlManager.setConnectWifiResult(new WifiControlManager.ConnectWifiResult() {
            @Override
            public void connectFail(int code) {
                openWifiFail(code);
            }

            @Override
            public void connectSuccess() {
                openWifiSuccess();
            }
        });

        WifiConnectManager.getInstance().setApplicationContext(MainActivity.this.getApplicationContext());

//        wifiScanResultReceiver = new WifiScanResultReceiver(new WifiConnectManager.WifiScannerResult() {
//            @Override
//            public void onResult(List<ScanResult> list) {
//                if (list != null) {
//                    adapter.setList(list);
//                }
//            }
//        });
//        registerReceiver(wifiScanResultReceiver, new IntentFilter(WifiScanResultReceiver.ACTION_SCAN_WIFI_BROADCAST));


        scanWifi2();

//        if (WifiConnectManager.getInstance().isWifiEnable()) {
//            WifiConnectManager.getInstance().startScan();
//        } else {
//            WifiConnectManager.getInstance().openWifi(new WifiConnectManager.OpenWifiResult() {
//                @Override
//                public void onResult(boolean success) {
//                    if (success) {
//                        WifiConnectManager.getInstance().startScan();
//                    } else {
//                        openWifiFail();
//                    }
//                }
//            });
//        }

//        if (mWifiManager.isWifiEnabled()) {
//            scanWifi();
//        } else {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 1);
////            if (hasChangeWifiPermission()) {
////                wifiAutoConnectManager.openWifi();
////            } else {
////                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 1);
////            }
//        }


//        Settings.Secure.putInt(getContentResolver(),Settings.Secure.LOCATION_MODE, 1);

//        CmccStart.getInstant(MainActivity.this.getApplicationContext()).scanWifi(new IScanWifiCallBack() {
//            @Override
//            public void onSuccess(Boolean aBoolean, List<ScanResult> list, ScanResult scanResult) {
////                if (aBoolean) {
////                    for (ScanResult result : list) {
////                        tv.append(result.BSSID);
////                        tv.append("  ");
////                        tv.append(result.capabilities);
////                        tv.append("\n");
////                    }
////                } else {
////                    tv.append("no cmcc-web");
////                }
//            }
//        });

//        wifiAutoConnectManager.openWifi();


//        scanWifi();


    }

    private void scanWifi2() {
        if (wifiControlManager.isWifiEnable()) {
            wifiControlManager.startScan();
        } else {
            wifiControlManager.openWifi(new WifiControlManager.OpenWifiResult() {
                @Override
                public void onResult(boolean success) {
                    if (success) {
                        wifiControlManager.startScan();
                    } else {
                        openWifiFail();
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if (wifiScanResultReceiver != null) {
//            unregisterReceiver(wifiScanResultReceiver);
//        }
    }

    private boolean hasChangeWifiPermission() {
        return PermissionChecker.checkSelfPermission(MainActivity.this, Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == 1 && hasChangeWifiPermission()) {
//            scanWifi2();
//        } else {
//            openWifiFail();
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if (requestCode == 1 && hasChangeWifiPermission()) {
////            wifiAutoConnectManager.openWifi();
//            scanWifi2();
//        } else {
//            openWifiFail();
//        }
    }

    private void openWifiFail(int code) {
        Toast.makeText(MainActivity.this, "打开wifi失败： " + code, Toast.LENGTH_SHORT).show();
    }

    private void openWifiFail() {
        Toast.makeText(MainActivity.this, "打开wifi失败2222", Toast.LENGTH_SHORT).show();
    }

    private void openWifiSuccess() {
        Toast.makeText(MainActivity.this, "打开wifi成功", Toast.LENGTH_SHORT).show();
    }

    private void scanWifi() {
//        WifiScanManager.getInstance().startScan(this, new WifiScanManager.WifiScannerResult() {
//            @Override
//            public void onResult() {
//                List<ScanResult> results = WifiScanManager.getInstance().getWifiList();
//
//                if (results != null) {
//                    adapter.setList(results);
//                }
//
////                if (results == null) {
////                    tv.append("no cmcc-web");
////                } else {
////                    for (ScanResult result : results) {
////                        tv.append(result.SSID);
////                        tv.append("\n");
////                    }
////                }
//            }
//        });

    }

    private void initData() {
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WifiAdapter(this);
        adapter.setItemClick(new WifiAdapter.ItemClick() {
            @Override
            public void onClick(ScanResult result) {

                if (result == null) {
                    return;
                }

                String password = "";
                WifiConnectManager.WifiCipherType cipherType = WifiConnectManager.getCipherType(result);
                if (cipherType == WifiConnectManager.WifiCipherType.WIFICIPHER_WEP ||
                        cipherType == WifiConnectManager.WifiCipherType.WIFICIPHER_WPA) {
                    password = "12345678";
                }

                wifiControlManager.connect(result, password);

//                MScanResultModule mScan = new MScanResultModule(result.SSID, result.BSSID, result.capabilities);

//                WLANUtils.reconnect(WifiConnectManager.getInstance().getWifiManager(), WLANUtils.transScanResult2WifiConfig(mScan, password));
//                doAPConnect(mScan, password);

//                WifiConnectManager.getInstance().connect(result, password, new WifiConnectManager.ConnectWifiResult() {
//                    @Override
//                    public void connectResult(int result) {
//                        Toast.makeText(MainActivity.this, "result: " + result, Toast.LENGTH_SHORT).show();
//                    }
//                });

//                new WiFiConnecter(MainActivity.this).connect(result.SSID, password, new WiFiConnecter.ActionListener() {
//                    @Override
//                    public void onStarted(String ssid) {
//                        Toast.makeText(MainActivity.this, "onStarted: ", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onSuccess(WifiInfo info) {
//                        Toast.makeText(MainActivity.this, "success: ", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onFailure() {
//                        Toast.makeText(MainActivity.this, "onFailure: ", Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onFinished(boolean isSuccessed) {
//                        Toast.makeText(MainActivity.this, "onFinished: ", Toast.LENGTH_SHORT).show();
//                    }
//                });

//                wifiAutoConnectManager.connect(result.SSID, "12397678467934",WifiAutoConnectManager.WifiCipherType.WIFICIPHER_NOPASS,
//                        new WifiAutoConnectManager.ConnectResultListener() {
//                            @Override
//                            public void onConnectResult(boolean isConnect) {
//                                Log.e(TAG, "onConnectResult: isConnect: " + isConnect);
////                                Toast.makeText(MainActivity.this, "isConnect: " + isConnect, Toast.LENGTH_SHORT).show();
//                            }
//                        });
            }
        });

        rv.setAdapter(adapter);
    }

    private void doAPConnect(MScanResultModule result, String password) {
        if (result != null) {
            WifiConfiguration config = WLANUtils.getConfigBySsidAndCapability(WifiConnectManager.getInstance().getWifiManager(), result.SSID, result.capabilities);
            if (config != null) {
                WLANUtils.reconnect(WifiConnectManager.getInstance().getWifiManager(), config);
            } else {
                config = WLANUtils.transScanResult2WifiConfig(result, password);
                int max = WLANUtils.getMaxPriority(WifiConnectManager.getInstance().getWifiManager(), (String) null);
                config.priority = 1 + max;
                int networkId = WifiConnectManager.getInstance().getWifiManager().addNetwork(config);
                if (networkId != -1) {
                    WifiConnectManager.getInstance().getWifiManager().enableNetwork(networkId, true);
                    WifiConnectManager.getInstance().getWifiManager().reconnect();
                }
            }

        }
    }
}
