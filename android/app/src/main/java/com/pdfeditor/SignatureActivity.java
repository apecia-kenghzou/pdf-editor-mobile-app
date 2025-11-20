package com.pdfeditor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SignatureActivity extends AppCompatActivity {

    public static final String EXTRA_SIGNATURE = "signature";

    private SignatureView signatureView;
    private SignatureManager signatureManager;
    private Button btnClear;
    private Button btnSaveSignature;
    private Button btnUseSignature;
    private RecyclerView savedSignaturesList;
    private SavedSignaturesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);

        signatureView = findViewById(R.id.signatureView);
        btnClear = findViewById(R.id.btnClear);
        btnSaveSignature = findViewById(R.id.btnSaveSignature);
        btnUseSignature = findViewById(R.id.btnUseSignature);
        savedSignaturesList = findViewById(R.id.savedSignaturesList);

        signatureManager = new SignatureManager(this);

        // Setup RecyclerView for saved signatures
        savedSignaturesList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        loadSavedSignatures();

        btnClear.setOnClickListener(v -> signatureView.clear());

        btnSaveSignature.setOnClickListener(v -> {
            if (!signatureView.isEmpty()) {
                Bitmap signature = signatureView.getSignatureBitmap();
                signatureManager.saveSignature(signature);
                Toast.makeText(this, "Signature saved", Toast.LENGTH_SHORT).show();
                loadSavedSignatures();
                signatureView.clear();
            } else {
                Toast.makeText(this, "Please draw a signature first", Toast.LENGTH_SHORT).show();
            }
        });

        btnUseSignature.setOnClickListener(v -> {
            if (!signatureView.isEmpty()) {
                Bitmap signature = signatureView.getSignatureBitmap();
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_SIGNATURE, signature);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please draw a signature first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadSavedSignatures() {
        List<Bitmap> signatures = signatureManager.getSavedSignatures();
        adapter = new SavedSignaturesAdapter(signatures, bitmap -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SIGNATURE, bitmap);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        });
        savedSignaturesList.setAdapter(adapter);
    }
}
