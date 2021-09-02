package com.zhihu.pdalbum;

import android.content.Context;
import android.widget.TextView;

public interface ImgContract {
    interface Presenter{
        // 图像识别
        void classifyImg(String imgPath);
        void convertImg2Float();
        void handleColorRgbs();



    }
    interface View{
        void showImg(Context context);


    }
}
