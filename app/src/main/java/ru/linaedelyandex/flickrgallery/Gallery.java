package ru.linaedelyandex.flickrgallery;

import android.Manifest;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.linaedelyandex.flickrgallery.room.Picture;


//TODO Реализовать возможность поиска по ID пользователя
/*Технически она была практически реализована (в master), однако сделать её эстетически приглядной
 * не нашлось возможности. Насколько я понял, это особенность LiveData и их обозревателей.
 * Поскольку они - единственное, через что общается база данных с активностью, код,
 * реагирующий на изменение базы данных, по идее, должен включаться в работу обсервера.
 * Идея была в том, что нерезультативный запрос поиска по тексту или тэгам можно игнорировать,
 * а вот аналогичный запрос на пользователя не должен приводить к изменению внешнего вида
 * меню, дабы не требовать от пользователя ненужных действий для повторения поиска. Значит, меню
 * требовалось бы переписать по факту изменения базы данных. Однако при новом поиске
 * база данных в любом раскладе возвращает пустой список (во время стирания предыдущей информации),
 * и только после этого или подгружает - или нет - новый результат. Попытка привязаться к
 * этим изменениям - просто визуальный караул с перезаписью одного меню другим спустя примерно
 * секунду. Это же осложняет реализацию идеи с информационными бэкграундом в Grid - при
 * штатной работе, когда всё находится реакция на удаление информации из базы данных всё равно
 * показывает то, что показываться не должно. =)
 * Возможно, для реализации подобного функционала нужно полностью переосмысливать архитектуру приложения*/


public class Gallery extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String TAG = "shithappens";

    public static final String IMAGE_POSITION = "IMAGE_POSITION";
    private static final String SAVED_TGQUERY = "SAVED_TGQUERY";
    private static final String SAVED_TXTQUERY = "SAVED_TXTQUERY";
    private static final String SAVED_OQUERY = "SAVED_OQUERY";
    private static final String SAVED_SMODE = "SAVED_SMODE";
    private static final String SAVED_CTYPE = "SAVED_CTYPE";
    private static final String SAVED_POSITION = "SAVED_POSITION";
    private static final String SAVED_PAGE = "SAVED_PAGE";
    public static final String SAVED_LIST = "SAVED_LIST";

    private SharedPreferences save;

    private PicsViewModel model;

    private GridView grid;
    private int gridPosition = 0;
    private Gallery_Adapter adapter;
    private ArrayList<Picture> list;

    // Пороговое значение для определения момента начала загрузки
    private static final  int threshold = 50;

    // Блок страниц
    private int currentPage = 1;
    private int numberOfPages = 1;
    // Блок запросов к API
    private String tagsQuery = "";
    private String textQuery = "";
    private String ownerQuery = "";
    private int searchMode = 1;

    private int contentType = 6;    //По причине конфликта бытовой и компьютерной логики,
    //единиццу проще прибавить перед отправкой в модель
    private String sortingType = "date-posted-desc";
    // Свитч загрузки, обеспечивающий выполнение одного запроса единовременно
    private boolean loading = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null){
            tagsQuery = savedInstanceState.getString(SAVED_TGQUERY,"");
            textQuery = savedInstanceState.getString(SAVED_TXTQUERY,"");
            ownerQuery = savedInstanceState.getString(SAVED_OQUERY,"");
            searchMode = savedInstanceState.getInt(SAVED_SMODE,1);
            contentType = savedInstanceState.getInt(SAVED_CTYPE,6);
            currentPage = savedInstanceState.getInt(SAVED_PAGE);
            gridPosition = savedInstanceState.getInt(SAVED_POSITION);
            list = (ArrayList<Picture>) savedInstanceState.getSerializable(SAVED_LIST);
        }
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_gallery);

        model = ViewModelProviders.of(this).get(PicsViewModel.class);

        // Тулбар
        Toolbar bar = findViewById(R.id.top_toolbar);
        setSupportActionBar(bar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        //Блок навигационного меню
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, bar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Спиннер для навигационного меню (может быть и топорно, но реализует нужный функционал)
        MenuItem item = navigationView.getMenu().findItem(R.id.nav_content);
        Spinner spinner = (Spinner) MenuItemCompat.getActionView(item);

        ArrayAdapter<CharSequence> arrAdapter = ArrayAdapter.createFromResource(this,R.array.content,android.R.layout.simple_spinner_item);
        arrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(arrAdapter);
        spinner.setSelection(contentType); //Выставляем в нужное место из сохранения
        spinner.setOnItemSelectedListener(this);

        //Поскольку часть настроек вынесена в Preferences Activity, отслеживаем их изменения
        save = PreferenceManager.getDefaultSharedPreferences(this);
        save.registerOnSharedPreferenceChangeListener(this);

        //Сетка изображений, скролл которой скрывает тулбар
        grid = findViewById(R.id.grid);
        grid.setNestedScrollingEnabled(true);

        //Создаем адаптер для сетки и выставляем нужную зону просмотра, в зависимости от статуса сохранения
        if (savedInstanceState!=null){
            adapter = new Gallery_Adapter(this,R.layout.item,list);
            grid.setAdapter(adapter);
            grid.setSelection(gridPosition);
            adapter.notifyDataSetChanged();
        }
        else {
            adapter = new Gallery_Adapter(this,R.layout.item,new ArrayList<Picture>());
            grid.setAdapter(adapter);
        }

        registerForContextMenu(grid);
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                //Самым простым способом избавиться от проблем с перезагрузкой
                //таблиц при отсутствии интернет соединения мне показалось
                //вообще лишить возможности пользователя что-либо делать, если
                //доступа в интернет нет
                if (checkInternetConnection()) {
                    Toast.makeText(Gallery.this, R.string.conn_alert, Toast.LENGTH_SHORT).show();
                    recreate();
                }
                // Загрузка осуществляется по факту остановки прокрутки
                if(scrollState == SCROLL_STATE_IDLE) {
                    //А также по факту достижения порогового значения, если уже не грузится и страница не последняя
                    if(grid.getLastVisiblePosition() >= grid.getCount() - threshold
                            && !loading
                            && currentPage < numberOfPages) {
                        loadMore(currentPage + 1);
                    }
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Gallery.this, Image.class);
                intent.putExtra(IMAGE_POSITION, position);
                intent.putExtra(SAVED_LIST, list);
                startActivityForResult(intent,55);
            }
        });

        //Получаем объекты LiveData из репозитория, подписываемся на изменения базы данных
        LiveData<List<Picture>> pics = model.getAll();
        pics.observe(this, new Observer<List<Picture>>() {
            @Override
            public void onChanged(@Nullable List<Picture> pics) {
                //LiveData любезно предоставляет нам список объектов Picture
                //Которые мы не побоимся использовать и не по прямому назначению
                list = (ArrayList<Picture>) pics;
                adapter.setPics(pics);
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


        LiveData<Throwable> error = model.getError();
        error.observe(this, new Observer<Throwable>() {
            @Override
            public void onChanged(@Nullable Throwable throwable) {
                //Заменил тост на запись в лог, дабы исключить дублирование сообщений об отсутствии сети
                Log.e(TAG,throwable.getLocalizedMessage());
            }
        });

        // При отсутствии сохранения, делаем первый запрос к API
        if (savedInstanceState == null) startOver();
    }


    // Метод запроса к API с очисткой базы данных (инициализация или обновление запроса)
    private void startOver() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 55); }

        //Предупредительный выстрел =)
        if (checkInternetConnection()) {
            Toast.makeText(Gallery.this, R.string.conn_alert, Toast.LENGTH_SHORT).show();
        }

        currentPage = 1; //При каждом новом поиске выставляем первую страницу
        model.startOver(tagsQuery,textQuery,ownerQuery,contentType+1,sortingType);
        invalidateOptionsMenu(); //Для обновления меню опций после взаимодействия с контекстным меню
    }

    //Тривиально, метод загрузки новой страницы поиска API
    private void loadMore(int page) {
        loading = true;
        model.loadMore(page);
    }

    //Проверка интернет-соединения на предмет необходимости разъяснений пользователю)
    private boolean checkInternetConnection() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() == null ||
                !cm.getActiveNetworkInfo().isAvailable() ||
                !cm.getActiveNetworkInfo().isConnected());
    }


    //БЛОК МЕНЮ ОПЦИЙ
    //Отрисовка меню идёт на основании значения переменной searchMode и наличия того или иного контента
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(6).setVisible(false);
        if (!ownerQuery.isEmpty()){
            menu.getItem(0).setIcon(R.drawable.ic_user_search);
            menu.getItem(1).setVisible(true);
            if (!textQuery.isEmpty()||!tagsQuery.isEmpty()) menu.getItem(6).setVisible(true);
        }
        else {
            menu.getItem(0).setIcon(R.drawable.ic_search_black_24dp);
            menu.getItem(1).setVisible(false);
        }
        switch (searchMode){
            case 1: menu.getItem(3).setChecked(true); break;
            case 2: menu.getItem(4).setChecked(true); break;
            case 3: menu.getItem(5).setChecked(true); break;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.main_search:
                handleSearch(item);
                return true;
            case R.id.main_clearowner: //Существует только при непустом поле owner
                ownerQuery = "";
                if (!textQuery.isEmpty()){
                    tagsQuery = "";
                    searchMode = 2;
                }
                else{
                    textQuery = "";
                    searchMode = 1;
                }
                startOver();
                invalidateOptionsMenu();
                return true;
            case R.id.main_bytags:
                item.setChecked(true);
                searchMode = 1;
                return true;
            case R.id.main_bytext:
                item.setChecked(true);
                searchMode = 2;
                return true;
            //Опция поиска всех изображений текущего пользователя(очистка тэгов)
            /*TODO Рассмотреть возможность использования кастомного SearchView с
            возможностью отправки пустых сообщений для этой цели*/
            case R.id.main_ownerall:    //Существует только при непустых полях owner и text||tags
                tagsQuery = "";
                textQuery = "";
                startOver();
                invalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleSearch(final MenuItem item) {
        SearchView searchView = (SearchView) item.getActionView();

        item.expandActionView();

        searchView.setMaxWidth( Integer.MAX_VALUE ); //Расширяем поле поиска до максимума(landscape)
        searchView.setQuery("", false);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Обработка сабмита из строки поиска в зависимости от searchMode
                switch (searchMode){
                    case 1:
                        tagsQuery = query;
                        textQuery = "";
                        break;
                    case 2:
                        textQuery = query;
                        tagsQuery = "";
                        break;
                }
                item.collapseActionView();
                invalidateOptionsMenu();
                startOver();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {return false;}
        });
    }


    //БЛОК КОНТЕКСТНОГО МЕНЮ
    //Для поиска всех изображений пользователя по нажатию на выбранное изображение

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        //Отрисовывается только если мы УЖЕ не ищем что-то по пользователю =)
        if (ownerQuery.isEmpty()) {
            getMenuInflater().inflate(R.menu.context, menu);
            //Второй пункт поиска (автор+контекст) рисуется ещё и при наличии непустого запроса
            if(!textQuery.isEmpty()||!tagsQuery.isEmpty()) menu.getItem(1).setVisible(true);
            else menu.getItem(1).setVisible(false);
            super.onCreateContextMenu(menu, v, menuInfo);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        //Получаем ID владельца текущего изображения из имеющегося листа (спасибо, LiveData!)
        String ownerID = list.get(position).getOwner();
        switch (item.getItemId()) {
            case R.id.context_searchuser:
                ownerQuery = ownerID;
                tagsQuery = "";
                textQuery = "";
                searchMode = 1; //Не забываем про режим поиска и отрисовку меню опций
                startOver();
                return true;
            case R.id.context_withtags:
                if (!textQuery.isEmpty()){
                    ownerQuery = ownerID;
                    tagsQuery = "";
                    searchMode = 2;
                }
                else{
                    ownerQuery = ownerID;
                    textQuery = "";
                    searchMode = 1;
                }
                startOver();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    //Обработка опций навигационного меню (в основном запуск доп.активностей)
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        //Поскольку у нас теперь есть спиннер, стандартный вариант обработки закрытия меню
        //пришлось переписать дабы обеспечить отсутствие реакции при нажатии на пункт меню
        //"Тип контента", выполняющий функции тайтла для спиннера
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            Intent intent = new Intent(Gallery.this, Nav_Settings.class);
            startActivity(intent);
            drawer.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_about) {
            Intent intent = new Intent(Gallery.this, Nav_About.class);
            startActivity(intent);
            drawer.closeDrawer(GravityCompat.START);
        } else if (id == R.id.nav_author) {
            Intent intent = new Intent(Gallery.this, Nav_Author.class);
            startActivity(intent);
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    //БЛОК СПИННЕРА
    //Обработка выбора позиции в нашем стрёмном спиннере
    //Оказалось, что в навигационную активность впихнуть что-то не так просто
    //Но без него меню выглядит пустынным =)
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (contentType != position){
            contentType = position;
            startOver();    //Обновляем поисковый запрос в соответствии с новым контент-фильтром
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START); //Вот теперь панельку можно закрыть
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {}

    //Обработка ответа на запрос разрешений. Будем угрожать пользователю в случае отказа. =)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[]grantResults) {
        if(requestCode == 55) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, getString(R.string.no_access), Toast.LENGTH_SHORT)
                        .show();
        } else { super.onRequestPermissionsResult(requestCode, permissions, grantResults); }
    }

    //Обработка изменений параметров сортировки в настройках приложения
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(!Objects.equals(sortingType, save.getString("PREF_SORT", "date-posted-desc"))){
            sortingType = save.getString("PREF_SORT","date-posted-desc");
            startOver(); //Если они изменились, нам нужно обновить и запрос
            grid.setSelection(0); //И не забыть выставить зону видимости на начало
        }
    }

    //Обработка возврата из детального просмотра
    //Выставляем область видимости сетки туда, докуда долистали страничками
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 55){
            gridPosition = data.getIntExtra(IMAGE_POSITION,0);
            grid.smoothScrollToPosition(gridPosition);
            adapter.notifyDataSetChanged();
        }
        else {super.onActivityResult(requestCode, resultCode, data);}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_POSITION,grid.getFirstVisiblePosition());
        outState.putInt(SAVED_CTYPE,contentType);
        outState.putInt(SAVED_PAGE,currentPage);
        outState.putInt(SAVED_SMODE,searchMode);
        outState.putSerializable(SAVED_LIST,list);
        outState.putString(SAVED_TGQUERY,tagsQuery);
        outState.putString(SAVED_TXTQUERY,textQuery);
        outState.putString(SAVED_OQUERY,ownerQuery);
        super.onSaveInstanceState(outState);
    }

    //Добавляем бэкпрессу способность закрывать навигационное меню
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}













