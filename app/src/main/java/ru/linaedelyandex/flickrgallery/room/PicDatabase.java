package ru.linaedelyandex.flickrgallery.room;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {Picture.class}, version = 1)
public abstract class PicDatabase extends RoomDatabase {
    public abstract PicDAO picDao();
}

