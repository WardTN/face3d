package com.wardtn.facemodel.compare;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.wardtn.facemodel.R;


/**
 * 对比SeekBar
 */
public class CompareSeekBar extends View {

    private Paint linePaint;//线的画笔
    private Paint verticalPaint;//竖直线


    Bitmap btnBitmap;

    int mBtnBitmapWith = 0;
    int mBtnBitmapHeight = 0;

    private float sumHeight;//总控件的高度
    private float sumWidth;//总空间的宽度



    float btnX1 = 0;

    Rect rectBtn1; //绘制滑轮的矩阵


    public CompareSeekBar(Context context) {
        this(context, null);
    }

    public CompareSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CompareSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        btnBitmap = BitmapFactory.decodeResource(getContext().getResources(), R.mipmap.icon_slide_button2);
        mBtnBitmapWith = btnBitmap.getWidth() + 30;
        mBtnBitmapHeight = btnBitmap.getHeight() + 30;

        //画线的画笔
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#17CAAA"));
        linePaint.setAntiAlias(true);
        linePaint.setTextSize(dp2px(getContext(), 12));
        linePaint.setStrokeWidth(dp2px(getContext(), 1));

        verticalPaint = new Paint();
        verticalPaint.setStyle(Paint.Style.STROKE);
        verticalPaint.setAntiAlias(true);
        verticalPaint.setStrokeWidth((float) 0.5);
        verticalPaint.setColor(Color.parseColor("#FFFFFF"));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        sumWidth = getMeasuredWidth();
        sumHeight = getMeasuredHeight();

        Log.e("CHEN","当前 sumWidth 为" + sumWidth);
//        setMeasuredDimension(sumWidth,sumHeight);

        btnX1 = sumWidth / 2 - mBtnBitmapWith / 2;

    }

    public int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private boolean isDownBtn1 = false;

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rectBtn1 != null && eventX >= rectBtn1.left && eventX <= rectBtn1.right && eventY >= rectBtn1.top && eventY <= rectBtn1.bottom) {
                    //触摸在按钮1
                    isDownBtn1 = true;
                    this.getParent().requestDisallowInterceptTouchEvent(true);//当该view获得点击事件，就请求父控件不拦截事件
                    return true;
                } else {
                    isDownBtn1 = false;
                    this.getParent().requestDisallowInterceptTouchEvent(false);//当该view获得点击事件，就请求父控件不拦截事件
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isDownBtn1) return false;
                if (eventX >= 0 && eventX <= (sumWidth - mBtnBitmapWith)) {
                    //触摸在按钮1
                    btnX1 = eventX;
                    if (listener != null) {
                        listener.seekChange(btnX1 + (float) (mBtnBitmapWith * 1.0 / 2));
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.getParent().requestDisallowInterceptTouchEvent(false);//当该view获得点击事件，就请求父控件不拦截事件
                break;
        }
        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        toDrawBitmap(canvas);
    }

    private void toDrawBitmap(Canvas canvas) {
        if (btnX1 >= 0) {
            rectBtn1 = new Rect((int) btnX1, (int) (sumHeight - mBtnBitmapHeight), (int) (btnX1 + mBtnBitmapWith), (int) sumHeight);
            canvas.drawBitmap(btnBitmap, null, rectBtn1, null);
            canvas.drawLine(btnX1 + (float) (mBtnBitmapWith * 1.0 / 2), 10, btnX1 + (float) (mBtnBitmapWith * 1.0 / 2), sumHeight - dp2px(getContext(),
                    20), verticalPaint);
        }
    }


    public ComPareSeekListener listener;

    public interface ComPareSeekListener {
        void seekChange(float leftDistance);
    }


    public void setListener(ComPareSeekListener listener) {
        this.listener = listener;
    }
}
