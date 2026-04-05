package com.example.btth1.audio;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.IOException;
import java.io.OutputStream;

public class SendAudioThread extends Thread {

    private final OutputStream outputStream;
    private volatile boolean isRecording;
    private AudioRecord audioRecord;

    public SendAudioThread(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void startRecording() {
        isRecording = true;
    }

    public void stopRecording() {
        isRecording = false;
        interrupt();
    }

    @Override
    @SuppressLint("MissingPermission")
    public void run() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                Constants.SAMPLE_RATE,
                Constants.CHANNEL_IN,
                Constants.AUDIO_FORMAT,
                Constants.BUFFER_SIZE
        );

        byte[] buffer = new byte[Constants.BUFFER_SIZE];
        audioRecord.startRecording();

        while (isRecording && !isInterrupted()) {
            int bytesRead = audioRecord.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                try {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                } catch (IOException e) {
                    break;
                }
            }
        }

        try {
            audioRecord.stop();
        } catch (IllegalStateException ignored) {
        }
        audioRecord.release();
    }
}


