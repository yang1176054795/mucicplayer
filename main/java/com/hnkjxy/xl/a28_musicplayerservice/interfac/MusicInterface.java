package com.hnkjxy.xl.a28_musicplayerservice.interfac;



public interface MusicInterface {
    void play();//播放
    void next();//下一首
    void pause();//暂停

    void seekTo(int progress);//指定位置播放

    boolean isPlay();//是否正在播放

    void play(int i);//播放指定歌曲

    void changeMode(int playMode);//更改播放模式
}
