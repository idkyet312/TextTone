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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int PICK_AUDIO_FILE_REQUEST_CODE = 2;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private TextView outputText;
    private ProgressBar volume;

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
            outputText.setText("Uploading...");
            transcribeAudio(audioFileUri);
        }
    }

    private void transcribeAudio(Uri audioFileUri) {
        new Thread(() -> {
            try (SpeechClient speechClient = SpeechClient.create()) {
                byte[] data = readFileContent(audioFileUri);
                if (data == null) {
                    Log.e("Transcription Error", "Could not read file content");
                    return;
                }
                RecognitionConfig config = RecognitionConfig.newBuilder()
                        .setEncoding(AudioEncoding.LINEAR16)
                        .setSampleRateHertz(16000)
                        .setLanguageCode("en-US")
                        .build();
                RecognitionAudio audio = RecognitionAudio.newBuilder()
                        .setContent(ByteString.copyFrom(data))
                        .build();

                RecognizeResponse response = speechClient.recognize(config, audio);
                for (SpeechRecognitionResult result : response.getResultsList()) {
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    runOnUiThread(() -> outputText.setText(alternative.getTranscript()));
                }
            } catch (Exception e) {
                Log.e("Transcription Error", "Error transcribing file", e);
            }
        }).start();
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
