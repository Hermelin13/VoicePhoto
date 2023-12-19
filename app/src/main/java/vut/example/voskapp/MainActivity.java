package vut.example.voskapp;

import static android.widget.Toast.makeText;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
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

public class MainActivity extends AppCompatActivity implements RecognitionListener {

    private static final String KEYVIDEO = "recording";
    private static final String KEYPHOTO = "photograph";
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    ImageButton capture, toggleFlash, flipCamera, question;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private ImageCapture imageCapture;

    private final ActivityResultLauncher<String> cameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (result) {
            startCamera(cameraFacing);
        }
    });
    private static final int PERMISSIONS_REQUEST = 1;

    private Model model;
    private Recognizer recognizer;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);
        question = findViewById(R.id.question);

        LibVosk.setLogLevel(LogLevel.INFO);

        // Request audio permission
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
        {
            String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST);
        } else {
            startCamera(cameraFacing);
            initModel();
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

        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                    grantResults.length > 2 && grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                startCamera(cameraFacing);
                initModel();
            } else {
                finish();
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
        /*if (hypothesis.contains(KEYVIDEO)) {
            stopRecognition();
            makeText(getApplicationContext(), "Keyword Spotted: " + KEYVIDEO, Toast.LENGTH_SHORT).show();
            captureVideo().thenRun(() -> speechService.reset());
        }
        else if (hypothesis.contains(KEYPHOTO)) {
            stopRecognition();
            makeText(getApplicationContext(), "Keyword Spotted: " + KEYPHOTO, Toast.LENGTH_SHORT).show();
            takePicture().thenRun(() -> speechService.reset());
        }*/
    }

    @Override
    public void onResult(String hypothesis) {
        if (hypothesis.contains(KEYVIDEO)) {
            stopRecognition();
            makeText(getApplicationContext(), "Keyword Spotted: " + KEYVIDEO, Toast.LENGTH_SHORT).show();
            captureVideo().thenRun(() -> speechService.setPause(false));
        }
        else if (hypothesis.contains(KEYPHOTO)) {
            stopRecognition();
            makeText(getApplicationContext(), "Keyword Spotted: " + KEYPHOTO, Toast.LENGTH_SHORT).show();
            takePicture().thenRun(() -> speechService.setPause(false));
        }
    }

    private void stopRecognition() {
        runOnUiThread(() -> {
            speechService.setPause(true);
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

    public void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(previewView.getWidth(), previewView.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = listenableFuture.get();

                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture, imageCapture);

                capture.setOnClickListener(view -> {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        cameraPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    } else {
                        captureVideo();
                    }
                });

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
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return future;
        }
        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                capture.setEnabled(true);

                // Set the duration for video capture (5 seconds in this example)
                long captureDurationMillis = 5000;

                // Schedule a task to stop the recording after the specified duration
                new Handler().postDelayed(() -> {
                    if (recording != null) {
                        recording.stop();
                        recording = null;
                    }
                }, captureDurationMillis);

            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    String msg = "Video capture succeeded: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    recording.close();
                    recording = null;
                    String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
            }
            future.complete(null);
        });
        return future;
    }

    public CompletableFuture<Void> takePicture() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        final File picturesDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!picturesDirectory.exists()) {
            picturesDirectory.mkdirs();
        }

        String imageFileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        final File file = new File(picturesDirectory, imageFileName);

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(file).build();
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                // Add image to the MediaStore
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
                contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Image saved at: " + file.getPath(), Toast.LENGTH_SHORT).show());

                future.complete(null);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to save: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
                future.completeExceptionally(exception);
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

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }
}