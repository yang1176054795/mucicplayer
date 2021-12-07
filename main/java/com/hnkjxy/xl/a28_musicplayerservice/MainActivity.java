package com.hnkjxy.xl.a28_musicplayerservice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.hnkjxy.xl.a28_musicplayerservice.entity.Music;
import com.hnkjxy.xl.a28_musicplayerservice.interfac.MusicInterface;
import com.hnkjxy.xl.a28_musicplayerservice.service.MusicService;
import com.hnkjxy.xl.a28_musicplayerservice.tool.MusicAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        AdapterView.OnItemClickListener {
    private static final int MY_PERMISSION_REQUEST_CODE = 10000;
    private ArrayList<Music> musics;
    public static Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            int currentPosition = msg.arg1;
            int duration = msg.arg2;
            if (flag) {
                sbMusicProgress.setProgress(currentPosition);
                sbMusicProgress.setMax(duration);
                tvMusicCurrentPosition.setText(getFormattedTime(currentPosition));
                tvMusicDuration.setText(getFormattedTime(duration));
            }
        }
    };
    /**
     * 显示歌曲列表
     */
    private ListView lvMusics;
    private ImageButton ibPlayPause;//播放暂停
    private ImageButton ibNext;//下一首
    private static SeekBar sbMusicProgress;
//    private TextView tv

    private static boolean flag = true;//是否更新SeekBar

    //定义binder(中间人)
    MusicInterface mi;
    private int currentMusicIndex;
    private MusicAdapter adapter;
    private static TextView tvMusicCurrentPosition;
    private static TextView tvMusicDuration;
    private ImageButton ibPlayMode;//模式按钮

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //隐藏系统标题栏
        if (getSupportActionBar() != null){
            getSupportActionBar().hide();
        }
        //版本判断，如果是API23以下，需不需要动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            boolean isAllGranted;
            //1. 检查权限
            isAllGranted = checkPermissionAllGranted(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
            // 如果检查权限都赋予了，则直行相应的业务（loadData）
            if (isAllGranted){
                //加载数据
                musics = loadData();
            }
            //2. 申请权限
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, MY_PERMISSION_REQUEST_CODE);

            //3. 处理申请权限的返回结果, 重写onRequestPermissionsResult
        }else {
            musics = loadData();
        }

        //初始化控件
        initViews();
        //设置监听
        setListener();

        //显示歌曲列表
        initMusicList();

        //绑定服务
        if (musics != null) {
            bindSer();
        }

        //注册广播接收者
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.hnkjxy.xl.music.position");
        registerReceiver(new MusiceReceiver(), intentFilter);
    }
    //------------显示歌曲列表-----------
    private void initMusicList() {
        //创建MusicAdapter
        adapter = new MusicAdapter(this, musics);
        //为ListView设置adapter，当此方法被调用后ListView将会显示歌曲相应信息
        lvMusics.setAdapter(adapter);
    }

    //----------初始化控件------
    private void initViews() {
        lvMusics = findViewById(R.id.lv_musics);
        ibPlayPause = findViewById(R.id.ib_play_or_pause);
        ibNext = findViewById(R.id.ib_next);
        ibPlayMode = findViewById(R.id.ib_play_mode);
        sbMusicProgress = findViewById(R.id.sb_music_progress);
        tvMusicCurrentPosition = findViewById(R.id.tv_music_current_position);
        tvMusicDuration = (TextView)findViewById(R.id.tv_music_duration);
    }

    //---------控件监听-----
    private void setListener(){
        ibPlayPause.setOnClickListener(this);
        ibNext.setOnClickListener(this);
        ibPlayMode.setOnClickListener(this);
        sbMusicProgress.setOnSeekBarChangeListener(this);
        lvMusics.setOnItemClickListener(this);//列表单击监听
    }

    //按钮监听的实现方法
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.ib_play_or_pause:
                //调用服务里的MediaPlayer中的start()
                //判断是否在播放，如果播放则切换到暂停图片
                if (mi.isPlay()){
                    //暂停播放，并更改为暂停图片
                    mi.pause();
                    ibPlayPause.setImageResource(R.drawable.selector_button_play);
                    return;
                }else {
                    mi.play();
//                    ibPlayPause.setImageResource(R.drawable.selector_button_pause);
                }

                break;
            case R.id.ib_next:
                mi.next();
                break;
            case R.id.ib_play_mode:
                switchPlayMode();//更改播放模式
                break;
        }
        setPlayMode(currentMusicIndex);
    }

    private int playMode = 0;
    private static final int PLAY_REPEAT = 0;//列表循环
    private static final int PLAY_SINGLE = 1;//单曲循环
    private static final int PLAY_RANDOM = 2;//随机

    private static final int[] playModeRes = {
            R.drawable.selector_button_mode_repeat,
            R.drawable.selector_button_mode_single,
            R.drawable.selector_button_mode_random
    };
    //更改播放模式
    private void switchPlayMode() {
        playMode++;
        playMode %= playModeRes.length;
        ibPlayMode.setImageResource(playModeRes[playMode]);
        mi.changeMode(playMode);
    }

    //------更改播放状态----
    public void setPlayMode(int currentPosition){
        ibPlayPause.setImageResource(R.drawable.selector_button_pause);
        for (Music m: musics) {
            m.setPlaying(false);
        }
        musics.get(currentPosition).setPlaying(true);
        adapter.notifyDataSetChanged();
    }

    //绑定服务
    private void bindSer(){
        Intent intent = new Intent(this, MusicService.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("musics", musics);
        intent.putExtras(bundle);
        bindService(intent, new ServiceConnection() {
                @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                mi = (MusicInterface) iBinder;//获取中间人
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        }, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSION_REQUEST_CODE){
            boolean isAllGranted = true;
            //判断是否所有权限都已经授权
            for (int grant : grantResults){
                if (grant != PackageManager.PERMISSION_GRANTED){
                    isAllGranted = false;
                    break;
                }
                if (isAllGranted){
                    musics = loadData();
                }else {
                    Toast.makeText(this, "需要授权", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    //检查权限
    private boolean checkPermissionAllGranted(String[] permissions){
        boolean isAllGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED){
                //没有权限
                isAllGranted = false;
                return isAllGranted;
            }
        }
        return isAllGranted;
    }

    private ArrayList<Music> loadData() {
        // 创建歌曲数据的对象
        ArrayList<Music> list = new ArrayList<>();

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DATA},
                MediaStore.Audio.Media.MIME_TYPE + "=? or "
                        + MediaStore.Audio.Media.MIME_TYPE + "=?",
                new String[]{"audio/mpeg", "audio/x-ms-wma"}, null
        );
        if (cursor.moveToFirst()) {
            Music music = null;
            do {
                music = new Music();
                // 文件名
                music.setFileName(cursor.getString(1));
                // 歌曲名
                music.setTitle(cursor.getString(2));
                // 时长
                music.setDuration(cursor.getInt(3));
                // 歌手名
                music.setSinger(cursor.getString(4));
                // 专辑名
                music.setAlbum(cursor.getString(5));
                // 年代
                if (cursor.getString(6) != null) {
                    music.setYear(cursor.getString(6));
                } else {
                    music.setYear("未知");
                }
                // 歌曲格式
                if ("audio/mpeg".equals(cursor.getString(7).trim())) {
                    music.setType("mp3");
                } else if ("audio/x-ms-wma".equals(cursor.getString(7).trim())) {
                    music.setType("wma");
                }
                // 文件大小
                if (cursor.getString(8) != null) {
                    float size = cursor.getInt(8) / 1024f / 1024f;
                    music.setSize((size + "").substring(0, 4) + "M");
                } else {
                    music.setSize("未知");
                }
                // 文件路径
                if (cursor.getString(9) != null) {
                    music.setFileUrl(cursor.getString(9));
                }
                Log.i("xl_hsj", music.toString());
                list.add(music);
            } while (cursor.moveToNext());

            cursor.close();

        }
        return list;
    }

//TODO seekBar监听
    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        flag = !flag;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        flag = !flag;
        //从指定位置开始播放
        mi.seekTo(seekBar.getProgress());
    }
//------------列表监听------------
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mi.play(i);//播放指定位置的歌曲
    }


    class MusiceReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.hnkjxy.xl.music.position".equals(intent.getAction())){
                //获取当前歌曲的索引，并设置高亮
                setPlayMode(intent.getIntExtra("POSITION", 0));
            }
        }
    }


    //--------------格式化时间
    private static SimpleDateFormat sdf =
            new SimpleDateFormat("mm:ss",Locale.CHINA);

    private static Date date = new Date();
    private static String getFormattedTime(long timeMillis){
        date.setTime(timeMillis);
        return sdf.format(date);
    }
}
