package com.pdfeditor;

import android.graphics.Bitmap;
import android.graphics.RectF;

public class SignatureElement {
    private Bitmap signatureBitmap;
    private float x;
    private float y;
    private float width;
    private float height;
    private RectF bounds;

    public SignatureElement(Bitmap signatureBitmap, float x, float y, float width, float height) {
        this.signatureBitmap = signatureBitmap;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        updateBounds();
    }

    private void updateBounds() {
        bounds = new RectF(x, y, x + width, y + height);
    }

    public boolean contains(float touchX, float touchY) {
        return bounds.contains(touchX, touchY);
    }

    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
        updateBounds();
    }

    public void resize(float scale) {
        this.width *= scale;
        this.height *= scale;
        updateBounds();
    }

    // Getters and setters
    public Bitmap getSignatureBitmap() { return signatureBitmap; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public RectF getBounds() { return bounds; }

    public void setX(float x) {
        this.x = x;
        updateBounds();
    }

    public void setY(float y) {
        this.y = y;
        updateBounds();
    }

    public void setWidth(float width) {
        this.width = width;
        updateBounds();
    }

    public void setHeight(float height) {
        this.height = height;
        updateBounds();
    }
}
