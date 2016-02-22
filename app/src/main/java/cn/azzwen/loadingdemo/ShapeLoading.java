package cn.azzwen.loadingdemo;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.FloatEvaluator;
import android.animation.IntEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;


/**
 * Created by azzwen on 2015/12/4.
 */
public class ShapeLoading extends View {

    /* 动画持续时间 */
    private static final int DURATION = 800;
    /* 下落高度 */
    private static final float DISTANCE_FALL = 150f;
    /* 文字大小 */
    private static final int TEXT_SIZE = 16;
    /* 文字颜色 */
    private static int TEXT_COLOR = 0xFF666666;
    /* 文字与底部阴影的距离 */
    private static final int DISTANCE_TEXT_TO_SHADOW = 16;
    /* 加载提示文字 */
    private static final String LOADING_TEXT = "加载中...";

    /* 减速插值器 */
    private DecelerateInterpolator mDecelerateInterpolator = new DecelerateInterpolator();
    /* 加速插值器 */
    private AccelerateInterpolator mAccelerateInterpolator = new AccelerateInterpolator();
    /* 先加速再减速插值器 */
    private AccelerateDecelerateInterpolator mAccelerateDecelerateInterpolator = new AccelerateDecelerateInterpolator();
    private AnimatorSet mAnimatorSet;
    private ShapeHolder mShapeHolder;
    /* 跳跃图形变换矩阵 */
    private Matrix mShapeMatrix;
    /* 底部阴影图形 */
    private Bitmap mBottomShadow;
    /* 底部阴影图形变换矩阵 */
    private Matrix mShadowMatrix;
    private Paint mPaint;
    private Rect mTextRect;
    private final int mTextSize;
    /*文字与底部阴影的距离*/
    private final int mDistanceTextToShadow;

    public ShapeLoading(Context context) {
        this(context, null);
    }

    public ShapeLoading(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ShapeLoading(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTextSize = dpToPx(TEXT_SIZE);
        mDistanceTextToShadow = dpToPx(DISTANCE_TEXT_TO_SHADOW);
        init();
    }

    private void init() {
        mShapeMatrix = new Matrix();
        mShadowMatrix = new Matrix();
        mTextRect = new Rect();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(TEXT_COLOR);
        mPaint.setTextSize(mTextSize);
        //测量文字绘制所需空间
        mPaint.getTextBounds(LOADING_TEXT, 0, LOADING_TEXT.length(), mTextRect);
        initShapeHolder();
        startAnimator();
    }

    private void initShapeHolder() {
        mBottomShadow = BitmapFactory.decodeResource(getResources(), R.drawable.loading_bottom);
        Bitmap[] bitmaps = new Bitmap[4];
        bitmaps[3] = bitmaps[0] = BitmapFactory.decodeResource(getResources(), R.drawable.loading_yuan);
        bitmaps[1] = BitmapFactory.decodeResource(getResources(), R.drawable.loading_fangxing);
        bitmaps[2] = BitmapFactory.decodeResource(getResources(), R.drawable.loading_sanjiao);
        mShapeHolder = new ShapeHolder(bitmaps);
    }

    public void startAnimator() {
        if (mAnimatorSet == null) {
            mAnimatorSet = new AnimatorSet();
            mAnimatorSet.playSequentially(initCircleDownAnimator(), initRectangleAnimator(), initTriangleAnimator(), initCircleUpAnimator());
            //循环播放
            mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animation.start();
                }
            });
        }
        mAnimatorSet.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int tempMax = Math.max(mShapeHolder.getCurrentBitmap().getWidth(), mBottomShadow.getWidth());
        //所需宽度为，跳跃图形、底部阴影图形、文字宽度的最大值
        int widthSize = Math.max(tempMax, mTextRect.width()) + getPaddingLeft() + getPaddingRight();
        //所需高度为，跳跃图形、跳跃高度之和的2倍
        int heightSize = mShapeHolder.getCurrentBitmap().getHeight() * 2 + ((int) (DISTANCE_FALL * 2)) + getPaddingTop() + getPaddingBottom();

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        switch (widthMode) {
            case MeasureSpec.UNSPECIFIED:
                //不处理
                break;
            case MeasureSpec.EXACTLY:
                widthSize = MeasureSpec.getSize(widthMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                widthSize = Math.min(widthSize, MeasureSpec.getSize(widthMeasureSpec));
                break;
        }
        switch (heightMode) {
            case MeasureSpec.UNSPECIFIED:
                //不处理
                break;
            case MeasureSpec.EXACTLY:
                heightSize = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                widthSize = Math.min(widthSize, MeasureSpec.getSize(widthMeasureSpec));
                break;
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = mShapeHolder.getCurrentBitmap();
        float distance2Bottom = mShapeHolder.getDistance();
        float degrees = mShapeHolder.getDegree();
        //设置移动
        mShapeMatrix.setTranslate(getWidth() / 2 - bitmap.getWidth() / 2, getHeight() / 2 - distance2Bottom - bitmap.getHeight());
        //设置旋转
        mShapeMatrix.preRotate(degrees, bitmap.getWidth() / 2, bitmap.getHeight() / 2);
        canvas.drawBitmap(bitmap, mShapeMatrix, null);
        //绘制底部阴影
        mShadowMatrix.setTranslate(getWidth() / 2 - mBottomShadow.getWidth() / 2, getHeight() / 2);
        //以图形跳跃高度控制底部阴影缩放
        mShadowMatrix.postScale(distance2Bottom / DISTANCE_FALL, 1, getWidth() / 2, getHeight() / 2);
        canvas.drawBitmap(mBottomShadow, mShadowMatrix, null);
        //绘制文字
        canvas.drawText(LOADING_TEXT, getWidth() / 2 - mTextRect.width() / 2, getHeight() / 2 + mBottomShadow.getHeight() + mTextRect.height() + mDistanceTextToShadow, mPaint);
    }

    /**
     * 圆形下落动画
     *
     * @return
     */
    private ObjectAnimator initCircleDownAnimator() {
        PropertyValuesHolder translateHolder = PropertyValuesHolder.ofObject("distance", new FloatEvaluator(), DISTANCE_FALL, 0);
        //设置当前图形为圆形
        PropertyValuesHolder changeShapeHolder = PropertyValuesHolder.ofObject("step", new IntEvaluator(), 0, 0);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(mShapeHolder, translateHolder, changeShapeHolder);
        animator.setDuration(DURATION / 2);
        animator.addUpdateListener(mUpdateViewListener);
        animator.setInterpolator(mAccelerateInterpolator);
        return animator;
    }

    /**
     * 矩形动画
     *
     * @return
     */
    private ObjectAnimator initRectangleAnimator() {
        PropertyValuesHolder translateHolder = PropertyValuesHolder.ofObject("distance", new FloatEvaluator(), 0, DISTANCE_FALL, 0);
        PropertyValuesHolder rotateHolder = PropertyValuesHolder.ofObject("degree", new FloatEvaluator(), 0, 180, 180);
        //设置当前图形为矩形,结束时为三角形
        PropertyValuesHolder changeShapeHolder = PropertyValuesHolder.ofObject("step", new IntEvaluator(), 1, 2);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(mShapeHolder, translateHolder, rotateHolder, changeShapeHolder);
        animator.setDuration(DURATION);
        animator.addUpdateListener(mUpdateViewListener);
        animator.setInterpolator(mAccelerateDecelerateInterpolator);
        return animator;
    }

    /**
     * 三角形动画
     *
     * @return
     */
    private ObjectAnimator initTriangleAnimator() {
        PropertyValuesHolder holder = PropertyValuesHolder.ofObject("distance", new FloatEvaluator(), 0, DISTANCE_FALL, 0);
        PropertyValuesHolder rotateHolder = PropertyValuesHolder.ofObject("degree", new FloatEvaluator(), 0, -120, -120);
        //矩形动画结束时图形已切换为三角形
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(mShapeHolder, holder, rotateHolder);
        animator.setDuration(DURATION);
        animator.addUpdateListener(mUpdateViewListener);
        animator.setInterpolator(mAccelerateDecelerateInterpolator);
        return animator;
    }

    /**
     * 圆形上抛动画
     *
     * @return
     */
    private ObjectAnimator initCircleUpAnimator() {
        PropertyValuesHolder translateHolder = PropertyValuesHolder.ofObject("distance", new FloatEvaluator(), 0, DISTANCE_FALL);
        //设置当前图形为圆形。
        PropertyValuesHolder changeShapeHolder = PropertyValuesHolder.ofObject("step", new IntEvaluator(), 0, 0);
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(mShapeHolder, translateHolder, changeShapeHolder);
        animator.setDuration(DURATION / 2);
        animator.addUpdateListener(mUpdateViewListener);
        animator.setInterpolator(mDecelerateInterpolator);
        return animator;
    }

    /**
     * 重新绘制监听器
     */
    private ValueAnimator.AnimatorUpdateListener mUpdateViewListener = new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            invalidate();
        }
    };

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    /**
     * 先加速再减速插值器
     * http://stackoverflow.com/questions/27269845/create-a-decelerateaccelerateinterpolator
     */
    private class AccelerateDecelerateInterpolator implements TimeInterpolator {
        @Override
        public float getInterpolation(float x) {
            float result;
            if (x < 0.5) {
                result = (float) (1.0f - Math.pow((1.0f - 2 * x), 2)) / 2;
            } else {
                result = (float) Math.pow((x - 0.5) * 2, 2) / 2 + 0.5f;
            }
            return result;
        }
    }

    private class ShapeHolder {
        private Bitmap[] mBitmaps;
        /* 控制 Bitmap 切换 */
        private int step = 0;
        /* Bitmap 旋转的角度*/
        private float degree;
        /* Bitmap 下落的高度，即与当前View纵向中心线的距离*/
        private float distance = DISTANCE_FALL;

        public ShapeHolder(Bitmap... bitmaps) {
            mBitmaps = bitmaps;
        }

        public Bitmap getCurrentBitmap() {
            return mBitmaps[step];
        }

        public float getDegree() {
            return degree;
        }

        public float getDistance() {
            return distance;
        }

        public void setStep(int step) {
            this.step = step;
        }

        public void setDegree(float degree) {
            this.degree = degree;
        }

        public void setDistance(float distance) {
            this.distance = distance;
        }
    }
}
