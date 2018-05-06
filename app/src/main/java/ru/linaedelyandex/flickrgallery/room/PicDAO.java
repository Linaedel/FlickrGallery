package ru.linaedelyandex.flickrgallery.room;


import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import java.util.List;

@Dao
public interface PicDAO {

    @Query("SELECT * FROM photos")
    LiveData<List<Picture>> getAll();

    @Insert
    void insertAll(List<Picture> pics);

    @Query("DELETE FROM photos")
    void deleteAll();
}

