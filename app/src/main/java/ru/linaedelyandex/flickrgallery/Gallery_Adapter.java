package ru.linaedelyandex.flickrgallery;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import java.util.ArrayList;
import java.util.List;
import ru.linaedelyandex.flickrgallery.room.Picture;

//Класс-адаптер для загрузки изображений в Grid

public class Gallery_Adapter extends ArrayAdapter {

    private List<Picture> pics;

    public Gallery_Adapter(@NonNull Context context, int resource, ArrayList<Picture> pics) {
        super(context, resource);
        this.pics = pics;
    }

    @Override
    public int getCount() {
        return pics.size();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        PicsHolder holder;
        Context context = parent.getContext();

        if(convertView == null){
            convertView = LayoutInflater.from(context).inflate(R.layout.item, parent, false);
            holder = new PicsHolder();
            holder.picture = convertView.findViewById(R.id.image);
            convertView.setTag(holder);
        }

        holder = (PicsHolder) convertView.getTag();

        GlideApp
                .with(context)
                .load(pics.get(position).getUrl())
                .centerCrop()
                .into(holder.picture);

        return convertView;
    }
    //Доп метод для подсовывания листа из LiveData`ы
    public void setPics(List<Picture> pics) {
        this.pics = pics;
    }
}
