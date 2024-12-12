package com.example.note_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PrivacyPolicyActivity extends AppCompatActivity {

    private EditText usernameEditText, emailEditText, phoneEditText;
    private EditText currentPasswordEditText, newPasswordEditText;
    private TextView changePasswordTextView;
    private Button confirmButton;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;
    private boolean isGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_policy);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.newPasswordEditText);
        changePasswordTextView = findViewById(R.id.changePasswordTextView);
        confirmButton = findViewById(R.id.confirmButton);

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        isGoogleSignIn = GoogleSignIn.getLastSignedInAccount(this) != null;
        if (isGoogleSignIn) {
            currentPasswordEditText.setVisibility(View.GONE);
            newPasswordEditText.setVisibility(View.GONE);
            changePasswordTextView.setVisibility(View.GONE);
        } else {
            currentPasswordEditText.setVisibility(View.VISIBLE);
            newPasswordEditText.setVisibility(View.VISIBLE);
            changePasswordTextView.setVisibility(View.VISIBLE);
        }

        loadUserInfo();

        confirmButton.setOnClickListener(v -> updateUserInfo());
    }

    private void loadUserInfo() {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    String email = documentSnapshot.getString("email");
                    String phone = documentSnapshot.getString("phone");

                    usernameEditText.setText(username);
                    emailEditText.setText(email);
                    phoneEditText.setText(phone);
                }
            }).addOnFailureListener(e -> Toast.makeText(PrivacyPolicyActivity.this, "Failed to load user info.", Toast.LENGTH_SHORT).show());
        }
    }

    private void updateUserInfo() {
        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String phone = phoneEditText.getText().toString();

        Map<String, Object> updatedData = new HashMap<>();
        if (!TextUtils.isEmpty(username)) updatedData.put("username", username);
        if (!TextUtils.isEmpty(email)) updatedData.put("email", email);
        if (!TextUtils.isEmpty(phone)) updatedData.put("phone", phone);

        if (isGoogleSignIn) {
            saveUserInfo(updatedData);
        } else {
            String currentPassword = currentPasswordEditText.getText().toString();
            String newPassword = newPasswordEditText.getText().toString();

            if (!TextUtils.isEmpty(currentPassword) && !TextUtils.isEmpty(newPassword)) {
                changePassword(currentPassword, newPassword, updatedData);
            } else {
                saveUserInfo(updatedData);
            }
        }
    }

    private void saveUserInfo(Map<String, Object> updatedData) {
        String userId = currentUser.getUid();
        db.collection("users").document(userId).update(updatedData).addOnSuccessListener(aVoid -> {
            Toast.makeText(PrivacyPolicyActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();
            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent); // Trả kết quả về SettingsActivity
            finish();
        }).addOnFailureListener(e -> Toast.makeText(PrivacyPolicyActivity.this, "Cập nhật thông tin thất bại", Toast.LENGTH_SHORT).show());
    }

    private void changePassword(String currentPassword, String newPassword, Map<String, Object> updatedData) {
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                currentUser.updatePassword(newPassword).addOnCompleteListener(task1 -> {
                    if (task1.isSuccessful()) {
                        Toast.makeText(PrivacyPolicyActivity.this, "Cập nhật password thành công", Toast.LENGTH_SHORT).show();
                        saveUserInfo(updatedData);
                    } else {
                        Toast.makeText(PrivacyPolicyActivity.this, "Cập nhật password thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(PrivacyPolicyActivity.this, "Mật khẩu hiện tại sai, hãy nhập lại.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
