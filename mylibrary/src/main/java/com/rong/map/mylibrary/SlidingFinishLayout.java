package com.rong.map.mylibrary;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.RelativeLayout;
import android.widget.Scroller;

/**
 * 滑动关闭的布局
 * Created by map on 2016/6/22.
 */
public class SlidingFinishLayout extends PercentRelativeLayout implements OnTouchListener {

    private static final String TAG = SlidingFinishLayout.class.getName();
    /**
     * SildingFinishLayout布局的父布局
     */
    private ViewGroup mParentView;
    /**
     * 处理滑动逻辑的View
     */
    private View touchView;
    /**
     * 滑动的最小距离
     */
    private int mTouchSlop;
    /**
     * 按下点的X坐标
     */
    private int downX;
    /**
     * 按下点的Y坐标
     */
    private int downY;
    /**
     * 临时存储X坐标
     */
    private int tempY;
    /**
     * 滑动类
     */
    private Scroller mScroller;
    /**
     * SildingFinishLayout的宽度
     */
    private int viewHeight;
    /**
     * 记录是否正在滑动
     */
    private boolean isSliding;

    private OnSlidingFinishListener onSlidingFinishListener;
    private boolean isFinish;

    /**
     * 改变背景阴影需要的数据
     *
     * @param context
     * @param attrs
     */
    private float screenHeight;

    public SlidingFinishLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingFinishLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mScroller = new Scroller(context);
        setOnTouchListener(this);
        setMotionEventSplittingEnabled(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mParentView = (ViewGroup) this.getParent();
        DisplayMetrics dm = ScreenUtils.getDeviceWidthHeight(getContext());
        screenHeight = (float) dm.heightPixels;
        viewHeight = h;
    }

    /**
     * onSlidingFinishListener, 在onSlidingFinish()方法中finish Activity
     *
     * @param onSlidingFinishListener
     */
    public void setOnSlidingFinishListener(
            OnSlidingFinishListener onSlidingFinishListener) {
        this.onSlidingFinishListener = onSlidingFinishListener;
    }

    /**
     * 设置Touch的View
     *
     * @param touchView
     */
    public void setTouchView(View touchView) {
        this.touchView = touchView;
        this.touchView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return SlidingFinishLayout.this.onTouch(v, event);
            }
        });
    }

    /**
     * 滚动出界面
     */
    private void scrollDown() {
        final int delta = (viewHeight + mParentView.getScrollY());
        // 调用startScroll方法来设置一些滚动的参数，我们在computeScroll()方法中调用scrollTo来滚动item
        mScroller.startScroll(0, mParentView.getScrollY(), 0, -delta + 1,
                Math.abs(delta) / 2);
        postInvalidate();
    }

    /**
     * 滚动到起始位置
     */
    private void scrollOrigin() {
        int delta = mParentView.getScrollY();
        mScroller.startScroll(0, mParentView.getScrollY(), 0, -delta,
                Math.abs(delta));
        postInvalidate();
    }

    /**
     * touch的View是否是AbsListView， 例如ListView, GridView等其子类
     *
     * @return
     */
    private boolean isTouchOnAbsListView(View v) {
        return v instanceof AbsListView ? true : false;
    }

    /**
     * touch的view是否是ScrollView或者其子类
     *
     * @return
     */
    private boolean isTouchOnLockableScrollView(View v) {
        return v instanceof NestedScrollView ? true : false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bgShadow) {//背景阴影渐变
            changeAlpha();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY = tempY = (int) event.getRawY();
                downX = (int) event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveY = (int) event.getRawY();

                if (moveY < 0) {
                    scrollOrigin();
                    break;
                }
                int deltaY = tempY - moveY;
                tempY = moveY;
                if (Math.abs(moveY - downY) > mTouchSlop
                        && Math.abs((int) event.getRawX() - downX) < mTouchSlop) {
                    isSliding = true;

                    // 若touchView是AbsListView，
                    // 则当手指滑动，取消item的点击事件，不然我们滑动也伴随着item点击事件的发生
                    if (isTouchOnAbsListView(v)) {
                        MotionEvent cancelEvent = MotionEvent.obtain(event);
                        cancelEvent
                                .setAction(MotionEvent.ACTION_CANCEL
                                        | (event.getActionIndex() << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        v.onTouchEvent(cancelEvent);
                    }
                }

                if (moveY - downY >= 0 && isSliding) {
                    /**
                     * 如果view是本身可滚动的view的话，必须在滚动到顶部后才能执行以下下拉滚动代码
                     * 如果view本身不可滚动的话，可以执行以下代码
                     */
                    if ((isTouchViewScrollTop() && (isTouchOnAbsListView(v) || isTouchOnLockableScrollView(v)))
                            || !((isTouchOnAbsListView(v) || isTouchOnLockableScrollView(v)))) {
                        if (deltaY + mParentView.getScrollY() > 0) {
                            scrollOrigin();
                        } else {
                            mParentView.scrollBy(0, deltaY);
                            if (bgShadow) {//背景阴影渐变
                                changeAlpha();
                            }
                        }
                        // 屏蔽在滑动过程中ListView ScrollView等自己的滑动事件
                        if (isTouchOnLockableScrollView(v) || isTouchOnAbsListView(v)) {
                            return true;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_POINTER_UP:
                isSliding = false;
                if (mParentView.getScrollY() <= -viewHeight / 3) {
                    isFinish = true;
                    scrollDown();
                } else {
                    scrollOrigin();
                    isFinish = false;
                }
                break;
        }

        // 假如touch的view是AbsListView或者ScrollView 我们处理完上面自己的逻辑之后
        // 再交给AbsListView, ScrollView自己处理其自己的逻辑
        if (isTouchOnLockableScrollView(v) || isTouchOnAbsListView(v)) {
            return touchView.onTouchEvent(event);
        }

        // 其他的情况直接返回true
        return true;
    }

    /**
     * 向下滑动的时候是否可以滚动
     *
     * @return
     */
    public boolean isTouchViewScrollTop() {
        if (null == touchView) return true;
        return touchView.getScrollY() == 0;
    }

    @Override
    public void computeScroll() {
        // 调用startScroll的时候scroller.computeScrollOffset()返回true，
        if (mScroller.computeScrollOffset()) {
            mParentView.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();

            if (mScroller.isFinished()) {

                if (onSlidingFinishListener != null && isFinish) {
                    onSlidingFinishListener.onSlidingFinish();
                }
            }
        }
    }


    /**
     * 背景阴影渐变
     */
    private static final float START_ALPHA = 0.8f;
    private static final int DEFAULT_SHADOW_COLOR = Color.BLACK;
    private View shadowView;
    //是否有背景阴影渐变功能
    private boolean bgShadow = false;
    //透明度的边界值
    private float mAlpha = START_ALPHA;
    //此view相对于屏幕的纵坐标
    private float screenY;
    //背景颜色
    private int shadowColor = DEFAULT_SHADOW_COLOR;

    public void setmAlpha(float mAlpha) {
        this.mAlpha = mAlpha;
    }

    public void setShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
    }

    public void setBgShadow(boolean bgShadow) {
        this.bgShadow = bgShadow;
        int[] location = new int[2];
        getLocationOnScreen(location);
        screenY = (float) location[1];
        if (bgShadow && getContext() instanceof Activity) {
            ViewGroup decorView = (ViewGroup) ((Activity) getContext()).getWindow().getDecorView();
            shadowView = new View(getContext());
            shadowView.setBackgroundColor(shadowColor);
            shadowView.setAlpha(START_ALPHA);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT
                    , RelativeLayout.LayoutParams.MATCH_PARENT);
            shadowView.setLayoutParams(params);
            decorView.addView(shadowView, 0);
        }
    }

    /**
     * 改变透明度
     */
    private void changeAlpha() {
        if (shadowView != null) {
            int[] location = new int[2];
            getLocationOnScreen(location);
            int screenY = location[1];
            //透明是从0-0.8
            if (0 != (screenHeight - this.screenY)) {
                float alpha = mAlpha - (screenY - this.screenY)
                        / (screenHeight - this.screenY) * mAlpha;
                shadowView.setAlpha(alpha);
            }
        }
    }

    public interface OnSlidingFinishListener {
        void onSlidingFinish();
    }

}
