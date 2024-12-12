package com.example.note_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NoteDetailActivity extends AppCompatActivity {

    private TextView noteTitleTextView, noteTimeTextView, noteDescriptionTextView;
    private ImageButton pinButton, deleteButton, passwordButton, shareButton;
    private Button addTagButton;
    private RecyclerView tagRecyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String noteId;
    private boolean isPinned;
    private String password;
    private TagAdapter tagAdapter;
    private List<String> tagList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_detail);

        // Khởi tạo FirebaseAuth và Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra người dùng đã đăng nhập
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Người dùng chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Kết nối giao diện
        noteTitleTextView = findViewById(R.id.noteTitle);
        noteTimeTextView = findViewById(R.id.noteTime);
        noteDescriptionTextView = findViewById(R.id.noteDescription);

        pinButton = findViewById(R.id.noteDetailPinButton);
        deleteButton = findViewById(R.id.noteDetailDeleteButton);
        passwordButton = findViewById(R.id.noteDetailPasswordButton);
        shareButton = findViewById(R.id.noteDetailShareButton);
        addTagButton = findViewById(R.id.addTagButton);
        tagRecyclerView = findViewById(R.id.tagRecyclerView);

        tagList = new ArrayList<>();
        tagAdapter = new TagAdapter(this, tagList, this::onTagClick);

        // Cấu hình RecyclerView
        tagRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        tagRecyclerView.setAdapter(tagAdapter);

        // Nhận dữ liệu từ Intent
        noteId = getIntent().getStringExtra("noteId");

        if (noteId != null) {
            loadNoteDetails(); // Tải dữ liệu ghi chú từ Firestore
            loadTags(); // Tải danh sách tag của ghi chú
        } else {
            Toast.makeText(this, "Không tìm thấy ghi chú!", Toast.LENGTH_SHORT).show();
            finish();
        }

        setupButtons();
    }

    private void loadNoteDetails() {
        db.collection("notes").document(noteId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        String description = documentSnapshot.getString("description");
                        Long createdTime = documentSnapshot.getLong("createdTime");
                        isPinned = Boolean.TRUE.equals(documentSnapshot.getBoolean("isPinned"));
                        password = documentSnapshot.getString("password");

                        // Hiển thị dữ liệu trên giao diện
                        noteTitleTextView.setText(title);
                        noteDescriptionTextView.setText(description);
                        if (createdTime != null) {
                            noteTimeTextView.setText("Thời gian: " + android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", createdTime));
                        }
                        updatePinIcon();
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu ghi chú!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NoteDetailActivity", "Lỗi khi tải dữ liệu ghi chú", e);
                    Toast.makeText(this, "Lỗi khi tải ghi chú!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void loadTags() {
        db.collection("notes").document(noteId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        tagList.clear();
                        Map<String, Boolean> tags = (Map<String, Boolean>) documentSnapshot.get("tags");
                        if (tags != null) {
                            tagList.addAll(tags.keySet());
                        }
                        tagAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi tải danh sách tag!", Toast.LENGTH_SHORT).show());
    }

    private void setupButtons() {
        pinButton.setOnClickListener(v -> togglePin());
        deleteButton.setOnClickListener(v -> deleteNote());
        passwordButton.setOnClickListener(v -> setPassword());
        shareButton.setOnClickListener(v -> shareNote());
        addTagButton.setOnClickListener(v -> showAddTagDialog());
    }

    private void showAddTagDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm Tag Mới");

        final EditText tagInput = new EditText(this);
        tagInput.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(tagInput);

        builder.setPositiveButton("Thêm", (dialog, which) -> {
            String newTag = tagInput.getText().toString().trim();
            if (!newTag.isEmpty()) {
                addTagToNote(newTag);
                addTagToFirestore(newTag);
            } else {
                Toast.makeText(this, "Tag không được để trống!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addTagToNote(String newTag) {
        db.collection("notes").document(noteId)
                .update("tags." + newTag, true)
                .addOnSuccessListener(aVoid -> {
                    tagList.add(newTag);
                    tagAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Đã thêm tag vào ghi chú!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi thêm tag vào ghi chú!", Toast.LENGTH_SHORT).show());
    }

    private void addTagToFirestore(String newTag) {
        db.collection("tags")
                .whereEqualTo("name", newTag)
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        db.collection("tags").add(Map.of(
                                        "name", newTag,
                                        "userId", mAuth.getCurrentUser().getUid()
                                ))
                                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Tag added to Firestore: " + newTag))
                                .addOnFailureListener(e -> Log.e("Firestore", "Error adding tag to Firestore", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error checking tag existence", e));
    }

    private void togglePin() {
        boolean newPinState = !isPinned;
        db.collection("notes").document(noteId)
                .update("isPinned", newPinState)
                .addOnSuccessListener(aVoid -> {
                    isPinned = newPinState;
                    updatePinIcon();
                    Toast.makeText(this, newPinState ? "Đã ghim ghi chú!" : "Đã bỏ ghim ghi chú!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi ghim ghi chú!", Toast.LENGTH_SHORT).show());
    }

    private void deleteNote() {
        db.collection("notes").document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Xóa ghi chú thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa ghi chú!", Toast.LENGTH_SHORT).show());
    }

    private void setPassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thiết lập mật khẩu");

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newPassword = passwordInput.getText().toString();
            if (newPassword.isEmpty()) {
                db.collection("notes").document(noteId)
                        .update("password", null)
                        .addOnSuccessListener(aVoid -> {
                            password = null;
                            Toast.makeText(this, "Mật khẩu đã được xóa!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa mật khẩu!", Toast.LENGTH_SHORT).show());
            } else {
                db.collection("notes").document(noteId)
                        .update("password", newPassword)
                        .addOnSuccessListener(aVoid -> {
                            password = newPassword;
                            Toast.makeText(this, "Đã thiết lập mật khẩu!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi lưu mật khẩu!", Toast.LENGTH_SHORT).show());
            }
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void shareNote() {
        String shareContent = "Tiêu đề: " + noteTitleTextView.getText().toString() + "\n" +
                "Mô tả: " + noteDescriptionTextView.getText().toString() + "\n" +
                "Thời gian: " + noteTimeTextView.getText().toString();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ ghi chú"));
    }

    private void updatePinIcon() {
        pinButton.setImageResource(isPinned ? R.drawable.un_pin : R.drawable.ghim);
    }

    private void onTagClick(String tag) {
        Toast.makeText(this, "Tag clicked: " + tag, Toast.LENGTH_SHORT).show();
    }
}
