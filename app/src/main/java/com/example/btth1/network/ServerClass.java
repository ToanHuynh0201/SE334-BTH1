package com.example.btth1.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ServerClass extends Thread {

    public interface Callback {
        void onConnected(Socket socket);

        void onTimeout(int timeoutMs);

        void onError(IOException e);
    }

    private static final int DEFAULT_ACCEPT_TIMEOUT_MS = 15000;

    private final Callback callback;
    private final int acceptTimeoutMs;
    private ServerSocket serverSocket;

    public ServerClass(Callback callback) {
        this(callback, DEFAULT_ACCEPT_TIMEOUT_MS);
    }

    public ServerClass(Callback callback, int acceptTimeoutMs) {
        this.callback = callback;
        this.acceptTimeoutMs = Math.max(3000, acceptTimeoutMs);
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(8888);
            serverSocket.setSoTimeout(acceptTimeoutMs);
            Socket socket = serverSocket.accept();
            callback.onConnected(socket);
        } catch (SocketTimeoutException e) {
            callback.onTimeout(acceptTimeoutMs);
        } catch (IOException e) {
            callback.onError(e);
        }
    }

    public void closeServer() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
    }
}


