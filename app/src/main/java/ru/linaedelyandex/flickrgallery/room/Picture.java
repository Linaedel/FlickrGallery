package ru.linaedelyandex.flickrgallery.room;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import java.io.Serializable;


@Entity (tableName = "photos")
public class Picture implements Serializable{
    @PrimaryKey (autoGenerate = true)
    private long id = 0;

    @ColumnInfo(name = "url")
    private String url;

    @ColumnInfo(name = "owner")
    private String owner;

    public Picture(String url, String owner) {
        this.url = url;
        this.owner = owner;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String url) {
        this.owner = owner;
    }
}
