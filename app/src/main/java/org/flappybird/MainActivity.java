package org.flappybird;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    GameView gameView;
    int Score = 0;

    private static final String TAG = "Location";
    private DatabaseHelper dbHelper;
    private boolean isFirstLocation = true;

    TextView ctimeTextView;
    TextView clocationTextView;

    public LocationClient mLocationClient;
    LocationClientOption option;
    private final MyLocationListener myListener = new MyLocationListener();

    public MapView mMapView = null;
    public BaiduMap mBaiduMap;

    private final Handler mHandler = new Handler();
    private final Runnable mTimeUpdater = new Runnable() {
        @Override
        public void run() {
            updateTimeTextView();
            mHandler.postDelayed(this, 1000); // 每秒更新一次
        }
    };

    class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location){
            //此处的BDLocation为定位结果信息类，通过它的各种get方法可获取定位相关的全部结果
            //以下只列举部分获取经纬度相关（常用）的结果信息
            //更多结果信息获取说明，请参照类参考中BDLocation类中的说明

            double latitude = location.getLatitude();    //获取纬度信息
            double longitude = location.getLongitude();    //获取经度信息
            float radius = location.getRadius();    //获取定位精度，默认值为0.0f

            String coorType = location.getCoorType();
            //获取经纬度坐标类型，以LocationClientOption中设置过的坐标类型为准

            int errorCode = location.getLocType();
            //获取定位类型、定位错误返回码，具体信息可参照类参考中BDLocation类中的说明

            // 输出经纬度到Logcat
            Log.d("LocationUpdate", "Latitude: " + latitude + ", Longitude: " + longitude);

            MyLocationData.Builder builder = new MyLocationData.Builder();
            MyLocationData locationData = builder.accuracy(location.getRadius())
                    .direction(location.getDirection())
                    .latitude(latitude)
                    .longitude(longitude)
                    .build();
            mBaiduMap.setMyLocationData(locationData);

            // 动画显示
            LatLng ll = new LatLng(latitude, longitude);
            MapStatus.Builder statusBuilder = new MapStatus.Builder();
            statusBuilder.target(ll).zoom(18.0f);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(statusBuilder.build()));

            // 更新Location TextView
            clocationTextView.setText("Location: " + latitude + ", " + longitude);

            if (isFirstLocation) {
                    insertLocation(latitude, longitude, "start"); // 记录开启时的位置
                    isFirstLocation = false;
                }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 初始化SDK和其他设置
        SDKInitializer.setAgreePrivacy(getApplicationContext(), true);
        SDKInitializer.initialize(getApplicationContext());
        LocationClient.setAgreePrivacy(true);

        // =====================================================



        // =====================================================


        // 设置全屏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        ctimeTextView = findViewById(R.id.ctime);
        clocationTextView = findViewById(R.id.clocation);

        // ============================================================

        mMapView = findViewById(R.id.bmapView);

        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMyLocationEnabled(true);

        dbHelper = new DatabaseHelper(this);

        try {
            mLocationClient = new LocationClient(getApplicationContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        option = new LocationClientOption();

        option.setIsNeedAddress(true);
        option.setNeedNewVersionRgc(true);
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);

        option.setCoorType("bd09ll");
        option.setScanSpan(1000);
        option.setOpenGnss(true);
        option.setLocationNotify(true);
        option.setIgnoreKillProcess(false);
        option.SetIgnoreCacheException(false);
        option.setWifiCacheTimeOut(5*60*1000);
        option.setEnableSimulateGnss(false);
        option.setNeedNewVersionRgc(true);

        mLocationClient.setLocOption(option);

        mLocationClient.registerLocationListener(myListener);

        // 启动定位
        mLocationClient.start();


        // 启动时间更新
        mHandler.post(mTimeUpdater);

        // ============================================================

        gameView = findViewById(R.id.gameView);
        gameView.setGameListener(new GameView.GameListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void addScore(final int score) {
                runOnUiThread(() -> {
                    Score = score;
                    ((TextView) findViewById(R.id.score)).setText("Current score: " + score);
                });
            }

            @SuppressLint("SetTextI18n")
            @Override
            public void gameOver() {
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.scoreTxt)).setText("Score:" + Score);
                    findViewById(R.id.relative).setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void gameReady() {
                runOnUiThread(() -> ((RelativeLayout) findViewById(R.id.relative)).setVisibility(View.GONE));
            }
        });

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                gameView.reSet();
                Score = 0; // 重置分数
                ((TextView) findViewById(R.id.score)).setText("Current score: " + Score); // 更新显示的分数
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void insertLocation(double latitude, double longitude, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LATITUDE, latitude);
        values.put(DatabaseHelper.COLUMN_LONGITUDE, longitude);
        values.put(DatabaseHelper.COLUMN_STATUS, status); // 记录状态

        // 获取当前时间并格式化为字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, timestamp);

        long newRowId = db.insert(DatabaseHelper.TABLE_NAME, null, values);
        if (newRowId != -1) {
            Log.d(TAG, "Location inserted successfully with ID: " + newRowId);
        } else {
            Log.e(TAG, "Error inserting location");
        }
    }

    private void updateTimeTextView() {
        // 获取当前时间并格式化为字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        // 更新时间TextView
        ctimeTextView.setText("Time: " + timestamp);
    }

    @Override
    protected void onStop() {
        Log.d("Status", "onStop");

        // 记录关闭时的位置
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.requestLocation();
            mLocationClient.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Log.d("Status", "onStop2");
                        insertLocation(latitude, longitude, "end"); // 记录关闭时的位置
                    }
                }
            });
        }

        mLocationClient.stop(); // 停止定位
        mHandler.removeCallbacks(mTimeUpdater); // 停止时间更新

        Log.d("Status", "onStop3");

        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d("Status", "onDestroy");

        // 记录关闭时的位置
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.requestLocation();
            mLocationClient.registerLocationListener(new BDAbstractLocationListener() {
                @Override
                public void onReceiveLocation(BDLocation location) {
                    if (location != null) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        insertLocation(latitude, longitude, "end"); // 记录关闭时的位置
                    }
                }
            });
        }

        mLocationClient.stop(); // 停止定位
        mHandler.removeCallbacks(mTimeUpdater); // 停止时间更新

        super.onDestroy();
    }
}