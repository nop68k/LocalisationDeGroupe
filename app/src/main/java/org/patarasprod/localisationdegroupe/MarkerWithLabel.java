package org.patarasprod.localisationdegroupe;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class MarkerWithLabel extends Marker {
    Paint textPaint = null;
    String mLabel = null;
    float labelFontSize ;

    public MarkerWithLabel(MapView mapView, String label) {
        super(mapView);
        labelFontSize = 40f;
        mLabel = label;
        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(labelFontSize);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
    }
    public void draw(final Canvas c, final MapView osmv, boolean shadow) {
        draw( c, osmv);
    }

    public void setLabelFontSize(float fontSize) {
        textPaint.setTextSize(fontSize);
        labelFontSize = fontSize;
    }

    public void setLabelTextColor(int color) {
        textPaint.setColor(color);
    }

    public void draw( final Canvas c, final MapView osmv) {
        super.draw( c, osmv, false);
        Point p = this.mPositionPixels;  // already provisioned by Marker
        c.drawText( mLabel, p.x, p.y+(int)(labelFontSize/1.2), textPaint);
    }
}
