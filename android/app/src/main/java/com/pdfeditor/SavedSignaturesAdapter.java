package com.pdfeditor;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SavedSignaturesAdapter extends RecyclerView.Adapter<SavedSignaturesAdapter.ViewHolder> {

    private List<Bitmap> signatures;
    private OnSignatureClickListener listener;

    public interface OnSignatureClickListener {
        void onSignatureClick(Bitmap signature);
    }

    public SavedSignaturesAdapter(List<Bitmap> signatures, OnSignatureClickListener listener) {
        this.signatures = signatures;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(200, 150));
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setPadding(8, 8, 8, 8);
        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bitmap signature = signatures.get(position);
        holder.imageView.setImageBitmap(signature);
        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSignatureClick(signature);
            }
        });
    }

    @Override
    public int getItemCount() {
        return signatures.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(ImageView imageView) {
            super(imageView);
            this.imageView = imageView;
        }
    }
}
