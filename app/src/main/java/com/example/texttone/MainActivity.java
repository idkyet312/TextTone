package com.example.texttone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
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

import android.util.Base64;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_AUDIO_FILE_REQUEST_CODE = 2;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextView outputText;
    private ProgressBar volume;



    public byte[] data;

    public String json;

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
        speechRecognizer.setRecognitionListener(this);

        startListeningButton.setOnClickListener(v -> startListening());
        stopListeningButton.setOnClickListener(v -> stopListening());
        uploadFileButton.setOnClickListener(v -> selectFileForUpload());
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
        OkHttpClient client = new OkHttpClient();
        String url = "https://speech.googleapis.com/v1/speech:recognize?key=AIzaSyA1KtZTx9JEGsVWyiRZ7SkjaUXNUzlbJNo";
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


        String s = """
                            outputText.setText("transcribin");
                            new Thread(() -> {
                                try (SpeechClient speechClient = SpeechClient.create()) {
                                    byte[] data = readFileContent(audioFileUri);
                                    if (data == null) {
                                        Log.e("Transcription Error", "Could not read file content");
                                        return;
                                    }
                                    runOnUiThread(() -> outputText.setText("outputting0"));
                                    RecognitionConfig config = RecognitionConfig.newBuilder()
                                            .setEncoding(AudioEncoding.LINEAR16)
                                            .setSampleRateHertz(16000)
                                            .setLanguageCode("en-US")
                                            .build();
                                    //RecognitionAudio audio = RecognitionAudio.newBuilder()
                                            //.setContent(ByteString.copyFrom(data))
                                            //.build();
                                    runOnUiThread(() -> outputText.setText("outputting1"));
                                    RecognitionAudio audio = RecognitionAudio.newBuilder().setUri(gcsUri).build();

                                    runOnUiThread(() -> outputText.setText("outputting2"));

                                    RecognizeResponse response = speechClient.recognize(config, audio);
                                    for (SpeechRecognitionResult result : response.getResultsList()) {
                                        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                                        runOnUiThread(() -> outputText.setText(alternative.getTranscript()));;
                }
                        } catch (Exception e) {
                            runOnUiThread(() -> outputText.setText("error"));
                            Log.e("Transcription Error", "Error transcribing file", e);
                        }
                    }).start();""";
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
        volume.setProgress((int) (rmsdB * 2));
    }
    @Override
    public void onBufferReceived(byte[] buffer) {}
    @Override
    public void onEndOfSpeech() {}
    @Override
    public void onError(int error) {
        Log.e("SpeechRecognizer", "Error: " + error);
        outputText.setText("Error occurred: " + error);
    }
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            outputText.setText(matches.get(0)); // Display the first match
        }
    }
    @Override
    public void onPartialResults(Bundle partialResults) {}
    @Override
    public void onEvent(int eventType, Bundle params) {}
}
