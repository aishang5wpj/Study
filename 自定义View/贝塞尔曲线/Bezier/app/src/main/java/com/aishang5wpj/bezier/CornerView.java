package com.aishang5wpj.bezier;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by wpj on 16/6/8上午10:56.
 */
public class CornerView extends View {
    private int mRadiusBg;
    private int mWidth;
    private Path mPathBg;
    private Paint mPaintBg;
    private float mHeight;

    public CornerView(Context context) {
        this(context, null);
    }

    public CornerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CornerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mPathBg = new Path();
        mPaintBg = new Paint();
        mPaintBg.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawRadius(canvas);
    }

    //四个都是圆角
    private void drawRadius(Canvas canvas) {

        //1、左上方的圆角
        mPathBg.moveTo(0, mRadiusBg);
        mPathBg.quadTo(0, 0, mRadiusBg, 0);
        //2、上方的直线
        mPathBg.lineTo(mWidth - mRadiusBg, 0);
        //3、右上方的圆角
        mPathBg.quadTo(mWidth, 0, mWidth, mRadiusBg);
        //4、右边的直线
        mPathBg.lineTo(mWidth, mHeight - mRadiusBg);
        //5、右下方的圆角
        mPathBg.quadTo(mWidth, mHeight, mWidth - mRadiusBg, mHeight);
        //6、底部的直线
        mPathBg.lineTo(mRadiusBg, mHeight);
        //7、左下方的圆角
        mPathBg.quadTo(0, mHeight, 0, mHeight - mRadiusBg);
        //8、把路径画到画布上
        mPaintBg.setColor(Color.WHITE);
        canvas.drawPath(mPathBg, mPaintBg);
    }

    //上方圆角
    private void drawTopRadius(Canvas canvas) {
        mPathBg.moveTo(0, mHeight);
        mPathBg.lineTo(0, mRadiusBg);
        //1、经过上一步的操作，现在path的位置已经移动到了(0, mRadiusBg)处，
        // 下面画贝塞尔曲线的时候，(0, mRadiusBg)即作为贝塞尔曲线的起点
        //2、quadTo()方法中，前两个参数表示控制点的x、y坐标，后两个表示曲线终点的x、y坐标
        mPathBg.quadTo(0, 0, mRadiusBg, 0);
        mPathBg.lineTo(mWidth - mRadiusBg, 0);
        mPathBg.quadTo(mWidth, 0, mWidth, mRadiusBg);
        mPathBg.lineTo(mWidth, mHeight);
        mPathBg.lineTo(0, mHeight);
        mPaintBg.setColor(Color.WHITE);
        canvas.drawPath(mPathBg, mPaintBg);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;
        //圆角的大小
        mRadiusBg = mWidth / 20;
    }
}
