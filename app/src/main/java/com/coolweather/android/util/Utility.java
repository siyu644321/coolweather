package com.coolweather.android.util;

import android.text.TextUtils;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.gson.Weather;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utility {
    /*
    * 解析和处理服务器返回的省级数据
    * */
    public static boolean handleProvinceResponse(String response){
        if(!TextUtils.isEmpty(response)){
            try {// 如果返回的数据不为null 或 空, 获取JSON数组
                JSONArray allProvinces = new JSONArray(response);
                for(int i=0;i<allProvinces.length();i++){
                    // // 获取每一个对象
                    JSONObject provinceObject = allProvinces.getJSONObject(i);
                    // 设置省份的内容,并通过LitePal保存到数据库..
                    Province province = new Province();
                    province.setProvinceName(provinceObject.getString("name"));
                    province.setProvinceCode(provinceObject.getInt("id"));
                    province.save();
                }
                return true;

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return false;
    }


    /*
    *  解析和处理服务器返回的市级数据
    * */
    public static boolean handleCityResponse(String response, int provinceId){
        if(!TextUtils.isEmpty(response)){
            try {
                JSONArray allCities = new JSONArray(response);
                for(int i = 0; i<allCities.length();i++){
                    // 获取到每一个城市对象
                    JSONObject cityObject = allCities.getJSONObject(i);
                    // 创建City对象，设置并存储到数据库中
                    City city = new City();
                    city.setCityName(cityObject.getString("name"));
                    city.setCityCode(cityObject.getInt("id"));
                    city.setProvinceId(provinceId); // 当前市所属省的id
                    city.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return false;
    }


    /*
    *  解析和处理服务器返回的县级数据
    * */
    public static boolean handleCountyResponse(String response, int cityId){
        if(!TextUtils.isEmpty(response)){
            try {
                JSONArray allCounties = new JSONArray(response);
                for(int i=0; i<allCounties.length(); i++){
                    JSONObject countryObject = allCounties.getJSONObject(i);
                    County county = new County();
                    // 设置县的数据并保存到数据库
                    county.setCountyName(countryObject.getString("name"));
                    county.setWeatherId(countryObject.getString("weather_id")); // 天气ID
                    county.setCityId(cityId); // 存储县所对应的市的id
                    county.save();
                }
                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    // ==================================【解析成Weather实体类】==============================================

    // 将返回的JSON数据解析成Weather实体类
    public static Weather handleWeatherResponse(String response){
        try{
            // 1. 先将获取到的数据变成一个JSON对象
            JSONObject jsonObject = new JSONObject(response);
            // 2. 然后获取这个对象中的内容,通过 JSONArray 获取
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather");
            // 3. 获取这个数组中的内容,转成String
            String weatherContent = jsonArray.getJSONObject(0).toString();

            // 4. 通过GSON，返回数据对象 -> Weather
            return new Gson().fromJson(weatherContent,Weather.class);

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
