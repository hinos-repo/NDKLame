package com.mysample.ndklame;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity
{
    private ExecutorService m_executorService;
    private AudioRecord m_recorder;
    private AudioTrack m_audioTrack = null;
    private boolean m_bRecord = false;
    private boolean m_bPlaying = false;

    int m_audioSource = MediaRecorder.AudioSource.MIC;
    int m_nSampleRates = 44100;
    int m_nRecordChannel = AudioFormat.CHANNEL_IN_STEREO;
    int m_nChannelCount = m_nRecordChannel == AudioFormat.CHANNEL_IN_MONO ? 1:2;
    int m_nAudioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int m_nTrackBufferSize = AudioTrack.getMinBufferSize(m_nSampleRates, m_nRecordChannel, m_nAudioFormat); // 버퍼사이즈
    int m_nRecoBufferSize = AudioRecord.getMinBufferSize(m_nSampleRates, m_nRecordChannel, m_nAudioFormat);
    String m_strPcmPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/record.pcm";
    String m_strMp3Path = Environment.getExternalStorageDirectory().getAbsolutePath() + (m_nChannelCount == 1 ? "/record.mp3" : "/record2.mp3");

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        ComPermission.INSTANCE.getPermissionState(this, ComPermission.EnumPermission.RECORD_PERMISSION);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermission();

        m_executorService = Executors.newSingleThreadExecutor();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        ComPermission.INSTANCE.onRequestPermissionsResult(requestCode, permissions, grantResults, ComPermission.EnumPermission.RECORD_PERMISSION);
    }

    private void initPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }
    }

    public void onlyRecord(View view) // 오직 녹음만 해서 PCM 파일을 만든다.
    {
        m_bRecord = true;
        m_recorder = new AudioRecord(m_audioSource, m_nSampleRates, m_nRecordChannel, m_nAudioFormat, m_nRecoBufferSize);

        m_executorService.execute(new OnlyRecordThread());
    }

    public void stopOnlyRecord(View view)
    {
        m_bRecord = false;
    }

    class OnlyRecordThread implements Runnable
    {
        public OnlyRecordThread()
        {

        }

        @Override
        public void run()
        {
            m_recorder.startRecording();

            byte [] recordData = new byte[m_nRecoBufferSize];
            FileOutputStream pcmFos = null;
            try {
                pcmFos = new FileOutputStream(m_strPcmPath);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while (m_bRecord)
            {
                m_recorder.read(recordData, 0, m_nRecoBufferSize);
                try {
                    pcmFos.write(recordData, 0, m_nRecoBufferSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            m_recorder.stop();
            m_recorder.release();
            try {
                pcmFos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void convertMp3(View view)
    {
        File mp3File = new File(m_strMp3Path);
        File pcmFile = new File(m_strPcmPath);

        m_executorService.execute(new PcmToMp3Thread(mp3File, pcmFile));
    }

    class PcmToMp3Thread implements Runnable
    {
        File mp3File;
        File pcmFile;

        public PcmToMp3Thread(File mp3File, File pcmFile)
        {
            this.mp3File = mp3File;
            this.pcmFile = pcmFile;
        }

        @Override
        public void run()
        {
            Mp3Encoder.init(m_nSampleRates, m_nChannelCount, m_nSampleRates, 128);
            int nResult = Mp3Encoder.encodeMp3(pcmFile.getAbsolutePath(),mp3File.getAbsolutePath());
            Log.d("Main", "run: " + nResult);
        }
    }

    public void playPCM(View view)
    {
        if (!m_bPlaying)
        {
            m_bPlaying = true;
            m_nTrackBufferSize = AudioTrack.getMinBufferSize(m_nSampleRates, m_nRecordChannel, m_nAudioFormat);
            m_audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, m_nSampleRates, m_nRecordChannel, m_nAudioFormat, m_nTrackBufferSize, AudioTrack.MODE_STREAM);

            m_executorService.execute(new PlayPCMThread());
        }
    }

    class PlayPCMThread implements Runnable
    {
        @Override
        public void run()
        {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(m_strPcmPath);
            } catch (FileNotFoundException e)
            {
                m_bPlaying = false;
                e.printStackTrace();
            }

            byte [] buffer = new byte[m_nTrackBufferSize];
            DataInputStream dis = new DataInputStream(fis);
            m_audioTrack.play();
            while (m_bPlaying)
            {
                try {
                    int nResult = dis.read(buffer, 0, m_nTrackBufferSize);
                    if (nResult <= 0)
                    {
                        m_bPlaying = false;
                        break;
                    }
                    m_audioTrack.write(buffer, 0, nResult);
                }catch (Exception e)
                {
                    m_bPlaying = false;
                    break;
                }
            }
            m_audioTrack.stop();
            m_audioTrack.release();
            m_audioTrack.flush();
            m_audioTrack = null;

            m_bPlaying = false;

            try {
                dis.close();
                fis.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }

        }
    }

    public void recordWithConvertMp3(View view)
    {
        m_bRecord = true;
        m_recorder = new AudioRecord(m_audioSource, m_nSampleRates, m_nRecordChannel, m_nAudioFormat, m_nRecoBufferSize);
        m_executorService.execute(new RecordWithMp3());
    }


    class RecordWithMp3 implements Runnable // PCM Buffer decode Mp3 Buffer
    {
        File mp3File;
        FileOutputStream mp3Fos = null;
        StreamHandler handler = new StreamHandler();
        byte [] mp3Buffer;

        public RecordWithMp3() //SampleRates 44100hz, Channel 2
        {
            mp3Buffer = new byte[(int) (7200 + (m_nRecoBufferSize * 2 * 1.25))];
            mp3File = new File(m_strMp3Path);
            m_recorder = new AudioRecord(m_audioSource, m_nSampleRates, m_nRecordChannel, m_nAudioFormat, m_nRecoBufferSize);
            Mp3Encoder.init(m_nSampleRates, m_nChannelCount, m_nSampleRates, 128);
        }

        @Override
        public void run()
        {
            m_recorder.startRecording();
            try {
                mp3Fos = new FileOutputStream(mp3File);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            short [] pcmBuffer = new short[m_nRecoBufferSize];
            while (m_bRecord)
            {
                int end = m_recorder.read(pcmBuffer, 0, pcmBuffer.length);
                short[] copyBuffer = pcmBuffer.clone();

                Message msg = new Message();
                msg.what = 1;
                msg.obj = new ChangeBuffer(copyBuffer.clone(), end);
                handler.sendMessage(msg);
            }
        }

        class StreamHandler extends Handler
        {
            @Override
            public void handleMessage(@NonNull Message msg)
            {
                if (msg.what == 1)
                {
                    ChangeBuffer shipData = (ChangeBuffer) msg.obj;
                    short [] copyBuffer = shipData.getData();
                    if (!m_bRecord)
                    {
                        final int flushResult = Mp3Encoder.flush(mp3Buffer);
                        if (flushResult > 0)
                        {
                            try {
                                mp3Fos.write(mp3Buffer, 0, flushResult);
                                mp3Fos.close();
                            } catch (final IOException e) {
                                Log.e("Main", e.getMessage());
                            }
                        }
                    }
                    else
                    {
                        int readSize = shipData.getReadSize();
                        if (readSize > 0)
                        {
                            int encodedSize = Mp3Encoder.encodeByte(copyBuffer, copyBuffer, readSize, mp3Buffer);
                            if (encodedSize < 0) {
                                Log.e("Main", "Lame encoded size: " + encodedSize);
                            }
                            try {
                                mp3Fos.write(mp3Buffer, 0, encodedSize);
                            }catch (Exception e)
                            {
                                Log.e("Main", "Unable to write to file");
                            }
                        }
                    }
                }
            }
        }
    }
}
