package com.zhihu.pdalbum;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class AudioRecordUtil implements PCMEncoderAAC.EncoderListener {

    //设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private final int sampleRateInHz = 8000;
    //设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    //音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    //录制状态
    private boolean recorderState = true;
    private byte[] buffer;
    private AudioRecord audioRecord;
    private static AudioRecordUtil audioRecordUtil = new AudioRecordUtil();

    private PCMEncoderAAC pcmEncoderAAC;

    public static AudioRecordUtil getInstance() {
        return audioRecordUtil;
    }

    private AudioRecordUtil() {
        init();
    }

    private void init() {
        int recordMinBufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        //指定 AudioRecord 缓冲区大小
        buffer = new byte[recordMinBufferSize];
        //根据录音参数构造AudioRecord实体对象
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig,
                audioFormat, recordMinBufferSize);

        pcmEncoderAAC = new PCMEncoderAAC(sampleRateInHz, this);
    }


    /**
     * 开始录制
     */
    public void start() {
        if (audioRecord.getState() == AudioRecord.RECORDSTATE_STOPPED) {
            recorderState = true;
            audioRecord.startRecording();
            new RecordThread().start();
        }
    }

    /**
     * 停止录制
     */
    public void stop() {
        recorderState = false;
        if (audioRecord.getState() == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord.stop();
        }
        audioRecord.release();
    }


    @Override
    public void encodeAAC(byte[] data) {
        Log.d("TAG", "AAC数据长度：" + data.length);
    }
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];

        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private class RecordThread extends Thread {

        @Override
        public void run() {
            short sData[] = new short[BufferElements2Rec];
            FileOutputStream os = null;
            try {
                os = new FileOutputStream(ImgActivity.recordFileName);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            while (recorderState) {
                int read = audioRecord.read(sData, 0, BufferElements2Rec);
                System.out.println("Short wirting to file" + sData.toString());
                try {
                    byte bData[] = short2byte(sData);
                    os.write(bData, 0, BufferElements2Rec * BytesPerElement);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

