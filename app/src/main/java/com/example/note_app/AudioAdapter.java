package com.example.note_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AudioAdapter extends RecyclerView.Adapter<AudioAdapter.ViewHolder> {

    private Context context;
    private ArrayList<String> audioUrls; // Sử dụng String cho URLs của âm thanh
    private OnRemoveClickListener onRemoveClickListener;

    public interface OnRemoveClickListener {
        void onRemoveClick(int position);
    }

    public AudioAdapter(Context context, ArrayList<String> audioUrls, OnRemoveClickListener listener) {
        this.context = context;
        this.audioUrls = audioUrls;
        this.onRemoveClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_audio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Hiển thị tên file âm thanh
        holder.audioTextView.setText("Audio " + (position + 1));

        // Khi nhấn vào TextView, mở file âm thanh trực tiếp
        holder.audioTextView.setOnClickListener(v -> {
            Uri audioUri = Uri.parse(audioUrls.get(position));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(audioUri, "audio/*");
            context.startActivity(intent);
        });

        holder.removeButton.setOnClickListener(v -> onRemoveClickListener.onRemoveClick(position));
    }

    @Override
    public int getItemCount() {
        return audioUrls.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView audioTextView;
        ImageButton removeButton;

        public ViewHolder(View itemView) {
            super(itemView);
            audioTextView = itemView.findViewById(R.id.audioTextView);
            removeButton = itemView.findViewById(R.id.removeAudioButton);
        }
    }
}
