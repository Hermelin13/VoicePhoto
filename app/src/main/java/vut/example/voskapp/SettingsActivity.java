package vut.example.voskapp;

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
    EditText keyphoto, keyvideo;
    String photoSTR, videoSTR;
    SharedPreferences ShPr;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.activity_settings);
        back = findViewById(R.id.back);
        confirm = findViewById(R.id.save);
        keyphoto = findViewById(R.id.editTextPhoto);
        keyvideo = findViewById(R.id.editTextVideo);

        ShPr = getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);

        back.setOnClickListener(v -> closeSet());
        confirm.setOnClickListener(view -> {
            photoSTR = keyphoto.getText().toString();
            videoSTR = keyvideo.getText().toString();

            SharedPreferences.Editor editor = ShPr.edit();
            if (!Objects.equals(photoSTR, "")){
                editor.putString("kPhoto", photoSTR);
            }
            if (!Objects.equals(videoSTR, "")){
                editor.putString("kVideo", videoSTR);
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