package com.snarayanan.parstagram;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseObject;

public class ParseApplication extends Application {
    @Override
    public void onCreate() {

        //Initializes Parse SDK as soon as the application is created
        super.onCreate();

        //Register your parse models
        ParseObject.registerSubclass(Post.class);
        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("wsRcb7aXeB83HOFxjqVDLWwzSlCuK93xIdeo51KI")
                .clientKey("2dAdIh3MV2NJQtlT9TeEoxdFurLlYmADnN8ub6dF")
                .server("https://parseapi.back4app.com")
                .build()
        );
    }
}
