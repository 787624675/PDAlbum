package com.zhihu.pdalbum;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
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
import com.yanzhenjie.album.app.Contract;

import de.hdodenhof.circleimageview.CircleImageView;

public class ImgActivity extends AppCompatActivity implements ImgContract.View{
    private ImageView img;
    private CircleImageView rec;
    private CircleImageView play;
    private ImgPresenter imgPresenter;
    private Context context;
    private String imgUrl;
    private MediaPlayer mediaPlayer;
    private String recordfilename;
    private MediaRecorder mediaRecorder;

    private TextView recNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_img);

        context = this;
        initView();
        showImg(context);
    }
    void initView(){
        img = findViewById(R.id.img);
        rec = findViewById(R.id.rec);
        play = findViewById(R.id.play);
        recNote = findViewById(R.id.rec_note);

        MyButtonListener mbl=new MyButtonListener();

        rec.setOnLongClickListener(mbl);
        rec.setOnTouchListener(mbl);

        // see 「empty_xl001」 https://blog.csdn.net/u010574567/article/details/51900453
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer=new MediaPlayer();
                try{
                    mediaPlayer.setDataSource(recordfilename);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                }catch (Exception e)
                {
                    // Log.v("RecordActivity play record ","Expection"+Log.getStackTraceString(e));
                }

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
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
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
                recordfilename= Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+System.currentTimeMillis();
                recordfilename+=".3gp";
                //mediarecorder 初始化
                mediaRecorder=new MediaRecorder();
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);  //录入设备
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);  //输出格式
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); //编码格式
                mediaRecorder.setOutputFile(recordfilename); //输出文件路径
                try {
                    mediaRecorder.prepare();
                    recNote.setVisibility(View.VISIBLE);
                    rec.setFillColor(getResources().getColor(R.color.blue_dark));
                }catch (Exception e)
                {
                    Log.v("RecordActivity","Expection"+Log.getStackTraceString(e));
                }
                mediaRecorder.start();
            }

            return true;
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.play){   // 播放所有录音

            }
            else if(view.getId() == R.id.exchange){  // 重新照相或选择图片

            }
            else if(view.getId() == R.id.comment){ // comment的显示与隐藏

            }
        }
    }


}