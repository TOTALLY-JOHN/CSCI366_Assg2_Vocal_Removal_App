package com.csci366.july2020.audiosamples;

public class AudioSpec {
    int freq;
    byte channels;

    public AudioSpec(int f, byte chs){
        freq = f;
        channels = chs;
    }

    public int getRate(){
        return freq;
    }

    public byte getChannels() {
        return channels;
    }

}
