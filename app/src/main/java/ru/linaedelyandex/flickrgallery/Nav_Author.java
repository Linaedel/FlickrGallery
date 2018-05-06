package ru.linaedelyandex.flickrgallery;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

//Активити с данными об авторе приложения, реализованная в виде
//"схлопывающегося" тулбара и ScrollView

public class Nav_Author extends AppCompatActivity {

    private TextView text;
    private ImageView author;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        Toolbar bar = findViewById(R.id.top_toolbar_author);
        setSupportActionBar(bar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.author));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        text = findViewById(R.id.author_text);
        author = findViewById(R.id.expandedImage);

        Spanned sp = Html.fromHtml(getString(R.string.author_about));
        text.setText(sp);

        //Анимация исчезновения и появления
        final AnimatorSet set_out = (AnimatorSet) AnimatorInflater.loadAnimator(Nav_Author.this, R.animator.alpha_out);
        final AnimatorSet set_in = (AnimatorSet) AnimatorInflater.loadAnimator(Nav_Author.this, R.animator.alpha_in);
        set_out.setTarget(author);
        set_in.setTarget(author);

    }

}