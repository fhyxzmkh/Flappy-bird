package org.flappybird;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    GameView gameView;
    int Score = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);

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
                runOnUiThread(() -> ((RelativeLayout)findViewById(R.id.relative)).setVisibility(View.GONE));
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
}