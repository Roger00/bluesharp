package com.rnfstudio.bluesharp;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

/**
 * See <a href="http://goo.gl/1N75IC">Android AudioRecord example</a>
 */
public class MainActivity extends AppCompatActivity {

    private static final String RECORDING_THREAD_NAME = "AudioRecorder Thread";

    private AudioRecord recorder;
    private Thread recordingThread;
    private boolean mIsRecording = false;

    private TextView volumeText;
    private TextView pitchText;
    private HarpView harpView;
    private WaveView waveView;
    private Handler mainHandler;
    private PitchTrackerFactory.PitchTracker pitchTracker;

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

        volumeText = (TextView) findViewById(R.id.volumeText);
        pitchText = (TextView) findViewById(R.id.pitchText);
        harpView = (HarpView) findViewById(R.id.harpView);
        waveView = (WaveView) findViewById(R.id.waveView);
        mainHandler = new Handler(Looper.getMainLooper());
        pitchTracker = PitchTrackerFactory.getACFPitchTracker();
    }

    private void analyzeAudioData() {
        short sData[] = new short[Configuration.BUFFER_ELEMENT_COUNT];

        while (mIsRecording) {

            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, Configuration.BUFFER_ELEMENT_COUNT);
            float decibel = Utilities.getDecibelVolume(sData);
            float pitchInHertz = pitchTracker.getPitchInHertz(sData);

            // volume threshold for pitch-tracking
            if (decibel < Utilities.getMaxDecibelVolume() * Configuration.VOLUME_THRESHOLD) {
                pitchInHertz = 0;
                waveView.setData(WaveView.KEY_RAW_DATA, sData);
            }

            notifyVolumeAndPitch(decibel, pitchInHertz);
        }
    }

    private void notifyVolumeAndPitch(final float volume,
                                      final float pitchInHertz) {
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                float semitone = Utilities.pitch2Semitone(pitchInHertz);
                volumeText.setText(String.format("%.1f dB", volume));
                pitchText.setText(String.format("%.1f Hz\n%.1f Semitone", pitchInHertz, semitone));

                harpView.setHighlight(semitone);
                waveView.invalidate();
            }
        };
        mainHandler.post(myRunnable);
    }

    private void startRecording() {

        // create recorder
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                Configuration.SAMPLE_RATE,
                Configuration.CHANNELS,
                Configuration.ENCODING,
                Configuration.BUFFER_ELEMENT_COUNT * Configuration.BYTES_PER_ELEMENT);
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
