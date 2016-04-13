package com.rnfstudio.bluesharp;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Arrays;

/**
 * See <a href="http://goo.gl/1N75IC">Android AudioRecord example</a>
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "[MainActivity]";
    private static final boolean DEBUG = false;

    // recording parameters
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BYTES_PER_ELEMENT = 2; // 2 bytes in 16bit format
    private static int BUFFER_ELEMENT_COUNT;

    private static final String RECORDING_THREAD_NAME = "AudioRecorder Thread";

    // ACF pitch tracking
    private static final float ACF_MAX_PITCH = 1000.0f;
    private static final float ACF_MIN_PITCH = 40.0f;
    private static final int ACF_MAX_PITCH_POINT = (int) Math.floor(SAMPLE_RATE / ACF_MAX_PITCH);
    private static final int ACF_MIN_PITCH_POINT = (int) Math.floor(SAMPLE_RATE / ACF_MIN_PITCH);
    private static final float BASE_A_PITCH = 440.0f;

    private AudioRecord recorder;
    private Thread recordingThread;
    private boolean mIsRecording = false;

    private TextView volumeText;
    private TextView pitchText;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        BUFFER_ELEMENT_COUNT = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNELS, ENCODING);
        Log.d(TAG, "Min. buffer size: " + BUFFER_ELEMENT_COUNT);

        volumeText = (TextView) findViewById(R.id.volumeText);
        pitchText = (TextView) findViewById(R.id.pitchText);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void startRecording() {

        // create recorder
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNELS,
                ENCODING,
                BUFFER_ELEMENT_COUNT * BYTES_PER_ELEMENT);
        recorder.startRecording();

        // start recording thread (to transfer data)
        recordingThread = new Thread(new Runnable() {

            public void run() {
                analyzeAudioData();
            }

        }, RECORDING_THREAD_NAME);
        recordingThread.start();

        // set recording flag to true
        mIsRecording = true;
    }

    private void analyzeAudioData() {
        short sData[] = new short[BUFFER_ELEMENT_COUNT];

        while (mIsRecording) {
            // gets the voice output from microphone to byte format

            recorder.read(sData, 0, BUFFER_ELEMENT_COUNT);
//            System.out.println("Short wirting to file" + Arrays.toString(sData));
            int simpleVolume = getSimpleVolume(sData);
            float decibel = getDecibelVolume(sData);
            float acfPitch = getAcfPitch(sData);
            float amdfPitch = getAMDFPitch(sData);

            // volume threshold for pitch-tracking
            if (decibel < getMaxDecibelVolume() * .0125f) {
                acfPitch = 0;
                amdfPitch = 0;
            }
            notifyVolumeAndPitch(simpleVolume, decibel, acfPitch, amdfPitch);
        }
    }

    private void stopRecording() {

        if (recorder != null) {
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;

            // set flag to false
            mIsRecording = false;
        }
    }

    private int getSimpleVolume(short[] data) {
        short median = (short) getMedianFromArray(data);

        int sum = 0;
        for (short aData : data) {
            sum += Math.abs(aData - median);
        }

        return sum;
    }

    private float getDecibelVolume(short[] data) {
        float mean = getMeanFromArray(data);

        float sum = 0;
        for (short d : data) {
            float v = ((float) d) - mean;
            sum += v * v;
        }

        return (float) (10 * Math.log10(sum) - 70);
    }

    private float getMaxDecibelVolume() {
        float max = Short.MAX_VALUE;
        int cPoints = BUFFER_ELEMENT_COUNT;
        float sum = max * max * cPoints;

        return (float) (10 * Math.log10(sum) - 70);
    }

    private float getMeanFromArray(short[] data) {
        int sum = 0;
        for (short d : data) {
            sum += d;
        }
        return ((float) sum) / data.length;
    }

    /**
     * See <a href="http://goo.gl/N8FgT1">How to calculate the median of an array?</a>
     */
    private float getMedianFromArray(short[] data) {
        // sort the copy of data array
        short[] dataCopy = Arrays.copyOf(data, data.length);
        Arrays.sort(dataCopy);

        // get median
        int centerIndex = dataCopy.length / 2;
        if (dataCopy.length % 2 == 0)
            return (dataCopy[centerIndex] + dataCopy[centerIndex - 1]) / 2f;
        else
            return dataCopy[centerIndex];
    }

    private void notifyVolumeAndPitch(final int value,
                                      final float decibel,
                                      final float acfPitch,
                                      final float amdfPitch) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                float acfSemitone = pitch2Semitone(acfPitch);
                float amdfSemitone = pitch2Semitone(amdfPitch);
                volumeText.setText(String.format("%.1f dB", decibel));
                pitchText.setText(String.format("%.1f Hz(%.1f Semitone), acf-amdf semitone(%.1f)",
                        amdfPitch, amdfSemitone, acfSemitone-amdfSemitone));
            }
        };
        mainHandler.post(myRunnable);
    }

    private float getAcfPitch(short[] data) {
        int[] shiftedInnerProduct = new int[data.length];

        for (int i = 0; i < data.length; i++) {

            // computing shifted inner product at point i
            int sum = 0;
            for (int j = 0; j < data.length; j++) {
                if ((i + j) > data.length - 1) break;
                sum += data[j] * data[j + i];
            }
            shiftedInnerProduct[i] = sum;
        }

        // set starting and ending data to prevent unreasonable pitch
        for (int i = 0; i < data.length; i++) {
            if (i < ACF_MAX_PITCH_POINT || i > ACF_MIN_PITCH_POINT) {
                shiftedInnerProduct[i] = Integer.MIN_VALUE;
            }
        }

        return ((float) SAMPLE_RATE) / getMaxValue(shiftedInnerProduct, true);
    }

    private float getAMDFPitch(short[] data) {
        int[] shiftedSumOfAbsDiff = new int[data.length];

        for (int i = 0; i < data.length; i++) {

            // computing shifted sum of absolute difference at point i
            int sum = 0;
            for (int j = 0; j < data.length; j++) {
                if ((i + j) > data.length - 1) break;
                sum += Math.abs(data[j] - data[j + i]);
            }
            shiftedSumOfAbsDiff[i] = sum;
        }

        // add augmentation terms for latter data; and flip data array upside-down
        int max = getMaxValue(shiftedSumOfAbsDiff, false);
        for (int i = 0; i < data.length; i++) {
            shiftedSumOfAbsDiff[i] += (max * ((float) i / data.length));
            shiftedSumOfAbsDiff[i] *= -1;
        }

        // set starting and ending data to prevent unreasonable pitch
        for (int i = 0; i < data.length; i++) {
            if (i < ACF_MAX_PITCH_POINT || i > ACF_MIN_PITCH_POINT) {
                shiftedSumOfAbsDiff[i] = Integer.MIN_VALUE;
            }
        }

        return ((float) SAMPLE_RATE) / getMaxValue(shiftedSumOfAbsDiff, true);
    }

    private int getMaxValue(int[] data, boolean getIndex) {
        if (data.length == 0) {
            return Integer.MIN_VALUE;
        }

        int maxIndex = 0;
        int max = data[maxIndex];
        for (int i = 1; i < data.length; i++) {
            if (data[i] > max) {
                max = data[i];
                maxIndex = i;
            }
        }
        return getIndex ? maxIndex : max;
    }

    private float pitch2Semitone(float pitch) {
        return (float) (69f + 12 * Math.log(pitch / BASE_A_PITCH) / Math.log(2));
    }

    @Override
    protected void onResume() {
        super.onResume();
        startRecording();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecording();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
