package com.example.hong.player;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements SurfaceVideoView.OnPlayStateListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, View.OnClickListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnInfoListener, SeekBar.OnSeekBarChangeListener {
    /** 播放控件 */
    private SurfaceVideoView mVideoView;
    /** 暂停按钮 */
    private ImageView mPlayerStatus;
    /**
     * 载入
     */
    private ProgressBar mLoading;
    /**
     * 进度条
     */
    private SeekBar seekBar;

    /** 播放路径 */
    private String mPath;

    /** 是否需要回复播放 */
    private boolean mNeedResume;

    protected ProgressDialog mProgressDialog;

    private Timer timer;

    private TimerTask task;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 防止锁屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mPath="http://shuhong.file.alimmdn.com/55fbc94760b21fbf5720225b/videoviewdemo.mp4?t=1443364057000";
        if (StringUtils.isEmpty(mPath)) {
            finish();
            return;
        }
        setContentView( R.layout.activity_main);
        mVideoView = (SurfaceVideoView) findViewById(R.id.videoview);
        mPlayerStatus = (ImageView) findViewById(R.id.play_status);
        mLoading = (ProgressBar) findViewById(R.id.loading);
        seekBar= (SeekBar) findViewById(R.id.sb);

        timer=new Timer();
        task=new TimerTask() {
            @Override
            public void run() {
                if(mVideoView!=null&&mVideoView.isPlaying()){
                    int progrogress=mVideoView.getCurrentPosition();
                    seekBar.setProgress(progrogress);
                }
            }
        };

        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnPlayStateListener(this);
        mVideoView.setOnErrorListener(this);
        mVideoView.setOnClickListener(this);
        mVideoView.setOnInfoListener(this);
        mVideoView.setOnCompletionListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        mVideoView.getLayoutParams().height = display.getWidth();

        findViewById(R.id.root).setOnClickListener(this);
        mVideoView.setVideoPath(mPath);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mVideoView != null && mNeedResume) {
            mNeedResume = false;
            if (mVideoView.isRelease())
                mVideoView.reOpen();
            else
                mVideoView.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoView != null) {
            if (mVideoView.isPlaying()) {
                mNeedResume = true;
                mVideoView.pause();
            }
        }
    }

    @Override
    protected void onDestroy() {
        timer.cancel();
        task.cancel();
        timer=null;
        task=null;
        if (mVideoView != null) {
            mVideoView.release();
            mVideoView = null;
        }
        super.onDestroy();
    }

    /**
     * 准备回调
     * @param mp
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        mVideoView.setVolume(SurfaceVideoView.getSystemVolumn(this));
        mVideoView.start();
        seekBar.setMax(mVideoView.getDuration());
        if(mLoading.getVisibility()==View.VISIBLE)
            timer.schedule(task, 500, 100);
        mLoading.setVisibility(View.GONE);
        mPlayerStatus.setSelected(true);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {//跟随系统音量走
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                mVideoView.dispatchKeyEvent(this, event);
                break;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onStateChanged(boolean isPlaying) {
        mPlayerStatus.setSelected(isPlaying ? true : false);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        if (!isFinishing()) {
            //播放失败
        }
        finish();
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.videoview:
                if (mVideoView.isPlaying())
                    mVideoView.pause();
                else
                    mVideoView.start();
                break;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (!isFinishing())
            mVideoView.reOpen();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                //音频和视频数据不正确
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                if (!isFinishing())
                    mVideoView.pause();
                break;
            case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                if (!isFinishing())
                    mVideoView.start();
                break;
            case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                if (Build.VERSION.SDK_INT >= 16) {
                    mVideoView.setBackground(null);
                } else {
                    mVideoView.setBackgroundDrawable(null);
                }
                break;
        }
        return false;
    }


    public ProgressDialog showProgress(String title, String message) {
        return showProgress(title, message, -1);
    }

    public ProgressDialog showProgress(String title, String message, int theme) {
        if (mProgressDialog == null) {
            if (theme > 0)
                mProgressDialog = new ProgressDialog(this, theme);
            else
                mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            mProgressDialog.setCanceledOnTouchOutside(false);// 不能取消
            mProgressDialog.setIndeterminate(true);// 设置进度条是否不明确
        }

        if (!StringUtils.isEmpty(title))
            mProgressDialog.setTitle(title);
        mProgressDialog.setMessage(message);
        mProgressDialog.show();
        return mProgressDialog;
    }

    public void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideProgress();
        mProgressDialog = null;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int position=seekBar.getProgress();
        if(mVideoView!=null){
            mVideoView.seekTo(position);
        }
    }
}
