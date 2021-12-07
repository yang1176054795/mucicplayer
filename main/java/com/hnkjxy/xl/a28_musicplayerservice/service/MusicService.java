package com.hnkjxy.xl.a28_musicplayerservice.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.annotation.NonNull;
import android.util.Log;

import com.hnkjxy.xl.a28_musicplayerservice.MainActivity;
import com.hnkjxy.xl.a28_musicplayerservice.entity.Music;
import com.hnkjxy.xl.a28_musicplayerservice.interfac.MusicInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;



public class MusicService extends Service {

    private MediaPlayer player;//播放器
    private ArrayList<Music> listMusics;//歌曲数据集合
    private Timer timer;//计时器，用来获播放进度
    private int currentMusicIndex = 0;//当前播放歌曲的索引
    private int currentPosition = 0;//当前歌曲播放位置
    private Intent musicBroadcast = new Intent();
    private int playMode = 0;


    @Override
    public IBinder onBind(Intent intent) {
        //接收由MainActivity传入的数据
        listMusics = (ArrayList<Music>) intent.getSerializableExtra("musics");
        //返回中间人
        return new MusicBinder();
    }

    //onCreate()只会调用一次
    @Override
    public void onCreate() {
        super.onCreate();
        //实例化MediaPlayer
        player = new MediaPlayer();
        //player设置播放完成的监听器
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                next();
            }
        });
    }

    public void play(){
        //播放音乐代码
        //装载音乐文件
        player.reset();//重置
        try {
            Log.i("xl_hsj", "play: " + listMusics.size());
            player.setDataSource(listMusics.get(currentMusicIndex).getFileUrl());
            //准备
            player.prepare();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    player.seekTo(currentPosition);
                    player.start();
                    //更新进度条
                    updateSeekBar();
                }
            });

            //发送歌曲播放的广播
            musicBroadcast.setAction("com.hnkjxy.xl.music.position");
            musicBroadcast.putExtra("POSITION", currentMusicIndex);
            sendBroadcast(musicBroadcast);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //更新进度条
    private void updateSeekBar() {
        if (timer == null){
            //创建计时器，运行子线程任务
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    int currentPosition = player.getCurrentPosition();
                    Handler handler = MainActivity.handler;
                    Message message = handler.obtainMessage();
                    message.arg1 = currentPosition;
                    message.arg2 = player.getDuration();
                    handler.sendMessage(message);
                }
            }, 100, //延迟100毫秒第一次执行run()
                1000//每隔1秒执行一次run()
            );
        }

    }
    //指定位置播放
    private void seekTo(int progress) {
        player.seekTo(progress);
    }
    //下一首

    private  Random random = new Random();
    private void next() {
        switch (playMode){

            case 0:
                currentMusicIndex++;
//        Log.i("xl_hsj", "歌曲索引：" + currentMusicIndex);
                if (currentMusicIndex == listMusics.size()){
                    currentMusicIndex = 0;
                }
                //

                break;
            case 1://单曲
                player.setLooping(true);
            break;
            case 2://随机
                if (listMusics.size()>1){
                    int i;
                    do {
                        i = random.nextInt(listMusics.size());
                    }while (i == currentMusicIndex);
                    currentMusicIndex = i;
                }
                break;
        }
        currentPosition = 0;
        play();
    }
    //暂停播放
    private void pause() {
        currentPosition = player.getCurrentPosition();
        player.pause();
    }
    //是否播放
    private boolean isPlay() {
        return player.isPlaying();
    }
    //播放指定位置的歌曲
    private void play(int position) {
        currentMusicIndex = position;
        play();
    }


    //------------------binder------------
    class MusicBinder extends Binder implements MusicInterface{

        @Override
        public void play() {
            MusicService.this.play();
        }

        @Override
        public void next() {
            MusicService.this.next();
        }

        @Override
        public void pause() {
            MusicService.this.pause();
        }

        @Override
        public void seekTo(int progress) {
            MusicService.this.seekTo(progress);
        }

        @Override
        public boolean isPlay() {
            return MusicService.this.isPlay();
        }

        @Override
        public void play(int position) {
            MusicService.this.play(position);
        }

        @Override
        public void changeMode(int playMode) {
            MusicService.this.changeMode(playMode);
        }
    }

    //更改模式
    private void changeMode(int mode) {
        playMode = mode;
    }


}
