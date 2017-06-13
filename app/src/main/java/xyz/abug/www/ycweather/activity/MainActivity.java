package xyz.abug.www.ycweather.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import xyz.abug.www.ycweather.R;
import xyz.abug.www.ycweather.db.SelectListDb;
import xyz.abug.www.ycweather.fragment.MainFragment;
import xyz.abug.www.ycweather.gson.Weather;
import xyz.abug.www.ycweather.utils.HttpUtils;
import xyz.abug.www.ycweather.utils.Utility;
import xyz.abug.www.ycweather.utils.Utils;

import static xyz.abug.www.ycweather.utils.Utils.URL_HEWEATHER_KEY;
import static xyz.abug.www.ycweather.utils.Utils.URL_HEWEATHER_SEARCH_1;
import static xyz.abug.www.ycweather.utils.Utils.URL_HEWEATHER_SEARCH_2;
import static xyz.abug.www.ycweather.utils.Utils.URL_HEWEATHER_WEATHER_1;
import static xyz.abug.www.ycweather.utils.Utils.URL_HEWEATHER_WEATHER_2;

public class MainActivity extends AppCompatActivity {
    //有无网络
    private boolean isNetWorkBool = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //判断有无网络
        isNetWork();
        //程序第一次运行
        isFirstRun();
    }

    /**
     * 判断网络
     */
    private void isNetWork() {
        isNetWorkBool = HttpUtils.isNetworkAvailable(MainActivity.this);
        if (!isNetWorkBool) {
            Toast.makeText(MainActivity.this, "请检查网络", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    /**
     * 运行
     */
    private void isFirstRun() {
        boolean firstRun = Utils.isFirstRun(MainActivity.this);
        if (firstRun) {
            //第一次运行
            getIP();
        } else {
            //非第一次运行
            getIP();
        }
    }

    /**
     * 获取IP地址
     */
    private void getIP() {
        HttpUtils.sendQuestBackResponse(Utils.URL_YY_GETIP, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String notJsonData = response.body().string();
                //打印数据
                Utils.logData(notJsonData);
                //解析城市
                String city = Utility.analyticFirstIp(notJsonData);
                //搜索城市
                searchCity(city);
                //打印城市
                Utils.logData("第一次运行:" + city);

            }
        });
    }

    /**
     * 搜索城市，子线程中,获取代码
     */
    private void searchCity(String city) {
        HttpUtils.sendQuestBackResponse(URL_HEWEATHER_SEARCH_1 + city + URL_HEWEATHER_SEARCH_2 + URL_HEWEATHER_KEY, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String jsonData = response.body().string();
                //打印数据
                Utils.logData("第一次搜索城市数据:" + jsonData);
                List<SelectListDb> dbs = Utility.analyticSearchCity(jsonData);
                //保存数据
                SelectListDb db = dbs.get(0);
                Utility.saveSelectCity(db);
                String weatherId = db.getmWeatherId();
                Utils.logData("第一次获取天气id：" + weatherId);
                //根据id获取天气
                getWeather(weatherId);
            }
        });
    }

    /**
     * 获取天气
     *
     * @param weatherId 天气代码
     */
    private void getWeather(final String weatherId) {
        HttpUtils.sendQuestBackResponse(URL_HEWEATHER_WEATHER_1 + weatherId + URL_HEWEATHER_WEATHER_2 + URL_HEWEATHER_KEY, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String data = response.body().string();
                Utils.logData("天气数据：" + data);
                final Weather weather = Utility.analyticWeather(data);
                //显示数据
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //保存搜索内容
                        SelectListDb db = new SelectListDb();
                        db.setmWeatherStatus(weather.now.cond.txt);
                        db.setmWeatherTemp(weather.now.tmp);
                        db.setmCityname(weather.basic.city);
                        db.setmWeatherId(weather.basic.weatherId);
                        Utility.upDateSelectCity(db);
                        MainFragment.showData(weather);
                    }
                });
            }
        });
    }

}