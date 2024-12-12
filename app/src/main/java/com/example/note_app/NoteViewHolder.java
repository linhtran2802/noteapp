package com.example.note_app;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

public class NoteViewHolder extends RecyclerView.ViewHolder {
    public TextView noteTitleTextView;
    public TextView noteDescriptionTextView;
    public TextView noteTimeTextView;
    public TextView noteTagsTextView; // TextView để hiển thị tags
    public ImageView pinIcon; // Icon ghim

    public NoteViewHolder(@NonNull View itemView) {
        super(itemView);

        noteTitleTextView = itemView.findViewById(R.id.noteTitle);
        noteDescriptionTextView = itemView.findViewById(R.id.noteDescription);
        noteTimeTextView = itemView.findViewById(R.id.noteTime);
        noteTagsTextView = itemView.findViewById(R.id.noteTags); // Phần tử để hiển thị danh sách tags
        pinIcon = itemView.findViewById(R.id.pinIcon); // Phần tử ImageView cho icon ghim
    }

    public void bind(Note note) {
        Log.d("NoteViewHolder", "Binding note: " + note.getTitle());

        Log.d("NoteViewHolder", "Note isPinned: " + note.isPinned());
        noteTitleTextView.setText(note.getTitle());

        // Cắt gọn mô tả nếu cần
        String truncatedDescription = note.getDescription();
        if (truncatedDescription.length() > 5) {
            truncatedDescription = truncatedDescription.substring(0, 5) + "...";
        }
        noteDescriptionTextView.setText(truncatedDescription);

        noteTimeTextView.setText("Thời gian: " + android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", note.getCreatedTime()));

        // Hiển thị danh sách tags
        Map<String, Boolean> tags = note.getTags();
        if (tags != null && !tags.isEmpty()) {
            noteTagsTextView.setVisibility(View.VISIBLE);

            // Lấy danh sách các key (tên tag) từ Map
            StringBuilder tagsString = new StringBuilder();
            for (String tag : tags.keySet()) {
                tagsString.append(tag).append(", ");
            }

            // Xóa dấu phẩy cuối cùng nếu có
            if (tagsString.length() > 2) {
                tagsString.setLength(tagsString.length() - 2);
            }

            noteTagsTextView.setText(tagsString.toString());
        } else {
            noteTagsTextView.setVisibility(View.GONE); // Ẩn nếu không có tag
        }

        // Hiển thị icon ghim nếu ghi chú được ghim
        if (note.isPinned()) {
            Log.d("NoteViewHolder", "Setting pinIcon to VISIBLE for note: " + note.getTitle());
            Log.d("NoteViewHolder", "Setting pinIcon to VISIBLE for note: " + note.getTitle());
            pinIcon.setVisibility(View.VISIBLE);
            pinIcon.setImageResource(R.drawable.ghim);
        } else {
            Log.d("NoteViewHolder", "Setting pinIcon to GONE for note: " + note.getTitle());
            pinIcon.setVisibility(View.GONE);
        }
        Context context = itemView.getContext();

        // Khi nhấn giữ ghi chú, chuyển đến NoteDetailActivity
        itemView.setOnLongClickListener(v -> {
            Log.d("NoteViewHolder", "Long press detected for note: " + note.getTitle());
            Intent intent = new Intent(context, NoteDetailActivity.class);
            intent.putExtra("noteId", note.getId()); // Truyền ID để truy vấn dữ liệu từ Firestore
            context.startActivity(intent);
            return true;
        });

        // Khi nhấn ghi chú, thực hiện mở chỉnh sửa
        itemView.setOnClickListener(v -> {
            Log.d("NoteViewHolder", "Single tap detected for note: " + note.getTitle());
            Intent intent = new Intent(context, EditNoteActivity.class);
            intent.putExtra("noteId", note.getId());
            context.startActivity(intent);
        });
    }
}
