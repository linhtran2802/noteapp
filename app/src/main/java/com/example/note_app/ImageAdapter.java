package com.example.note_app;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private Context context;
    private ArrayList<String> imageUrls;
    private OnRemoveClickListener onRemoveClickListener;

    public ImageAdapter(Context context, ArrayList<String> imageUrls, OnRemoveClickListener onRemoveClickListener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.onRemoveClickListener = onRemoveClickListener;
    }

    public interface OnRemoveClickListener {
        void onRemoveClick(int position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageTextView.setText("Image " + (position + 1));

        // Khi nhấn vào TextView, mở Dialog để hiển thị ảnh
        holder.imageTextView.setOnClickListener(v -> showImageDialog(imageUrls.get(position)));

        holder.removeButton.setOnClickListener(v -> onRemoveClickListener.onRemoveClick(position));
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView imageTextView;
        ImageButton removeButton;

        public ViewHolder(View itemView) {
            super(itemView);
            imageTextView = itemView.findViewById(R.id.imageTextView);
            removeButton = itemView.findViewById(R.id.removeImageButton);
        }
    }

    // Hiển thị ảnh trong Dialog
    private void showImageDialog(String imageUrl) {
        Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.dialog_image_view);
        ImageView imageView = dialog.findViewById(R.id.dialogImageView);

        // Sử dụng Glide để tải ảnh từ URL vào ImageView
        Glide.with(context)
                .load(imageUrl)
                .into(imageView);

        dialog.show();
    }
}
