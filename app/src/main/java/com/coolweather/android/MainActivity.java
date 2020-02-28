package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 获取本地存储的 share_preference 库
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // 获取库中的内容,
        // 如果不为null,则启动天气预报界面
        // 这样在下次启动app时，就直接显示了天气预报界面
        if(prefs.getString("weather",null) != null){
            Intent intent = new Intent(this,WeatherActivity.class);
            startActivity(intent);
        }
    }
}
