package ru.linaedelyandex.flickrgallery;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.persistence.room.Room;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ru.linaedelyandex.flickrgallery.flickr.FlickrSearch;
import ru.linaedelyandex.flickrgallery.flickr.FlickrService;
import ru.linaedelyandex.flickrgallery.flickr.Photo;
import ru.linaedelyandex.flickrgallery.flickr.Photos;
import ru.linaedelyandex.flickrgallery.flickr.RFInterceptor;
import ru.linaedelyandex.flickrgallery.room.Picture;
import ru.linaedelyandex.flickrgallery.room.PicDatabase;

//Хранилище логики программы в ключе взаимодействия с API и базой данных

public class Repository implements Callback<FlickrSearch> {

    public static final String TAG = "shithappens";

    private static Repository repository;
    private static PicDatabase database;
    private static Retrofit retrofit;
    private static FlickrService service;

    //Блок для получения доступа к самому классу, базе данных и сервисам

    public static Repository getRepository(){
        if(repository == null)
            repository = new Repository();
        return repository;
    }

    private Repository(){}

    public static PicDatabase getDatabase(){
        if(database == null){
            database = Room.databaseBuilder(ApplicationGallery.getInstance(),
                    PicDatabase.class, "pictures").build();
        }
        return database;
    }

    private static Retrofit getRetrofit(){
        if(retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_ENDPOINT)
                    .client(new OkHttpClient.Builder()
                            // Интерсептор для добавления неизменяемых параметров запроса
                            .addInterceptor(new RFInterceptor())
                            .build())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static FlickrService getService(){
        if(service == null){
            Retrofit retrofit = getRetrofit();
            service = retrofit.create(FlickrService.class);
        }
        return service;
    }

    //Создаём LiveData и методы для доступа к ним извне
    private static MutableLiveData<Throwable> error = new MutableLiveData<>();
    public static LiveData<Throwable> getError() {
        return error;
    }

    private static MutableLiveData<Integer> page = new MutableLiveData<>();
    static { page.postValue(1); }
    public static LiveData<Integer> getPage() { return page; }

    private static MutableLiveData<Integer> lastPage = new MutableLiveData<>();
    static { lastPage.postValue(1); }
    public static LiveData<Integer> getNumOfPages() { return lastPage; }

    public LiveData<List<Picture>> getAll()
    {
        return getDatabase().picDao().getAll();
    }

    //БЛОК ПОИСКОВЫХ ЗАПРОСОВ

    private String method = "flickr.photos.getRecent"; //Метод API для получения недавно добавленных фото
    private String qTags = null;
    private String qText = null;
    private String qOwner = null;
    private int qContent = 7; //Весь контент
    private String qSort = null;


    public void search(String method, String tags, String text, String owner, int content, String sort, int page) {
        FlickrService service = getService();
        Call<FlickrSearch> searchCall = service.search(method, tags, text, owner, content, sort, page);
        searchCall.enqueue(this);
    }

    //Метод для получения URL картинки из объекта (https://www.flickr.com/services/api/misc.urls.html)
    private static String createUrl(Photo p) {
        return String.format(
                "https://farm%s.staticflickr.com/%s/%s_%s_q.jpg",
                p.getFarm(),
                p.getServer(),
                p.getId(),
                p.getSecret()
        );
    }

    //Обработка коллбэка поискового запроса
    @Override
    public void onResponse(Call<FlickrSearch> call, Response<FlickrSearch> response) {
        FlickrSearch result = response.body();
        if(result.getStat().equals("ok")) {
            Photos body = response.body().getPhotos();

            //Получаем номер страницы
            int currentPage = body.getPage();
            page.postValue(currentPage);
            //И их количество
            Integer numOfPages = Integer.valueOf(body.getPages());
            lastPage.postValue(numOfPages);

            //Создаём список объектов типа Picture
            final List<Picture> pics = new ArrayList<>();
            for (Photo p : body.getPhoto()) {
                pics.add(new Picture(createUrl(p),p.getOwner()));
            }

            //И помещаем его в базу данных в порождённом потоке
            new Thread() {
                @Override
                public void run() {
                    getDatabase().picDao().insertAll(pics);
                }
            }.start();
        } else {
            //Если результат не "ок", возвращаем извинения сервера тостиком
            error.postValue(new Throwable(result.getMessage()));
        }
    }

    //Если вообще ничего не получилось, тоже возвращаем результат тостиком
    @Override
    public void onFailure(Call<FlickrSearch> call, Throwable t) {
        error.postValue(t);
        Log.e(TAG, t.getMessage());
    }

    //Реализация метода начальной загрузки изображений
    public void startOver(
            final String tags,
            final String text,
            final String owner,
            final int content,
            final String sort) {
        //При пустых запросах используем метод Recents, иначе - search
        if(tags.isEmpty()&&text.isEmpty()&&owner.isEmpty()) method = "flickr.photos.getRecent";
        else method = "flickr.photos.search";
        qTags = tags;
        qText = text;
        qOwner = owner;
        qContent = content;
        qSort = sort;

        //Стираем содержимое базы данных и запускаем поиск
        new Thread() {
            @Override
            public void run() {
                getDatabase().picDao().deleteAll();
                search(method, qTags, qText, qOwner, qContent, qSort,1);
            }
        }.start();
    }

    //Реализация метода загрузки следующей страницы изображений
    //Все остальные параметры сохранены при вызове startOver и не меняются
    public void loadMore(final int page){
        search(method, qTags, qText, qOwner, qContent, qSort, page);
    }
}
