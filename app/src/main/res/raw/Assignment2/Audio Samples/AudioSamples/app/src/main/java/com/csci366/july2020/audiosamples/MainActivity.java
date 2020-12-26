package com.csci366.july2020.audiosamples;

import androidx.appcompat.app.AppCompatActivity;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ShortBuffer;

public class MainActivity extends AppCompatActivity {

    private final String fileName = "buddy.wav";
    private TextView tvInfo;
    private WavInfo wavInfo;
    private AudioTrack audioTrack;
    private boolean continuePlaying = false;
    ShortBuffer mSamples;   // the samples to play
    int mNumSamples;        // number of samples to play
    short[] audioSamples;   // "buddy.wav" is 16-bit per sample

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvInfo = (TextView)findViewById(R.id.textViewInfo);
        // Load audio samples to array
        audioSamples = readWavData();
    }

    public void startPlaying(View view) {
        continuePlaying = true;

        // Allocate ShortBuffer
        mSamples = ShortBuffer.allocate(mNumSamples);
        // put audio samples to ShortBuffer
        mSamples.put(audioSamples);
        playAudio();
    }

    // Down sample by 2 times. i.e. if originally we have 10000 samples, reduce to 5000
    public void downSample(View view) {
        continuePlaying = true;

        short[] downAudioSamples = new short[mNumSamples/2];
        for (int i=0; i<mNumSamples/2; i++) {
            downAudioSamples[i] = audioSamples[i*2];
        }

        // Allocate ShortBuffer
        mSamples = ShortBuffer.allocate(mNumSamples/2);
        // put data to ShortBuffer
        mSamples.put(downAudioSamples);
        playAudio();

    }

    // Up sample by 2 times. i.e. if originally we have 10000 samples, increase to 20000
    public void upSample(View view) {
        continuePlaying = true;

        short[] upAudioSamples = new short[mNumSamples*2];
        for (int i=0; i<mNumSamples*2; i++) {
            upAudioSamples[i] = audioSamples[i/2];

        }

        // Allocate ShortBuffer
        mSamples = ShortBuffer.allocate(mNumSamples*2);
        // put data to ShortBuffer
        mSamples.put(upAudioSamples);
        playAudio();

    }

    // Up sample by 2 times. i.e. if originally we have 10000 samples, increase to 20000
    public void reversePlay(View view) {
        continuePlaying = true;

        short[] reverseAudioSamples = new short[mNumSamples];
        for (int i=0; i<mNumSamples; i++) {
            reverseAudioSamples[i] = audioSamples[mNumSamples-i-1];
        }

        // Allocate ShortBuffer
        mSamples = ShortBuffer.allocate(mNumSamples);
        // put data to ShortBuffer
        mSamples.put(reverseAudioSamples);
        playAudio();

    }

    public void stopPlaying(View view) {
        continuePlaying = false;
    }


    private short[] readWavData() {
        // read WAV file
        InputStream in = getApplicationContext().getResources().openRawResource(R.raw.buddy);
        LoadWav loadWav = new LoadWav();
        int numOfChannels, samplingRate, numOfSamples;
        float lengthInSecond;
        byte[] byteData;
        short[] shortData;
        try {
            wavInfo = loadWav.readHeader(in);       //read input file header
            numOfChannels = wavInfo.getSpec().getChannels();
            samplingRate = wavInfo.getSpec().getRate();
            numOfSamples = wavInfo.getSize() / 2;       //each sample is 2 bytes (16-bit)
            mNumSamples = numOfSamples;
            lengthInSecond = (float) numOfSamples / (samplingRate * numOfChannels);
            byteData = loadWav.readWavPcm(wavInfo, in);   //read samples
            shortData = new short[numOfSamples];
            // convert Audio data from 8-bit to 16-bit
            for (int i = 0; i<numOfSamples; i++) {
                short LSB = (short) byteData[2 * i];
                short MSB = (short) byteData[2 * i + 1];
                shortData[i] = (short) ( MSB << 8 | (0xFF & LSB) );
            }
            tvInfo.setText(fileName + "\n" + "" +
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

    void playAudio() {
        // run as thread to get better responsiveness
        new Thread(new Runnable() {
            @Override
            public void run() {
                // for mono 16 bits
                // Estimate minimum buffer size to store the audio samples that is going to play
                // buffer space is required for smooth playback
                int bufferSize = AudioTrack.getMinBufferSize(wavInfo.spec.getRate(),
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                // if the above fail, set buffer size to 2 times of sampling rate
                if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                    bufferSize = wavInfo.spec.getRate() * 2;
                }

                // create AudioTrack object for audio playback
                AudioTrack audioTrack = new AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        wavInfo.spec.getRate(),
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM);


                // create buffer - short (16-bit) data type
                short[] buffer = new short[bufferSize];
                mSamples.rewind();          //go to the beginning of mSamples
                int limit = mNumSamples;
                int totalWritten = 0;
                // Start to write samples to buffer
                while (mSamples.position() < limit && continuePlaying) {
                    int numSamplesLeft = limit - mSamples.position();
                    int samplesToWrite;
                    if (numSamplesLeft >= buffer.length) {
                        mSamples.get(buffer);               //Transfer data from mSamples to buffer upto length of buffer
                        samplesToWrite = buffer.length;
                    } else {
                        // fill up the extra buffer space with 0
                        for (int i = numSamplesLeft; i < buffer.length; i++) {
                            buffer[i] = 0;
                        }
                        mSamples.get(buffer, 0, numSamplesLeft);
                        samplesToWrite = numSamplesLeft;
                    }
                    totalWritten += samplesToWrite;
                    // write the audio samples to the audio device for playback
                    audioTrack.write(buffer, 0, samplesToWrite);
                }

                audioTrack.play();
                    
                if (!continuePlaying) {
                    audioTrack.release();
                }

            }
        }).start();
    }

}
