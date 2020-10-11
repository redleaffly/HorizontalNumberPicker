package com.xiaoyifei.myutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Scroller;

public class HorizontalNumberPicker extends View {

    private int mCenterColumn;
    private int mValueFrom;
    private int mValueTo;
    private int mDefaultValue;
    private int mColumnCount;

    private float mLastX;
    private float mDownX;
    private float mStartOffsetX = 0.0f;
    private int mItemWidth;

    private int mSelectedNumber;
    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mVelocity;
    private int mCenterItemLeftOffsetX;
    private int mCenterItemRightOffsetX;
    private int mLeftOffsetX;
    private int mRightOffsetX;
    private int mHeight;
    private boolean isFling = false;
    private boolean isClick = false;
    private Paint mPaint;
    private int mCount;
    private OnValueChangeListener mOnValueChangeListener;

    public interface OnValueChangeListener {
        void onValueChange(HorizontalNumberPicker picker, int oldVal, int newVal);
    }
    public void setOnValueChangedListener(OnValueChangeListener onValueChangedListener) {
        mOnValueChangeListener = onValueChangedListener;
    }


    public HorizontalNumberPicker(Context context) {
        super(context, null);
    }


    public HorizontalNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        mScroller = new Scroller(context);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.HorizontalNumberPicker);
        float mTextSize = array.getDimension(R.styleable.HorizontalNumberPicker_android_textSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 32, getResources().getDisplayMetrics()));
        mValueFrom = array.getInt(R.styleable.HorizontalNumberPicker_android_valueFrom, 1);
        mValueTo = array.getInt(R.styleable.HorizontalNumberPicker_android_valueTo, 31);
        mDefaultValue = array.getInt(R.styleable.HorizontalNumberPicker_android_defaultValue, 8);
        mColumnCount = array.getInt(R.styleable.HorizontalNumberPicker_android_columnCount, 3);
        int mTextColor = array.getColor(R.styleable.HorizontalNumberPicker_android_textColor, Color.BLACK);
        mColumnCount = mColumnCount % 2 == 0 ? mColumnCount + 1 : mColumnCount;
        array.recycle();
        mSelectedNumber = mDefaultValue;
        mCenterColumn = (int) Math.ceil(mColumnCount / 2f);
        mPaint = new Paint();
        mPaint.setColor(mTextColor);
        mPaint.setTextSize(mTextSize);
        mCount = Math.abs(mValueTo - mValueFrom) + 1;

    }

    private void smoothScroll(int offset) {
        mScroller.startScroll(getScrollX(), 0, offset, 0, 300);
        invalidate();
    }

    private void correctOffset() {
        int scrollX = getScrollX();
        if (scrollX < mLeftOffsetX) { //mStartOffsetX - mCenterItemLeftOffsetX
            smoothScroll((int) (mLeftOffsetX - scrollX));
        } else if (scrollX > mRightOffsetX) {//mEndOffsetX - mCenterItemLeftOffsetX
            smoothScroll((int) (mRightOffsetX - scrollX));
        } else {
            float diffOffset = scrollX % mItemWidth;
            if (Math.abs(diffOffset) < mItemWidth / 2f) {
                smoothScroll((int) -diffOffset);
            } else {
                if (diffOffset > 0) {
                    smoothScroll((int) (mItemWidth - diffOffset));
                } else {
                    smoothScroll((int) (-mItemWidth - diffOffset));
                }
            }
        }

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float mCurrentX = event.getX();

        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mDownX = mCurrentX;
                mVelocityTracker = VelocityTracker.obtain();
                if (isFling) {//点击停止滚动
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                mVelocityTracker.addMovement(event);
                mVelocityTracker.computeCurrentVelocity(1000);
                mVelocity = (int) mVelocityTracker.getXVelocity();//测速

                float mOffsetX = mCurrentX - mLastX;
                scrollBy(-Math.round(mOffsetX), 0);//移动
                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(mCurrentX - mDownX) < ViewConfiguration.get(getContext()).getScaledTouchSlop()) {//点击
                    isClick = true;
                    int offsetNumber = (int) Math.ceil(event.getX() / mItemWidth);

                    if (offsetNumber > mCenterColumn) {
                        smoothScroll((int) ((offsetNumber - mCenterColumn) * mItemWidth));
                    }
                    if (offsetNumber < mCenterColumn) {
                        smoothScroll((int) (-(mCenterColumn - offsetNumber) * mItemWidth));
                    }
                }

                if (Math.abs(mVelocity) > ViewConfiguration.get(getContext()).getScaledMinimumFlingVelocity()) {//滚动
                    isFling = true;
                    mScroller.fling(getScrollX(), 0, -mVelocity, 0, mLeftOffsetX, mRightOffsetX, 0, 0);
                    invalidate();
                }

                if (!isFling && !isClick) {
                    correctOffset();
                }
                mVelocityTracker.recycle();
                mVelocity = 0;
                break;
        }
        mLastX = mCurrentX;
        return true;
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            invalidate();
        } else {
            if (isFling || isClick) { //如果是滚动或点击，结束后自动修正
                correctOffset();
                isFling = false;
                isClick = false;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int mWidth=MeasureSpec.getSize(widthMeasureSpec);
        mHeight=MeasureSpec.getSize(heightMeasureSpec);
        int heightSpecMode=MeasureSpec.getMode(heightMeasureSpec);
        if(heightSpecMode==MeasureSpec.AT_MOST){
            mHeight= (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,50,getResources().getDisplayMetrics());
            setMeasuredDimension(mWidth,mHeight);
        }
        mItemWidth = mWidth / mColumnCount;
        mLeftOffsetX = (int) (mItemWidth * (mValueFrom - mDefaultValue));
        mRightOffsetX = (int) (mItemWidth * (mValueTo - mDefaultValue));
        mCenterItemLeftOffsetX = (int) (mItemWidth * (Math.floor(mColumnCount / 2f)));
        mCenterItemRightOffsetX = (int) mItemWidth + mCenterItemLeftOffsetX;
        mStartOffsetX = mLeftOffsetX + mCenterItemLeftOffsetX;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int scrollX = getScrollX();
        int oldValue=mSelectedNumber;
        mSelectedNumber = mDefaultValue + Math.round(scrollX / (float) mItemWidth);
        if (mOnValueChangeListener != null) {
            mOnValueChangeListener.onValueChange(this, oldValue, mSelectedNumber);
        }

        for (int i = 0; i < mCount; i++) {
            @SuppressLint("DrawAllocation") Rect bounds = new Rect();
            String str = String.valueOf(mValueFrom + i);
            int number = mValueFrom + i;
            if (number == mSelectedNumber) {
                mPaint.setAlpha(255);
            } else {
                mPaint.setAlpha(100);
            }
            mPaint.getTextBounds(str, 0, str.length(), bounds);
            canvas.drawText(String.valueOf(mValueFrom + i), (mItemWidth * i) + mItemWidth / 2f - bounds.width() / 2f + mStartOffsetX, mHeight / 2f + bounds.height() / 2f, mPaint);
        }
        canvas.drawLine(mCenterItemLeftOffsetX + scrollX, 0, mCenterItemLeftOffsetX + scrollX, mHeight, mPaint);
        canvas.drawLine(mCenterItemRightOffsetX + scrollX, 0, mCenterItemRightOffsetX + scrollX, mHeight, mPaint);
    }

    public int getNumber() {
        return mSelectedNumber;
    }

}
