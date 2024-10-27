package org.flappybird;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MysqlHelp {
    private static final String TAG = "MysqlHelp";

    private static final String CLS = "com.mysql.jdbc.Driver";
    private static final String URL = "jdbc:mysql://address:port/DatabaseName?characterEncoding=UTF-8";
    private static final String USER = "Username";
    private static final String PWD = "Password";

    // 创建表 user（如果不存在）
    public static void createUserTableIfNotExists() {
        try {
            Class.forName(CLS);
            Connection conn = DriverManager.getConnection(URL, USER, PWD);
            String sql = "CREATE TABLE IF NOT EXISTS user (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "phone_id VARCHAR(255) NOT NULL," +
                    "time TIMESTAMP NOT NULL," +
                    "latitude DOUBLE NOT NULL," +
                    "longitude DOUBLE NOT NULL," +
                    "address VARCHAR(255) NOT NULL" +
                    ")";

            Statement st = conn.createStatement();
            st.execute(sql);
            Log.d(TAG, "Table 'user' created or already exists.");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception", e); // 输出异常信息到 LogCat
        }
    }

    // 测试数据库连接
    public static boolean testConnection() {
        try {
            Class.forName(CLS);
            Connection conn = DriverManager.getConnection(URL, USER, PWD);
            if (conn != null) {
                Log.d(TAG, "Database connection successful.");
                return true;
            } else {
                Log.e(TAG, "Database connection failed.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception", e); // 输出异常信息到 LogCat
            return false;
        }
    }

    // 写入一行信息
    public static boolean insertUserInfo(String phoneId, String time, double latitude, double longitude, String address) {
        try {
            Class.forName(CLS);
            Connection conn = DriverManager.getConnection(URL, USER, PWD);
            String sql = "INSERT INTO user (phone_id, time, latitude, longitude, address) VALUES (?, ?, ?, ?, ?)";

            java.sql.PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, phoneId);
            pst.setString(2, time);
            pst.setDouble(3, latitude);
            pst.setDouble(4, longitude);
            pst.setString(5, address);

            int rowsAffected = pst.executeUpdate();
            if (rowsAffected > 0) {
                Log.d(TAG, "Inserted user info successfully.");
                return true;
            } else {
                Log.e(TAG, "Failed to insert user info.");
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception", e); // 输出异常信息到 LogCat
            return false;
        }
    }

    // 获取用户数量
    public static int getUserSize() {
        int count = 0;

        try {
            Class.forName(CLS);
            Connection conn = DriverManager.getConnection(URL, USER, PWD);
            String sql = "SELECT COUNT(1) AS sl FROM user";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                count = rs.getInt("sl");
                Log.d(TAG, "查询到的用户数量为：" + count);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Exception", e); // 输出异常信息到 LogCat
        }

        return count;
    }
}