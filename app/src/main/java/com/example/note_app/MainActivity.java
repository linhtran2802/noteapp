package com.example.note_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView noteRecyclerView, tagRecyclerView;
    private FirestoreRecyclerAdapter<Note, NoteViewHolder> adapter;
    private TagAdapter tagAdapter;
    private List<String> tagList = new ArrayList<>();
    private ImageView addNoteButton, homeButton, settingsButton;
    private EditText searchEditText;
    private String selectedTag = ""; // Tag được chọn để lọc ghi chú

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Yêu cầu quyền thông báo cho Android 13+ nếu chưa được cấp
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Khởi tạo Firebase Auth và Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra xem người dùng đã đăng nhập hay chưa
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Gán RecyclerView và EditText tìm kiếm
        noteRecyclerView = findViewById(R.id.noteRecyclerView);
        noteRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tagRecyclerView = findViewById(R.id.tagRecyclerView);
        searchEditText = findViewById(R.id.searchEditText);

        // Gán các nút vào biến
        addNoteButton = findViewById(R.id.addNoteButton);
        homeButton = findViewById(R.id.homeButton);
        settingsButton = findViewById(R.id.settingsButton);

        // Xử lý sự kiện cho nút Add Note
        addNoteButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddNoteActivity.class);
            startActivity(intent);
        });

        // Xử lý sự kiện cho nút Settings
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Thiết lập tìm kiếm theo tiêu đề ghi chú
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedTag = ""; // Reset selected tag khi tìm kiếm
                searchNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Thiết lập RecyclerView cho tag
        setupTagRecyclerView();

        // Tải danh sách tags từ Firestore
        loadTags(currentUser.getUid());

        // Thiết lập truy vấn để lấy dữ liệu từ Firestore và gán vào adapter
        initializeRecyclerView(currentUser.getUid(), "", "");
    }

    private void setupTagRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        tagRecyclerView.setLayoutManager(layoutManager);

        tagAdapter = new TagAdapter(this, tagList, tag -> {
            if (selectedTag.equals(tag)) {
                // Bỏ chọn nếu click lại tag đang chọn
                selectedTag = "";
            } else {
                // Chọn tag mới
                selectedTag = tag;
            }
            initializeRecyclerView(mAuth.getCurrentUser().getUid(), "", selectedTag); // Cập nhật danh sách ghi chú
        });
        tagRecyclerView.setAdapter(tagAdapter);
    }

    private void loadTags(String userId) {
        // Lấy danh sách tags của tài khoản hiện tại
        db.collection("tags")
                .whereEqualTo("userId", userId) // Lọc theo userId
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    tagList.clear(); // Xóa danh sách cũ
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String tagName = document.getString("name");
                        if (tagName != null) {
                            tagList.add(tagName); // Thêm từng tag
                        }
                    }
                    tagAdapter.notifyDataSetChanged(); // Cập nhật RecyclerView
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi tải danh sách tag!", Toast.LENGTH_SHORT).show());
    }

    private void searchNotes(String keyword) {
        initializeRecyclerView(mAuth.getCurrentUser().getUid(), keyword, selectedTag);
    }

    private void initializeRecyclerView(String userId, String keyword, String tag) {
        Query query = db.collection("notes").whereEqualTo("userId", userId);

        if (!tag.isEmpty()) {
            // Lọc ghi chú có chứa tag cụ thể
            query = query.whereEqualTo("tags." + tag, true); // Truy vấn Firestore với Map
        }

        if (!keyword.isEmpty()) {
            query = query.orderBy("title")
                    .startAt(keyword)
                    .endAt(keyword + "\uf8ff");
        } else {
            // Sắp xếp theo `isPinned` trước, sau đó là `createdTime`
            query = query.orderBy("isPinned", Query.Direction.DESCENDING)
                    .orderBy("createdTime", Query.Direction.DESCENDING);
        }

        FirestoreRecyclerOptions<Note> options = new FirestoreRecyclerOptions.Builder<Note>()
                .setQuery(query, Note.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Note, NoteViewHolder>(options) {
            @NonNull
            @Override
            public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
                return new NoteViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull NoteViewHolder holder, int position, @NonNull Note model) {
                holder.bind(model);

                // Long click: Chuyển đến NoteDetailActivity sau khi kiểm tra mật khẩu
                holder.itemView.setOnLongClickListener(v -> {
                    checkPasswordAndProceed(model, () -> {
                        Intent intent = new Intent(MainActivity.this, NoteDetailActivity.class);
                        intent.putExtra("noteId", model.getId());
                        startActivity(intent);
                    });
                    return true;
                });

                // Single tap: Chuyển đến EditNoteActivity sau khi kiểm tra mật khẩu
                holder.itemView.setOnClickListener(v -> {
                    checkPasswordAndProceed(model, () -> {
                        Intent intent = new Intent(MainActivity.this, EditNoteActivity.class);
                        intent.putExtra("noteId", model.getId());
                        startActivity(intent);
                    });
                });
            }
        };

        noteRecyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    private void checkPasswordAndProceed(Note model, Runnable action) {
        db.collection("notes").document(model.getId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String storedPassword = documentSnapshot.getString("password");
                        if (storedPassword != null && !storedPassword.isEmpty()) {
                            showPasswordDialog(storedPassword, action);
                        } else {
                            action.run();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Không tìm thấy ghi chú!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi kiểm tra mật khẩu!", Toast.LENGTH_SHORT).show());
    }

    private void showPasswordDialog(String storedPassword, Runnable action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập mật khẩu");

        final EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(passwordInput);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String inputPassword = passwordInput.getText().toString();
            if (inputPassword.equals(storedPassword)) {
                action.run();
            } else {
                Toast.makeText(this, "Mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeRecyclerView(mAuth.getCurrentUser().getUid(), "", selectedTag);
        loadTags(mAuth.getCurrentUser().getUid());
    }
}
