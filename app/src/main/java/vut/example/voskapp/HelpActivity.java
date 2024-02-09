package vut.example.voskapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    ImageButton back;
    TextView kphoto, kvideo, beep, length;

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

        kphoto.setText(colorChange(getString(R.string.photo) + " ", ShPr.getString("kPhoto", "snap")));
        kvideo.setText(colorChange(getString(R.string.video) + " ", ShPr.getString("kVideo", "action")));
        length.setText(colorChange(getString(R.string.length) + " ", ShPr.getString("length", "10") + " seconds"));
        beep.setText(colorChange(getString(R.string.func) + " ", ShPr.getString("count", "3") + " seconds"));

        back.setOnClickListener(v -> closeHelp());
    }

    public SpannableString colorChange (String staticText, String dynamicText) {
        String combinedText = staticText + dynamicText;
        SpannableString spannableString = new SpannableString(combinedText);

        int startIndex = staticText.length();
        int endIndex = startIndex + dynamicText.length();

        spannableString.setSpan(new ForegroundColorSpan(Color.YELLOW), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    public void closeHelp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
    }
}