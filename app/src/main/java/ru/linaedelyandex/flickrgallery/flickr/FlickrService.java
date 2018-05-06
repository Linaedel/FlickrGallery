package ru.linaedelyandex.flickrgallery.flickr;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

//Сервис запросов к API Flickr.com

public interface FlickrService {

    @GET("/services/rest/")
    Call<FlickrSearch> search(
            @Query("method") String method,
            @Query("tags") String tags,
            @Query("text") String text,
            @Query("user_id") String userId,
            @Query("content_type") int contentType,
            @Query("sort") String sortingMethod,
            // @Query("format") String format, // В интерсепторе
            // @Query("api_key") String key, // В интерсепторе
            // @Query("nojsoncallback") int flag, // В интерсепторе
            @Query("page") int page
    );

}




