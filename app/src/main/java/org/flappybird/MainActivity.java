package org.flappybird;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
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

import com.tencent.map.geolocation.TencentLocation;
import com.tencent.map.geolocation.TencentLocationListener;
import com.tencent.map.geolocation.TencentLocationManager;
import com.tencent.map.geolocation.TencentLocationRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Location";
    private final Handler mHandler = new Handler();
    public double latitude = 0.;
    public double longitude = 0.;
    String address;
    GameView gameView;
    int Score = 0;
    TextView ctimeTextView;
    private final Runnable mTimeUpdater = new Runnable() {
        @Override
        public void run() {
            updateTimeTextView();
            mHandler.postDelayed(this, 1000); // 每秒更新一次
        }
    };
    TextView clocationTextView;
    TencentLocationManager mLocationManager;
    MyActivity myListener;
    private DatabaseHelper dbHelper;
    private boolean isFirstLocation = true;

    boolean isTableExist = false;
    // 用于每15秒写入一次数据库的Runnable
    private final Runnable mDatabaseUpdater = new Runnable() {
        @Override
        public void run() {
            new InsertLocationTask().execute(latitude, longitude, address);
            mHandler.postDelayed(this, 15000); // 每15秒执行一次
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TencentLocationManager.setUserAgreePrivacy(true);
        mLocationManager = TencentLocationManager.getInstance(this);

        dbHelper = new DatabaseHelper(this);
        myListener = new MyActivity();

        // 设置全屏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

        ctimeTextView = findViewById(R.id.ctime);
        clocationTextView = findViewById(R.id.clocation);

        // 启动时间更新
        mHandler.post(mTimeUpdater);

        // 启动数据库更新
        mHandler.post(mDatabaseUpdater);

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

    private void updateTimeTextView() {
        // 获取当前时间并格式化为字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        // 更新时间TextView
        ctimeTextView.setText("Time: " + timestamp);
    }

    @Override
    protected void onStart() {
        Log.e(TAG, "onStart");

        super.onStart();
        requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");

        insertLocation(latitude, longitude, "pause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.e(TAG, "onStop");

        insertLocation(latitude, longitude, "stop");
        mLocationManager.removeUpdates(myListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");

        insertLocation(latitude, longitude, "destroy");
        mLocationManager.removeUpdates(myListener);
        dbHelper.close();
        super.onDestroy();
    }

    private void requestLocationUpdates() {
        TencentLocationRequest request = TencentLocationRequest.create();
        request.setInterval(1000);
        request.setRequestLevel(TencentLocationRequest.REQUEST_LEVEL_ADMIN_AREA);
        request.setAllowGPS(true);
        request.setAllowDirection(true);
        request.setIndoorLocationMode(true);
        request.setLocMode(TencentLocationRequest.HIGH_ACCURACY_MODE);
        request.setGpsFirst(true);

        mLocationManager.requestLocationUpdates(request, myListener);
    }

    private void insertLocation(double latitude, double longitude, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LATITUDE, latitude);
        values.put(DatabaseHelper.COLUMN_LONGITUDE, longitude);
        values.put(DatabaseHelper.COLUMN_STATUS, status);

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

    private class InsertLocationTask extends AsyncTask<Object, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Object... params) {
            if (!isTableExist) {
                MysqlHelp.createUserTableIfNotExists();
                isTableExist = true;
            }

            double latitude = (double) params[0];
            double longitude = (double) params[1];
            String address = (String) params[2];

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String timestamp = sdf.format(new Date());

            // 生成UUID作为phone_id
            String phoneId = UUID.randomUUID().toString();

            if (latitude == 0. || longitude == 0. || address == null)
                return false;

            return MysqlHelp.insertUserInfo(
                    phoneId, // 使用UUID作为手机ID
                    timestamp,
                    latitude,
                    longitude,
                    address // 地址
            );
        }

        @Override
        protected void onPostExecute(Boolean isInserted) {
            if (isInserted) {
                Log.d(TAG, "Location inserted to MySQL successfully.");
            } else {
                Log.e(TAG, "Error inserting location to MySQL.");
            }
        }
    }

    class MyActivity implements TencentLocationListener {

        @Override
        public void onLocationChanged(TencentLocation location, int error, String reason) {
            if (error == TencentLocation.ERROR_OK) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                address = location.getAddress();

                runOnUiThread(() -> {
                    clocationTextView.setText("Location: " + latitude + ", " + longitude + "\n" + address);
                });

                if (isFirstLocation) {
                    insertLocation(latitude, longitude, "start");
                    Log.e(TAG, "record isFirstLocation");
                    isFirstLocation = false;
                }
            } else {
                Log.e(TAG, "Location error: " + reason);
            }
        }

        @Override
        public void onStatusUpdate(String name, int status, String desc) {
            Log.d(TAG, "Status update: " + name + ", " + status + ", " + desc);
        }
    }
}