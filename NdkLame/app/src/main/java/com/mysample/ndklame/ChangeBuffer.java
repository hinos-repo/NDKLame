package com.mysample.ndklame;

public class ChangeBuffer
{
    private short[] rawData;
    private int readSize;

    public ChangeBuffer(short[] rawData, int readSize)
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
