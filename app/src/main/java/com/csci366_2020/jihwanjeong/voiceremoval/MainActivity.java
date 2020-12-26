package com.csci366_2020.jihwanjeong.voiceremoval;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class MainActivity extends AppCompatActivity {

    private final String fileName = "audio2.wav";
    private TextView audioInfoTextView;
    private WavInfo wavInfo;
    private AudioTrack audioOut;
    private volatile PlayAudio audioOutThread;
    private boolean continuePlaying = false;
    ShortBuffer mSamples;
    int mNumSamples;
    short[] audioSamples;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioInfoTextView = findViewById(R.id.audioInfoTextView);

        // LOAD AUDIO SAMPLES TO ARRAY
        audioSamples = readWavData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // CREATION AND START OF AUDIO PLAYBACK THREAD
        audioOutThread = new PlayAudio();
        audioOutThread.start();
    }


    public void startPlaying(View view) {
        continuePlaying = true;

        // ALLOCATE SHORTBUFFER.
        mSamples = ShortBuffer.allocate(mNumSamples);

        // PUT AUDIO SAMPLES TO SHORTBUFFER.
        mSamples.put(audioSamples);
        audioOutThread.playThread();
    }

    public void startRemoveVocalPlaying(View view) {
        continuePlaying = true;

        // ALLOCATE SHORTBUFFER.
        mSamples = ShortBuffer.allocate(mNumSamples);

        // PUT AUDIO SAMPLES TO SHORTBUFFER.
        mSamples.put(audioSamples);
        audioOutThread.playRemoveVocalThread();
    }

    public void stopPlaying(View view) {
        audioOutThread.stopMusic();
    }

    private short[] readWavData() {
        // read WAV file
        InputStream in = getApplicationContext().getResources().openRawResource(R.raw.audio2);
        LoadWav loadWav = new LoadWav();
        int numOfChannels, samplingRate, numOfSamples;
        float lengthInSecond;
        byte[] byteData;
        short[] shortData;
        try {
            wavInfo = loadWav.readHeader(in); //read input file header
            numOfChannels = wavInfo.getSpec().getChannels();
            samplingRate = wavInfo.getSpec().getRate();
            numOfSamples = wavInfo.getSize() / 2; //each sample is 2 bytes (16-bit)
            mNumSamples = numOfSamples;
            lengthInSecond = (float) numOfSamples / (samplingRate * numOfChannels);
            byteData = loadWav.readWavPcm(wavInfo, in); //read samples
            shortData = new short[numOfSamples];
            // convert Audio data from 8-bit to 16-bit
            for (int i = 0; i<numOfSamples; i++) {
                short LSB = (short) byteData[2 * i];
                short MSB = (short) byteData[2 * i + 1];
                shortData[i] = (short) ( MSB << 8 | (0xFF & LSB) );
            }
            audioInfoTextView.setText(fileName + "\n" + "" +
                    "Number of channels : " + numOfChannels + "\n" +
                    "Sampling rate : " + samplingRate + " Hz" + "\n" +
                    "Number of samples : " + numOfSamples + "\n" +
                    "Duration : " + lengthInSecond + " seconds");
            return shortData;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public class PlayAudio extends Thread implements Runnable {

        boolean filtered = false;

        int sampleRate = 44100;
        int i = 0;
        int j = 0;
        int k = 0;
        int order = 1;
        int playStop = 0;
        int SR;
        int flag = 0;

        int movingAverage = 10;

        byte[] music;
        short[] musicFiltered;
        short[] music2Short;
        short[] buf;

        InputStream is;

        // STEREO
        private int minSize = AudioTrack.getMinBufferSize(sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        @Override
        public void run() {
            super.run();

            Thread thisThread = Thread.currentThread();

            /* Thread Loop, until audioOutThread is made null. */
            while (audioOutThread == thisThread) {
                // ORIGINAL PLAYING
                if (flag == 1) {
                    try {
                        audioOut.play();

                        while (((i = is.read(music)) != -1) && (playStop == 1)) {

                            /* Take the bytes vector in little endian and make it a shorts vector */
                            ByteBuffer.wrap(music).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music2Short);
                            audioOut.write(music, 0, i);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("IO_Sound", "audioOut.Play: finished playing.");
                    audioOut.stop();
                    audioOut.release();
                    flag = 0;

                    this.initialize();
                }

                // REMOVE VOCAL AND PLAY.
                else if (flag == 2) {
                    try {
                        audioOut.play();
                        short[] leftChannels;
                        short[] rightChannels;


                        while (((i = is.read(music)) != -1) && (playStop == 1)) {
                            /* Take the bytes vector in little endian and make it a shorts vector */
                            ByteBuffer.wrap(music).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(music2Short);

                            /* First order-1 samples of output vector, processed with data previously
                             * stored in the 'buf' buffer */
                            int left = 0, right = 0;

                            for (int i = 0; i < music2Short.length-1; i++) {
                                musicFiltered[i] = (short) ((music2Short[i] - music2Short[++i]));
                            }


                            audioOut.write(musicFiltered, 0, i / 2);

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d("IO_Sound", "audioOut.Play: finished playing.");
                    audioOut.stop();
                    audioOut.release();
                    flag = 0;

                    this.initialize();
                }
            }
        }

        /* Public constructor of the thread class for the Server  */
        public PlayAudio() {

            this.initialize();

            SR = audioOut.getSampleRate();

            if ((minSize / 2) % 2 != 0) {
                /*If minSize divided by 2 is odd, then subtract 1 and make it even*/
                musicFiltered = new short[((minSize / 2) - 1) / 2];
                music2Short = new short[((minSize / 2) - 1) / 2];
                music = new byte[(minSize / 2) - 1];
            } else {
                /* Else it is even already */
                musicFiltered = new short[minSize / 4];
                music2Short = new short[minSize / 4];
                music = new byte[minSize / 2];
            }

        }

        /* Thread method to start playback */
        public void playThread() {

            playStop = 0;        // Reset Play / Stop instruction
            flag = 1;        // Used for when reading the input file has finished.
            playStop = 1;        // Set ready to play.
        }

        public void playRemoveVocalThread() {
            playStop = 0;
            flag = 2;
            playStop = 1;
        }

        /* Thread method to stop playback */
        public void stopMusic() {
            flag = 0;
            playStop = 0;
        }

        /* This functions closes the thread by making serverThread1 null */
        public void closeThread() {
            audioOutThread = null;
        }

        public void initialize() {
            is = getResources().openRawResource(R.raw.audio2);

            audioOut = new AudioTrack(
                    AudioManager.STREAM_MUSIC,          // Stream Type
                    sampleRate,                         // Initial Sample Rate in Hz
                    AudioFormat.CHANNEL_OUT_STEREO,       // Channel Configuration
                    AudioFormat.ENCODING_PCM_16BIT,     // Audio Format
                    minSize,                            // Buffer Size in Bytes
                    AudioTrack.MODE_STREAM);            // Streaming static Buffer
        }
    }
}