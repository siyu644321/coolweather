package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.LitePal;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;  // 进度条控件
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter; // 数组配置器
    private List<String> dataList = new ArrayList<>(); // 用于显示到列表框控件(省,市,县)内容的数组

    /*
    *   省列表
    * */
    private List<Province> provinceList;

    /*
    *   市列表
    * */
    private List<City> cityList;

    /*
    *   县列表
    * */
    private  List<County> countyList;

    // 选中的省份
    private Province selectedProvince;

    // 选中的城市
    private City selectedCity;

    // 当前选中的级别
    private int currentLevel;


    // [----> 创建碎片 <-----]
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // 获取碎片布局
        View view = inflater.inflate(R.layout.choose_area,container,false);
        // 加载控件对象
        titleText = (TextView)view.findViewById(R.id.title_text);
        backButton = (Button)view.findViewById(R.id.back_button);
        // 创建列表控件 及 配置器
        listView = (ListView)view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter); // 设置配置器

        return view;
    }

    // [---> 当获取被创建后启动,晚于onCreateView <--]
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // 1）列表: 当窗口被创建完毕,调用列表框控件的响应
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 1. 如果是[省]
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position); // 获取省列表中当前选中项
                    queryCities(); // 自定义函数,则查询-->城市 (因为点击以后显示的是其下一级的内容)
                }else if(currentLevel == LEVEL_CITY){
                    // 2. 如果是[市]
                    selectedCity = cityList.get(position);
                    queryCounties(); // 自定义函数,查询-->县
                }else if(currentLevel == LEVEL_COUNTY){
                    // 如果是县,显示城市天气Activity
                    String weatherId = countyList.get(position).getWeatherId();
                    // 【如果当前碎片 是 MainActivity的实例】
                    if(getActivity() instanceof MainActivity){ // 创建这个碎片对应的活动
                        Intent intent = new Intent(getActivity(),WeatherActivity.class);
                        // 传递值给窗口 并 启动天气预报窗口.
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        // 关闭当前碎片窗口
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity/*如果是WeatherActivity实例*/){

                        // 获取当前的Weather对象
                        WeatherActivity activity = (WeatherActivity)getActivity();
                        // 关闭当前的滑动布局
                        activity.drawerLayout.closeDrawers();
                        // 设置等待刷新
                        activity.swipeRefresh.setRefreshing(true);
                        // 重新请求天气
                        activity.requestWeather(weatherId);
                    }

                }
            }
        });

        // 2）按钮: 当返回键被响应,处理其响应
        backButton.setOnClickListener( v -> {
            // 点击返回键,如果当前访问的是县区,则调用市函数,
            // 即由于是返回,调用其当前选择的高一级对象
            if(currentLevel == LEVEL_COUNTY){
                queryCities();
            }else if(currentLevel == LEVEL_CITY){
                queryProvinces();
            }
        });

        // 3) 啥都没选择,默认查询[省]
        queryProvinces();
    }

    // =======================【查询 省/市/县 函数】 =======================

    // --> [查询省],优先查询数据库,如果没有再去网页查询 <--
    private void queryProvinces() {

        titleText.setText("中国"); // 设置标题
        backButton.setVisibility(View.GONE); // 如果查询的是省,则因此返回按钮(因为已经在顶层了)
        // 优先查询数据库..
        provinceList = LitePal.findAll(Province.class);

        // 1. 如果数据库有内容,则遍历数据库并显示出来
        if(provinceList.size()>0){

            // 1.1 先清空用于存放显示列表的数组
            dataList.clear();

            // 1.2 更新到数组中
            for(Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            // 1.3 通知列表配适器,我们的列表数据更新了
            adapter.notifyDataSetChanged();
            // 1.4 让列表框默认选择第0项
            listView.setSelection(0);
            // 1.5 修改当前选择级别为省
            currentLevel = LEVEL_PROVINCE;
        }else{

            // 2.1 如果数据库没有内容,去网页请求数据
            String address = "http://guolin.tech/api/china";
            // 2.2 自定义函数: 去服务器请求数据
            queryFromServer(address, "province");
        }

    }

    // --> [查询城市],优先查询数据库,如果没有再去网页查询 <--
    private void queryCities() {

        // 1. 设置城市标题,该对象从按钮控件中获取
        titleText.setText(selectedProvince.getProvinceName());
        // 2. 将返回键设置为可见
        backButton.setVisibility(View.VISIBLE);
        // 3. 获取城市列表 -> 调用LitePal去数据库查询数据
        // 获取到当前id相符的数组
        cityList = LitePal.where("provinceid = ?",
                String.valueOf(selectedProvince.getId())) // 将int数据转换成字符串
                .find(City.class);
        // 4.1 如果数据库有数据,则直接调用数据库
        if(cityList.size() > 0){
            dataList.clear();
            for(City city: cityList){
                dataList.add(city.getCityName());
            }
            // 通知配置器数据被更新了
            adapter.notifyDataSetChanged();
            listView.setSelection(0); // 默认选择第0个
            currentLevel = LEVEL_CITY; // 变更选择项
        }else{

            // 4.2 如果数据库无数据,去服务器请求
            int provinceCode = selectedProvince.getProvinceCode(); // 获取省的id，用于字符串拼接
            String address = "http://guolin.tech/api/china/" + String.valueOf(provinceCode);
            // 请求数据
            queryFromServer(address,"city");
        }
    }


    // --> [查询县区],优先查询数据库,如果没有再去网页查询 <--
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?",
                String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0 ){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }else{
            // 获取省,市的id，用于字符串拼接
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            // 拼接,组成网站地址
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" +  String.valueOf(cityCode);

            // 去服务器请求数据
            queryFromServer(address,"county");
        }
    }




    // =======================【 从服务器请求数据到本地相关方法 】 =======================

    // -> 从服务器请求数据
    private void queryFromServer(String address, final String type) {
        // 1. 显示加载进度条
        showProgressDialog();
        // 2. 去服务器请求数据（Callback内部会开启一条子线程..
        // 后续需要调用getActivity()/runOnUiThread且会主线程）
        // 参数2为线程回调函数,
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            // 2.1 callback回调函数,成功时的响应
            @Override // Callback中的response存放了从服务器响应到的数据
            public void onResponse(Call call, Response response) throws IOException {

                // 3. 获取响应到的 网页body 中的内容
                String responseText = response.body().string(); // 获取响应,body部分的字符串
                boolean result = false;

                // 4. 如果是[省]
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    // 5. 如果是 [市]
                    result = Utility.handleCityResponse(responseText,
                            selectedProvince.getId() /*该值已经在设置省时初始化了*/);
                } else if ("county".equals(type)) {
                    // 6. 如果是 [区县]
                    result = Utility.handleCountyResponse(responseText,
                            selectedCity.getId() /*该值已经在设置市时初始化了*/);
                }

                // 7. 如果将数据正确的存放到了数据库
                if (result) {
                    // 8. 调用runOnUiThread,切回到主线程..将数据显示到列表框中
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // 8.1 关闭进度条
                            closeProgressDialog();
                            // 8.2 将数据 根据 省/市/县 调用不同的方法 显示到ListView中
                            if("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }


            // 2.2 callback回调函数,失败时的响应
            @Override
            public void onFailure(Call call, IOException e) {
                // 通过 runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog(); // 关闭进度条显示
                        // getActivity() 获得Fragment依附的Activity对象
                        // getContext()  返回的是当前View运行在哪个Activity Context中
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
    }

    // -> 加载并显示进度条..
    private void showProgressDialog() {
        if(progressDialog == null){
            // 如果未创建对象,则创建
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            //dialog弹出后会点击屏幕，dialog不消失；点击物理返回键dialog消失
            progressDialog.setCanceledOnTouchOutside(false);
        }

        progressDialog.show();
    }

    // -> 关闭进度条
    private void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

}
