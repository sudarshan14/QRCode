package com.truiton.mobile.vision.qrcode;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.media.MediaPlayer.OnCompletionListener;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import java.io.IOException;
import java.util.Collection;


public class QRCodeScanner extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final float BEEP_VOLUME = 0.10f;

    private static final long VIBRATE_DURATION = 200L;

    private ViewfinderView mViewfinderView = null;

    private MediaPlayer mMediaPlayer = null;


    private final boolean mPlayBeep = true;

    private boolean mVibrate = false;



    private CameraManager cameraManager;

    private CaptureActivityHandler handler;


    private boolean hasSurface;


    private Collection<BarcodeFormat> decodeFormats;

    private String characterSet;

    private boolean mHasShow = true;

    private boolean mScanNow = false;


    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner2);

        hasSurface = false;
        mViewfinderView = findViewById(R.id.viewfinder_view);


        findViewById(R.id.scan_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraManager = new CameraManager(getApplication());

        getmViewfinderView().setCameraManager(cameraManager);

        handler = null;


        SurfaceView surfaceView = findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            if (mHasShow && !mScanNow) {
                initCamera();
            }
        } else {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        if (!hasSurface) {
            hasSurface = true;
            if (mHasShow && !mScanNow) {
                initCamera();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        hasSurface = false;
    }


    private void initCamera() {
        try {

            initBeepSound();

            mVibrate = true;

            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, characterSet, cameraManager);
            }

        } catch (IOException ioe) {


        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service

            //  displayFrameworkBugMessageAndExit();
        }
    }


    private void initBeepSound() {
        if (mPlayBeep) {
            // The volume on STREAM_SYSTEM is not adjustable, and users found it
            // too loud,
            // so we now play on the music stream.
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnCompletionListener(beepListener);

            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
            try {
                mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                file.close();
                mMediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME);
                mMediaPlayer.prepare();
            } catch (IOException e) {
                mMediaPlayer = null;
            }
        }
    }

    private final OnCompletionListener beepListener = new OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };


    public ViewfinderView getmViewfinderView() {
        return mViewfinderView;
    }


    public void handleDecode(String resultString, Bitmap barcode) {

        playBeepSoundAndVibrate();

        mScanNow = false;
        if(resultString !=null && !resultString.equals("")){
            returnScanResultToKony(resultString);
        }

    }

    public String returnScanResultToKony(String resultString){


        Toast.makeText(QRCodeScanner.this,""+resultString,Toast.LENGTH_LONG).show();
        return resultString;


    }


    private void playBeepSoundAndVibrate() {
        if (mPlayBeep && mMediaPlayer != null) {
            mMediaPlayer.start();
        }
        if (mVibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            vibrator.vibrate(VIBRATE_DURATION);
        }
    }

}
