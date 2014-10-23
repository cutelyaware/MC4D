package com.superliminal.util.android;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;

/**
 * A shim replacement for java.awt.Graphics.
 * Wraps an Android canvas so you can leave some of your Java SE code alone.
 * It doesn't try to support the full Graphics interface but just the parts
 * I've needed. It should be straightforward to implement more as needed.
 * 
 * @author Melinda Green
 */
public class Graphics {
    private Canvas mCanvas;
    private Paint mPaint = new Paint();
    private FontMetrics mFontMetrics = new FontMetrics();

    protected Canvas getCanvas() {
        return mCanvas;
    }

    protected Paint getPaint() {
        return mPaint;
    }

    // Internal caches for speed.
    protected static final int CACHE_MAX = 1024;
    private int[]
            tmpXs = new int[CACHE_MAX],
            tmpYs = new int[CACHE_MAX];
    private float[]
            tmpFXs = new float[CACHE_MAX],
            tmpFYs = new float[CACHE_MAX];

    public static class Font {
        public static final int PLAIN = 0, BOLD = 1, ITALIC = 2;
        public String mName;
        public int mStyle;
        public int mPoints;
        private Typeface mTypeface;

        public Font(String name, int style, int points) {
            mName = name;
            mStyle = style;
            mPoints = points;
            mTypeface = Typeface.create(mName, style);
        }
    }

    public class FontMetrics {
        public int getHeight() {
            return -mPaint.getFontMetricsInt().ascent + mPaint.getFontMetricsInt().descent; // NOTE: Inverting Android's negative ascent.
        }

        private Rect tmpRect = new Rect();

        public int stringWidth(String string) {
            mPaint.getTextBounds(string, 0, string.length(), tmpRect);
            return tmpRect.width();
        }

        public int getAscent() {
            return -mPaint.getFontMetricsInt().ascent; // NOTE: Inverting Android's negative ascent.
        }

        public int getDescent() {
            return mPaint.getFontMetricsInt().descent;
        }
    };

    public static class Label {
        public void setText(String string) {}
    }

    public Graphics(Canvas canvas) {
        mCanvas = canvas;
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);
    }

    public void setColor(Color color) {
        int iColor = color.intValue();
        mPaint.setColor(iColor);
    }

    private Path loadPath(float[] xs, float[] ys, int length) {
        Path path = new Path(); // because canvas.drawVertices ignores fill style.
        path.moveTo(xs[0], ys[0]);
        for(int i = 1; i < length; i++)
            path.lineTo(xs[i], ys[i]);
        return path;
    }

    private Path loadPath(int[] xs, int[] ys, int length) {
        Path path = new Path(); // because canvas.drawVertices ignores fill style.
        path.moveTo(xs[0], ys[0]);
        for(int i = 1; i < length; i++)
            path.lineTo(xs[i], ys[i]);
        return path;
    }

    public void fillPolygon(int[] xs, int[] ys, int length) {
        mCanvas.drawPath(loadPath(xs, ys, length), mPaint);
    }

    public void fillPolygon(float[] xs, float[] ys, int length) {
        mCanvas.drawPath(loadPath(xs, ys, length), mPaint);
    }

    public void drawPolygon(int[] xs, int[] ys, int length) {
        System.out.println("Stub Graphics.drawPolygon");
    }

    public void fillRect(int x, int y, int w, int h) {
        tmpXs[0] = x;
        tmpYs[0] = y;
        tmpXs[1] = x + w;
        tmpYs[1] = y;
        tmpXs[2] = x + w;
        tmpYs[2] = y + h;
        tmpXs[3] = x;
        tmpYs[3] = y + h;
        fillPolygon(tmpXs, tmpYs, 4);
    }

    public void drawLine(int startX, int startY, int stopX, int stopY) {
        mCanvas.drawLine(startX, startY, stopX, stopY, mPaint);
    }

    public void drawString(String str, int x, int y) {
        mCanvas.drawText(str, x, y, mPaint);
    }

    //
    // FONT SUPPORT
    //

    public FontMetrics getFontMetrics() {
        return mFontMetrics;
    }

    public void setFont(Font font) {
        // Convert the dips to pixels
        float dpi = mCanvas.getDensity();
        float pixPerPt = dpi / 72;
        mPaint.setTypeface(font.mTypeface);
        mPaint.setTextSize(font.mPoints * pixPerPt);
    }

    /**
     * Copies a variable list of ints into an array.
     */
    public void loadInts(int into[], int... ints) {
        System.arraycopy(ints, 0, into, 0, ints.length);
    }

    /**
     * Copies a variable list of floats into an array.
     */
    public void loadFloats(float into[], float... floats) {
        System.arraycopy(floats, 0, into, 0, floats.length);
    }

    /**
     * Copies a variable list of floats of the form X0,Y0,X1,Y1,...
     * into given lists of X and Y arrays.
     */
    public void loadFloats(float intoX[], float[] intoY, float... floats) {
        for(int i = 0; i < floats.length / 2; i++) {
            intoX[i] = floats[i * 2];
            intoY[i] = floats[i * 2 + 1];
        }
        return;
    }

    /**
     * Copies a variable list of ints of the form X0,Y0,X1,Y1,...
     * into given lists of X and Y arrays.
     */
    public void loadInts(int intoX[], int[] intoY, int... ints) {
        for(int i = 0; i < ints.length / 2; i++) {
            intoX[i * 2] = ints[i * 2];
            intoY[i * 2 + 1] = ints[i * 2 + 1];
        }
    }


    //
    // Additional drawing utilities.
    //
    public void drawTriangle(
            float x0, float y0,
            float x1, float y1,
            float x2, float y2)
    {
        loadFloats(tmpFXs, tmpFYs, x0, y0, x1, y1, x2, y2);
        fillPolygon(tmpFXs, tmpFYs, 3);
    }

    public void drawTriangle(
            int x0, int y0,
            int x1, int y1,
            int x2, int y2)
    {
        loadInts(tmpXs, tmpYs, x0, y0, x1, y1, x2, y2);
        fillPolygon(tmpXs, tmpYs, 3);
    }

    public void drawOval(int x, int y, int width, int height) {
        mCanvas.drawOval(new RectF(x - width / 2, y - height / 2, x + width / 2, y + width / 2), mPaint);
    }

    public void drawOval(float x, float y, float width, float height) {
        mCanvas.drawOval(new RectF(x - width / 2, y - height / 2, x + width / 2, y + width / 2), mPaint);
    }

//    public void drawImage(int id, int left, int top, int right, int bottom) {
//        Picture picture = new Picture();
//        picture.createFromStream(stream);
//        mCanvas.drawPicture(picture, new Rect(left, top, right, bottom));
//    }

}
