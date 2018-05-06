package ru.linaedelyandex.flickrgallery;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import java.util.List;
import ru.linaedelyandex.flickrgallery.room.Picture;

//Класс-адаптер для загрузки изображений во VIewPager

public class Image_Adaptor extends PagerAdapter {

    private List<Picture> pics;
    private String picSize;

    Image_Adaptor(List<Picture> pics, String picSize) {
        this.pics = pics;
        this.picSize = picSize;
    }

    public void setPics(List<Picture> pics) {
        this.pics = pics;
    }

    @Override
    public int getCount() {
        return pics.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
    @Override
    public void destroyItem(ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        final Context context = collection.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        //Получаем нужный элемент из списка
        Picture p = pics.get(position);
        //Его адрес
        String url = p.getUrl();
        //И меняем окончание на значение размера из Preferences
        url = url.replace("_q.jpg", picSize);

        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.page, collection,false);

        PicsHolder holder = new PicsHolder();
        holder.zoomablePicture = layout.findViewById(R.id.picture);

        GlideApp
                .with(context)
                .load(url)
                .placeholder(R.drawable.loading)
                .fitCenter()
                .into(holder.zoomablePicture);

        collection.addView(layout);
        return layout;
    }
}
