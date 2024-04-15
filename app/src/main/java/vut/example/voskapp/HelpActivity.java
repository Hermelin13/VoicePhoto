/*
 * FILE: HelpActivity
 * AUTHOR: Adam Dalibor Jurčík
 * LOGIN: xjurci08
 * APP: VoicePhoto
 */

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

/**
 * Help Class
 */
public class HelpActivity extends AppCompatActivity {

    ImageButton back;
    TextView kphoto, kvideo, beep, length, language;

    /**
     * Init function
     *
     * @param state If the activity is being re-initialized after
     *              previously being shut down then this Bundle contains the data it most
     *              recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        setContentView(R.layout.activity_help);
        back = findViewById(R.id.back);
        language = findViewById(R.id.language);
        kphoto = findViewById(R.id.keywordsphoto);
        kvideo = findViewById(R.id.keywordsvideo);
        length = findViewById(R.id.lengthvideo);
        beep = findViewById(R.id.count);
        back.setOnClickListener(v -> closeHelp());

        // LOAD Shared Preferences
        SharedPreferences ShPr = getApplicationContext().getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);
        language.setText(colorChange(getString(R.string.language) + " ", ShPr.getString("model", "model-en-us")));
        if(ShPr.getString("model", "model-en-us").equals("model-en-us")){
            kphoto.setText(colorChange(getString(R.string.photo) + " ", ShPr.getString("kPhoto", "picture")));
            kvideo.setText(colorChange(getString(R.string.video) + " ", ShPr.getString("kVideo", "action")));
        } else if (ShPr.getString("model", "model-en-us").equals("model-cz")) {
            kphoto.setText(colorChange(getString(R.string.photo) + " ", ShPr.getString("kPhoto", "foto")));
            kvideo.setText(colorChange(getString(R.string.video) + " ", ShPr.getString("kVideo", "akce")));
        }

        length.setText(colorChange(getString(R.string.length) + " ", ShPr.getString("length", "10") + " seconds"));
        beep.setText(colorChange(getString(R.string.countdownlength) + " ", ShPr.getString("count", "3") + " seconds"));
    }

    /**
     * Function to put color to the dynamic text
     *
     * @param staticText  static text - not changing
     * @param dynamicText dynamic text - can change and has diff color
     * @return complete string of the both
     */
    public SpannableString colorChange(String staticText, String dynamicText) {
        String combinedText = staticText + dynamicText;
        SpannableString spannableString = new SpannableString(combinedText);

        int startIndex = staticText.length();
        int endIndex = startIndex + dynamicText.length();

        spannableString.setSpan(new ForegroundColorSpan(Color.YELLOW), startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }

    /**
     * Function to close intent and open the main intent
     */
    public void closeHelp() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
    }
}