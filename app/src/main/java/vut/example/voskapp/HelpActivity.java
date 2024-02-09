package vut.example.voskapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    ImageButton back;
    TextView kphoto, kvideo, beep, length;
    String phototext, videotext, beeptext, lengthtext;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.activity_help);
        back = findViewById(R.id.back);
        kphoto = findViewById(R.id.keywordsphoto);
        kvideo = findViewById(R.id.keywordsvideo);
        length = findViewById(R.id.lengthvideo);
        beep = findViewById(R.id.beep);

        SharedPreferences ShPr = getApplicationContext().getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);
        phototext = getString(R.string.photo) + " " + "<font color='#EE0000'>" + ShPr.getString("kPhoto", "snap") + "</font>";
        videotext = getString(R.string.video) + " " + ShPr.getString("kVideo", "action");
        lengthtext = getString(R.string.length) + " " + ShPr.getString("length", "10") + " seconds";
        beeptext = getString(R.string.func) + " " + ShPr.getString("count", "3") + " seconds";

        kphoto.setText(phototext);
        kvideo.setText(videotext);
        length.setText(lengthtext);
        beep.setText(Html.fromHtml(beeptext));

        back.setOnClickListener(v -> closeHelp());
    }

    public void closeHelp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
    }
}