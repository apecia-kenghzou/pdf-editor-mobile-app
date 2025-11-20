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

public class PdfEditorActivity extends AppCompatActivity {

    private PdfEditorView pdfEditorView;
    private Button btnAddText;
    private Button btnAddSignature;
    private Button btnRemovePage;
    private Button btnSave;
    private Button btnPrevPage;
    private Button btnNextPage;
    private TextView tvPageNumber;

    private PdfRenderer pdfRenderer;
    private Uri pdfUri;
    private File tempPdfFile;

    private ActivityResultLauncher<Intent> signatureLauncher;

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

        btnSave.setOnClickListener(v -> savePdf());

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
                    TextElement newElement = new TextElement(text, 100, 200, fontSize, fontType);
                    pdfEditorView.addTextElement(newElement);
                }
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter text", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addSignatureToPage(Bitmap signature) {
        // Add signature at center of the page
        float x = pdfEditorView.getWidth() / 2f - 100;
        float y = pdfEditorView.getHeight() / 2f - 50;
        SignatureElement element = new SignatureElement(signature, x, y, 200, 100);
        pdfEditorView.addSignatureElement(element);
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
                pdfEditorView.addTextElement(element);
            }
        }

        List<SignatureElement> signatureElements = pageSignatureElements.get(currentPage);
        if (signatureElements != null) {
            for (SignatureElement element : signatureElements) {
                pdfEditorView.addSignatureElement(element);
            }
        }
    }

    private void updatePageNumber() {
        int current = pdfEditorView.getCurrentPageIndex() + 1;
        int total = pdfEditorView.getPageCount();
        tvPageNumber.setText(current + " / " + total);
    }

    private void savePdf() {
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

                // Add overlays to each page
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    PDPage page = document.getPage(i);
                    PDPageContentStream contentStream = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true);

                    // Get page height for coordinate conversion
                    PDRectangle mediaBox = page.getMediaBox();
                    float pageHeight = mediaBox.getHeight();

                    // Add text elements
                    List<TextElement> textElements = pageTextElements.get(i);
                    if (textElements != null) {
                        for (TextElement element : textElements) {
                            PDFont font = getFont(element.getFontType());
                            contentStream.beginText();
                            contentStream.setFont(font, element.getFontSize());
                            // Convert Android coordinates to PDF coordinates
                            float pdfY = pageHeight - element.getY();
                            contentStream.newLineAtOffset(element.getX(), pdfY);
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
                            // Convert Android coordinates to PDF coordinates
                            float pdfY = pageHeight - element.getY() - element.getHeight();
                            contentStream.drawImage(image, element.getX(), pdfY,
                                    element.getWidth(), element.getHeight());
                        }
                    }

                    contentStream.close();
                }

                // Save to new file
                File outputFile = new File(getExternalFilesDir(null),
                        "edited_pdf_" + System.currentTimeMillis() + ".pdf");
                document.save(outputFile);
                document.close();

                runOnUiThread(() -> {
                    Toast.makeText(this, "PDF saved to: " + outputFile.getAbsolutePath(),
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
