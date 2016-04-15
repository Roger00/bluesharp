package com.rnfstudio.bluesharp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by Roger on 2016/4/14.
 */
public class HarpView extends View {

    static final int[][] semitoneTable = new int[][] {
            { 0,  0,  0,  0,  0,  0,  0,  0,  0, 93},
            {63, 68, 72, 75, 77, 81,  0, 86, 89, 94},
            {60, 64, 67, 72, 76, 78, 83, 87, 90, 95},
            { 1,  2,  3,  4,  5,  6,  7,  8,  9, 10},
            {62, 67, 71, 74, 77, 80, 82, 85, 88, 92},
            {61, 66, 70, 73,  0, 79, 84, 88, 91, 96},
            { 0, 65, 69,  0,  0,  0,  0,  0,  0,  0},
            { 0,  0, 68,  0,  0,  0,  0,  0,  0,  0}
    };

    static final int cRow = semitoneTable.length;
    static final int cColumn = semitoneTable[0].length;
    static float harpW;
    static float harpH;
    static float cellW;
    static float cellH;
    static float textSize;

    static Paint cellPaint;
    static Paint textPaint;
    int mHighlight;

    public HarpView(Context context, AttributeSet attrs) {
        super(context, attrs);

        harpW = context.getResources().getDimension(R.dimen.harp_width);
        harpH = context.getResources().getDimension(R.dimen.harp_height);
        textSize = context.getResources().getDimension(R.dimen.cell_text_size);
        cellW = harpW / cColumn;
        cellH = harpH / cRow;

        cellPaint = new Paint();
        cellPaint.setColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        cellPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        textPaint = new Paint();
        textPaint.setColor(ContextCompat.getColor(context, android.R.color.white));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(context.getResources().getDimension(R.dimen.cell_text_size));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < cRow; i++) {
            for (int j = 0; j < cColumn; j++) {

                int semitone = semitoneTable[i][j];

                if (semitone == 0) {
                    continue;
                }

                if (mHighlight == semitone) {
                    cellPaint.setColor(ContextCompat.getColor(getContext(), android.R.color.holo_orange_light));
                } else if (semitone <= 10) {
                    cellPaint.setColor(ContextCompat.getColor(getContext(), android.R.color.black));
                } else {
                    cellPaint.setColor(ContextCompat.getColor(getContext(), android.R.color.darker_gray));
                }

                // draw cell
                int left = (int) (j * cellW);
                int top = (int) (i * cellH);
                int right = (int) ((j + 1) * cellW);
                int bottom = (int) ((i + 1) * cellH);

                canvas.drawRect(left, top, right, bottom, cellPaint);

                // draw cell text
                float y = (i + 1) * cellH - (cellH - textSize) * 0.5f;
                float x = (j + 0.5f) * cellW;
                canvas.drawText(String.valueOf(semitone), x, y, textPaint);
            }
        }
    }

    public void setHighlight(float semitone) {
        mHighlight = Math.round(semitone);
        invalidate();
    }
}
