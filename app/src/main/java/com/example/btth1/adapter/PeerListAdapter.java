package com.example.btth1.adapter;

import android.net.wifi.p2p.WifiP2pDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btth1.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;

import java.util.List;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.PeerViewHolder> {

    public interface OnPeerClickListener {
        void onPeerClick(WifiP2pDevice device);
    }

    private final List<WifiP2pDevice> peers;
    private final OnPeerClickListener listener;
    private String connectingDeviceAddress;
    private String connectedDeviceAddress;

    public PeerListAdapter(List<WifiP2pDevice> peers, OnPeerClickListener listener) {
        this.peers = peers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peer, parent, false);
        return new PeerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        WifiP2pDevice device = peers.get(position);
        String name = device.deviceName == null || device.deviceName.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.peer_name_unknown)
                : device.deviceName;
        holder.tvDeviceName.setText(name);
        holder.tvDeviceAddress.setText(holder.itemView.getContext().getString(R.string.peer_address, device.deviceAddress));

        String chipText = resolvePeerChipText(holder, device);
        holder.chipPeerState.setText(chipText);

        MaterialCardView cardView = (MaterialCardView) holder.itemView;
        boolean isActivePeer = TextUtils.equals(device.deviceAddress, connectingDeviceAddress)
                || TextUtils.equals(device.deviceAddress, connectedDeviceAddress);
        cardView.setStrokeWidth(isActivePeer ? 3 : 1);

        holder.itemView.setOnClickListener(v -> listener.onPeerClick(device));
    }

    private String resolvePeerChipText(@NonNull PeerViewHolder holder, WifiP2pDevice device) {
        if (TextUtils.equals(device.deviceAddress, connectingDeviceAddress)) {
            return holder.itemView.getContext().getString(R.string.peer_state_connecting);
        }

        if (TextUtils.equals(device.deviceAddress, connectedDeviceAddress)
                || device.status == WifiP2pDevice.CONNECTED) {
            return holder.itemView.getContext().getString(R.string.peer_state_connected);
        }

        switch (device.status) {
            case WifiP2pDevice.INVITED:
                return holder.itemView.getContext().getString(R.string.peer_state_invited);
            case WifiP2pDevice.FAILED:
                return holder.itemView.getContext().getString(R.string.peer_state_failed);
            case WifiP2pDevice.UNAVAILABLE:
                return holder.itemView.getContext().getString(R.string.peer_state_unavailable);
            case WifiP2pDevice.AVAILABLE:
            default:
                return holder.itemView.getContext().getString(R.string.peer_state_available);
        }
    }

    public void setConnectingDeviceAddress(String connectingDeviceAddress) {
        this.connectingDeviceAddress = connectingDeviceAddress;
        notifyDataSetChanged();
    }

    public void setConnectedDeviceAddress(String connectedDeviceAddress) {
        this.connectedDeviceAddress = connectedDeviceAddress;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return peers.size();
    }

    static class PeerViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        Chip chipPeerState;

        PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            chipPeerState = itemView.findViewById(R.id.chipPeerState);
        }
    }
}

