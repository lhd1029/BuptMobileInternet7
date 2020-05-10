package com.bytedance.videoplayer;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    protected SurfaceView surfaceView;
    private MediaPlayer player;
    public static int currentPosition;//视频被切换前播放位置
    private SeekBar seekBar;
    protected TextView tv_start;//开始时间
    protected TextView tv_end;//结束时间
    public static Timer timer;
    private boolean isSeekbarChaning;

    @SuppressLint("CutPasteId")
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("MediaPlayer");

        setContentView(R.layout.activity_main);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        seekBar = findViewById(R.id.seekBar);
        tv_start = findViewById(R.id.tv_start);
        tv_end = findViewById(R.id.tv_end);
        player = new MediaPlayer();

        try {
            player.setDataSource(getResources().openRawResourceFd(R.raw.bytedance));


            final SurfaceHolder holder = surfaceView.getHolder();
            holder.addCallback(new PlayerCallBack());
            player.prepare();
            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    surfaceView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1080));
//                    changeVideoSize(player);
                    player.setLooping(true);
                }
            });

            int duration2 = player.getDuration() / 1000;
            int position = player.getCurrentPosition();
            tv_start.setText(calculateTime(position / 1000));
            tv_end.setText(calculateTime(duration2));

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int duration2 = player.getDuration() / 1000;//获取音乐总时长
                    int position = player.getCurrentPosition();//获取当前播放的位置
                    tv_start.setText(calculateTime(position / 1000));//开始时间
                    tv_end.setText(calculateTime(duration2));//总时长
                }
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isSeekbarChaning = true;
                }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isSeekbarChaning = false;
                    player.seekTo(seekBar.getProgress());//在当前位置播放
                    player.start();
                    tv_start.setText(calculateTime(player.getCurrentPosition() / 1000));
                }
            });

            seekBar.setMax(player.getDuration());//将音乐总时间设置为Seekbar的最大值

            player.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    changeVideoSize(mp);
                }
            });
            if (savedInstanceState != null)//横屏继续播放，无论之前是否暂停。重启计时器
            {
                int cp = savedInstanceState.getInt("cp");
                Log.d("debug", String.valueOf(currentPosition));
                seekBar.setProgress(currentPosition);
                player.seekTo(cp);
                player.start();
                timer = new Timer();//重启计时器
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(!isSeekbarChaning){
                            try {
                                seekBar.setProgress(player.getCurrentPosition());
                            } catch (Exception e) {
                                Log.d("debug","wrong!");
                                e.printStackTrace();
                            }
                        }
                    }
                },0,50);
            }

            player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    System.out.println(percent);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        findViewById(R.id.buttonPlay).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.start();
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(!isSeekbarChaning){
                            try {
                                seekBar.setProgress(player.getCurrentPosition());
                            } catch (Exception e) {
                                Log.d("debug","wrong!");
                                e.printStackTrace();
                            }
                        }
                    }
                },0,50);
            }
        });
        //开辟计时器，定时更新seekbar

        findViewById(R.id.buttonPause).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.pause();
            }
        });


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("cp", currentPosition);
    }
    //此时保存cp已没有意义，只能将之前保存好的cp压入到outstate中

    public void changeVideoSize(MediaPlayer mediaPlayer) {

        int surfaceWidth = surfaceView.getWidth();
        int surfaceHeight = surfaceView.getHeight();

        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();

        //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
        float max;
        if (getResources().getConfiguration().orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            //竖屏模式下按视频宽度计算放大倍数值
            max = Math.max((float) videoWidth / (float) surfaceWidth, (float) videoHeight / (float) surfaceHeight);

            //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
            videoWidth = (int) Math.ceil((float) videoWidth / max);
            videoHeight = (int) Math.ceil((float) videoHeight / max);

            //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
            surfaceView.setLayoutParams(new LinearLayout.LayoutParams(videoWidth, videoHeight));

        } else {
            //横屏模式下全屏播放
            surfaceView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            timer.cancel();//结束计时器
            currentPosition = player.getCurrentPosition();//在pause时保存当前位置，否则将会被销毁
            player.stop();
            player.release();
        }
    }


    private class PlayerCallBack implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            player.setDisplay(holder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    }

    public String calculateTime(int time){
        int minute;
        int second;
        if(time > 60){
            minute = time / 60;
            second = time % 60;
            //分钟再0~9
            if(minute >= 0 && minute < 10){
                //判断秒
                if(second >= 0 && second < 10){
                    return "0"+minute+":"+"0"+second;
                }else {
                    return "0"+minute+":"+second;
                }
            }else {
                //分钟大于10再判断秒
                if(second >= 0 && second < 10){
                    return minute+":"+"0"+second;
                }else {
                    return minute+":"+second;
                }
            }
        }else if(time < 60){
            second = time;
            if(second >= 0 && second < 10){
                return "00:"+"0"+second;
            }else {
                return "00:"+ second;
            }
        }
        return null;
    }
    //将时间转换为字符串，显示在进度条两侧
}
