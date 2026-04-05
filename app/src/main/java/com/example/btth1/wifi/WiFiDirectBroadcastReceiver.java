package com.example.btth1.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    public interface Callbacks {
        void onWifiP2pStateChanged(boolean enabled);

        void onPeersChanged();

        void onConnectionChanged(NetworkInfo networkInfo);

        void onThisDeviceChanged(WifiP2pDevice device);
    }

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final Callbacks callbacks;

    public WiFiDirectBroadcastReceiver(
            WifiP2pManager manager,
            WifiP2pManager.Channel channel,
            Callbacks callbacks
    ) {
        this.manager = manager;
        this.channel = channel;
        this.callbacks = callbacks;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            callbacks.onWifiP2pStateChanged(state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
            return;
        }

        if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null && channel != null) {
                callbacks.onPeersChanged();
            }
            return;
        }

        if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            callbacks.onConnectionChanged(networkInfo);
            return;
        }

        if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            callbacks.onThisDeviceChanged(device);
        }
    }
}

