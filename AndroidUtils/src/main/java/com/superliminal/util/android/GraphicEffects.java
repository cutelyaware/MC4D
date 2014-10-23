package com.superliminal.util.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;

public class GraphicEffects {
    // A shared static Path object to avoiding "new" while drawing.
    // Just remember to always synchronize on it before using!
    private static Path synchronize_this_spiral_path = new Path();

    public static void drawSpirals(Canvas into, Paint paint, int num_spirals, int segments_per_line, float start_deg, float wrap) {
        int w = into.getWidth();
        int h = into.getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = (w < h ? w : h) / 2f;
        final float nwraps = 1;
        float angle_delta_deg = 360f * nwraps / segments_per_line * wrap;
        synchronized(synchronize_this_spiral_path) {
            synchronize_this_spiral_path.reset();
            for(int spiral = 0; spiral < num_spirals; spiral++) {
                synchronize_this_spiral_path.moveTo(cx, cy);
                float deg = start_deg + spiral * 360 / num_spirals;
                for(int i = 0; i <= segments_per_line; i++) {
                    float r2 = i * r / segments_per_line;
                    float x = r2 * (float) MathUtils.fastCos(deg + i * angle_delta_deg);
                    float y = r2 * (float) MathUtils.fastSin(deg + i * angle_delta_deg);
                    synchronize_this_spiral_path.lineTo(cx + x, cy + y);
                }
            }
            Style oStyle = paint.getStyle();
            paint.setStyle(Style.STROKE);
            into.drawPath(synchronize_this_spiral_path, paint);
            paint.setStyle(oStyle);
        }
    }

    public static void drawSlantedTics(Canvas into, Paint paint, int num_tics, float tic_span_deg, float start_deg, float slant_deg) {
        int w = into.getWidth();
        int h = into.getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = (w < h ? w : h) / 2f;
        for(int i = 0; i < num_tics; i++) {
            float c = (float) Math.cos(DTOR(start_deg + i * tic_span_deg));
            float s = (float) Math.sin(DTOR(start_deg + i * tic_span_deg));
            float stop_x = cx + r * c;
            float stop_y = cy + r * s;
            c = (float) Math.cos(DTOR(start_deg + i * tic_span_deg - slant_deg));
            s = (float) Math.sin(DTOR(start_deg + i * tic_span_deg - slant_deg));
            float start_x = cx + r * .8f * c;
            float start_y = cy + r * .8f * s;
            into.drawLine(start_x, start_y, stop_x, stop_y, paint);
        }
    }


    public final static float DTOR(float d) {
        return (float) (d * Math.PI / 180);
    }
}
