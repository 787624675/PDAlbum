package com.zhihu.pdalbum;

import static android.content.ContentValues.TAG;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.liulishuo.engzo.lingorecorder.LingoRecorder;
import com.liulishuo.engzo.lingorecorder.processor.AudioProcessor;
import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ImgActivity extends AppCompatActivity implements ImgContract.View,
        ImgPresenter.ClassifyCallBack ,
        ImgPresenter.AudioToTextCallBack,
        ImgPresenter.SematicCallBack,
        AudioRecordUtil.RecCallBack
{
    static {
        System.loadLibrary("native-lib");
        System.loadLibrary("yuyin-lib");
        System.loadLibrary("semantic-lib");

    }
    private ImageView img;
    private CircleImageView rec;
    private CircleImageView exchange;
    private CircleImageView play;
    private CircleImageView browse;
    private CircleImageView comment;

    private ImgPresenter imgPresenter;
    private Context context;
    private String imgUrl;
    private MediaPlayer mediaPlayer;
    private String recordFileName;
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

    private int audioRecorderSetCallBack = 0;
    private boolean isStop;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor ;

    // 用于缓存
    private String sematicValue;
    private String cvValue;
    private String voiceTextValue;

    private  LingoRecorder lingoRecorder;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        sharedPreferences = getSharedPreferences("data",Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        context = this;
        initView();

        showImg(context);

        lingoRecorder = new LingoRecorder();
        lingoRecorder.bitsPerSample(16);
        lingoRecorder.setOnRecordStopListener(new LingoRecorder.OnRecordStopListener() {
            @Override
            public void onRecordStop(Throwable throwable,
                                     Result result) {
                imgPresenter.audioToText(recordFileName);
            }
        });
        lingoRecorder.setOnProcessStopListener(new LingoRecorder.OnProcessStopListener() {
            @Override
            public void onProcessStop(Throwable throwable, Map<String, AudioProcessor> map) {

            }
        });



    }
    void initView(){

        imgPresenter = new ImgPresenter(this,this,this);

        img = findViewById(R.id.img);
        rec = findViewById(R.id.rec);
        play = findViewById(R.id.play);
        browse = findViewById(R.id.browse);
        comment = findViewById(R.id.comment);
        exchange = findViewById(R.id.exchange);
        recNote = findViewById(R.id.rec_note);
        tvSematic = findViewById(R.id.tv_sematic);
        tvClassifyRes = findViewById(R.id.classify_res);
        etAudioToText = findViewById(R.id.et_audio_to_text);

        MyButtonListener mbl=new MyButtonListener();

        rec.setOnLongClickListener(mbl);
        rec.setOnTouchListener(mbl);
        browse.setOnClickListener(mbl);
        play.setOnClickListener(mbl);
        comment.setOnClickListener(mbl);
        exchange.setOnClickListener(mbl);

        voiceTextValue = sharedPreferences.getString(imgUrl+"voiceText","今天天气真好");
        etAudioToText.setText(voiceTextValue);

        cvValue = sharedPreferences.getString(imgUrl+"cv","#这是什么");
        tvClassifyRes.setText(cvValue);

        sematicValue = sharedPreferences.getString(imgUrl+"sematic","sematic");
        tvSematic.setText(sematicValue);
        etAudioToText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                voiceTextValue = charSequence.toString();
                editor.putString(imgUrl+"voiceText",charSequence.toString());
                editor.commit();
            }

            @Override
            public void afterTextChanged(Editable editable) {


            }
        });


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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                etAudioToText.setText(data);
            }
        });
        imgPresenter.audioSematic(recordFileName);


    }

    @Override
    public void onSematicCallBack(String data) {
        tvSematic.setText(data);

        editor.putString(imgUrl+"sematic",data);
    }

    @Override
    public void onRecCallBack(String filePath){
        imgPresenter.audioToText(filePath);
    }

    // 把几个按钮的点击事件写在了一起，用if-else判断是哪个按钮的
    class MyButtonListener implements View.OnLongClickListener,View.OnTouchListener, View.OnClickListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(v.getId() == R.id.rec){
                if (event.getAction()==MotionEvent.ACTION_DOWN){
                    // Log.v("Record","1");
                }
                if (event.getAction()==MotionEvent.ACTION_UP) {
//                    AudioRecordUtil.getInstance().stop();
                    lingoRecorder.stop();

                    recNote.setVisibility(View.INVISIBLE);
                    rec.setFillColor(getResources().getColor(R.color.blue_tianyi));


                }
            }
            return false;
        }

        @Override
        public boolean onLongClick(View v) {
            if(v.getId() == R.id.rec){
                isStop = true;  // 停止播放录音
                //设置录音文件名，编码格式和输出格式都可以根据自身需要修改，这里使用3gp的配置方式，使用默认的方式产生.amr文件
                recordFileName = Environment.getExternalStorageDirectory().getAbsolutePath()+File.separator+System.currentTimeMillis();
                try {
//                    if(audioRecorderSetCallBack == 0){
//                        AudioRecordUtil.getInstance().setRecCallBack(ImgActivity.this);
//                        audioRecorderSetCallBack = 1;
//                    }
//
//                    AudioRecordUtil.getInstance().start(recordFileName);
                    lingoRecorder.start(recordFileName+".pcm");
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

//                Thread mt = new Thread(playPCMRecord, "playPCM");
//                mt.start();
                int bufferSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                FileInputStream fis = null;
                try {
                    audioTrack.play();
                    fis = new FileInputStream(recordFileName+".pcm");
                    byte[] buffer = new byte[bufferSize];
                    int len = 0;
                    isPlaying = true;
                    while ((len = fis.read(buffer)) != -1 && !isStop) {
//                    Log.d(TAG, "playPCMRecord: len " + len);
                        audioTrack.write(buffer, 0, len);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "playPCMRecord: e : " + e);
                } finally {
                    isPlaying = false;
                    isStop = false;
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
            else if(view.getId() == R.id.exchange){  // 重新照相或选择图片
                Album.image(context) // Image selection.
                        .singleChoice()
                        .camera(true)
                        .columnCount(4)
                        .onResult(new Action<ArrayList<AlbumFile>>() {
                            @Override
                            public void onAction(@NonNull ArrayList<AlbumFile> result) {
                                imgUrl = result.get(0).getPath();
                                Glide.with(img.getContext())
                                        .load(imgUrl)
                                        .error(R.drawable.ic_launcher_foreground)
                                        .placeholder(R.drawable.ic_launcher_foreground)
                                        .transition(withCrossFade())
                                        .into(img);
                            }
                        })
                        .onCancel(new Action<String>() {
                            @Override
                            public void onAction(@NonNull String result) {
                            }
                        })
                        .start();

            }
            else if(view.getId() == R.id.comment){ // 语义分析
                imgPresenter.audioSematic(recordFileName);

            }
            else if(view.getId() == R.id.browse){ // 图像识别/Image Classify
                imgPresenter.classifyImg(imgUrl);
            }
        }
        private Runnable playPCMRecord = new Runnable() {
            @Override
            public void run() {
                int bufferSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 16000, AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
                FileInputStream fis = null;
                try {
                    audioTrack.play();
                    fis = new FileInputStream(recordFileName+".pcm");
                    byte[] buffer = new byte[bufferSize];
                    int len = 0;
                    isPlaying = true;
                    //play.setFillColor(getResources().getColor(R.color.blue_dark));
                    while ((len = fis.read(buffer)) != -1 && !isStop) {
                        audioTrack.write(buffer, 0, len);
                    }

                } catch (Exception e) {
                    Log.e("TAG", "playPCMRecord: e : " + e);
                    e.printStackTrace();
                } finally {
                    isPlaying = false;
                    isStop = true;
                    //play.setFillColor(getResources().getColor(R.color.blue_tianyi));
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