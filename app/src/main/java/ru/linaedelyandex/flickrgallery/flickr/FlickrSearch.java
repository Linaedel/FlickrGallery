package ru.linaedelyandex.flickrgallery.flickr;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

// Класс создан посредством сервиса http://www.jsonschema2pojo.org /

public class FlickrSearch {

    @SerializedName("photos")
    @Expose
    private Photos photos;

    @SerializedName("message")
    @Expose
    private String message;


    @SerializedName("stat")
    @Expose
    private String stat;

    /**
     *
     * @return
     * The photos
     */
    public Photos getPhotos() {
        return photos;
    }

    /**
     *
     * @param photos
     * The photos
     */
    public void setPhotos(Photos photos) {
        this.photos = photos;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /**
     *
     * @return
     * The stat
     */
    public String getStat() {
        return stat;
    }

    /**
     *
     * @param stat
     * The stat
     */
    public void setStat(String stat) {
        this.stat = stat;
    }

}