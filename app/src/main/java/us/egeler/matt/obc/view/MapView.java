package us.egeler.matt.obc.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class MapView extends View {
    Paint paint;

    MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDraw();
    }

    private void initDraw() {
        // create paint
        paint = new Paint();
        paint.setAntiAlias(false); // make sure AA is off. our display only has on/off pixels

        // set paint color
        int color = 0xff << 24 | 0x00 << 16 | 0x00 << 8 | 0x00;
        paint.setColor(color);

        // set font
        // Helvetica bold is readable down to ~size 12 while angled, and readable-ish around size 11/10
        paint.setTypeface(Typeface.create("Helvetica",Typeface.BOLD));
        paint.setTextSize(12);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw rotated test text
        canvas.rotate(40);
        canvas.drawText("Lincoln st -- Grand River ave -- 28th st -- 44th st", 40,50,paint);
        canvas.restore();

        // draw non-rotated rect
        canvas.drawRect(0,0, 20,20,paint);
    }
}
