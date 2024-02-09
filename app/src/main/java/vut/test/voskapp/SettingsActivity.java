package vut.test.voskapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    ImageButton back, confirm;
    EditText keyphoto, keyvideo, timer;
    int time;
    String photoSTR, videoSTR, timerSTR;
    SharedPreferences ShPr;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.activity_settings);
        back = findViewById(R.id.back);
        confirm = findViewById(R.id.save);
        keyphoto = findViewById(R.id.editTextPhoto);
        keyvideo = findViewById(R.id.editTextVideo);
        timer = findViewById(R.id.editTextTimer);

        ShPr = getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);

        back.setOnClickListener(v -> closeSet());
        confirm.setOnClickListener(view -> {
            photoSTR = keyphoto.getText().toString();
            videoSTR = keyvideo.getText().toString();
            timerSTR = timer.getText().toString();

            if (Objects.equals(timerSTR, "")) {
                time = -1;
            } else {
                time = Integer.parseInt(timerSTR);
            }

            SharedPreferences.Editor editor = ShPr.edit();
            if (!Objects.equals(photoSTR, "")){
                editor.putString("kPhoto", photoSTR);
            }
            if (!Objects.equals(videoSTR, "")){
                editor.putString("kVideo", videoSTR);
            }
            if (time > 0) {
                editor.putString("time", String.valueOf(time));
            }

            editor.apply();
            Toast.makeText(SettingsActivity.this, "Settings Saved", Toast.LENGTH_LONG).show();
        });
    }

    public void closeSet() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
    }
}