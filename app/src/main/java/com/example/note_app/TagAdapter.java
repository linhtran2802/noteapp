package com.example.note_app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TagAdapter extends RecyclerView.Adapter<TagAdapter.TagViewHolder> {

    private final Context context;
    private final List<String> tags;
    private final TagClickListener tagClickListener;
    private int selectedPosition = RecyclerView.NO_POSITION; // Vị trí tag được chọn

    public TagAdapter(Context context, List<String> tags, TagClickListener tagClickListener) {
        this.context = context;
        this.tags = tags;
        this.tagClickListener = tagClickListener;
    }

    @NonNull
    @Override
    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tag, parent, false);
        return new TagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
        String tag = tags.get(position);
        holder.tagName.setText(tag);

        // Hiển thị trạng thái tag đã chọn
        holder.itemView.setSelected(position == selectedPosition);

        // Xử lý sự kiện click vào tag
        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition == position) {
                // Bỏ chọn nếu click lại tag đang chọn
                selectedPosition = RecyclerView.NO_POSITION;
                tagClickListener.onTagClick(""); // Xóa bộ lọc
            } else {
                // Chọn tag mới
                selectedPosition = position;
                tagClickListener.onTagClick(tag);
            }
            notifyDataSetChanged(); // Cập nhật hiển thị
        });
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public static class TagViewHolder extends RecyclerView.ViewHolder {
        TextView tagName;

        public TagViewHolder(@NonNull View itemView) {
            super(itemView);
            tagName = itemView.findViewById(R.id.tagName);
        }
    }

    public interface TagClickListener {
        void onTagClick(String tag);
    }
}

