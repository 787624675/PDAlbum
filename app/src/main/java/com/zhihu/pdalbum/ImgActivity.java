package com.zhihu.pdalbum;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class ImgActivity extends AppCompatActivity implements ImgContract.View,
        ImgPresenter.ClassifyCallBack ,
        ImgPresenter.AudioToTextCallBack,
        ImgPresenter.SematicCallBack
{
    static {
        System.loadLibrary("native-lib");
    }
    private ImageView img;
    private CircleImageView rec;
    private CircleImageView play;
    private CircleImageView browse;

    private ImgPresenter imgPresenter;
    private Context context;
    private String imgUrl;
    private MediaPlayer mediaPlayer;
    public static String recordFileName;
    private MediaRecorder mediaRecorder;

    private TextView recNote;
    private PCMEncoderAAC pcmEncoderAAC;
    private Boolean recorderState;

    private AudioTrack audioTrack;
    private Boolean isPlaying;

    private byte[] pcmBuffer;
    private TextView tvClassifyRes;

    private EditText etAudioToText;
    private TextView tvSematic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        initFilePath();
        context = this;
        initView();

        showImg(context);
    }
    void initView(){

        imgPresenter = new ImgPresenter(this,this,this);

        img = findViewById(R.id.img);
        rec = findViewById(R.id.rec);
        play = findViewById(R.id.play);
        browse = findViewById(R.id.browse);
        recNote = findViewById(R.id.rec_note);
        tvSematic = findViewById(R.id.tv_sematic);
        tvClassifyRes = findViewById(R.id.classify_res);
        etAudioToText = findViewById(R.id.et_audio_to_text);

        MyButtonListener mbl=new MyButtonListener();

        rec.setOnLongClickListener(mbl);
        rec.setOnTouchListener(mbl);
        browse.setOnClickListener(mbl);
        play.setOnClickListener(mbl);


    }

    void initFilePath(){
        recordFileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+System.currentTimeMillis();
    }

    @Override
    public void showImg(Context context) {
        Bundle argument = getIntent().getExtras();
        imgUrl = argument.getString("path");
        Glide.with(img.getContext())
                .load(imgUrl)
                .error(R.drawable.ic_launcher_foreground)
                .placeholder(R.drawable.ic_launcher_foreground)
                .transition(withCrossFade())
                .into(img);
    }

    @Override
    public void notifyChange(String data) {
        tvClassifyRes.setVisibility(View.VISIBLE);
        tvClassifyRes.setText(data);
    }

    @Override
    public void onAudioToTextCallBack(String data) {
        etAudioToText.setText(data);

    }

    @Override
    public void onsematicCallBack(String data) {
        tvSematic.setText(data);
    }
// https://blog.csdn.net/u010574567/article/details/51900453
// 首先是Button实现onClick和onTouchListener,
// 新建一个类，实现这两个接口，注意重写这两个方法，
// onLongClick
// onTouch方法中，通过event.getAction() 方法与MotionEvent.ACTION_UP(或其他)的比较，对不同的操作进事件绑定。

    class MyButtonListener implements View.OnLongClickListener,View.OnTouchListener, View.OnClickListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(v.getId() == R.id.rec){
                if (event.getAction()==MotionEvent.ACTION_DOWN){
                    // Log.v("Record","1");
                }
                if (event.getAction()==MotionEvent.ACTION_UP) {
                    AudioRecordUtil.getInstance().stop();

                    recNote.setVisibility(View.INVISIBLE);
                    rec.setFillColor(getResources().getColor(R.color.blue_tianyi));

                    imgPresenter.audioToText(recordFileName);
                }
            }
            return false;
        }

        @Override
        public boolean onLongClick(View v) {
            if(v.getId() == R.id.rec){
                //设置录音文件名，编码格式和输出格式都可以根据自身需要修改，这里使用3gp的配置方式，使用默认的方式产生.amr文件
                recordFileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+System.currentTimeMillis();
                recordFileName +=".pcm";
                Log.d("TAG", "recordFileName: " + recordFileName);
                try {
                    AudioRecordUtil.getInstance().start();
                    recNote.setVisibility(View.VISIBLE);
                    rec.setFillColor(getResources().getColor(R.color.blue_dark));
                }catch (Exception e)
                {
                    Log.v("RecordActivity","Expection"+Log.getStackTraceString(e));
                }
//                mediaRecorder.start();
            }

            return true;
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.play){   // 播放录音
                Thread mt = new Thread(playPCMRecord, "playPCM");
                mt.start();
            }
            else if(view.getId() == R.id.exchange){  // 重新照相或选择图片

            }
            else if(view.getId() == R.id.comment){ // comment的显示与隐藏

            }
            else if(view.getId() == R.id.browse){ // 图像识别/Image Classify
                imgPresenter.classifyImg(imgUrl);
            }
        }
        private Runnable playPCMRecord = new Runnable() {

            @Override
            public void run() {
                Log.d("TAG", "recordFileName: " + recordFileName);
                int bufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_IN_STEREO,AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                FileInputStream fis = null;
                try {
                    Log.d("TAG", "recordFileName: " + recordFileName);
                    audioTrack.play();
                    fis = new FileInputStream(recordFileName);
                    byte[] buffer = new byte[bufferSize];
                    int len = 0;
                    isPlaying = true;
                    play.setFillColor(getResources().getColor(R.color.blue_dark));
                    //    while ((len = fis.read(buffer)) != -1 && !isStop) {
                    while ((len = fis.read(buffer)) != -1 ) {
                        Log.d("TAG", "playPCMRecord: len " + len);
                        audioTrack.write(buffer, 0, len);
                    }

                } catch (Exception e) {
                    Log.e("TAG", "playPCMRecord: e : " + e);
                    e.printStackTrace();
                } finally {
                    isPlaying = false;
                    play.setFillColor(getResources().getColor(R.color.blue_tianyi));
                    //isStop = false;
                    if (audioTrack != null) {
                        audioTrack.stop();
                        audioTrack = null;
                    }
                    if (fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

    }






}