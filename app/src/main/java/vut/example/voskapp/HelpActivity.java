package vut.example.voskapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    ImageButton back;
    TextView kphoto, kvideo, timer;
    String phototext, videotext, timertext;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.activity_help);
        back = findViewById(R.id.back);
        kphoto = findViewById(R.id.keywordsphoto);
        kvideo = findViewById(R.id.keywordsvideo);
        timer = findViewById(R.id.timerhelp);

        SharedPreferences ShPr = getApplicationContext().getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);
        phototext = getString(R.string.photo) + " " +ShPr.getString("kPhoto", "snap");
        videotext = getString(R.string.video) + " " + ShPr.getString("kVideo", "action");
        timertext = getString(R.string.timerhelp) + " " + ShPr.getString("time", "15")  + " minutes";

        kphoto.setText(phototext);
        kvideo.setText(videotext);
        timer.setText(timertext);

        back.setOnClickListener(v -> closeHelp());
    }

    public void closeHelp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
    }
}