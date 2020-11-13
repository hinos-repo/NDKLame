package com.mysample.ndklame;

import android.media.AudioFormat;

class Mp3Encoder
{
    static {
        System.loadLibrary("native-lib");
    }

    public native static void init(int inSampleRate, int outChannel, int outSampleRate, int outBitrate, int quality);

    public static void init(int inSampleRate, int outChannel, int outSampleRate, int outBitrate) {
        init(inSampleRate, outChannel, outSampleRate, outBitrate, 7);
    }

    public native static int encodeByte(short[] buffer_l, short[] buffer_r, int samples, byte[] mp3buf);

    public native static int flush(byte[] mp3buf);

    public native static int encodeMp3(String pcmPath, String mp3Path);
}
