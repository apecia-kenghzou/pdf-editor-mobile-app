package com.pdfeditor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SignatureActivity extends AppCompatActivity {

    public static final String EXTRA_SIGNATURE_PATH = "signature_path";

    private SignatureView signatureView;
    private Button btnClear;
    private Button btnUseSignature;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signature);

        signatureView = findViewById(R.id.signatureView);
        btnClear = findViewById(R.id.btnClear);
        btnUseSignature = findViewById(R.id.btnUseSignature);

        btnClear.setOnClickListener(v -> signatureView.clear());

        btnUseSignature.setOnClickListener(v -> {
            if (!signatureView.isEmpty()) {
                Bitmap signature = signatureView.getSignatureBitmap();
                String path = saveTempSignature(signature);
                if (path != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_SIGNATURE_PATH, path);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(this, "Failed to save signature", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please draw a signature first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String saveTempSignature(Bitmap bitmap) {
        try {
            File tempFile = new File(getCacheDir(), "temp_signature.png");
            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
