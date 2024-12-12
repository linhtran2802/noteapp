package com.example.note_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {

    private TextView usernameTextView, emailTextView, phoneTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ImageView profileImageView, settingsButton;
    private GoogleSignInClient googleSignInClient;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneTextView = findViewById(R.id.phoneTextView);

        // Khởi tạo settingsButton
        settingsButton = findViewById(R.id.settingsButton);
        swipeRefreshLayout = findViewById(R.id.fraglimbah_swiperefreshlayout); // Khởi tạo SwipeRefreshLayout

        // Thiết lập sự kiện kéo để làm mới
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserProfile();
            swipeRefreshLayout.setRefreshing(false); // Tắt vòng xoay khi hoàn tất
        });
        loadUserProfile();

        profileImageView.setOnClickListener(v -> openGallery());

        findViewById(R.id.deleteAccount).setOnClickListener(v -> showDeleteAccountDialog());
        findViewById(R.id.logout).setOnClickListener(v -> showLogoutDialog());
        findViewById(R.id.privacyPolicy).setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, PrivacyPolicyActivity.class);
            startActivity(intent);
        });

        // Sự kiện khi bấm vào settingsButton
        settingsButton.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Setting", Toast.LENGTH_SHORT).show();
        });

        // Nút Home quay về MainActivity
        ImageView homeButton = findViewById(R.id.homeButton);
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Cấu hình Google Sign-In để đăng xuất
        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build());
    }

    private void loadUserProfile() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    String email = documentSnapshot.getString("email");
                    String phone = documentSnapshot.getString("phone");

                    usernameTextView.setText(username != null ? username : "No username");
                    emailTextView.setText(email != null ? email : "No email");
                    phoneTextView.setText(phone != null ? phone : "No phone number");
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(SettingsActivity.this, "Failed to load user profile", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();

            // Sử dụng Glide để tải và hiển thị ảnh tròn
            Glide.with(this)
                    .load(imageUri)
                    .circleCrop() // Cắt ảnh thành hình tròn
                    .into(profileImageView); // Đặt ảnh vào profileImageView trong nền tròn
        }
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa tài khoản")
                .setMessage("Bạn chắc chắn muốn xóa tài khoản?")
                .setPositiveButton("Yes", (dialog, which) -> deleteAccount())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAccount() {
        if (currentUser != null) {
            String email = currentUser.getEmail();  // Lưu lại email trước khi xóa
            googleSignInClient.revokeAccess().addOnCompleteListener(this, revokeTask -> {
                if (revokeTask.isSuccessful()) {
                    // Sau khi revokeAccess, tiến hành xóa tài khoản Firebase
                    currentUser.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            Toast.makeText(SettingsActivity.this, "Tài khoản " + email + " đã bị xóa thành công. Vui lòng đăng nhập lại!", Toast.LENGTH_LONG).show();
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(SettingsActivity.this, "Xóa tài khoản Firebase không thành công.", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(SettingsActivity.this, "Không thể xóa quyền truy cập Google Sign-In.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn chắc chắn muốn đăng xuất?")
                .setPositiveButton("Yes", (dialog, which) -> logout())
                .setNegativeButton("No", null)
                .show();
    }

    private void logout() {
        mAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(SettingsActivity.this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            }
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
            finish();
        });
    }
}
