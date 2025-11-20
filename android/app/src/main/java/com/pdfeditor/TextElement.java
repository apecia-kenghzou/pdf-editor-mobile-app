package com.pdfeditor;

import android.graphics.RectF;

public class TextElement {
    private String text;
    private float x;
    private float y;
    private float fontSize;
    private String fontType;
    private RectF bounds;

    public TextElement(String text, float x, float y, float fontSize, String fontType) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.fontSize = fontSize;
        this.fontType = fontType;
        updateBounds();
    }

    private void updateBounds() {
        float width = text.length() * fontSize * 0.6f;
        float height = fontSize * 1.2f;
        bounds = new RectF(x, y - height, x + width, y);
    }

    public boolean contains(float touchX, float touchY) {
        return bounds.contains(touchX, touchY);
    }

    public void move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
        updateBounds();
    }

    // Getters and setters
    public String getText() { return text; }
    public void setText(String text) {
        this.text = text;
        updateBounds();
    }

    public float getX() { return x; }
    public void setX(float x) {
        this.x = x;
        updateBounds();
    }

    public float getY() { return y; }
    public void setY(float y) {
        this.y = y;
        updateBounds();
    }

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
        updateBounds();
    }

    public String getFontType() { return fontType; }
    public void setFontType(String fontType) { this.fontType = fontType; }

    public RectF getBounds() { return bounds; }
}
