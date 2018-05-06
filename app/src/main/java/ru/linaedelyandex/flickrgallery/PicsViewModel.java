package ru.linaedelyandex.flickrgallery;
import android.arch.lifecycle.LiveData;
import java.util.List;
import ru.linaedelyandex.flickrgallery.room.Picture;

//ViewModel для взаимодействия с моделью приложения

public class PicsViewModel extends android.arch.lifecycle.ViewModel {

    LiveData<List<Picture>> getAll()
    {
        return Repository.getRepository().getAll();
    }

    void loadMore(int page)
    {
        Repository.getRepository().loadMore(page);
    }

    LiveData<Throwable> getError()
    {
        return Repository.getRepository().getError();
    }

    LiveData<Integer> getPage()
    {
        return Repository.getRepository().getPage();
    }

    LiveData<Integer> getNumOfPages(){ return Repository.getRepository().getNumOfPages();}

    public void startOver(String tags, String text, String owner, int content, String sort) {
        Repository.getRepository().startOver(tags,text,owner,content,sort);
    }
}
