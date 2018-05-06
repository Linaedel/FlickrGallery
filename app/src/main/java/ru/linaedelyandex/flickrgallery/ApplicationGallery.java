package ru.linaedelyandex.flickrgallery;

import android.app.Application;

public class ApplicationGallery extends Application {

    private static ApplicationGallery instance;


    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static ApplicationGallery getInstance()
    {
        return instance;
    }

}
