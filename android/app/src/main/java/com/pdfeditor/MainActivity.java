package com.pdfeditor;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize PDFBox
        PDFBoxResourceLoader.init(getApplicationContext());

        Button btnSelectPdf = findViewById(R.id.btnSelectPdf);

        // Register permission request launcher (modern approach)
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openPdfPicker();
                    } else {
                        Toast.makeText(this, "Permission denied. Cannot access PDF files.", Toast.LENGTH_LONG).show();
                    }
                });

        // Register PDF picker
        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Uri pdfUri = data.getData();
                            openPdfEditor(pdfUri);
                        }
                    }
                });

        btnSelectPdf.setOnClickListener(v -> {
            // For Android 13+, we don't need storage permission for ACTION_OPEN_DOCUMENT
            // For older versions, we request permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ - no permission needed for ACTION_OPEN_DOCUMENT
                openPdfPicker();
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6-12 - request READ_EXTERNAL_STORAGE
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                // Below Android 6 - permissions granted at install time
                openPdfPicker();
            }
        });
    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pdfPickerLauncher.launch(intent);
    }

    private void openPdfEditor(Uri pdfUri) {
        Intent intent = new Intent(this, PdfEditorActivity.class);
        intent.setData(pdfUri);
        startActivity(intent);
    }
}
