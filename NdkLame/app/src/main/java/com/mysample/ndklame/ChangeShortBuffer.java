package com.mysample.ndklame;

public class ChangeShortBuffer
{
    private short[] rawData;
    private int readSize;

    public ChangeShortBuffer(short[] rawData, int readSize)
    {
        this.rawData = rawData.clone();
        this.readSize = readSize;
    }

    short[] getData() {
        return rawData;
    }

    int getReadSize() {
        return readSize;
    }
}
