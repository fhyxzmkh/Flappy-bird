package org.flappybird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private Bitmap bp_bg_day,
            bp_bg_night,
            bp_bird0,
            bp_bird1,
            bp_bird2,
            bp_land,
            bp_pipe_up,
            bp_pipe_down,
            bp_panel;

    private Canvas mCanvas;

    private int ViewWidth, ViewHeight;
    private SurfaceHolder surfaceHolder;
    private boolean isDraw = true; // 绘制线程开关

    private Body background;
    private Body bird;
    private Body[] grounds;
    private List<Body> obstacles;

    private void initView(Context context) {
        bp_bg_day = BitmapFactory.decodeResource(context.getResources(), R.drawable.bg_day);
        bp_bg_night = BitmapFactory.decodeResource(context.getResources(), R.drawable.bg_night);
        bp_bird0 = BitmapFactory.decodeResource(context.getResources(), R.drawable.bird1_0);
        bp_bird1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.bird1_1);
        bp_bird2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.bird1_2);
        bp_land = BitmapFactory.decodeResource(context.getResources(), R.drawable.land);
        bp_pipe_up = BitmapFactory.decodeResource(context.getResources(), R.drawable.pipe2_up);
        bp_pipe_down = BitmapFactory.decodeResource(context.getResources(), R.drawable.pipe2_down);
        bp_panel = BitmapFactory.decodeResource(context.getResources(), R.drawable.score_panel);

        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
    }

    private void initData() {
        ViewWidth = getMeasuredWidth();
        ViewHeight = getMeasuredHeight();

        // 获取缩放后的背景图bitmap，缩放比例为屏幕宽高：原图片宽高（全屏）
        bp_bg_day = getRatioBitmap(
                bp_bg_day,
                (float) ViewWidth / bp_bg_day.getWidth(),
                (float) ViewHeight / bp_bg_day.getHeight()
        );

        bp_bird0 = getRatioBitmap(bp_bird0, 2f, 2f);
        bp_bird1 = getRatioBitmap(bp_bird0, 2f, 2f);
        bp_bird2 = getRatioBitmap(bp_bird0, 2f, 2f);

        background = new Body(bp_bg_day, 0, 0, bp_bg_day.getWidth(), bp_bg_day.getHeight());
        bird = new Body(
                bp_bird0,
                ViewWidth / 2 - bp_bird0.getWidth() / 2,
                ViewHeight / 2,
                bp_bird0.getWidth(),
                bp_bird0.getHeight()
        );
        grounds = createGrounds();
        obstacles = new ArrayList<>();

        new Thread(this).start(); // 开启绘制线程
    }

    private Body[] createGrounds() {
        Body body = new Body(bp_land, 0, ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        Body body1 = new Body(bp_land, 0, ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        return new Body[] {body, body1};
    }

    // 获取dx dy缩放比例后的bitmap
    private Bitmap getRatioBitmap(Bitmap bitmap, float dx, float dy) {
        return Bitmap.createScaledBitmap(
                bitmap,
                (int)(bitmap.getWidth() * dx),
                (int)(bitmap.getHeight() * dy),
                true
        );
    }




    public GameView(Context context) {
        super(context);
        initView(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public GameView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    //初始化绘图资源
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        initData();
    }

    //处理视图尺寸变化后的逻辑
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

    }

    //释放资源
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) { // 销毁时关闭线程
        isDraw = false;
    }

    @Override
    public void run() {
        while (isDraw) {
            drawMain();
        }
    }

    private void drawMain() { // 绘制整个游戏
        mCanvas = surfaceHolder.lockCanvas();
        drawBackground();
        drawBird();

        surfaceHolder.unlockCanvasAndPost(mCanvas);
    }

    private void drawBackground() {
        mCanvas.drawBitmap(background.getBitmap(), background.getX(), background.getY(), null);
    }

    int BirdSpinData; // 小鸟旋转角度
    private void drawBird() {
        Matrix matrix = new Matrix();
        matrix.setTranslate(bird.getX(), bird.getY());
        matrix.postRotate(BirdSpinData);
        mCanvas.drawBitmap(bird.getBitmap(), matrix, null);
    }

}

class Body {
    Bitmap bitmap;
    int x, y, w, h;

    public Body(Bitmap bitmap, int x, int y, int w, int h) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }
}
