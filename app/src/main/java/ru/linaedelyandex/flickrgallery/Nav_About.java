package ru.linaedelyandex.flickrgallery;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;

//Активити с данными о приложении

public class Nav_About extends AppCompatActivity {

    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        //Ландшафтная ориентация для этой странички не обязательна, поэтому, чтобы не париться
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        text = findViewById(R.id.about_text);

        Spanned sp = Html.fromHtml(getString(R.string.about_text2));
        text.setText(sp);
    }
}
