package com.zhihu.pdalbum;

import android.app.Activity;

public interface MainContract {
    interface Presenter{

        void getAllImagePath();
        void verifyPermissions(Activity activity);
        void startImageActivity(String url);
    }
    interface View{
        void init();
        void showImages();

    }
}
