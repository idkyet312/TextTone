package com.example.texttone;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AudioRecorderExample extends AppCompatActivity {

    public static final int SAMPLE_RATE = 44100; // Sample rate in Hz
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    public int bufferSizeInBytes = 0;
    public AudioRecord audioRecord;
    public boolean isRecording = false;
    public String outputPath = "/path/to/save/audio.pcm";

    public static MainActivity mainact;

    public byte[] audioData;

    public int readbytes;

    public FileOutputStream os;

    //public FileOutputStream os;

    public AudioRecorderExample() {
        bufferSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    }

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    public void startRecording(Context context, MainActivity self) {
        mainact = self;
        outputPath = context.getExternalFilesDir(null).getAbsolutePath() + "/audio.pcm";
        try {
            os = new FileOutputStream(outputPath);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(self, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSizeInBytes);
            //outputPath = "test.txt";
            audioRecord.startRecording();
            isRecording = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }).start();
        }
    }

    private void writeAudioDataToFile() {
        isRecording = true;
        audioData = new byte[bufferSizeInBytes];
        try {
            os = new FileOutputStream(outputPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {

            readbytes = audioRecord.read(audioData, 0, bufferSizeInBytes);
            if (readbytes > 0) {
                try {
                    os.write(audioData, 0, readbytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    // Handle write error
                }
            }
        }


    }

    public void stopRecording() {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if (audioRecord != null) {
                isRecording = false;
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            try {
                InputStream is = new FileInputStream(outputPath);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    byte[] Byteread = is.readAllBytes();
                    mainact.readAudioFile(outputPath);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    // Usage Example
    public static void main(String[] args) {
        AudioRecorderExample recorder = new AudioRecorderExample();
    }

}
