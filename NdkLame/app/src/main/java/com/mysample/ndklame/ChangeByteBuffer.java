package com.mysample.ndklame;

public class ChangeByteBuffer
{
    private byte[] rawData;
    private int readSize;

    public ChangeByteBuffer(byte[] rawData, int readSize)
    {
        this.rawData = rawData.clone();
        this.readSize = readSize;
    }

    byte[] getData() {
        return rawData;
    }

    int getReadSize() {
        return readSize;
    }
}
