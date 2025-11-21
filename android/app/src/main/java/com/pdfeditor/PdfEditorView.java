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
    private long lastTapTime = 0;

    // PDF positioning
    private float pdfOffsetX = 0;
    private float pdfOffsetY = 0;
    private float pdfScale = 1.0f;

    private OnElementEventListener eventListener;

    public PdfEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(false);  // Disable bitmap filtering for sharp rendering
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

        // If view not measured yet, wait for onSizeChanged
        if (viewWidth == 0 || viewHeight == 0) {
            return;
        }

        // Calculate scale to fit PDF within view bounds
        float pageWidth = currentPage.getWidth();
        float pageHeight = currentPage.getHeight();

        float scaleX = viewWidth / pageWidth;
        float scaleY = viewHeight / pageHeight;
        pdfScale = Math.min(scaleX, scaleY);

        int bitmapWidth = (int) (pageWidth * pdfScale);
        int bitmapHeight = (int) (pageHeight * pdfScale);

        // Center the PDF in the view
        pdfOffsetX = (viewWidth - bitmapWidth) / 2f;
        pdfOffsetY = (viewHeight - bitmapHeight) / 2f;

        pageBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(pageBitmap);
        canvas.drawColor(Color.WHITE);

        currentPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Re-render PDF when view size changes
        if (pdfRenderer != null && currentPage != null) {
            int currentIndex = currentPageIndex;
            currentPage.close();
            currentPage = null;
            showPage(currentIndex);
        }
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public int getPageCount() {
        return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
    }

    public void addTextElement(TextElement element) {
        textElements.add(element);
        selectedTextElement = element;
        selectedSignatureElement = null;
        invalidate();
    }

    public void addSignatureElement(SignatureElement element) {
        signatureElements.add(element);
        selectedSignatureElement = element;
        selectedTextElement = null;
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
        selectedTextElement = null;
        selectedSignatureElement = null;
        invalidate();
    }

    public void deleteSelectedElement() {
        if (selectedTextElement != null) {
            textElements.remove(selectedTextElement);
            selectedTextElement = null;
            invalidate();
        } else if (selectedSignatureElement != null) {
            signatureElements.remove(selectedSignatureElement);
            selectedSignatureElement = null;
            invalidate();
        }
    }

    public boolean hasSelectedElement() {
        return selectedTextElement != null || selectedSignatureElement != null;
    }

    public void setOnElementEventListener(OnElementEventListener listener) {
        this.eventListener = listener;
    }

    public interface OnElementEventListener {
        void onElementDoubleTapped(TextElement element);
        void onElementDoubleTapped(SignatureElement element);
        void onSelectionChanged();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pageBitmap != null) {
            // Draw PDF bitmap centered with offset
            canvas.drawBitmap(pageBitmap, pdfOffsetX, pdfOffsetY, paint);
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
                boolean hadSelection = selectedTextElement != null || selectedSignatureElement != null;
                selectedTextElement = null;
                selectedSignatureElement = null;

                // Check signatures first (usually on top)
                for (int i = signatureElements.size() - 1; i >= 0; i--) {
                    SignatureElement element = signatureElements.get(i);
                    if (element.contains(x, y)) {
                        selectedSignatureElement = element;
                        lastTouchX = x;
                        lastTouchY = y;

                        // Check for double tap
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastTapTime < 300) {
                            if (eventListener != null) {
                                eventListener.onElementDoubleTapped(element);
                            }
                        }
                        lastTapTime = currentTime;

                        if (eventListener != null) {
                            eventListener.onSelectionChanged();
                        }
                        invalidate();
                        return true;
                    }
                }

                // Then check text elements
                for (int i = textElements.size() - 1; i >= 0; i--) {
                    TextElement element = textElements.get(i);
                    if (element.contains(x, y)) {
                        selectedTextElement = element;
                        lastTouchX = x;
                        lastTouchY = y;

                        // Check for double tap
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastTapTime < 300) {
                            if (eventListener != null) {
                                eventListener.onElementDoubleTapped(element);
                            }
                        }
                        lastTapTime = currentTime;

                        if (eventListener != null) {
                            eventListener.onSelectionChanged();
                        }
                        invalidate();
                        return true;
                    }
                }

                // Store touch position even if no element selected
                lastTouchX = x;
                lastTouchY = y;

                if (hadSelection && eventListener != null) {
                    eventListener.onSelectionChanged();
                }
                invalidate();
                return true;

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
                // Keep selection active after touch up
                break;
        }

        return true;
    }

    public TextElement getSelectedTextElement() {
        return selectedTextElement;
    }

    public SignatureElement getSelectedSignatureElement() {
        return selectedSignatureElement;
    }

    public void updateSelectedTextElement(String text, float fontSize, String fontType) {
        if (selectedTextElement != null) {
            selectedTextElement.setText(text);
            selectedTextElement.setFontSize(fontSize);
            selectedTextElement.setFontType(fontType);
            invalidate();
        }
    }

    public float getPdfOffsetX() {
        return pdfOffsetX;
    }

    public float getPdfOffsetY() {
        return pdfOffsetY;
    }

    public float getPdfScale() {
        return pdfScale;
    }

    public float getPdfCenterX() {
        if (pageBitmap == null) return getWidth() / 2f;
        return pdfOffsetX + pageBitmap.getWidth() / 2f;
    }

    public float getPdfCenterY() {
        if (pageBitmap == null) return getHeight() / 2f;
        return pdfOffsetY + pageBitmap.getHeight() / 2f;
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
