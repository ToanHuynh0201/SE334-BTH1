package com.example.btth1.ui;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.btth1.R;

public class ConnectionStateView extends FrameLayout {

    public enum UiState {
        DISCONNECTED,
        DISCOVERING,
        CONNECTING,
        CONNECTED,
        TALKING
    }

    private TextView tvConnectionLabel;
    private TextView tvStatus;
    private TextView tvStatusHint;
    private ProgressBar progressConnection;

    public ConnectionStateView(Context context) {
        super(context);
        init(context);
    }

    public ConnectionStateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ConnectionStateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_connection_state, this, true);
        tvConnectionLabel = findViewById(R.id.tvConnectionLabel);
        tvStatus = findViewById(R.id.tvStatus);
        tvStatusHint = findViewById(R.id.tvStatusHint);
        progressConnection = findViewById(R.id.progressConnection);
    }

    public void render(UiState state, String statusText, @Nullable String hintText) {
        tvStatus.setText(statusText);

        if (TextUtils.isEmpty(hintText)) {
            tvStatusHint.setVisibility(GONE);
            tvStatusHint.setText(null);
        } else {
            tvStatusHint.setVisibility(VISIBLE);
            tvStatusHint.setText(hintText);
        }

        switch (state) {
            case DISCOVERING:
                progressConnection.setVisibility(VISIBLE);
                tvConnectionLabel.setText(R.string.status_discovering_label);
                break;
            case CONNECTING:
                progressConnection.setVisibility(VISIBLE);
                tvConnectionLabel.setText(R.string.status_connecting_label);
                break;
            case CONNECTED:
                progressConnection.setVisibility(GONE);
                tvConnectionLabel.setText(R.string.status_connected_label);
                break;
            case TALKING:
                progressConnection.setVisibility(VISIBLE);
                tvConnectionLabel.setText(R.string.status_talking_label);
                break;
            case DISCONNECTED:
            default:
                progressConnection.setVisibility(GONE);
                tvConnectionLabel.setText(R.string.status_disconnected_label);
                break;
        }
    }
}

