package com.pdfeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class SignatureView extends View {

    private Paint paint;
    private Path path;
    private Bitmap signatureBitmap;
    private Canvas signatureCanvas;

    public SignatureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5f);

        path = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            signatureBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            signatureCanvas = new Canvas(signatureBitmap);
            signatureCanvas.drawColor(Color.TRANSPARENT);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (signatureBitmap != null) {
            canvas.drawBitmap(signatureBitmap, 0, 0, null);
        }
        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                return true;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                signatureCanvas.drawPath(path, paint);
                path.reset();
                invalidate();
                return true;
        }

        return false;
    }

    public void clear() {
        if (signatureCanvas != null) {
            signatureCanvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
            path.reset();
            invalidate();
        }
    }

    public Bitmap getSignatureBitmap() {
        return signatureBitmap;
    }

    public boolean isEmpty() {
        if (signatureBitmap == null) return true;

        for (int x = 0; x < signatureBitmap.getWidth(); x++) {
            for (int y = 0; y < signatureBitmap.getHeight(); y++) {
                int pixel = signatureBitmap.getPixel(x, y);
                // Check if pixel is not transparent
                if (Color.alpha(pixel) != 0) {
                    return false;
                }
            }
        }
        return true;
    }
}
