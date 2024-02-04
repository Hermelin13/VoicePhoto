package vut.example.voskapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    String KEYVIDEO;
    String KEYPHOTO;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    ImageButton capture, toggleFlash, flipCamera, question, cogwheel;
    ImageView rec;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private ImageCapture imageCapture;
    private static final int PERMISSIONS_REQUEST = 1;
    private Recognizer recognizer;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private ToneGenerator toneGenerator;
    long captureDurationMillis;
    int delayInSeconds;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);
        question = findViewById(R.id.question);
        cogwheel = findViewById(R.id.cogwheel);
        rec = findViewById(R.id.record);
        rec.setVisibility(View.INVISIBLE);

        SharedPreferences ShPr = getApplicationContext().getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);

        KEYPHOTO = ShPr.getString("kPhoto", "snap");
        KEYVIDEO = ShPr.getString("kVideo", "action");
        captureDurationMillis = Long.parseLong(ShPr.getString("length", "10")) * 1000;
        delayInSeconds = Integer.parseInt(ShPr.getString("count", "3"));

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        question.setOnClickListener(v -> open("help"));
        cogwheel.setOnClickListener(v -> open("settings"));
        LibVosk.setLogLevel(LogLevel.INFO);


        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
            } else {
                startCamera(cameraFacing);
                initModel();
            }
        } else {
            // Request audio permission
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
                ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
            } else {
                startCamera(cameraFacing);
                initModel();
            }
        }

        flipCamera.setOnClickListener(view -> {
            if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera(cameraFacing);
        });
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
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                        grantResults.length > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                    startCamera(cameraFacing);
                    initModel();
                } else {
                    finish();
                }
            } else {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                        grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    startCamera(cameraFacing);
                    initModel();
                } else {
                    finish();
                }
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
        if (hypothesis.contains(KEYVIDEO)) {
            speechService.reset();
            stopRecognition();
            Log.e("RECOGNITION", "Keyword Spotted: " + KEYVIDEO);
            try {
                for (int i = 0; i < delayInSeconds; i++) {
                    playBeep(ToneGenerator.TONE_PROP_BEEP);
                    Thread.sleep(1000);
                }
                playBeep(ToneGenerator.TONE_CDMA_ABBR_ALERT);
                captureVideo().thenRun(() -> speechService.setPause(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else if (hypothesis.contains(KEYPHOTO)) {
            speechService.reset();
            stopRecognition();

            Log.e("RECOGNITION", "Keyword Spotted: " + KEYPHOTO);
            try {
                for (int i = 0; i < delayInSeconds; i++) {
                    playBeep(ToneGenerator.TONE_PROP_BEEP);
                    Thread.sleep(1000);
                }
                playBeep(ToneGenerator.TONE_CDMA_ABBR_ALERT);
                takePicture().thenRunAsync(() -> runOnUiThread(() -> speechService.setPause(false)));
            } catch (InterruptedException e) {
                e.printStackTrace();
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
            }
        } else {
            Log.e("MainActivity", "Recognizer not initialized");
        }
    }

    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = listenableFuture.get();

                Preview preview = new Preview.Builder().build();
                previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation((int) previewView.getRotation())
                        .build();

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing)
                        .build();

                cameraProvider.unbindAll();
                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture, imageCapture);

                toggleFlash.setOnClickListener(view -> setFlashIcon(camera));

                preview.setSurfaceProvider(previewView.getSurfaceProvider());

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public CompletableFuture<Void> captureVideo() {
        CompletableFuture<Void> future = new CompletableFuture<>();

        Recording recording1 = recording;
        if (recording1 != null) {
            recording1.stop();
            recording = null;
            return future;
        }

        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES); // Specify Movies directory

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(
                getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return future;
        }

        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                rec.setVisibility(View.VISIBLE);
                capture.setEnabled(true);

                // Schedule a task to stop the recording after the specified duration
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (recording != null) {
                            recording.stop();
                            recording = null;
                            timer.cancel(); // Stop the timer after stopping the recording
                        }
                    }
                }, captureDurationMillis);

            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                rec.setVisibility(View.INVISIBLE);
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    playBeep(ToneGenerator.TONE_CDMA_ABBR_ALERT);
                    String msg = "Video Captured and Saved";

                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    recording.close();
                    recording = null;
                    String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    String err = "Video Capture Failed ";
                    Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
                    Log.e("VIDEO", msg);
                }
            }
            future.complete(null);
        });

        return future;
    }


    public CompletableFuture<Void> takePicture() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        String nameTimeStamp = "IMG_" + System.currentTimeMillis();
        String name = nameTimeStamp + ".jpeg";
        ImageCapture.OutputFileOptions outputFileOptions = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, nameTimeStamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
            contentValues.put(MediaStore.MediaColumns.ORIENTATION, 90);

            outputFileOptions = new ImageCapture.OutputFileOptions.Builder(
                    this.getContentResolver(),
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
            ).build();
        } else {
            File mImageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            boolean isDirectoryCreated = mImageDir.exists() || mImageDir.mkdirs();

            if (isDirectoryCreated) {
                File file = new File(mImageDir, name);
                outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
            }
        }

        assert outputFileOptions != null;
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.e("IMAGE", "Image Capture Success");

                        // Get the saved image URI
                        Uri savedUri = outputFileResults.getSavedUri();
                        assert savedUri != null;
                        //Log.v("IMAGE", "Saved Image Uri " + savedUri);

                        // Update the MediaStore to make the image appear in the gallery
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(savedUri);
                        sendBroadcast(mediaScanIntent);

                        Toast.makeText(getApplicationContext(), "Image Captured and Saved", Toast.LENGTH_SHORT).show();
                        future.complete(null); // Complete the CompletableFuture
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("IMAGE", "Image Capture Failed With Exception : " + exception);
                        Toast.makeText(MainActivity.this, "Image Capture Failed", Toast.LENGTH_SHORT).show();
                        future.completeExceptionally(exception); // Complete the CompletableFuture exceptionally on error
                    }
                });

        return future;
    }

    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.baseline_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.baseline_flash_on_24);
            }
        } else {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Flash is not available currently", Toast.LENGTH_SHORT).show());
        }
    }

    private void playBeep(int tone) {
        if (toneGenerator != null) {
            toneGenerator.startTone(tone, 400);
        }
    }

    public void open(String what) {
        if (Objects.equals(what, "help")) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            finish();
        } else if (Objects.equals(what, "settings")){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            finish();
        }

    }
}