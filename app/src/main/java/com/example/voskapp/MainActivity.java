package com.example.voskapp;

import static android.widget.Toast.makeText;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity implements RecognitionListener {

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private Model model;
    private Recognizer recognizer;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);

        LibVosk.setLogLevel(LogLevel.INFO);

        // Request audio permission
        ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);

        // Initialize model
        initModel();
    }

    private void initModel() {
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    // Initialize recognizer and start recognizing
                    try {
                        recognizer = new Recognizer(model, 16000.0f);
                        startRecognition();
                    } catch (IOException e) {
                        Log.e("MainActivity", "Recognizer not initialized");
                    }
                },
                (exception) -> setErrorState("Failed to unpack the model" + exception.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start recognizing immediately
                try {
                    startRecognition();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                finish();  // Finish the activity if permission is not granted
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }

        if (speechStreamService != null) {
            speechStreamService.stop();
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        // Check if the recognized text contains the keyword to stop recognition
        if (hypothesis.contains("stop recognition")) {
            makeText(getApplicationContext(), "Keyword Spotted: recognition", Toast.LENGTH_SHORT).show();
            //stopRecognition();
        }
    }

    @Override
    public void onResult(String hypothesis) {
        // Check if the recognized text contains the keyword to stop recognition
        if (hypothesis.contains("stop recognition")) {
            makeText(getApplicationContext(), "Keyword Spotted: recognition", Toast.LENGTH_SHORT).show();
            //stopRecognition();
        }
    }

    private void stopRecognition() {
        // Perform the action to stop recognition or change the background color here
        // For example, you can stop recognition by calling speechService.stop() or change the background color of resultView.
        // Make sure to update the UI on the main thread if needed.
        runOnUiThread(() -> {
            // Update UI or stop recognition
            speechService.stop(); // This stops the recognition
            // You can also change the background color like this:
            //resultView.setBackgroundColor(getResources().getColor(R.color.newBackgroundColor));
        });
    }

    @Override
    public void onFinalResult(String hypothesis) {
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    @Override
    public void onTimeout() {
    }

    private void setErrorState(String message) {
        makeText(getApplicationContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
    }

    private void startRecognition() throws IOException {
        if (recognizer != null) {
            // Initialize speech service if not already initialized
            if (speechService == null) {
                speechService = new SpeechService(recognizer, 16000.0f);
                speechService.startListening(this);
            }
        } else {
            Log.e("MainActivity", "Recognizer not initialized");
        }
    }
}