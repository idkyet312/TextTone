package com.example.texttone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import android.content.Context;

import java.util.List;



import android.util.Base64;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import android.text.style.ForegroundColorSpan;
import android.graphics.Color;


public class MainActivity extends AppCompatActivity {

    private static final int SAMPLE_RATE = 44100; // Sample rate in Hz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private AudioRecord audioRecord;
    private boolean isCapturing = false;

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_AUDIO_FILE_REQUEST_CODE = 2;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextView outputText;
    private ProgressBar volume;

    public int bytesRead;
    public int count = 0;
    public byte[] data;

    public String json;

    public String oldlastword;

    public Float db_holder;

    int sampleRate = 44100; // Sample rate in Hz
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    public Button startListeningButton;
    AudioRecorderExample ARE;


    List<Float> Dblist = new ArrayList<>();
    public Context context;

    public MainActivity self;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }

        outputText = findViewById(R.id.outputText);
        Button startListeningButton = findViewById(R.id.startRecordingButton);
        Button stopListeningButton = findViewById(R.id.stopRecordingButton);
        Button uploadFileButton = findViewById(R.id.addFileButton);
        volume = findViewById(R.id.progressBar);

        this.context = context;

        //speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        //recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        //speechRecognizer.setRecognitionListener(this);

        AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSizeInBytes);

        stopListeningButton.setOnClickListener(v -> ARE.stopRecording());
        uploadFileButton.setOnClickListener(v -> selectFileForUpload());
        //File Dir = context.getFilesDir();

        Dblist.add(1.2F);


    }

    public void Pass(View view) {
        self = this;
        ARE = new AudioRecorderExample();
        checkPermissionAndStartRecording();
    }

    // In your MainActivity or the calling Activity
    public void checkPermissionAndStartRecording() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            // Permission has already been granted, start recording
            ARE.startRecording(this, self);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, start recording
                ARE.startRecording(this, self);
            } else {
                // Permission was denied. Handle the failure.
            }
        }
    }

    private void selectFileForUpload() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_FILE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri audioFileUri = data.getData();
            outputText.setText(data.getDataString());
            transcribeAudio(audioFileUri);
            //outputText.setText();
        }
    }

    public void setoutput(String firstWord, double responseData, Float colorFromSpeed) {
        //responseData = Float.valueOf(firstWord + " = " + responseData + " ");
        //String finalResponseData = String.valueOf(responseData);



        firstWord = firstWord + " ";

        SpannableString spannableString = new SpannableString(firstWord);



// Apply a RelativeSizeSpan for "example" word to increase the size by 1.5 times
        Float fontsize = (float) (responseData / 1000);
        //Collections.reverse(Dblist);
            if (fontsize > 4) {
                fontsize = 4F;
            }
            if(fontsize < 0.4){
                fontsize = 0.4F;
            }
            spannableString.setSpan(new RelativeSizeSpan(fontsize), 0, spannableString.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            colorFromSpeed

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            spannableString.setSpan(new ForegroundColorSpan(Color.argb(100, colorFromSpeed,0,0)), 0, spannableString.length() - 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        runOnUiThread(() -> {
            // Parse and display the response string here, ensure this is done on the UI thread
            outputText.append(spannableString);
        });
    }


    private void transcribeAudio(Uri audioFileUri) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Increase connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Increase read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Increase write timeout
                .build();
        String url = "https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyCzhMmNO7zJBCWfqLRMjAf4EHwwj-c7h-w";
        String jsonTemplate;


        // Step 1: Load the JSON template
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.request);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            jsonTemplate = new String(buffer, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON template", e);
        }

        // Step 2: Encode the audio file content
        String encodedAudio;
        try {
            byte[] audioData = readFileContent(audioFileUri);
            encodedAudio = Base64.encodeToString(audioData, Base64.NO_WRAP);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read and encode audio file", e);
        }

        // Step 3: Insert the encoded audio into the JSON template
        String jsonRequestBody = jsonTemplate.replace("\"placeholder\"", "\"" + encodedAudio + "\"");

        // Step 4: Send the request
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonRequestBody, mediaType);
        Request request = new Request.Builder()
                .url(url)
                .post(body)

                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    // Parse the response string here
                    Log.d("Response", responseStr);

                    runOnUiThread(() -> {
                        // Parse and display the response string here, ensure this is done on the UI thread
                        outputText.setText(responseStr);
                    });
                }
            }
        });

    }


    private byte[] readFileContent(Uri uri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, length);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.e("Read File", "Error reading file", e);
            throw e;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    public void readAudioFile(String filePath) {
        File file = new File(filePath);
        byte[] data = new byte[(int) file.length()]; // This assumes the file size fits into an int

        try (FileInputStream fis = new FileInputStream(file)) {
            bytesRead = fis.read(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String len = String.valueOf(data.length);
        // Handle case where file length is not as expected
        Findwordstats(data);
    }
    public void Findwordstats(byte[] audioBytes)
    {
        String jsonTemplate;


        // Step 1: Load the JSON template
        try {
            InputStream inputStream = getResources().openRawResource(R.raw.micrequest);
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            jsonTemplate = new String(buffer, "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON template", e);
        }

        // Step 2: Encode the audio file content
        String encodedAudio;
        encodedAudio = Base64.encodeToString(audioBytes, Base64.NO_WRAP);

        // Step 3: Insert the encoded audio into the JSON template
        String jsonRequestBody = jsonTemplate.replace("\"placeholder\"", "\"" + encodedAudio + "\"");

        // Step 4: Send the request
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(600, TimeUnit.SECONDS) // Increase connection timeout
                .readTimeout(600, TimeUnit.SECONDS) // Increase read timeout
                .writeTimeout(600, TimeUnit.SECONDS) // Increase write timeout
                .build();

        RequestBody body = RequestBody.create(jsonRequestBody, mediaType);
        Request request = new Request.Builder()
                .url("https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyCzhMmNO7zJBCWfqLRMjAf4EHwwj-c7h-w")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                // Handle the response
                String responseData = response.body().string();
                //setoutput(responseData);
                Gson gson = new Gson();
                SpeechRecognitionResponse myresponse = gson.fromJson(responseData, SpeechRecognitionResponse.class);

                for(int i = 0; i < myresponse.getResults().get(0).getAlternatives().get(0).words.size(); i++)
                {
                if (!myresponse.getResults().isEmpty() && !myresponse.getResults().get(0).getAlternatives().isEmpty() && !myresponse.getResults().get(0).getAlternatives().get(0).getWords().isEmpty()) {
                    Word firstWord = myresponse.getResults().get(0).getAlternatives().get(0).getWords().get(i);
                    String startTime = firstWord.getStartTime();
                    String endTime = firstWord.getEndTime();

                    startTime = startTime.substring(0, startTime.length() - 1);

// Converting the modified string back to a double
                    Float startTimeSeconds = Float.parseFloat(startTime);

                    String theword = firstWord.getWord();

                    endTime = endTime.substring(0, endTime.length() - 1);

// Converting the modified string back to a double
                    Float endTimeSeconds = Float.parseFloat(endTime) + 0.2F;

                    Float ColorFromSpeed = endTimeSeconds - startTimeSeconds / theword.length();


                    double RMS = Math.round(AudioProcessor.calculateRMSVolume(audioBytes, 44100, 16, 1, startTimeSeconds, endTimeSeconds));
                    try {
                        setoutput(theword, RMS, ColorFromSpeed);
                    } catch (Exception e) {
                        //setoutput("error", RMS);
                        throw new RuntimeException(e);
                    }
                    //setoutput(Base64.encodeToString(audioBytes, Base64.NO_WRAP));
                }

                    // Parse the JSON response to extract word timestamps
                }
            }
        });

    }
}
