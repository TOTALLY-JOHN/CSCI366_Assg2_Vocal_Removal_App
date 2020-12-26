package com.csci366.july2020.audiosamples;

public class WavInfo {
    AudioSpec spec;
    int size;

    public WavInfo(AudioSpec sp, int sz){
        spec = sp;
        size = sz;
    }

    int getSize() {
        return size;
    }

    public AudioSpec getSpec() {
        return spec;
    }

}
