package com.coolweather.android.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

// 用于Http数据交互
public class HttpUtil {
    public static void sendOkHttpRequest(String address,okhttp3.Callback callback){
        // 创建OKHttp客户区
        OkHttpClient client = new OkHttpClient();
        // 创建请求
        Request request = new Request.Builder()
                .url(address)
                .build();
        // enqueue() 方法内部帮我开启了子线程,然后回在子线程中执行HTTP请求,
        // 并将最终请求结果回调到 okhttp3.Callback 当中
        client.newCall(request).enqueue(callback);
    }
}
