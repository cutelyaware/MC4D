package com.superliminal.util.android.svg;

import com.superliminal.util.android.svg.SVG;
import com.superliminal.util.android.svg.SVGParser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.PictureDrawable;
import android.widget.ImageView;

/*

   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
/**
 * Describes a vector Picture object, and optionally its bounds.
 *
 * @author Larva Labs, LLC
 */
public class SVG {

    /**
     * The parsed Picture object.
     */
    private Picture picture;
    

    /**
     * These are the bounds for the SVG specified as a hidden "bounds" layer in the SVG.
     */
    private RectF bounds;

    /**
     * These are the estimated bounds of the SVG computed from the SVG elements while parsing.
     * Note that this could be null if there was a failure to compute limits (ie. an empty SVG).
     */
    private RectF limits = null;

    /**
     * Construct a new SVG.
     * @param picture the parsed picture object.
     * @param bounds the bounds computed from the "bounds" layer in the SVG.
     */
    SVG(Picture picture, RectF bounds) {
        this.picture = picture;
        this.bounds = bounds;
    }
    public static void tintImage(Context context, int svgID, ImageView iv, int tintColorID, int newColorID) {
        tintImage(context, svgID, iv, tintColorID, newColorID, -1);
    }

    public static void tintImage(Context context, int svgID, ImageView iv, int tintColorID, int newColorID, float scale) {
        SVG svg = null;
        if(tintColorID >= 0) {
            String oldstr = context.getString(tintColorID);
            String newstr = context.getString(newColorID);
            int replaceColor = Color.parseColor(oldstr);
            int withColor = Color.parseColor(newstr);
            svg = SVGParser.getSVGFromResource(context.getResources(), svgID, replaceColor, withColor);
        }
        else {
            svg = SVGParser.getSVGFromResource(context.getResources(), svgID);
        }
        PictureDrawable pd = svg.createPictureDrawable(scale);
        iv.setImageDrawable(pd);
        iv.setColorFilter(context.getResources().getColor(tintColorID, context.getTheme()), PorterDuff.Mode.SRC_ATOP);
    }
    
    public static void tintImage(Context context, int svgID, ImageView iv) {
        tintImage(context, svgID, iv, -1, -1);
    }    


    /**
     * Set the limits of the SVG, which are the estimated bounds computed by the parser.
     * @param limits the bounds computed while parsing the SVG, may not be entirely accurate.
     */
    void setLimits(RectF limits) {
        this.limits = limits;
    }
    

    public PictureDrawable createPictureDrawable() {
        return createPictureDrawable(-1);
    }

    /**
     * Create a picture drawable from the SVG.
     * @param scale An optional argument to resize the underlying picture. -1 to use the default.
     * @return the PictureDrawable.
     */
    public PictureDrawable createPictureDrawable(final float scale) {
        return new PictureDrawable(picture) {

            @Override public void draw(Canvas canvas) {
                float s = scale < 0 ? 1 : scale;//canvas.getClipBounds().height() / (limits.bottom - limits.top);
                canvas.save();
                    //canvas.translate(-limits.left, -limits.top);
                    canvas.scale(s, s);
                    //super.draw(canvas); // This was the nasty bug I had created.
                    picture.draw(canvas); // This is what it *should* have been. - MG
                canvas.restore();
            }

            // To make SVG pictures automatically scale, simply comment out these methods
            // or add another argument to the enclosing createPictureDrawable method
            // to affect these methods.
            @Override
            public int getIntrinsicWidth() { // reserves this much space in its container.
                float iw = super.getIntrinsicWidth();
                float s = scale < 0 ? 1 : scale;
                return Math.round(s * iw); 
            } 
            @Override
            public int getIntrinsicHeight(){ // reserves this much space in its container.
                float ih = super.getIntrinsicHeight();
                float s = scale < 0 ? 1 : scale;
                return Math.round(s * ih); 
            }

            
//            @Override
//            public int getIntrinsicWidth() {
//                if (bounds != null) {
//                    return (int) bounds.width();
//                } else if (limits != null) {
//                    return (int) limits.width();
//                } else {
//                    return -1;
//                }
//            }
//
//            @Override
//            public int getIntrinsicHeight() {
//                if (bounds != null) {
//                    return (int) bounds.height();
//                } else if (limits != null) {
//                    return (int) limits.height();
//                } else {
//                    return -1;
//                }
//            }
        };
    }

    /**
     * Get the parsed SVG picture data.
     * @return the picture.
     */
    public Picture getPicture() {
        return picture;
    }

    /**
     * Gets the bounding rectangle for the SVG, if one was specified.
     * @return rectangle representing the bounds.
     */
    public RectF getBounds() {
        return bounds;
    }

    /**
     * Gets the bounding rectangle for the SVG that was computed upon parsing. It may not be entirely accurate for certain curves or transformations, but is often better than nothing.
     * @return rectangle representing the computed bounds.
     */
    public RectF getLimits() {
        return limits;
    }
}
