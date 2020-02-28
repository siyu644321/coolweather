package com.coolweather.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    // 滑动控件对象
    public DrawerLayout drawerLayout;
    // 滑动布局的控制按钮
    private Button navButton;

    // 下拉菜单
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;

    // 定义控件对象
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    // 添加背景图片
    private ImageView bingPicImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 优化图片显示布局
        if(Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        // 初始化各种控件
        weatherLayout = (ScrollView)findViewById(R.id.weather_layout); // 天气滚动条布局
        titleCity = (TextView)findViewById(R.id.title_city);
        titleUpdateTime = (TextView)findViewById(R.id.title_update_time);
        degreeText = (TextView)findViewById(R.id.degree_text);
        weatherInfoText = (TextView)findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout)findViewById(R.id.forecast_layout);
        aqiText = (TextView)findViewById(R.id.aqi_text);
        pm25Text = (TextView)findViewById(R.id.pm25_text);
        comfortText = (TextView)findViewById(R.id.comfort_text);
        carWashText = (TextView)findViewById(R.id.car_wash_text);
        sportText = (TextView)findViewById(R.id.sport_text);

        // 初始化背景图片控件,并动态加载背景图片
        bingPicImg = (ImageView)findViewById(R.id.bing_pic_img);

        // 创建下拉刷新
        swipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary); // 设置下拉转圈颜色


        // 获取滑动布局/控制滑动布局按钮的对象
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        navButton = (Button)findViewById(R.id.nav_button);
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 按钮事件,打开布局控件
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });


        // 用于存储到shared_prefs文件夹下
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // 判断图片是否已经有缓存了
        String bingPic = prefs.getString("bing_pic",null);
        if(bingPic != null){
            // 调用Glide加载图片资源
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            // 没有缓存,从服务器获取图片
            loadBingPic();
        }

        // 判断是否存放了"weather"这个数据
        String weatherString = prefs.getString("weather",null);
        if(weatherString != null){
            //数据已在缓存中,直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            // [保存数据ID]
            mWeatherId = weather.basic.weatherId;
            // 将数据解析并显示到界面上
            showWeatherInfo(weather);
        }else{
            // 无缓存数据,通过服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            // 隐藏天气布局滚动条,否则会很奇怪..
            weatherLayout.setVisibility(View.INVISIBLE);
            // 通过服务器查询
            requestWeather(mWeatherId);
        }

        // 刷新数据监听
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 根据ID，请求网络数据
                requestWeather(mWeatherId);
            }
        });

    }

    // ---> 从必应服务器上获取图片
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        // 发送请求，获取服务器数据
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {

            // callBack,成功返回
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 转换成数据
                final String bingPic = response.body().string();
                // 将数据存入本地缓存库
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);   // 存入数据
                editor.apply();
                // 回到主线程,设置图片
                runOnUiThread(()->{
                    Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                });
            }

            // callBack,失败
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    // ---> 根据天气ID,在服务器请求weather数据
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid="+
                weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";

        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            // 获取到数据
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 服务器请求数据,并分析出数据
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                // 启动UI线程(回主界面)
                runOnUiThread(()->{
                    if(weather != null && "ok".equals(weather.status)){
                        // 获取到了天气数据,将数据存入preference文件夹
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(WeatherActivity.this).edit();
                        editor.putString("weather",responseText);
                        editor.apply();
                        // 将天气显示出来
                        showWeatherInfo(weather);
                    }else{
                        // 提示获取失败
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",
                                Toast.LENGTH_SHORT).show();
                    }
                    // 关闭刷新..
                    swipeRefresh.setRefreshing(false);
                });


            }

            // 数据获取失败
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                // 启动Ui线程,提示获取数据失败
                runOnUiThread(()->{
                    Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                    swipeRefresh.setRefreshing(false); // 关闭刷新
                });
            }
        });

        // 显示背景图片
        loadBingPic();
    }



    // ---> 显示天气信息到界面上
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.CityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1]; // 切割字符串
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        // 显示到界面上
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);

        // 清空当前的列表框
        forecastLayout.removeAllViews();
        // 将数据添加到文本控件中
        for(Forecast forecast:weather.forecastList){
            // 动态加载布局
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
            TextView dateText = (TextView)view.findViewById(R.id.date_text);
            TextView infoText = (TextView)view.findViewById(R.id.info_text);
            TextView maxText = (TextView)view.findViewById(R.id.max_text);
            TextView minText = (TextView)view.findViewById(R.id.min_text);

            // 设置内容到控件
            dateText.setText(forecast.data);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if(weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度: " + weather.suggestion.comfort.info;
        String carWash = "洗车指数: " + weather.suggestion.carWash.info;
        String sport = "运动建议: " + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        // 显示天气布局的滚动条
        weatherLayout.setVisibility(View.VISIBLE);

        // 启动服务
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }
}
