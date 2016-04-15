package com.rnfstudio.bluesharp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

/**
 * Created by Roger on 2016/4/16.
 */
public class WaveView extends View {

    short[] data = null;
    short[] mData1 = null;
    short[] mData2 = null;

    SparseArray<short[]> dataMap = null;

    static float viewW;
    static float viewH;
    static float startX;
    static float startY;
    static float fractionY;

    static final int DATA_RESOLUTION = 2 << (16 - 1);

    public static final int KEY_RAW_DATA = 0;
    public static final int KEY_ACF_PITCH = 1;

    static Paint mPaint;
    static Path mPath;

    public WaveView(Context context, AttributeSet attrs) {
        super(context, attrs);

        viewW = context.getResources().getDimension(R.dimen.harp_width);
        viewH = context.getResources().getDimension(R.dimen.harp_height);
        startX = 0;
        startY = 0.5f * viewH;

        fractionY = viewH / DATA_RESOLUTION;

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2f);

        mPath = new Path();

        dataMap = new SparseArray<>();
    }

    public void setData(int key, short[] data) {
        dataMap.put(key, data);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw axis
        mPaint.setColor(ContextCompat.getColor(getContext(), android.R.color.black));
        drawAxis(canvas, mPaint);

        // draw data

        for (int i = 0; i < dataMap.size(); i++) {
            setPaintColor(i);
            drawData(canvas, dataMap.valueAt(i), mPaint);
        }
    }

    private void drawAxis(Canvas canvas, Paint paint) {
        canvas.drawLine(0, startY, viewW, startY, paint);
        canvas.drawRect(0, 0, viewW, viewH, paint);
    }

    private void drawData(Canvas canvas, short[] data, Paint paint) {
        if (data == null) {
            return;
        }

        mPath.reset();
        mPath.moveTo(startX, startY);

        float fractionX = viewW / data.length;

        for (int i = 0; i < data.length; i++) {
            float x = i * fractionX + startX;
            float y = data[i] * fractionY + startY;
            mPath.lineTo(x, y);
        }

        canvas.drawPath(mPath, paint);
    }

    private void setPaintColor(int index) {
        int colorId = android.R.color.holo_orange_light;

        switch (index) {
            case 0:
                colorId = android.R.color.holo_orange_light;
                break;
            case 1:
                colorId = android.R.color.holo_green_light;
                break;
            case 2:
                colorId = android.R.color.holo_purple;
                break;
        }

        mPaint.setColor(ContextCompat.getColor(getContext(), colorId));
    }
}
