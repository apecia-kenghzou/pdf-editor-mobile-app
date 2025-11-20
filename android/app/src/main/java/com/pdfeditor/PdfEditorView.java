package com.pdfeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.pdf.PdfRenderer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PdfEditorView extends View {

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private Bitmap pageBitmap;
    private int currentPageIndex = 0;
    private Paint paint;

    private List<TextElement> textElements = new ArrayList<>();
    private List<SignatureElement> signatureElements = new ArrayList<>();

    private TextElement selectedTextElement;
    private SignatureElement selectedSignatureElement;
    private float lastTouchX;
    private float lastTouchY;

    public PdfEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    public void setPdfRenderer(PdfRenderer renderer) {
        this.pdfRenderer = renderer;
        showPage(0);
    }

    public void showPage(int pageIndex) {
        if (pdfRenderer == null) return;

        if (currentPage != null) {
            currentPage.close();
        }

        currentPageIndex = pageIndex;
        currentPage = pdfRenderer.openPage(pageIndex);

        int viewWidth = getWidth();
        int viewHeight = getHeight();

        // If view not measured yet, use page dimensions
        if (viewWidth == 0 || viewHeight == 0) {
            viewWidth = currentPage.getWidth() * 2;
            viewHeight = currentPage.getHeight() * 2;
        }

        // Calculate scale to fit the page to view width
        float pageWidth = currentPage.getWidth();
        float pageHeight = currentPage.getHeight();
        float scale = viewWidth / pageWidth;

        int bitmapWidth = viewWidth;
        int bitmapHeight = (int) (pageHeight * scale);

        pageBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(pageBitmap);
        canvas.drawColor(Color.WHITE);

        currentPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Request layout to adjust view size
        requestLayout();
        invalidate();
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public int getPageCount() {
        return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
    }

    public void addTextElement(TextElement element) {
        textElements.add(element);
        invalidate();
    }

    public void addSignatureElement(SignatureElement element) {
        signatureElements.add(element);
        invalidate();
    }

    public List<TextElement> getTextElements() {
        return textElements;
    }

    public List<SignatureElement> getSignatureElements() {
        return signatureElements;
    }

    public void clearElementsForCurrentPage() {
        textElements.clear();
        signatureElements.clear();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // Set minimum height to fit the PDF bitmap
        if (pageBitmap != null) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = pageBitmap.getHeight();
            setMeasuredDimension(width, height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pageBitmap != null) {
            canvas.drawBitmap(pageBitmap, 0, 0, paint);
        }

        // Draw text elements
        for (TextElement element : textElements) {
            paint.setColor(Color.BLACK);
            paint.setTextSize(element.getFontSize());
            canvas.drawText(element.getText(), element.getX(), element.getY(), paint);

            // Draw selection box if selected
            if (element == selectedTextElement) {
                paint.setColor(Color.BLUE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                canvas.drawRect(element.getBounds(), paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        // Draw signature elements
        for (SignatureElement element : signatureElements) {
            canvas.drawBitmap(element.getSignatureBitmap(),
                    null,
                    element.getBounds(),
                    paint);

            // Draw selection box if selected
            if (element == selectedSignatureElement) {
                paint.setColor(Color.BLUE);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2);
                canvas.drawRect(element.getBounds(), paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if touching an existing element
                selectedTextElement = null;
                selectedSignatureElement = null;

                for (TextElement element : textElements) {
                    if (element.contains(x, y)) {
                        selectedTextElement = element;
                        lastTouchX = x;
                        lastTouchY = y;
                        invalidate();
                        return true;
                    }
                }

                for (SignatureElement element : signatureElements) {
                    if (element.contains(x, y)) {
                        selectedSignatureElement = element;
                        lastTouchX = x;
                        lastTouchY = y;
                        invalidate();
                        return true;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (selectedTextElement != null) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    selectedTextElement.move(dx, dy);
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                    return true;
                } else if (selectedSignatureElement != null) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    selectedSignatureElement.move(dx, dy);
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                break;
        }

        return super.onTouchEvent(event);
    }

    public TextElement getSelectedTextElement() {
        return selectedTextElement;
    }

    public void updateSelectedTextElement(String text, float fontSize, String fontType) {
        if (selectedTextElement != null) {
            selectedTextElement.setText(text);
            selectedTextElement.setFontSize(fontSize);
            selectedTextElement.setFontType(fontType);
            invalidate();
        }
    }

    public void cleanup() {
        if (currentPage != null) {
            currentPage.close();
        }
        if (pdfRenderer != null) {
            pdfRenderer.close();
        }
    }
}
