package com.zhihu.pdalbum;

import android.content.Context;
import android.widget.TextView;

public interface ImgContract {
    interface Presenter{
        // 图像识别
        void classifyImg(String imgPath);
        void convertImg2Float();
        void handleColorRgbs();
        // 语音处理
        void audioDenoise(String audioPath);
        void audioToText(String audioPath);
        void audioSematic(String audioPath);




    }
    interface View{
        void showImg(Context context);


    }
}
