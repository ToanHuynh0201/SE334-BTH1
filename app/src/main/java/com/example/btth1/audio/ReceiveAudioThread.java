package com.example.btth1.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.io.IOException;
import java.io.InputStream;

public class ReceiveAudioThread extends Thread {

    private final InputStream inputStream;
    private AudioTrack audioTrack;

    public ReceiveAudioThread(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(Constants.SAMPLE_RATE)
                        .setEncoding(Constants.AUDIO_FORMAT)
                        .setChannelMask(Constants.CHANNEL_OUT)
                        .build())
                .setBufferSizeInBytes(Constants.BUFFER_SIZE)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        byte[] buffer = new byte[Constants.BUFFER_SIZE];
        audioTrack.play();

        while (!isInterrupted()) {
            try {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    audioTrack.write(buffer, 0, bytesRead);
                } else if (bytesRead < 0) {
                    break;
                }
            } catch (IOException e) {
                break;
            }
        }

        try {
            audioTrack.stop();
        } catch (IllegalStateException ignored) {
        }
        audioTrack.release();
    }
}

