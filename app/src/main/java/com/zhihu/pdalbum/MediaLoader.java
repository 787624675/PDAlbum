package com.zhihu.pdalbum;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.AlbumLoader;


    public class MediaLoader implements AlbumLoader {

        @Override
        public void load(ImageView imageView, AlbumFile albumFile) {
            load(imageView, albumFile.getPath());
        }

        @Override
        public void load(ImageView imageView, String url) {
            Glide.with(imageView.getContext())
                    .load(url)
                    .error(R.drawable.ic_launcher_foreground)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .transition(withCrossFade())
                    .into(imageView);
        }
    }

