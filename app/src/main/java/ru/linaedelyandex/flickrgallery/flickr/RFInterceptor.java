package ru.linaedelyandex.flickrgallery.flickr;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import ru.linaedelyandex.flickrgallery.BuildConfig;

// Интерсептор для перехвата запросов Ретрофита и добавления в них не меняющихся параметров

public class RFInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        HttpUrl url = request.url().newBuilder()
                .addQueryParameter("api_key", BuildConfig.API_KEY)
                .addQueryParameter("format", "json")
                .addQueryParameter("nojsoncallback", "1")
                .build();
        request = request.newBuilder().url(url).build();
        return chain.proceed(request);
    }
}
