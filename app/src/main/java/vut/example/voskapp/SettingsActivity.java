/*
 * FILE: SettingsActivity
 * AUTHOR: Adam Dalibor Jurčík
 * LOGIN: xjurci08
 * APP: VoicePhoto
 */

package vut.example.voskapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

/**
 * Settings class
 */
public class SettingsActivity extends AppCompatActivity {

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    Switch language;
    ImageButton back, confirm;
    EditText keyphoto, keyvideo, setlength, setcount;
    String photoSTR, videoSTR, lengthSTR, countSTR;
    int length, count;
    SharedPreferences ShPr;

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

        setContentView(R.layout.activity_settings);
        language = findViewById(R.id.switchlanguage);
        back = findViewById(R.id.back);
        confirm = findViewById(R.id.save);
        keyphoto = findViewById(R.id.editTextPhoto);
        keyvideo = findViewById(R.id.editTextVideo);
        setlength = findViewById(R.id.editTextLength);
        setcount = findViewById(R.id.editTextCount);
        SharedPreferences ShPrapp = getApplicationContext().getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);
        String lanText = ShPrapp.getString("model", "model-en-us");

        switch (lanText) {
            case "model-en-us":
                language.setChecked(false);
                break;
            case "model-cz":
                language.setChecked(true);
                break;
        }

        back.setOnClickListener(v -> closeSet());

        // LOAD Shared Preferences
        ShPr = getSharedPreferences("VoiceSet", Context.MODE_PRIVATE);


        language.setOnClickListener(v -> {
            // SAVE
            SharedPreferences.Editor editor = ShPr.edit();
            if (language.isChecked()) {
                // Switch is ON
                editor.putString("model", "model-cz");
            } else {
                // Switch is OFF
                editor.putString("model", "model-en-us");
            }

            // CLOSE EDIT
            editor.apply();
            Toast toast = Toast.makeText(SettingsActivity.this, "Language Switched", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 250);
            toast.show();
        });

        // save user input to SP
        confirm.setOnClickListener(view -> {
            photoSTR = keyphoto.getText().toString();
            videoSTR = keyvideo.getText().toString();
            lengthSTR = setlength.getText().toString();
            countSTR = setcount.getText().toString();

            // INT to STR
            if (Objects.equals(lengthSTR, "")) {
                length = -1;
            } else {
                length = Integer.parseInt(lengthSTR);
            }

            if (Objects.equals(countSTR, "")) {
                count = -1;
            } else {
                count = Integer.parseInt(countSTR);
            }

            // SAVE
            SharedPreferences.Editor editor = ShPr.edit();
            if (!Objects.equals(photoSTR, "")) {
                editor.putString("kPhoto", photoSTR);
            }
            if (!Objects.equals(videoSTR, "")) {
                editor.putString("kVideo", videoSTR);
            }
            if (length > 0) {
                editor.putString("length", String.valueOf(length));
            }
            if (count >= 0) {
                editor.putString("count", String.valueOf(count));
            }

            // CLOSE EDIT
            editor.apply();
            Toast toast = Toast.makeText(SettingsActivity.this, "Settings Saved", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 250);
            toast.show();
        });
    }

    /**
     * Function to close intent and open the main intent
     */
    public void closeSet() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        finish();
    }
}