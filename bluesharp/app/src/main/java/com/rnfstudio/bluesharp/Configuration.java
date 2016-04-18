package com.rnfstudio.bluesharp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

/**
 * Created by Roger on 2016/4/18.
 */
public class Configuration {

    private static final String TAG = "[Utilities]";

    // recording parameters
    public static final int SAMPLE_RATE = 8000;
    public static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int BYTES_PER_ELEMENT = 2; // 2 bytes in 16bit format
    public static int BUFFER_ELEMENT_COUNT;

    // pitch tracking parameters
    public static final float MAX_PITCH = 1000.0f;
    public static final float MIN_PITCH = 40.0f;
    public static final int MAX_PITCH_POINT = (int) Math.floor(SAMPLE_RATE / MAX_PITCH);
    public static final int MIN_PITCH_POINT = (int) Math.floor(SAMPLE_RATE / MIN_PITCH);
    public static final float VOLUME_THRESHOLD = .125f;

    public static final float PITCH_BASE_IN_HERTZ = 440.0f;

    static {
        BUFFER_ELEMENT_COUNT = AudioRecord.getMinBufferSize(Configuration.SAMPLE_RATE,
                Configuration.CHANNELS,
                Configuration.ENCODING);
        Log.v(TAG, "Min. buffer size: " + BUFFER_ELEMENT_COUNT);
    }
}
