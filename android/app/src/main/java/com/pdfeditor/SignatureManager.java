package com.pdfeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SignatureManager {

    private static final String SIGNATURE_DIR = "signatures";
    private Context context;

    public SignatureManager(Context context) {
        this.context = context;
        createSignatureDirectory();
    }

    private void createSignatureDirectory() {
        File dir = new File(context.getFilesDir(), SIGNATURE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void saveSignature(Bitmap signature) {
        try {
            File dir = new File(context.getFilesDir(), SIGNATURE_DIR);
            String filename = "signature_" + System.currentTimeMillis() + ".png";
            File file = new File(dir, filename);

            FileOutputStream fos = new FileOutputStream(file);
            signature.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Bitmap> getSavedSignatures() {
        List<Bitmap> signatures = new ArrayList<>();
        File dir = new File(context.getFilesDir(), SIGNATURE_DIR);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".png")) {
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        if (bitmap != null) {
                            signatures.add(bitmap);
                        }
                    }
                }
            }
        }

        return signatures;
    }

    public void deleteSignature(int index) {
        File dir = new File(context.getFilesDir(), SIGNATURE_DIR);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && index >= 0 && index < files.length) {
                files[index].delete();
            }
        }
    }
}
