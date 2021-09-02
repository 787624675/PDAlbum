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
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.Resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class ImgActivity extends AppCompatActivity implements ImgContract.View{
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        context = this;
        initView();

        showImg(context);
    }
    void initView(){

        imgPresenter = new ImgPresenter();

        img = findViewById(R.id.img);
        rec = findViewById(R.id.rec);
        play = findViewById(R.id.play);
        browse = findViewById(R.id.browse);
        recNote = findViewById(R.id.rec_note);
        tvClassifyRes = findViewById(R.id.classify_res);

        MyButtonListener mbl=new MyButtonListener();

        rec.setOnLongClickListener(mbl);
        rec.setOnTouchListener(mbl);
        browse.setOnClickListener(mbl);
        play.setOnClickListener(mbl);



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
            if (view.getId() == R.id.play){   // 播放所有录音
                Thread mt = new Thread(playPCMRecord, "playPCM");
                // 步骤4：通过 线程对象 控制线程的状态，如 运行、睡眠、挂起  / 停止
                mt.start();
            }
            else if(view.getId() == R.id.exchange){  // 重新照相或选择图片

            }
            else if(view.getId() == R.id.comment){ // comment的显示与隐藏

            }
            else if(view.getId() == R.id.browse){ // 图像识别/Image Classify
                imgPresenter.classifyImg(imgUrl,tvClassifyRes);
                if (tvClassifyRes.getVisibility() == View.VISIBLE){
                    tvClassifyRes.setVisibility(View.INVISIBLE);
                }
                else if(tvClassifyRes.getVisibility() == View.INVISIBLE){
                    tvClassifyRes.setVisibility(View.VISIBLE);
                }
            }
        }
        //  https://darksilber.tistory.com/61
        private void playShortAudioFileViaAudioTrack(String filePath) throws IOException
        {
            // We keep temporarily filePath globally as we have only two sample sounds now..
            if (filePath==null)
                return;

            //Reading the file..
            byte[] byteData = null;
            File file = null;
            file = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"
            byteData = new byte[(int) file.length()];
            FileInputStream in = null;
            try {
                in = new FileInputStream( file );
                in.read( byteData );
                in.close();

            } catch (FileNotFoundException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }
// Set and push to audio track..
            int intSize = android.media.AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_8BIT);
            AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_8BIT, intSize, AudioTrack.MODE_STREAM);
            if (at!=null) {
                at.play();
// Write the byte array to the track
                at.write(byteData, 0, byteData.length);
                at.stop();
                at.release();
            }
            else
                Log.d("TCAudio", "audio track is not initialised ");

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