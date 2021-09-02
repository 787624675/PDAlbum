package com.zhihu.pdalbum;

import android.app.Activity;

public interface MainContract {
    interface Presenter{
        void init();
        void getAllImagePath();
        void verifyPermissions(Activity activity);
        void startImageActivity();
    }
    interface View{

        void showImages();

    }
}
