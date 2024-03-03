package com.example.texttone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



import android.util.Base64;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements RecognitionListener {

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


    public int count = 0;
    public byte[] data;

    public String json;

    public String oldlastword;

    public Float db_holder;

    List<Float> Dblist = new ArrayList<>();

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


        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechRecognizer.setRecognitionListener(this);

        startListeningButton.setOnClickListener(v -> startListening());
        stopListeningButton.setOnClickListener(v -> stopListening());
        uploadFileButton.setOnClickListener(v -> selectFileForUpload());

        Dblist.add(1.2F);
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



    private void startListening() {
        speechRecognizer.startListening(recognizerIntent);
    }

    private void stopListening() {
        speechRecognizer.stopListening();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }



    // RecognitionListener methods
    @Override
    public void onReadyForSpeech(Bundle params) {}
    @Override
    public void onBeginningOfSpeech() {}
    @Override
    public void onRmsChanged(float rmsdB) {

        db_holder = rmsdB;
        // Assuming rmsdB is a value that can be directly used after scaling
        final int progress = scaleRmsdBToProgress(rmsdB);

        // Assuming volumeProgressBar is a ProgressBar instance
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                volume.setProgress(progress);
            }
        });
    }

    private int scaleRmsdBToProgress(float rmsdB) {
        // This is a simplified example. You'll need to adapt this based on your progress bar's max value
        // and how you want to represent the volume level. This is just a linear scaling example.
        int minDB = -50; // Example minimum dB value for silence or near silence
        int maxDB = 50; // Example maximum dB value for the loudest sound

        // Normalize and scale rmsdB value
        float normalizedValue = (rmsdB - minDB) / (maxDB - minDB);
        normalizedValue = Math.max(0, Math.min(normalizedValue, 1)); // Clamp between 0 and 1

        int maxProgress = volume.getMax(); // Get the progress bar's maximum value
        db_holder = normalizedValue * maxProgress;
        return (int) (normalizedValue * maxProgress);
    }
        @Override
        public void onBufferReceived(byte[] buffer){}
    @Override
    public void onEndOfSpeech() {}
    @Override
    public void onError(int error) {
        Log.e("SpeechRecognizer", "Error: " + error);
        outputText.setText("Error occurred: " + error);
        startListening();
    }
    @Override
    public void onResults(Bundle results) {
        //Collections.reverse(Dblist);
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String text = matches.get(0);

// Create a SpannableString with the full text
            SpannableString spannableString = new SpannableString(text);

// Apply a RelativeSizeSpan for "example" word to increase the size by 1.5 times
            String[] textword = text.split(" ");
            int start = 0;
            for (int i = 0; i < textword.length; i++) {
                if (i != 0) {
                    start = text.indexOf(" " + textword[i]);
                    start = start + 1;
                }
                else {
                    start = text.indexOf(textword[i]);
                }
                int end = start + textword[i].length();
                //int reversei = Dblist.toArray().length - i;
                spannableString.setSpan(new RelativeSizeSpan(Dblist.get(i)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            }

            //for (Float item : Dblist) {
            outputText.append(spannableString);

        }
    }
    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty() && !Objects.equals(matches.get(0), "")) {
            // Process the first match to work with the most likely words spoken so far
            String[] words = matches.get(0).split("\\s+");


            // Perform action on the last word detected for simplicity
            String lastWord = words[words.length - 1];
            if(!lastWord.equals(oldlastword)){
                oldlastword = lastWord;
                //if(volume.getProgress() < -2.0)
                final int progress = scaleRmsdBToProgress(db_holder);
                if((double) progress / 6 == 1.0)
                {
                    outputText.append("A");
                    Dblist.add(3F);
                }
                else
                {
                    outputText.append("B");
                    Dblist.add(db_holder / 6);
                }
                //outputText.append(lastWord + " ");
            }// Do something with lastWord
        }
    }


    @Override
    public void onEvent(int eventType, Bundle params) {}
}
