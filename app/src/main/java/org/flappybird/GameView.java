package org.flappybird;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    public static final int GAME_START = 200; // 游戏开始
    public static final int GAME_STOP = 400; // 游戏停止
    public static final int GAME_OVER = 500; // 游戏结束
    public static final int GAME_READY = 100; // 游戏准备
    int obstacleInterval; // 障碍物上下间距
    int score = 0;
    Body collisionBody;
    int CreateObstacleTime = 0;
    int moveSpeed = 10;
    boolean isDown = true;
    int downSpeed = 0; // 下落速度 每一帧下落的坐标
    int upStartTime = 0; // 已经跳跃时间
    int upTime = 10; // 需要跳跃时间
    int upSpeed = 16; // 每一帧跳跃距离
    int birdBitmapData = 1;
    int changeBitmapTime = 0;
    int BirdSpinData = 0; // 小鸟旋转角度
    private int gameStatus; // 游戏状态
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
    private GameListener gameListener;

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

    public GameListener getGameListener() {
        return gameListener;
    }

    public void setGameListener(GameListener gameListener) {
        this.gameListener = gameListener;
    }

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
        gameStatus = GAME_READY;

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

//    public void reSet() {
//        BirdSpinData = 0;
//        bird.setX(ViewWidth / 2 - bp_bird0.getWidth() / 2);
//        bird.setY(ViewHeight / 2);
//        isDown = false;
//        CreateObstacleTime = 0;
//        obstacles = new ArrayList<>();
//        changeBitmapTime = 0;
//        gameStatus = GAME_READY;
//        score = 0;
//
//        if (gameListener != null) {
//            gameListener.gameReady();
//        }
//    }

    private Body[] createGrounds() {
        Body body1 = new Body(bp_land, 0, ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        Body body2 = new Body(bp_land, body1.getX() + body1.getW(), ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        Body body3 = new Body(bp_land, body2.getX() + body2.getW(), ViewHeight - bp_land.getHeight(), bp_land.getWidth(), bp_land.getHeight());
        return new Body[]{body1, body2, body3};
    }

    // 获取dx dy缩放比例后的bitmap
    private Bitmap getRatioBitmap(Bitmap bitmap, float dx, float dy) {
        return Bitmap.createScaledBitmap(
                bitmap,
                (int) (bitmap.getWidth() * dx),
                (int) (bitmap.getHeight() * dy),
                true
        );
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

        if (gameStatus == GAME_START) {
            birdMove(); // 小鸟飞翔
            birdDown(); // 小鸟掉落
            groundsMove();
            createObstacle();
            obstacleMove();
        }

        if (gameStatus == GAME_STOP) {
            overAnim();
        }

        drawBackground(); // 绘制背景
        drawBird(); // 绘制鸟
        drawGrounds(); // 绘制地板
        drawObstacle(); // 绘制障碍物

        checkGameOver();

        surfaceHolder.unlockCanvasAndPost(mCanvas);
    }

    private void overAnim() {
        BirdSpinData += 30;

        if (bird.getX() + bird.getW() / 2 > collisionBody.getX() + collisionBody.getW() / 2) { // 右侧碰撞
            if (bird.getX() < collisionBody.getX() + collisionBody.getW()) {
                bird.setX(bird.getX() + 5);
                bird.setY(bird.getY() - 20);
                return;
            }
            bird.setY(bird.getY() + 20);
            bird.setX(bird.getX() + 5);
        } else { // 左侧碰撞
            if (bird.getX() + bird.getW() > collisionBody.getX()) {
                bird.setX(bird.getX() - 5);
                bird.setY(bird.getY() - 20);
                return;
            }
            bird.setY(bird.getY() + 20);
            bird.setX(bird.getX() - 5);
        }
    }

    private void checkGameOver() {
        if (bird.getY() + bird.getH() >= grounds[0].getY()) {
            gameStatus = GAME_OVER;
            if (gameListener != null) {
                gameListener.gameOver();
            }
        } else {
            for (Body body : obstacles) {
                if (CollisionRect(bird.getX(), bird.getY(), bird.getW(), bird.getH(),
                        body.getX(), body.getY(), body.getW(), body.getH())) {
                    gameStatus = GAME_STOP;
                    collisionBody = body;
                }
            }
        }
    }

    public boolean CollisionRect(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        // 缩小管道的宽度和高度
        int collisionWidth = w2 - 50;
        int collisionHeight = h2 - 50;

        // 调整管道的左上角坐标
        int adjustedX2 = x2 + 25; // 向右移动25像素
        int adjustedY2 = y2 + 25; // 向下移动25像素

        if (x1 >= adjustedX2 && x1 >= adjustedX2 + collisionWidth) {
            return false;
        } else if (x1 <= adjustedX2 && x1 + w1 <= adjustedX2) {
            return false;
        } else if (y1 >= adjustedY2 && y1 >= adjustedY2 + collisionHeight) {
            return false;
        } else if (y1 <= adjustedY2 && y1 + h1 <= adjustedY2) {
            return false;
        }

        return true;
    }

    private void obstacleMove() {
        List<Body> bodies = new ArrayList<>();
        for (Body body : obstacles) {
            body.setX(body.getX() - moveSpeed);
            if (body.getX() + body.getW() + 10 < 0) {
                bodies.add(body);
            }

            if (!body.isScore && body.getX() + body.getW() / 2 < bird.getX()) {
                score += 1;

                if (gameListener != null) {
                    gameListener.addScore(score);
                    body.setScore(true);
                }
            }
        }
        obstacles.removeAll(bodies);
    }

    private void createObstacle() {
        CreateObstacleTime++;
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

    private void birdDown() {
        if (isDown) { // 下落
            BirdSpinData = 45;
            bird.setY(bird.getY() + (downSpeed++)); // 下落速度递增
        } else { // 跳跃
            if (upTime - upStartTime >= 0) {
                BirdSpinData = -45;
                bird.setY(bird.getY() - upSpeed);
                upStartTime++;
            } else { // 跳跃结束
                isDown = true;
                upStartTime = 0;
                downSpeed = 0;
            }
        }
    }

    private void birdMove() {
        changeBitmapTime++;
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
                if (gameStatus == GAME_READY) {
                    gameStatus = GAME_START;
                } else if (gameStatus == GAME_START) {
                    isDown = false;
                }

                break;
        }

        return super.onTouchEvent(event);
    }

    interface GameListener {
        void addScore(int score);

        void gameOver();

        void gameReady();
    }

    public void reSet() {
        BirdSpinData = 0;
        bird.setX(ViewWidth / 2 - bp_bird0.getWidth() / 2);
        bird.setY(ViewHeight / 2);
        isDown = false;
        CreateObstacleTime = 0;
        obstacles = new ArrayList<>();
        changeBitmapTime = 0;
        gameStatus = GAME_READY;
        score = 0;

        new Handler().postDelayed(() -> {
            if (gameListener != null) {
                gameListener.gameReady();
            }
        }, 50);

    }
}

class Body {
    Bitmap bitmap;
    int x, y, w, h;

    boolean isScore = false;

    public Body(Bitmap bitmap, int x, int y, int w, int h) {
        this.bitmap = bitmap;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean isScore() {
        return isScore;
    }

    public void setScore(boolean score) {
        isScore = score;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getW() {
        return w;
    }

    public void setW(int w) {
        this.w = w;
    }

    public int getH() {
        return h;
    }

    public void setH(int h) {
        this.h = h;
    }
}
