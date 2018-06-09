package com.example.cb.testwifiscan.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * wifi连接结果、状态变化 BroadcastReceiver
 */
public final class WifiConnectionReceiver extends BroadcastReceiver {
    public static final String ACTION_NETWORK_STATE_CHANGED = WifiManager.NETWORK_STATE_CHANGED_ACTION;
    public static final String ACTION_SUPPLICANT_STATE_CHANGED = WifiManager.SUPPLICANT_STATE_CHANGED_ACTION;
    
    private final WifiConnectionCallback mWifiConnectionCallback;
    private ScanResult mScanResult;
    private final WifiManager mWifiManager;
    private Disposable disposable;

    public WifiConnectionReceiver(@NonNull WifiConnectionCallback callback, @NonNull WifiManager wifiManager,
                                  @Nullable ScanResult scanResult, long delayMillis) {
        this.mWifiConnectionCallback = callback;
        this.mWifiManager = wifiManager;
        this.mScanResult = scanResult;
        startTimer(delayMillis);
    }

    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        String action = intent.getAction();
        if (ACTION_NETWORK_STATE_CHANGED.equals(action)) {
            //Note here we dont check if has internet connectivity, because we only validate
            //if the connection to the hotspot is active, and not if the hotspot has internet.
            if (isTargetWifiConnect()) {
                stopTimer();
                connectSuccess();
            }
        } else if (ACTION_SUPPLICANT_STATE_CHANGED.equals(action)) {
            final SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
            final int supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);

            if (state == null) {
                stopTimer();
                connectFail();
                return;
            }

            switch (state) {
                case COMPLETED:
                    if (isTargetWifiConnect()) {
                        stopTimer();
                        connectSuccess();
                    }
                    break;
                case FOUR_WAY_HANDSHAKE:
                    if (isTargetWifiConnect()) {
                        stopTimer();
                        connectSuccess();
                    }
                    break;
                case DISCONNECTED:
                    if (supplicantError == WifiManager.ERROR_AUTHENTICATING) {
                        stopTimer();
                        connectFail();
                    } else {
                        stopTimer();
                        WifiUtils.reEnableNetworkIfPossible(mWifiManager, mScanResult);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void connectSuccess() {
        if (mWifiConnectionCallback != null) {
            mWifiConnectionCallback.successfulConnect();
        }
    }

    private void connectFail() {
        if (mWifiConnectionCallback != null) {
            mWifiConnectionCallback.errorConnect();
        }
    }

    private boolean isTargetWifiConnect() {
        return mScanResult != null && WifiUtils.isAlreadyConnected(mWifiManager, mScanResult.BSSID);
    }

    private void startTimer(long delayMillis) {
        disposable = Observable.just("")
                .delay(delayMillis, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        processTimeout();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }

    private void processTimeout() {
        WifiUtils.reEnableNetworkIfPossible(mWifiManager, mScanResult);

        if (isTargetWifiConnect()) {
            connectSuccess();
        } else {
            connectFail();
        }
    }

    private void stopTimer() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    /**
     * 连接wifi回调
     */
    public interface WifiConnectionCallback {
        /**
         * successfulConnect
         */
        void successfulConnect();

        /**
         * successfulConnect
         */
        void errorConnect();
    }
}
