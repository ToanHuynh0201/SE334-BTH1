package com.example.btth1.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.btth1.R;

public class PeerListStateView extends FrameLayout {

    public enum Mode {
        HIDDEN,
        LOADING,
        EMPTY
    }

    private ProgressBar progressPeerList;
    private TextView tvPeerStateTitle;
    private TextView tvPeerStateHint;

    public PeerListStateView(Context context) {
        super(context);
        init(context);
    }

    public PeerListStateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PeerListStateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_peer_list_state, this, true);
        progressPeerList = findViewById(R.id.progressPeerList);
        tvPeerStateTitle = findViewById(R.id.tvPeerStateTitle);
        tvPeerStateHint = findViewById(R.id.tvPeerStateHint);
    }

    public void render(Mode mode) {
        if (mode == Mode.HIDDEN) {
            setVisibility(GONE);
            return;
        }

        setVisibility(VISIBLE);
        if (mode == Mode.LOADING) {
            progressPeerList.setVisibility(VISIBLE);
            tvPeerStateTitle.setText(R.string.peer_list_loading);
            tvPeerStateHint.setText(R.string.status_discovering);
            return;
        }

        progressPeerList.setVisibility(GONE);
        tvPeerStateTitle.setText(R.string.peer_list_empty);
        tvPeerStateHint.setText(R.string.peer_list_empty_hint);
    }
}

