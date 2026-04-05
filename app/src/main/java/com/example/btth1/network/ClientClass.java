package com.example.btth1.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientClass extends Thread {

    public interface Callback {
        void onConnected(Socket socket);

        void onRetry(int attempt, int maxRetries, long backoffMs);

        void onError(IOException e);
    }

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final long DEFAULT_INITIAL_BACKOFF_MS = 1000L;

    private final Callback callback;
    private final String hostAddress;
    private final int maxRetries;
    private final int connectTimeoutMs;
    private final long initialBackoffMs;

    public ClientClass(InetAddress hostAddress, Callback callback) {
        this(hostAddress, callback, DEFAULT_MAX_RETRIES, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_INITIAL_BACKOFF_MS);
    }

    public ClientClass(
            InetAddress hostAddress,
            Callback callback,
            int maxRetries,
            int connectTimeoutMs,
            long initialBackoffMs
    ) {
        this.hostAddress = hostAddress.getHostAddress();
        this.callback = callback;
        this.maxRetries = Math.max(1, maxRetries);
        this.connectTimeoutMs = Math.max(1000, connectTimeoutMs);
        this.initialBackoffMs = Math.max(250L, initialBackoffMs);
    }

    @Override
    public void run() {
        IOException lastError = null;
        long backoffMs = initialBackoffMs;

        for (int attempt = 1; attempt <= maxRetries && !isInterrupted(); attempt++) {
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress(hostAddress, 8888), connectTimeoutMs);
                callback.onConnected(socket);
                return;
            } catch (IOException e) {
                lastError = e;
                try {
                    socket.close();
                } catch (IOException ignored) {
                }

                if (attempt < maxRetries) {
                    callback.onRetry(attempt + 1, maxRetries, backoffMs);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException interruptedException) {
                        interrupt();
                        callback.onError(new IOException("Client connection interrupted", interruptedException));
                        return;
                    }
                    backoffMs = Math.min(backoffMs * 2L, 4000L);
                }
            }
        }

        callback.onError(lastError != null ? lastError : new IOException("Client failed to connect"));
    }
}


