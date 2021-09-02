package com.zhihu.pdalbum;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class MainPresenter implements MainContract.Presenter {

    private Context context;



    @Override
    public void getAllImagePath() {

    }

    @Override
    public void verifyPermissions(Activity activity) {

    }

    @Override
    public void startImageActivity(String url) {
        String path= url;
        Intent intent=new Intent(context,ImgActivity.class);
        intent.putExtra("path", path);
        context.startActivity(intent);

    }
}
