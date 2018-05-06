package ru.linaedelyandex.flickrgallery;

import android.Manifest;
import android.app.DownloadManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import java.util.List;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import ru.linaedelyandex.flickrgallery.room.Picture;

//Класс детального просмотра
//Реализован на базе кастомного ViewPager и библиотеки ImageViewZoom

public class Image extends AppCompatActivity implements ViewPager.OnPageChangeListener{

    private static final String TAG = "shithappens";

    private PicsViewModel model;
    private Image_Adaptor adapter;

    private boolean loading = false;

    private static final  int threshold = 25;

    private List<Picture> list;
    private int currentListSize = 0;

    private int currentPosition;
    private int currentPage = 1;
    private int numberOfPages = 1;

    private String imageSize;

    private boolean isOpened = false; //Свитч для меню

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Обеспечиваем максимальную зону видимости
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        //И убираем заголовок
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_image);
        Toolbar bar = findViewById(R.id.toolbar);
        setSupportActionBar(bar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        model = ViewModelProviders.of(this).get(PicsViewModel.class);

        //Получаем список объектов и позицию для просмотра из MainActivity
        Intent intent = getIntent();
        if (intent.hasExtra(Gallery.SAVED_LIST)) {
            list = (List<Picture>) intent.getSerializableExtra(Gallery.SAVED_LIST);
            currentListSize = list.size();
        }
        if (intent.hasExtra(Gallery.IMAGE_POSITION)) {
            currentPosition = intent.getIntExtra(Gallery.IMAGE_POSITION,0);
        }

        CustomViewPager viewPager = findViewById(R.id.pager);

        //Получаем желаемый размер изображения из настроек и передаём его в адаптер
        SharedPreferences save = PreferenceManager.getDefaultSharedPreferences(this);
        imageSize = save.getString("PREF_SIZE","_b.jpg");
        adapter = new Image_Adaptor(list, imageSize);

        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);
        viewPager.setCurrentItem(currentPosition); //Ставим вью в нужное место
        adapter.notifyDataSetChanged();

        LiveData<List<Picture>> pics = model.getAll();
        pics.observe(this, new Observer<List<Picture>>() {
            @Override
            public void onChanged(@Nullable List<Picture> pics) {
                adapter.setPics(pics);
                currentListSize = pics.size(); //Новая длина списка для проверки в onPageSelected
                adapter.notifyDataSetChanged();
                loading = false;
            }
        });

        LiveData<Integer> page = model.getPage();
        page.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer p) {
                currentPage = p;
                loading = false;
            }
        });

        LiveData<Integer> numOfPages = model.getNumOfPages();
        numOfPages.observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer nop) {
                numberOfPages = nop;
                loading = false;
            }
        });
    }


    private void loadMore(int page) {
        loading = true;
        model.loadMore(page);
    }

    //Обработка бэкпресса (возвращаем новую позицию для Grid)
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra(Gallery.IMAGE_POSITION,currentPosition);
        setResult(55,intent);
        super.onBackPressed();
    }

    //Покидая старую страницу, убираем её зум
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        try {
            ImageViewTouch imageViewTouch = findViewById(R.id.picture);
            if (imageViewTouch != null) {
                imageViewTouch.zoomTo(1f, 0);
            }
        } catch (ClassCastException ex) {
            Log.e(TAG, "This view pager should have only ImageViewTouch as a children.", ex);
        }
    }

    //При переходе на новую страницу делаем проверку на необходимость загрузить новую порцию изображений
    @Override
    public void onPageSelected(int position) {
        if (!checkInternetConnection()) {
            Toast.makeText(Image.this, R.string.conn_alert, Toast.LENGTH_SHORT).show();
            onBackPressed();
        }
        if(position >= currentListSize - threshold
                && !loading
                && currentPage < numberOfPages) {
            loadMore(currentPage + 1);
        }
        currentPosition = position; //И сохраняем новую позицию в качестве текущей
    }

    @Override
    public void onPageScrollStateChanged(int state) {}

    private boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected());
    }

    //БЛОК МЕНЮ ОПЦИЙ
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.image_main, menu);
        if (isOpened){
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(true);
            menu.getItem(3).setVisible(false);
            //Если доступа к хранилищу нет, нечего и искушать пользователя)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                            PackageManager.PERMISSION_GRANTED) {
                menu.getItem(1).setVisible(false);
            } else {menu.getItem(1).setVisible(true);}
        } else {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setVisible(false);
            menu.getItem(3).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.image_share:
                //Простенький функционал, позволяющий поделиться ссылкой на фото
                Intent share = new Intent(android.content.Intent.ACTION_SEND);
                share.setType("text/plain");
                share.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                share.putExtra(Intent.EXTRA_TEXT, getURL(currentPosition,imageSize));
                startActivity(Intent.createChooser(share, getString(R.string.share)));
                return true;
                //Аналогичный - для скачивания изображений
            case R.id.image_download:
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(getURL(currentPosition,imageSize)));
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "FlickrDownload.jpg");
                r.allowScanningByMediaScanner();
                r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                assert dm != null;
                dm.enqueue(r);
                return true;
            case R.id.image_close:
                isOpened = false;
                invalidateOptionsMenu();
                return true;
            case R.id.image_open:
                isOpened = true;
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Вспомогательная функция, возвращающая адрес картинки размера, указанного в настройках
    public String getURL (int position, String size){
        Picture p = list.get(position);
        String url = p.getUrl();
        url = url.replace("_q.jpg", size);
        return url;
    }
}

