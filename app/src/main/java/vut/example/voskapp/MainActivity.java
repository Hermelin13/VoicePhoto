/**
* FILE: MainActivity
* AUTHOR: Adam Dalibor Jurčík
* LOGIN: xjurci08
* APP: VoicePhoto
*/

package vut.example.voskapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import java.util.concurrent.ExecutionException;

/**
 * Main Class
 */
public class MainActivity extends AppCompatActivity implements RecognitionListener {

    String KEYVIDEO;
    String KEYPHOTO;
    String model;
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

    /**
     * Init function
     *
     * @param state If the activity is being re-initialized after
     *     previously being shut down then this Bundle contains the data it most
     *     recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        previewView = findViewById(R.id.cameraPreview);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);
        question = findViewById(R.id.question);
        cogwheel = findViewById(R.id.cogwheel);
        rec = findViewById(R.id.record);
        rec.setVisibility(View.INVISIBLE);

        SharedPreferences ShPr = getApplicationContext().getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);

        model = ShPr.getString("model", "model-en-us");
        KEYPHOTO = ShPr.getString("kPhoto", "picture");
        KEYVIDEO = ShPr.getString("kVideo", "action");
        captureDurationMillis = Long.parseLong(ShPr.getString("length", "10")) * 1000;
        delayInSeconds = Integer.parseInt(ShPr.getString("count", "3"));

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        question.setOnClickListener(v -> open("help"));
        cogwheel.setOnClickListener(v -> open("settings"));
        LibVosk.setLogLevel(LogLevel.INFO);

        // IF Android 9 and lower
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
                toggleFlash.setImageResource(R.drawable.reshot_icon_no_flash_ftd27v6m4l);
            } else {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
                toggleFlash.setImageResource(R.drawable.reshot_icon_no_flash_ftd27v6m4l);
                previewView.setBackgroundColor(Color.BLACK);
            }
            startCamera(cameraFacing);
        });
    }

    /**
     * Function for Init of model fro recognition
     */
    private void initModel() {
        StorageService.unpack(this, model, "model",
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

    /**
     * Function to display a modal window for user permissions
     *
     * @param requestCode The request code passed in requestPermissions(
     * android.app.Activity, String[], int)
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either {@link android.content.pm.PackageManager#PERMISSION_GRANTED}
     *     or {@link android.content.pm.PackageManager#PERMISSION_DENIED}. Never null.
     */
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

    /**
     * Function when the app ends
     */
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

    /**
     * Function for displaying the best hypothesis so far
     *
     * @param hypothesis model hypothesis
     */
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
                captureVideo();
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
                takePicture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Function for displaying the best hypothesis
     *
     * @param hypothesis model hypothesis
     */
    @Override
    public void onResult(String hypothesis) {
    }

    /**
     * Function to stop recognition
     */
    private void stopRecognition() {
        runOnUiThread(() -> speechService.setPause(true));
    }

    /**
     * Function for displaying the best hypothesis and stop recognition
     *
     * @param hypothesis model hypothesis
     */
    @Override
    public void onFinalResult(String hypothesis) {
        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    /**
     * Function to set an error condition
     *
     * @param e Error
     */
    @Override
    public void onError(Exception e) {
        setErrorState(e.getMessage());
    }

    /**
     * Timeout capture function
     */
    @Override
    public void onTimeout() {
    }

    /**
     * Function for writing error messages to Logcat
     *
     * @param message Error message
     */
    private void setErrorState(String message) {
        Log.e("MainActivity", "Recognizer not initialized, ERROR: " + message);
    }

    /**
     * Function to start recognition
     *
     * @throws IOException catching errors
     */
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

    /**
     * Function to start the camera view
     *
     * @param cameraFacing front or back camera facing
     */
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

    /**
     * Video capture function
     */
    public void captureVideo() {
        Recording recording1 = recording;
        if (recording1 != null) {
            recording1.stop();
            recording = null;
            return;
        }

        // set file
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES);

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(
                getContentResolver(),
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // saving video
        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                rec.setVisibility(View.VISIBLE);
                capture.setEnabled(true);

                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (recording != null) {
                            recording.stop();
                            recording = null;
                            timer.cancel();
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
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    Log.e("VIDEO", msg);
                }
                speechService.setPause(false);
            }
        });
    }

    /**
     * Photo capture function
     */
    public void takePicture() {
        String nameTimeStamp = "IMG_" + System.currentTimeMillis();
        String name = nameTimeStamp + ".jpeg";
        ImageCapture.OutputFileOptions outputFileOptions = null;

        // Set file
        // IF Android 11 and above
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

        // save photo
        assert outputFileOptions != null;
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.e("IMAGE", "Image Capture Success");
                        Uri savedUri = outputFileResults.getSavedUri();
                        assert savedUri != null;
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(savedUri);
                        sendBroadcast(mediaScanIntent);
                        Toast.makeText(getApplicationContext(), "Image Captured and Saved", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("IMAGE", "Image Capture Failed With Exception : " + exception);
                        Toast.makeText(MainActivity.this, "Image Capture Failed", Toast.LENGTH_LONG).show();
                    }
                });
        speechService.setPause(false);
    }

    /**
     * Function to turn on/of flashlight
     *
     * @param camera Instance of camera
     */
    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.reshot_icon_flash_5emnhf8c7t);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.reshot_icon_no_flash_ftd27v6m4l);
            }
        } else {
            if (camera.getCameraInfo().getLensFacing() == CameraSelector.LENS_FACING_FRONT) {
                ColorDrawable viewColor = (ColorDrawable) previewView.getBackground();
                int colorId = viewColor.getColor();
                if (colorId == Color.BLACK) {
                    toggleFlash.setImageResource(R.drawable.reshot_icon_flash_5emnhf8c7t);
                    previewView.setBackgroundColor(Color.WHITE);
                } else {
                    toggleFlash.setImageResource(R.drawable.reshot_icon_no_flash_ftd27v6m4l);
                    previewView.setBackgroundColor(Color.BLACK);
                }
            } else {
                Toast.makeText(MainActivity.this, "Flash is not available currently", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Beeping function
     *
     * @param tone tone to play
     */
    private void playBeep(int tone) {
        if (toneGenerator != null) {
            toneGenerator.startTone(tone, 400);
        }
    }

    /**
     * Function to start another page
     *
     * @param what which Intent to start
     */
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