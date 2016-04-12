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

    // recording parameters
    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BYTES_PER_ELEMENT = 2; // 2 bytes in 16bit format
    private static int BUFFER_ELEMENT_COUNT;

    private static final String RECORDING_THREAD_NAME = "AudioRecorder Thread";

    private AudioRecord recorder;
    private Thread recordingThread;
    private boolean mIsRecording = false;

    private TextView volumeText;
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

            notifyVolume(simpleVolume, decibel);
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

    private void notifyVolume(final int value, final float decibel) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                volumeText.setText(String.format("%.1f dB", decibel));
            }
        };
        mainHandler.post(myRunnable);
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
