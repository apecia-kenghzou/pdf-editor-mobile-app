package com.pdfeditor;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfEditorActivity extends AppCompatActivity implements PdfEditorView.OnElementEventListener {

    private PdfEditorView pdfEditorView;
    private Button btnAddText;
    private Button btnAddSignature;
    private Button btnRemovePage;
    private Button btnSave;
    private Button btnPrevPage;
    private Button btnNextPage;
    private TextView tvPageNumber;
    private View editToolbar;
    private Button btnEdit;
    private Button btnDelete;

    private PdfRenderer pdfRenderer;
    private Uri pdfUri;
    private File tempPdfFile;

    private ActivityResultLauncher<Intent> signatureLauncher;
    private ActivityResultLauncher<Intent> saveFileLauncher;

    // Store elements for each page
    private Map<Integer, List<TextElement>> pageTextElements = new HashMap<>();
    private Map<Integer, List<SignatureElement>> pageSignatureElements = new HashMap<>();
    private List<Integer> pagesToRemove = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_editor);

        pdfEditorView = findViewById(R.id.pdfEditorView);
        btnAddText = findViewById(R.id.btnAddText);
        btnAddSignature = findViewById(R.id.btnAddSignature);
        btnRemovePage = findViewById(R.id.btnRemovePage);
        btnSave = findViewById(R.id.btnSave);
        btnPrevPage = findViewById(R.id.btnPrevPage);
        btnNextPage = findViewById(R.id.btnNextPage);
        tvPageNumber = findViewById(R.id.tvPageNumber);
        editToolbar = findViewById(R.id.editToolbar);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);

        pdfUri = getIntent().getData();

        // Register signature activity launcher
        signatureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        String signaturePath = result.getData().getStringExtra(SignatureActivity.EXTRA_SIGNATURE_PATH);
                        if (signaturePath != null) {
                            Bitmap signature = BitmapFactory.decodeFile(signaturePath);
                            if (signature != null) {
                                addSignatureToPage(signature);
                            }
                        }
                    }
                });

        // Register save file picker
        saveFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            savePdfToUri(uri);
                        }
                    }
                });

        // Set element event listener
        pdfEditorView.setOnElementEventListener(this);

        loadPdf();
        setupListeners();
    }

    private void loadPdf() {
        try {
            // Copy PDF to temp file for editing
            tempPdfFile = new File(getCacheDir(), "temp_pdf.pdf");
            InputStream inputStream = getContentResolver().openInputStream(pdfUri);
            FileOutputStream outputStream = new FileOutputStream(tempPdfFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            inputStream.close();
            outputStream.close();

            // Open PDF with PdfRenderer
            ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(
                    tempPdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            pdfRenderer = new PdfRenderer(fileDescriptor);

            pdfEditorView.setPdfRenderer(pdfRenderer);
            updatePageNumber();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupListeners() {
        btnAddText.setOnClickListener(v -> showAddTextDialog());

        btnAddSignature.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignatureActivity.class);
            signatureLauncher.launch(intent);
        });

        btnRemovePage.setOnClickListener(v -> removeCurrentPage());

        btnSave.setOnClickListener(v -> showSaveDialog());

        btnEdit.setOnClickListener(v -> editSelectedElement());

        btnDelete.setOnClickListener(v -> {
            pdfEditorView.deleteSelectedElement();
            updateEditToolbarVisibility();
        });

        btnPrevPage.setOnClickListener(v -> {
            saveCurrentPageElements();
            if (pdfEditorView.getCurrentPageIndex() > 0) {
                pdfEditorView.showPage(pdfEditorView.getCurrentPageIndex() - 1);
                loadCurrentPageElements();
                updatePageNumber();
            }
        });

        btnNextPage.setOnClickListener(v -> {
            saveCurrentPageElements();
            if (pdfEditorView.getCurrentPageIndex() < pdfEditorView.getPageCount() - 1) {
                pdfEditorView.showPage(pdfEditorView.getCurrentPageIndex() + 1);
                loadCurrentPageElements();
                updatePageNumber();
            }
        });
    }

    private void showAddTextDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_text_edit, null);
        builder.setView(dialogView);

        EditText etTextContent = dialogView.findViewById(R.id.etTextContent);
        SeekBar seekBarFontSize = dialogView.findViewById(R.id.seekBarFontSize);
        TextView tvFontSize = dialogView.findViewById(R.id.tvFontSize);
        Spinner spinnerFontType = dialogView.findViewById(R.id.spinnerFontType);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        // Setup font type spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.font_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFontType.setAdapter(adapter);

        // Check if editing existing text
        TextElement selectedElement = pdfEditorView.getSelectedTextElement();
        if (selectedElement != null) {
            etTextContent.setText(selectedElement.getText());
            seekBarFontSize.setProgress((int) selectedElement.getFontSize());
            tvFontSize.setText(selectedElement.getFontSize() + " pt");
        }

        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvFontSize.setText(progress + " pt");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String text = etTextContent.getText().toString();
            float fontSize = seekBarFontSize.getProgress();
            String fontType = spinnerFontType.getSelectedItem().toString();

            if (!text.isEmpty()) {
                if (selectedElement != null) {
                    pdfEditorView.updateSelectedTextElement(text, fontSize, fontType);
                } else {
                    // Position text at center of PDF
                    float x = pdfEditorView.getPdfCenterX();
                    float y = pdfEditorView.getPdfCenterY();
                    TextElement newElement = new TextElement(text, x, y, fontSize, fontType);
                    pdfEditorView.addTextElement(newElement);
                    updateEditToolbarVisibility();
                }
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addSignatureToPage(Bitmap signature) {
        // Calculate size maintaining aspect ratio
        float aspectRatio = (float) signature.getWidth() / signature.getHeight();
        float targetWidth = 200;
        float targetHeight = targetWidth / aspectRatio;

        // Add signature at center of the PDF
        float x = pdfEditorView.getPdfCenterX() - targetWidth / 2;
        float y = pdfEditorView.getPdfCenterY() - targetHeight / 2;
        SignatureElement element = new SignatureElement(signature, x, y, targetWidth, targetHeight);
        pdfEditorView.addSignatureElement(element);
        updateEditToolbarVisibility();
    }

    private void editSelectedElement() {
        TextElement textElement = pdfEditorView.getSelectedTextElement();
        SignatureElement signatureElement = pdfEditorView.getSelectedSignatureElement();

        if (textElement != null) {
            showEditTextDialog(textElement);
        } else if (signatureElement != null) {
            showEditSignatureDialog(signatureElement);
        }
    }

    private void showEditSignatureDialog(SignatureElement element) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_signature_edit, null);
        builder.setView(dialogView);

        SeekBar seekBarWidth = dialogView.findViewById(R.id.seekBarWidth);
        TextView tvWidth = dialogView.findViewById(R.id.tvWidth);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnOk = dialogView.findViewById(R.id.btnOk);

        // Set current values
        seekBarWidth.setProgress((int) element.getWidth());
        tvWidth.setText((int) element.getWidth() + " px");

        seekBarWidth.setMax(500);
        seekBarWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvWidth.setText(progress + " px");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnOk.setOnClickListener(v -> {
            float newWidth = seekBarWidth.getProgress();
            float aspectRatio = element.getSignatureBitmap().getWidth() / (float) element.getSignatureBitmap().getHeight();
            float newHeight = newWidth / aspectRatio;
            element.setWidth(newWidth);
            element.setHeight(newHeight);
            pdfEditorView.invalidate();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateEditToolbarVisibility() {
        editToolbar.setVisibility(pdfEditorView.hasSelectedElement() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onElementDoubleTapped(TextElement element) {
        showEditTextDialog(element);
    }

    @Override
    public void onElementDoubleTapped(SignatureElement element) {
        showEditSignatureDialog(element);
    }

    @Override
    public void onSelectionChanged() {
        updateEditToolbarVisibility();
    }

    private void showEditTextDialog(TextElement element) {
        showAddTextDialog();
    }

    private void showSaveDialog() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, "edited_pdf_" + System.currentTimeMillis() + ".pdf");
        saveFileLauncher.launch(intent);
    }

    private void removeCurrentPage() {
        int currentPage = pdfEditorView.getCurrentPageIndex();

        if (pdfEditorView.getPageCount() <= 1) {
            Toast.makeText(this, "Cannot remove the last page", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Remove Page")
                .setMessage("Are you sure you want to remove page " + (currentPage + 1) + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    pagesToRemove.add(currentPage);
                    Toast.makeText(this, "Page marked for removal. Save to apply.", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveCurrentPageElements() {
        int currentPage = pdfEditorView.getCurrentPageIndex();
        pageTextElements.put(currentPage, new ArrayList<>(pdfEditorView.getTextElements()));
        pageSignatureElements.put(currentPage, new ArrayList<>(pdfEditorView.getSignatureElements()));
    }

    private void loadCurrentPageElements() {
        int currentPage = pdfEditorView.getCurrentPageIndex();
        pdfEditorView.clearElementsForCurrentPage();

        List<TextElement> textElements = pageTextElements.get(currentPage);
        if (textElements != null) {
            for (TextElement element : textElements) {
                pdfEditorView.getTextElements().add(element);
            }
        }

        List<SignatureElement> signatureElements = pageSignatureElements.get(currentPage);
        if (signatureElements != null) {
            for (SignatureElement element : signatureElements) {
                pdfEditorView.getSignatureElements().add(element);
            }
        }

        pdfEditorView.invalidate();
        updateEditToolbarVisibility();
    }

    private void updatePageNumber() {
        int current = pdfEditorView.getCurrentPageIndex() + 1;
        int total = pdfEditorView.getPageCount();
        tvPageNumber.setText(current + " / " + total);
    }

    private void savePdfToUri(Uri uri) {
        saveCurrentPageElements();

        new Thread(() -> {
            try {
                PDDocument document = PDDocument.load(tempPdfFile);

                // Remove marked pages (in reverse order to avoid index issues)
                pagesToRemove.sort((a, b) -> b - a);
                for (int pageIndex : pagesToRemove) {
                    if (pageIndex < document.getNumberOfPages()) {
                        document.removePage(pageIndex);
                    }
                }

                // Get view dimensions for coordinate conversion
                final int viewWidth = pdfEditorView.getWidth();
                final int viewHeight = pdfEditorView.getHeight();

                // Add overlays to each page
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    PDPage page = document.getPage(i);
                    PDPageContentStream contentStream = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true);

                    // Get page dimensions
                    PDRectangle mediaBox = page.getMediaBox();
                    float pageWidth = mediaBox.getWidth();
                    float pageHeight = mediaBox.getHeight();

                    // Calculate scale and offset for this specific page
                    // (same calculation as in PdfEditorView.renderPage)
                    float scaleX = viewWidth / pageWidth;
                    float scaleY = viewHeight / pageHeight;
                    float pageScale = Math.min(scaleX, scaleY);

                    int bitmapWidth = (int) (pageWidth * pageScale);
                    int bitmapHeight = (int) (pageHeight * pageScale);

                    float pageOffsetX = (viewWidth - bitmapWidth) / 2f;
                    float pageOffsetY = (viewHeight - bitmapHeight) / 2f;

                    // Add text elements
                    List<TextElement> textElements = pageTextElements.get(i);
                    if (textElements != null) {
                        for (TextElement element : textElements) {
                            PDFont font = getFont(element.getFontType());
                            contentStream.beginText();
                            contentStream.setFont(font, element.getFontSize());

                            // Convert screen coordinates to PDF coordinates using this page's scale/offset
                            float pdfX = (element.getX() - pageOffsetX) / pageScale;
                            float pdfY = pageHeight - ((element.getY() - pageOffsetY) / pageScale);

                            contentStream.newLineAtOffset(pdfX, pdfY);
                            contentStream.showText(element.getText());
                            contentStream.endText();
                        }
                    }

                    // Add signature elements
                    List<SignatureElement> signatureElements = pageSignatureElements.get(i);
                    if (signatureElements != null) {
                        for (SignatureElement element : signatureElements) {
                            Bitmap bitmap = element.getSignatureBitmap();
                            PDImageXObject image = LosslessFactory.createFromImage(document, bitmap);

                            // Convert screen coordinates to PDF coordinates using this page's scale/offset
                            float pdfX = (element.getX() - pageOffsetX) / pageScale;
                            float pdfWidth = element.getWidth() / pageScale;
                            float pdfHeight = element.getHeight() / pageScale;
                            float pdfY = pageHeight - ((element.getY() - pageOffsetY) / pageScale) - pdfHeight;

                            contentStream.drawImage(image, pdfX, pdfY, pdfWidth, pdfHeight);
                        }
                    }

                    contentStream.close();
                }

                // Save to temp file first, then copy to selected location
                File tempOutput = new File(getCacheDir(), "temp_output.pdf");
                document.save(tempOutput);
                document.close();

                // Copy to user-selected location
                try (InputStream in = new java.io.FileInputStream(tempOutput);
                     java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = in.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                }

                tempOutput.delete();

                runOnUiThread(() -> {
                    Toast.makeText(this, "PDF saved successfully!",
                            Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error saving PDF: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private PDFont getFont(String fontType) {
        switch (fontType) {
            case "Times-Roman":
                return PDType1Font.TIMES_ROMAN;
            case "Courier":
                return PDType1Font.COURIER;
            case "Helvetica":
            default:
                return PDType1Font.HELVETICA;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pdfEditorView.cleanup();
        if (tempPdfFile != null && tempPdfFile.exists()) {
            tempPdfFile.delete();
        }
    }
}
