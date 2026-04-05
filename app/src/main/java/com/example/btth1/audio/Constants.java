package com.example.btth1.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;

public final class Constants {

    public static final int SAMPLE_RATE = 16000;
    public static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BUFFER_SIZE = Math.max(
            AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, AUDIO_FORMAT),
            2048
    );

    private Constants() {
    }
}

