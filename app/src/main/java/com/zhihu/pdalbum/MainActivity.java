package com.zhihu.pdalbum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.Image;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.xiaomi.mace.demo.MaceApp;
import com.xiaomi.mace.demo.camera.MessageEvent;
import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumConfig;
import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.widget.photoview.gestures.OnGestureListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity implements
        MainContract.View,
        View.OnTouchListener,
        GestureDetector.OnGestureListener
{
    /**
     * 存储手机中所有图片的list集合
     */
    List<String> paths = new ArrayList<String>();

    //用来显示手机中所有图片的GridView
    private GridView mGridView;
    private CircleImageView circleImg;
    private MyGridViewAdapter adapter;

    private LinearLayout root;
    boolean start = false;
    int last_param = 240;
    int cur_param = 240;
    ImageView addImg;
    ImageView takeImg;

    Context context;

    private ArrayList<AlbumFile> mAlbumFiles;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        adapter=new MyGridViewAdapter();
        init();
        verifyPermissions(this);
        getAllImagePath();
        gestureDetector = new GestureDetector(this);

    }
    View.OnClickListener addImgClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Album.image(context) // Image selection.
                    .multipleChoice()
                    .camera(true)
                    .columnCount(4)
                    .selectCount(5)
                    .checkedList(mAlbumFiles)
                    .onResult(new Action<ArrayList<AlbumFile>>() {
                        @Override
                        public void onAction(@NonNull ArrayList<AlbumFile> result) {
                            for (int a = 0; a < result.size();a++){
                                paths.add(0,result.get(a).getPath());

                            }
                            adapter.notifyDataSetChanged();

                        }
                    })
                    .onCancel(new Action<String>() {
                        @Override
                        public void onAction(@NonNull String result) {
                        }
                    })
                    .start();
        }
    };
    View.OnClickListener takeImgClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view.getId()==R.id.take_img){
                Album.camera(context) // Camera function.
                        .image() // Take Picture.
                        .onResult(new Action<String>() {
                            @Override
                            public void onAction(@NonNull String result) {
                                String path= result;
                                paths.add(0,path);
                                adapter.notifyDataSetChanged();
                            }
                        })
                        .onCancel(new Action<String>() {
                            @Override
                            public void onAction(@NonNull String result) {
                            }
                        })
                        .start();

            }
            else if(view.getId() == R.id.circle_img){
                Album.camera(context) // Camera function.
                        .image() // Take Picture.
                        .onResult(new Action<String>() {
                            @Override
                            public void onAction(@NonNull String result) {
                                String path= result;
                                Intent intent=new Intent(context,ImgActivity.class);
                                intent.putExtra("path", path);
                                startActivity(intent);
                            }
                        })
                        .onCancel(new Action<String>() {
                            @Override
                            public void onAction(@NonNull String result) {
                            }
                        })
                        .start();

            }


        }
    };

    @Override
    public void init() {
        mGridView=(GridView) findViewById(R.id.grid_view);
        addImg = findViewById(R.id.add_img);
        takeImg = findViewById(R.id.take_img);
        circleImg = findViewById(R.id.circle_img);


        mGridView.setAdapter(adapter);
        mAlbumFiles = new ArrayList<AlbumFile>();
        Album.initialize(AlbumConfig.newBuilder(this)
                .setAlbumLoader(new MediaLoader())
                .setLocale(Locale.getDefault())
                .build()
        );
        cur_param = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,240,getResources().getDisplayMetrics());
        //设置GridView的条目点击事件
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String path=paths.get(arg2);
                //当我点击某个图片的时候代表要打开对应的页面
                Intent intent=new Intent(context,ImgActivity.class);
                //仅仅跳转过去不行，必须将当前点击图片的路径带过去
                intent.putExtra("path", path);
                startActivity(intent);
            }
        });
        addImg.setOnClickListener(addImgClick);
        takeImg.setOnClickListener(takeImgClick);
        circleImg.setOnClickListener(takeImgClick);
    }

    @Override
    public void showImages() {

    }

    private int firstItemPos;
    private float lastY;
    private float curY;
    private GestureDetector gestureDetector;

    @Override
    public void scaleCircleImage() {
        mGridView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return false;
            }
        });
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                firstItemPos = i;

            }
        });
    }



    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
//        if(firstItemPos >= 0 && firstItemPos <= 20){
//            if(firstItemPos <= 0 && firstItemPos >= -20){
//            LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(circleImg.getLayoutParams());
//            newParams.height = (int) ((float)cur_param-v1);
//            newParams.weight = (int) ((float)cur_param-v1);
        Log.d("onScroll firstItemPos:",""+firstItemPos);
        Log.d("onScroll motionEvent1:",motionEvent1.toString());
//        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    class MyGridViewAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return paths.size();
        }
        @Override
        public Object getItem(int position) {
            return null;
        }
        @Override
        public long getItemId(int position) {
            return 0;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            if(convertView==null){
                //这里面的item是一个自定义的View继承线性布局，继承什么布局不重要，
                // 重要的是将item的宽高设置成一样；感觉这个效果项目中很多地方都能用到
                convertView = LayoutInflater.from(context).inflate(R.layout.gridview_item, parent, false);
                vh = new ViewHolder();
                vh.imageView = (ImageView) convertView.findViewById(R.id.iv_head);
                convertView.setTag(vh);
            }else{
                vh=(ViewHolder) convertView.getTag();
            }
            //当前item要加载的图片路径
            String path=paths.get(position);
            //使用谷歌官方提供的Glide加载图片
            Glide.with(context).load(new File(path)).diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(vh.imageView);

            return convertView;
        }

    }
    class ViewHolder{
        ImageView imageView;
    }


    public void getAllImagePath() {
        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        //遍历相册
        while (cursor.moveToNext()) {
            String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            //将图片路径添加到集合
            paths.add(0,path);
        }
        cursor.close();
    }

    private static final int GET_RECODE_AUDIO = 1;

    private static String[] PERMISSION_ALL = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.WAKE_LOCK,
    };
    /** 申请录音权限*/

    public void verifyPermissions(Activity activity) {
        boolean permission = (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                || (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED);
        if (permission) {
            ActivityCompat.requestPermissions(activity, PERMISSION_ALL,
                    GET_RECODE_AUDIO);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GET_RECODE_AUDIO && resultCode == RESULT_OK){
            getAllImagePath();
            adapter=new MyGridViewAdapter();
            mGridView.setAdapter(adapter);
        }
    }




}