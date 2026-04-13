package com.example.btth1;

import android.annotation.SuppressLint;
import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btth1.adapter.PeerListAdapter;
import com.example.btth1.audio.ReceiveAudioThread;
import com.example.btth1.audio.SendAudioThread;
import com.example.btth1.network.ClientClass;
import com.example.btth1.network.ServerClass;
import com.example.btth1.ui.ConnectionStateView;
import com.example.btth1.ui.PeerListStateView;
import com.example.btth1.wifi.WiFiDirectBroadcastReceiver;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        PeerListAdapter.OnPeerClickListener,
        WifiP2pManager.PeerListListener,
        WifiP2pManager.ConnectionInfoListener,
        WiFiDirectBroadcastReceiver.Callbacks {

    private static final int REQUEST_CODE_PERMISSIONS = 1001;
    private static final int MAX_P2P_CONNECT_RETRIES = 3;
    private static final long INITIAL_P2P_BACKOFF_MS = 1000L;
    private static final long CONNECTION_INFO_TIMEOUT_MS = 15000L;
    private static final int SERVER_ACCEPT_TIMEOUT_MS = 15000;
    private static final int CLIENT_CONNECT_TIMEOUT_MS = 5000;
    private static final long CLIENT_INITIAL_BACKOFF_MS = 1000L;
    private static final int CLIENT_MAX_RETRIES = 3;

    private ConnectionStateView connectionStateView;
    private TextView tvTalkHint;
    private Button btnDiscover;
    private Button btnTalk;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean receiverRegistered;

    private final List<WifiP2pDevice> peers = new ArrayList<>();
    private PeerListAdapter peerListAdapter;

    private ServerClass serverClass;
    private Socket socket;
    private SendAudioThread sendAudioThread;
    private ReceiveAudioThread receiveAudioThread;
    private WifiP2pDevice pendingPeer;
    private String connectedPeerAddress;
    private boolean discoveryInProgress;
    private PeerListStateView peerListStateView;
    private ConnectionStateView.UiState currentConnectionUiState = ConnectionStateView.UiState.DISCONNECTED;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable connectionTimeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initWifiDirect();
        requestPermissions();
        setupActions();
    }

    private void initViews() {
        connectionStateView = findViewById(R.id.connectionStateView);
        peerListStateView = findViewById(R.id.peerListStateView);
        tvTalkHint = findViewById(R.id.tvTalkHint);
        btnDiscover = findViewById(R.id.btnDiscover);
        btnTalk = findViewById(R.id.btnTalk);

        RecyclerView rvPeers = findViewById(R.id.rvPeers);
        peerListAdapter = new PeerListAdapter(peers, this);
        rvPeers.setLayoutManager(new LinearLayoutManager(this));
        rvPeers.setAdapter(peerListAdapter);

        renderPeerListState();
        renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_idle), null);
        updateTalkUiState(false, false);
    }

    private void initWifiDirect() {
        manager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        if (manager != null) {
            channel = manager.initialize(this, getMainLooper(), null);
        }

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    private void setupActions() {
        btnDiscover.setOnClickListener(v -> discoverPeers());

        btnTalk.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                startTalking();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                stopTalking();
                v.performClick();
                return true;
            }
            return false;
        });
    }

    @SuppressLint("MissingPermission")
    private void discoverPeers() {
        if (!hasRuntimePermissions()) {
            requestPermissions();
            return;
        }
        if (manager == null || channel == null) {
            showStatus(getString(R.string.status_idle));
            return;
        }

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                discoveryInProgress = true;
                renderPeerListState();
                renderConnectionState(ConnectionStateView.UiState.DISCOVERING, getString(R.string.status_discovering), null);
            }

            @Override
            public void onFailure(int reason) {
                discoveryInProgress = false;
                renderPeerListState();
                renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_idle), null);
                Toast.makeText(MainActivity.this, getString(R.string.toast_discover_failed, reason), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectToPeer(WifiP2pDevice device) {
        if (manager == null || channel == null || device == null) {
            return;
        }

        if (!hasRuntimePermissions()) {
            requestPermissions();
            return;
        }

        pendingPeer = device;
        peerListAdapter.setConnectingDeviceAddress(device.deviceAddress);
        connectWithRetry(device, 1);
    }

    private void connectWithRetry(WifiP2pDevice device, int attempt) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        renderConnectionState(
                ConnectionStateView.UiState.CONNECTING,
                getString(R.string.status_connecting, getDisplayName(device), attempt, MAX_P2P_CONNECT_RETRIES),
                getString(R.string.status_connecting_socket)
        );
        connectWithPermissionChecked(config, device, attempt);
    }

    @SuppressLint("MissingPermission")
    private void connectWithPermissionChecked(WifiP2pConfig config, WifiP2pDevice device, int attempt) {
        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                renderConnectionState(ConnectionStateView.UiState.CONNECTING, getString(R.string.status_connecting_socket), null);
                startConnectionTimeoutWatchdog();
            }

            @Override
            public void onFailure(int reason) {
                if (attempt < MAX_P2P_CONNECT_RETRIES) {
                    long delayMs = INITIAL_P2P_BACKOFF_MS * (1L << (attempt - 1));
                    renderConnectionState(ConnectionStateView.UiState.CONNECTING, getString(R.string.status_retrying, delayMs), null);
                    mainHandler.postDelayed(() -> connectWithRetry(device, attempt + 1), delayMs);
                    return;
                }

                peerListAdapter.setConnectingDeviceAddress(null);
                renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_connect_failed), null);
                Toast.makeText(MainActivity.this, getString(R.string.toast_connect_failed, reason), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startConnectionTimeoutWatchdog() {
        clearConnectionTimeoutWatchdog();
        connectionTimeoutRunnable = () -> {
            if (socket == null || !socket.isConnected()) {
                String status = pendingPeer == null
                        ? getString(R.string.status_timeout)
                        : getString(R.string.status_timeout_device, getDisplayName(pendingPeer));
                peerListAdapter.setConnectingDeviceAddress(null);
                renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, status, null);
            }
        };
        mainHandler.postDelayed(connectionTimeoutRunnable, CONNECTION_INFO_TIMEOUT_MS);
    }

    private void clearConnectionTimeoutWatchdog() {
        if (connectionTimeoutRunnable != null) {
            mainHandler.removeCallbacks(connectionTimeoutRunnable);
            connectionTimeoutRunnable = null;
        }
    }

    private String getDisplayName(WifiP2pDevice device) {
        if (device == null) {
            return getString(R.string.peer_name_unknown);
        }
        if (device.deviceName == null || device.deviceName.trim().isEmpty()) {
            return device.deviceAddress;
        }
        return device.deviceName;
    }

    private void startTalking() {
        if (socket == null || !socket.isConnected()) {
            Toast.makeText(this, R.string.toast_socket_not_connected, Toast.LENGTH_SHORT).show();
            updateTalkUiState(false, false);
            return;
        }
        if (sendAudioThread != null && sendAudioThread.isAlive()) {
            return;
        }

        try {
            sendAudioThread = new SendAudioThread(socket.getOutputStream());
            sendAudioThread.startRecording();
            sendAudioThread.start();
            renderConnectionState(ConnectionStateView.UiState.TALKING, getString(R.string.status_talking), null);
            updateTalkUiState(true, true);
        } catch (IOException e) {
            Toast.makeText(this, R.string.toast_cannot_start_microphone, Toast.LENGTH_SHORT).show();
            updateTalkUiState(true, false);
        }
    }

    private void stopTalking() {
        if (sendAudioThread != null) {
            sendAudioThread.stopRecording();
            sendAudioThread = null;
        }
        if (socket != null && socket.isConnected()) {
            renderConnectionState(ConnectionStateView.UiState.CONNECTED, getString(R.string.status_connected), null);
            updateTalkUiState(true, false);
        } else {
            updateTalkUiState(false, false);
        }
    }

    private void updateTalkUiState(boolean connected, boolean talking) {
        if (btnTalk != null) {
            btnTalk.setEnabled(connected);
            btnTalk.setAlpha(connected ? 1.0f : 0.5f);
        }

        if (tvTalkHint != null) {
            if (talking) {
                tvTalkHint.setText(R.string.talk_hint_talking);
            } else if (connected) {
                tvTalkHint.setText(R.string.talk_hint_connected);
            } else {
                tvTalkHint.setText(R.string.talk_hint_disconnected);
            }
        }
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        permissions.add(Manifest.permission.RECORD_AUDIO);

        List<String> toRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                toRequest.add(permission);
            }
        }

        if (!toRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, toRequest.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean hasRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showStatus(String status) {
        runOnUiThread(() -> renderConnectionState(currentConnectionUiState, status, null));
    }

    private void renderConnectionState(ConnectionStateView.UiState uiState, String statusText, String hintText) {
        currentConnectionUiState = uiState;
        if (connectionStateView != null) {
            connectionStateView.render(uiState, statusText, hintText);
        }
    }

    private void renderPeerListState() {
        if (peerListStateView == null) {
            return;
        }

        if (discoveryInProgress && peers.isEmpty()) {
            peerListStateView.render(PeerListStateView.Mode.LOADING);
            return;
        }

        if (!discoveryInProgress && peers.isEmpty()) {
            peerListStateView.render(PeerListStateView.Mode.EMPTY);
            return;
        }

        peerListStateView.render(PeerListStateView.Mode.HIDDEN);
    }

    private void onSocketConnected(Socket connectedSocket) {
        clearConnectionTimeoutWatchdog();
        closeSocketOnly();
        socket = connectedSocket;
        connectedPeerAddress = pendingPeer != null ? pendingPeer.deviceAddress : connectedPeerAddress;
        peerListAdapter.setConnectingDeviceAddress(null);
        peerListAdapter.setConnectedDeviceAddress(connectedPeerAddress);
        pendingPeer = null;

        try {
            receiveAudioThread = new ReceiveAudioThread(socket.getInputStream());
            receiveAudioThread.start();
            String status = connectedPeerAddress == null
                    ? getString(R.string.status_connected)
                    : getString(R.string.status_connected_to, connectedPeerAddress);
            renderConnectionState(ConnectionStateView.UiState.CONNECTED, status, null);
            updateTalkUiState(true, false);
        } catch (IOException e) {
            Toast.makeText(this, R.string.toast_cannot_start_receiver, Toast.LENGTH_SHORT).show();
            updateTalkUiState(false, false);
        }
    }

    private void closeSocketOnly() {
        if (sendAudioThread != null) {
            sendAudioThread.stopRecording();
            sendAudioThread = null;
        }

        if (receiveAudioThread != null) {
            receiveAudioThread.interrupt();
            receiveAudioThread = null;
        }

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }

        peerListAdapter.setConnectedDeviceAddress(null);
        updateTalkUiState(false, false);
    }

    private void cleanupAllConnections() {
        clearConnectionTimeoutWatchdog();
        mainHandler.removeCallbacksAndMessages(null);
        closeSocketOnly();

        if (serverClass != null) {
            serverClass.closeServer();
            serverClass = null;
        }

        if (manager != null && channel != null) {
            manager.removeGroup(channel, null);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!receiverRegistered) {
            registerReceiver(receiver, intentFilter);
            receiverRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (receiverRegistered) {
            unregisterReceiver(receiver);
            receiverRegistered = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanupAllConnections();
    }

    @Override
    public void onPeerClick(WifiP2pDevice device) {
        connectToPeer(device);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        discoveryInProgress = false;
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        peerListAdapter.notifyDataSetChanged();
        renderPeerListState();

        if (peers.isEmpty()) {
            renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_idle), null);
        } else {
            showStatus(getString(R.string.status_found_peers, peers.size()));
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        clearConnectionTimeoutWatchdog();

        if (info == null || info.groupOwnerAddress == null || !info.groupFormed) {
            return;
        }

        if (info.isGroupOwner) {
            renderConnectionState(ConnectionStateView.UiState.CONNECTING, getString(R.string.status_waiting_client), null);
            if (serverClass != null) {
                serverClass.closeServer();
            }
            serverClass = new ServerClass(new ServerClass.Callback() {
                @Override
                public void onConnected(Socket socket) {
                    onSocketConnected(socket);
                }

                @Override
                public void onTimeout(int timeoutMs) {
                    runOnUiThread(() -> renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_server_timeout), null));
                }

                @Override
                public void onError(IOException e) {
                    runOnUiThread(() -> {
                        renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_connect_failed), null);
                        Toast.makeText(MainActivity.this, R.string.toast_server_error, Toast.LENGTH_SHORT).show();
                    });
                }
            }, SERVER_ACCEPT_TIMEOUT_MS);
            serverClass.start();
        } else {
            renderConnectionState(ConnectionStateView.UiState.CONNECTING, getString(R.string.status_connecting_socket), null);
            ClientClass client = new ClientClass(info.groupOwnerAddress, new ClientClass.Callback() {
                @Override
                public void onConnected(Socket socket) {
                    onSocketConnected(socket);
                }

                @Override
                public void onRetry(int attempt, int maxRetries, long backoffMs) {
                    runOnUiThread(() -> renderConnectionState(
                            ConnectionStateView.UiState.CONNECTING,
                            getString(R.string.status_client_retry, attempt, maxRetries, backoffMs),
                            null
                    ));
                }

                @Override
                public void onError(IOException e) {
                    runOnUiThread(() -> {
                        renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_client_connect_failed), null);
                        Toast.makeText(MainActivity.this, R.string.toast_client_error, Toast.LENGTH_SHORT).show();
                    });
                }
            }, CLIENT_MAX_RETRIES, CLIENT_CONNECT_TIMEOUT_MS, CLIENT_INITIAL_BACKOFF_MS);
            client.start();
        }
    }

    @Override
    public void onWifiP2pStateChanged(boolean enabled) {
        if (enabled) {
            renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_idle), null);
        } else {
            renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_wifi_direct_disabled), null);
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onPeersChanged() {
        if (manager == null || channel == null || !hasRuntimePermissions()) {
            return;
        }
        manager.requestPeers(channel, this);
    }

    @Override
    public void onConnectionChanged(NetworkInfo networkInfo) {
        if (networkInfo == null) {
            return;
        }

        if (networkInfo.isConnected()) {
            if (manager != null && channel != null && hasRuntimePermissions()) {
                manager.requestConnectionInfo(channel, this);
            }
        } else {
            clearConnectionTimeoutWatchdog();
            pendingPeer = null;
            connectedPeerAddress = null;
            peerListAdapter.setConnectingDeviceAddress(null);
            peerListAdapter.setConnectedDeviceAddress(null);
            renderConnectionState(ConnectionStateView.UiState.DISCONNECTED, getString(R.string.status_idle), null);
            closeSocketOnly();
        }
    }

    @Override
    public void onThisDeviceChanged(WifiP2pDevice device) {
        if (device == null) {
            return;
        }

        String deviceName = device.deviceName == null || device.deviceName.trim().isEmpty()
                ? device.deviceAddress
                : device.deviceName;
        showStatus(getString(R.string.status_this_device, deviceName));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE_PERMISSIONS) {
            return;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }
}
