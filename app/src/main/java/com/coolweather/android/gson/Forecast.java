package com.coolweather.android.gson;

import com.google.gson.annotations.SerializedName;

public class Forecast {
    // 它在网页中显示的是JSON数组,我们只需要定义出单日天气
    // 实体类就可以了,然后在实体类引用的时候使用集合类型
    // 进行声明
    public String data;
    @SerializedName("tmp")
    public Temperature temperature;
    @SerializedName("cond")
    public More more;

    public class Temperature{
        public String max;
        public String min;
    }

    public class More{
        @SerializedName("txt_d")
        public String info;
    }
}
