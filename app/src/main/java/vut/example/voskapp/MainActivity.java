package vut.example.voskapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    String KEYVIDEO, KEYPHOTO, timertext;
    ImageButton question, cogwheel, init;
    TextView timer, numPHOTO, numVIDEO;
    Button start;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;
    private static final int PERMISSIONS_REQUEST = 1;
    private Recognizer recognizer;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private ToneGenerator toneGenerator;
    int counterphoto;
    int countervideo;
    int timerMin;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        timer = findViewById(R.id.clock);
        start = findViewById(R.id.start);
        numPHOTO = findViewById(R.id.counterpho);
        numVIDEO = findViewById(R.id.countervid);
        question = findViewById(R.id.question);
        cogwheel = findViewById(R.id.cogwheel);
        init = findViewById(R.id.init);

        start.setOnClickListener(v -> {
            numVIDEO.setText(String.valueOf(countervideo = 0));
            numPHOTO.setText(String.valueOf(counterphoto = 0));
            if (isTimerRunning) {
                isTimerRunning = false;
                countDownTimer.cancel();
            } else {
                startTimer();
            }
        });

        SharedPreferences ShPr = getApplicationContext().getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);

        KEYPHOTO = ShPr.getString("kPhoto", "snap");
        KEYVIDEO = ShPr.getString("kVideo", "action");
        timerMin = Integer.parseInt(ShPr.getString("time", "15"));
        timertext = ShPr.getString("time", "15") + ":00";
        timer.setText(timertext);

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        question.setOnClickListener(v -> open("help"));
        cogwheel.setOnClickListener(v -> open("settings"));
        LibVosk.setLogLevel(LogLevel.INFO);

        // Request audio permission
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
        } else {
            initModel();
        }
    }

    private void initModel() {
        StorageService.unpack(this, "model-en-us", "model",
                (model) -> {
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

        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initModel();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }

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
        if (isTimerRunning) {
            if (hypothesis.contains(KEYVIDEO)) {
                speechService.reset();
                stopRecognition();
                speechService.setPause(false);
                Log.e("RECOGNITION", "Keyword Spotted: " + KEYVIDEO);
                playBeep(ToneGenerator.TONE_PROP_BEEP);
                numVIDEO.setText(String.valueOf(++countervideo));
            } else if (hypothesis.contains(KEYPHOTO)) {
                speechService.reset();
                stopRecognition();
                speechService.setPause(false);
                Log.e("RECOGNITION", "Keyword Spotted: " + KEYPHOTO);
                playBeep(ToneGenerator.TONE_PROP_BEEP);
                numPHOTO.setText(String.valueOf(++counterphoto));
            }
        }
    }

    @Override
    public void onResult(String hypothesis) {
    }

    private void stopRecognition() {
        runOnUiThread(() -> speechService.setPause(true));
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
        Log.e("MainActivity", "Recognizer not initialized, ERROR: " + message);
    }

    private void startRecognition() throws IOException {
        if (recognizer != null) {
            // Initialize speech service if not already initialized
            if (speechService == null) {
                speechService = new SpeechService(recognizer, 16000.0f);
                speechService.startListening(this);
                init.setColorFilter(Color.GREEN);
            }
        } else {
            Log.e("MainActivity", "Recognizer not initialized");
        }
    }

    private void playBeep(int tone) {
        if (toneGenerator != null) {
            toneGenerator.startTone(tone, 400);
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer((long) timerMin * 60 * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                updateTimerText(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                updateTimerText(0);
                isTimerRunning = false;
                playBeep(ToneGenerator.TONE_CDMA_ABBR_ALERT);
            }
        };

        countDownTimer.start();
        isTimerRunning = true;
    }

    private void updateTimerText(long millisecondsUntilFinished) {
        int seconds = (int) (millisecondsUntilFinished / 1000);
        String timeLeftFormatted = String.format("%02d:%02d", seconds / 60, seconds % 60);
        timer.setText(timeLeftFormatted);
    }

    public void open(String what) {
        if (Objects.equals(what, "help")) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            finish();
        } else if (Objects.equals(what, "settings")) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            finish();
        }

    }
}