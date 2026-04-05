package com.example.btth1.adapter;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btth1.R;

import java.util.List;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.PeerViewHolder> {

    public interface OnPeerClickListener {
        void onPeerClick(WifiP2pDevice device);
    }

    private final List<WifiP2pDevice> peers;
    private final OnPeerClickListener listener;

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
        holder.itemView.setOnClickListener(v -> listener.onPeerClick(device));
    }

    @Override
    public int getItemCount() {
        return peers.size();
    }

    static class PeerViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceAddress;

        PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
        }
    }
}

