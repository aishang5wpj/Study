package com.aishang5wpj.bezier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by wpj on 16/6/20下午4:00.
 */
public class CurveView extends View {

    private static final int HORIZONTAL_DIVER = 30;
    private static final int VERTICAL_DIVER = 30;
    private Paint mPaint;
    private Path mPath;
    private float mScreenW;
    private float mScreenH;

    public CurveView(Context context) {
        this(context, null);
    }

    public CurveView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CurveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mScreenW = windowManager.getDefaultDisplay().getWidth();
        mScreenH = windowManager.getDefaultDisplay().getHeight();


        mPath = new Path();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float offsetX = mScreenW * 0.1f;
        float offsetY = mScreenH * 0.5f;

        canvas.drawLine(offsetX, offsetY, mScreenW - offsetX, offsetY, mPaint);
        mPath.reset();

        float diverH = dp2px(HORIZONTAL_DIVER);
        float diverV = dp2px(VERTICAL_DIVER);

        mPath.moveTo(offsetX, offsetY + diverV);

        mPath.quadTo(offsetX + diverH, offsetY - diverV, offsetX + 2 * diverH, offsetY + diverV);

        mPath.quadTo(offsetX + 2 * diverH, offsetY + diverV, offsetX + 3 * diverH, offsetY - diverV);

        mPath.quadTo(offsetX + 3 * diverH, offsetY - diverV, offsetX + 4 * diverH, offsetY + diverV);

        mPath.quadTo(offsetX + 4 * diverH, offsetY + diverV, offsetX + 5 * diverH, offsetY - diverV);

        mPath.quadTo(offsetX + 5 * diverH, offsetY - diverV, offsetX + 6 * diverH, offsetY + diverV);

        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(mPath, mPaint);
    }

    private float dp2px(float dp) {

        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}