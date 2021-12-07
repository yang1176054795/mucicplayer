package com.hnkjxy.xl.a28_musicplayerservice.tool;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.hnkjxy.xl.a28_musicplayerservice.R;
import com.hnkjxy.xl.a28_musicplayerservice.entity.Music;

import java.util.ArrayList;


public class MusicAdapter extends BaseAdapter {

    Context context; //上下文对象
    ArrayList<Music> listMusic;//歌曲数据
    public MusicAdapter(Context context, ArrayList<Music> musics) {
        if (context == null){
            throw new IllegalArgumentException("错误！参数Context对象不允许为空。");
        }
        this.context = context;
        if (musics == null){
            musics = new ArrayList<Music>();
        }
        this.listMusic = musics;
    }

    /**
     * 模版类型标记：默认的
     */
    public static final int TYPE_DEFAULT = 0;
    /**
     * 模版类型标记：选中的
     */
    public static final int TYPE_SELECTED = 1;

    @Override
    public int getItemViewType(int position) {
        return listMusic.get(position).getPlaying() ?
                TYPE_SELECTED : TYPE_DEFAULT;
    }

    @Override
    public int getCount() {
        //返回歌曲的数量
        return listMusic.size();
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        //1. 根据position确定需要显示的数据是哪一条
        Music music = listMusic.get(position);
        //2. 判断view 是否为空
        if (view == null) {
            // 2.1. 如果为null，则需要根据模版加载得到
            LayoutInflater inflater = LayoutInflater.from(context);
            // 2.2. 判断加载什么样的模版
            if (getItemViewType(position) == TYPE_DEFAULT) {
                view = inflater.inflate(R.layout.list_item_music_default, null);
            } else {
                view = inflater.inflate(R.layout.list_item_music_selected, null);
            }
//            Log.i("xl_hsj", "getView: +++++++++" + getItemViewType(position)) ;
        }

        // 3. 从convertView中找到具体显示数据的控件
        TextView tvTitle = (TextView) view.findViewById(R.id.tv_music_item_title);
        TextView tvPath = (TextView) view.findViewById(R.id.tv_music_item_path);

        // 4. 将数据显示到控件中
        tvTitle.setText(music.getTitle());
        tvPath.setText(music.getSinger());

        // 5. 返回整个模版对象 
        return view;
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }
}
