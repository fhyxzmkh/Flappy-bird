package org.flappybird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import java.io.ObjectStreamClass;
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

    int obstacleInterval; // 障碍物上下间距

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
        bp_bird1 = getRatioBitmap(bp_bird1, 2f, 2f);
        bp_bird2 = getRatioBitmap(bp_bird2, 2f, 2f);

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

        obstacleInterval = bird.getH() * 4;

        new Thread(this).start(); // 开启绘制线程
    }

    private Body[] createGrounds() {
        Body body1 = new Body(bp_land, 0, ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        Body body2 = new Body(bp_land, body1.getX() + body1.getW(), ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        Body body3 = new Body(bp_land, body2.getX() + body2.getW(), ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        return new Body[] {body1, body2, body3};
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

        birdMove(); // 小鸟飞翔
        birdDown(); // 小鸟掉落

        groundsMove();

        obstacleMove();

        createObstacle();


        drawBackground(); // 绘制背景
        drawBird(); // 绘制鸟
        drawGrounds(); // 绘制地板
        drawObstacle();

        surfaceHolder.unlockCanvasAndPost(mCanvas);
    }

    private void obstacleMove() {
        for (Body body : obstacles) {
            body.setX(body.getX() - moveSpeed);
        }
    }

    int CreateObstacleTime = 0;
    private void createObstacle() {
        CreateObstacleTime ++;
        if (CreateObstacleTime >= 100) {
            CreateObstacleTime = 0;
            int ranH = 240 + (int) (Math.random() * (grounds[0].getY() - obstacleInterval - 240)); // 管道最小高度

            Body body = new Body(
                    getObstacleBitmap(ranH, true),
                    ViewWidth + 50,
                    0,
                    bird.getW() * 2,
                    ranH
            );

            Bitmap bitmap = getObstacleBitmap(
                    grounds[0].getY() - obstacleInterval - body.getH(),
                    false
            );

            Body body1 = new Body(
                    bitmap,
                    ViewWidth + 50,
                    body.getH() + obstacleInterval,
                    bird.getW() * 2,
                    bitmap.getHeight()
            );

            obstacles.add(body);
            obstacles.add(body1);
        }
    }

    private Bitmap getObstacleBitmap(int h, boolean isRot) {
        Bitmap obstacle;
        if (isRot) {
            // 使用向下的管道图片
            obstacle = getRatioBitmap(bp_pipe_down,
                    (float) (bird.getW() * 2) / bp_pipe_down.getWidth(),
                    (float) h / bp_pipe_down.getHeight());
        } else {
            // 使用向上的管道图片
            obstacle = getRatioBitmap(bp_pipe_up,
                    (float) (bird.getW() * 2) / bp_pipe_up.getWidth(),
                    (float) h / bp_pipe_up.getHeight());
        }

        // 创建一个新的位图，并将缩放后的管道图片绘制到这个位图上
        Bitmap bitmap = Bitmap.createBitmap(obstacle.getWidth(), obstacle.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(obstacle, 0, 0, null);

        return bitmap;
    }

    private void drawObstacle() {
        for (Body body : obstacles) {
            mCanvas.drawBitmap(body.getBitmap(), body.getX(), body.getY(), null);
        }
    }

    int moveSpeed = 10;
    private void groundsMove() {
        // 移动第一个地板
        grounds[0].setX(grounds[0].getX() - moveSpeed);
        // 如果第一个地板的右边缘移出屏幕，将其移动到第三个地板的右侧
        if (grounds[0].getX() + grounds[0].getW() <= 0) {
            grounds[0].setX(grounds[2].getX() + grounds[2].getW() - 20);
        }

        // 移动第二个地板
        grounds[1].setX(grounds[1].getX() - moveSpeed);
        // 如果第二个地板的右边缘移出屏幕，将其移动到第一个地板的右侧
        if (grounds[1].getX() + grounds[1].getW() <= 0) {
            grounds[1].setX(grounds[0].getX() + grounds[0].getW() - 20);
        }

        // 移动第三个地板
        grounds[2].setX(grounds[2].getX() - moveSpeed);
        // 如果第三个地板的右边缘移出屏幕，将其移动到第二个地板的右侧
        if (grounds[2].getX() + grounds[2].getW() <= 0) {
            grounds[2].setX(grounds[1].getX() + grounds[1].getW() - 20);
        }
    }

    private void drawGrounds() {
        mCanvas.drawBitmap(grounds[0].getBitmap(), grounds[0].getX(), grounds[0].getY(), null);
        mCanvas.drawBitmap(grounds[1].getBitmap(), grounds[1].getX(), grounds[1].getY(), null);
        mCanvas.drawBitmap(grounds[2].getBitmap(), grounds[2].getX(), grounds[2].getY(), null);

    }

    boolean isDown = true;
    int downSpeed = 0; // 下落速度 每一帧下落的坐标
    int upStartTime = 0; // 已经跳跃时间
    int upTime = 20; // 需要跳跃时间
    int upSpeed = 12; // 每一帧跳跃距离

    private void birdDown() {
        if (isDown) { // 下落
            BirdSpinData = 45;
            bird.setY(bird.getY() + (downSpeed ++)); // 下落速度递增
        }
        else { // 跳跃
            if (upTime - upStartTime >= 0) {
                BirdSpinData = -45;
                bird.setY(bird.getY() - upSpeed);
                upStartTime ++;
            }
            else { // 跳跃结束
                isDown = true;
                upStartTime = 0;
                downSpeed = 0;
            }
        }
    }

    int birdBitmapData = 1;
    int changeBitmapTime = 0;
    private void birdMove() {
        changeBitmapTime ++;
        if (changeBitmapTime >= 10) {
            switch (birdBitmapData) {
                case 1:
                    bird.setBitmap(bp_bird0);
                    break;
                case 2:
                    bird.setBitmap(bp_bird1);
                    break;
                case 3:
                    bird.setBitmap(bp_bird2);
                    break;
            }
            changeBitmapTime = 0;
            birdBitmapData = birdBitmapData == 3 ? 1 : birdBitmapData + 1;
        }
    }

    private void drawBackground() {
        mCanvas.drawBitmap(background.getBitmap(), background.getX(), background.getY(), null);
    }

    int BirdSpinData = 0; // 小鸟旋转角度
    private void drawBird() {
        Matrix matrix = new Matrix();
        matrix.setTranslate(bird.getX(), bird.getY());
        matrix.postRotate(BirdSpinData, bird.getX() + (float) bird.getW() / 2, bird.getY() + (float) bird.getH() / 2);
        mCanvas.drawBitmap(bird.getBitmap(), matrix, null);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: // 手指按下
                isDown = false;

                break;
        }

        return super.onTouchEvent(event);
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

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setW(int w) {
        this.w = w;
    }

    public void setH(int h) {
        this.h = h;
    }
}
