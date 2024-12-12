package com.example.note_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText usernameEditText, emailEditText, phoneEditText, passwordEditText;
    private Button registerButton;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        ImageView eyeIcon = findViewById(R.id.eyeIcon);

        TextView loginLinkTextView = findViewById(R.id.loginLinkTextView);
        loginLinkTextView.setOnClickListener(v -> startActivity(new Intent(RegisterActivity.this, LoginActivity.class)));

        registerButton.setOnClickListener(v -> registerUser());

        // Handle eye icon click to show/hide password
        eyeIcon.setOnClickListener(v -> {
            if (isPasswordVisible) {
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                eyeIcon.setImageResource(R.drawable.close_view);
                isPasswordVisible = false;
            } else {
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                eyeIcon.setImageResource(R.drawable.pass);
                isPasswordVisible = true;
            }
            passwordEditText.setSelection(passwordEditText.length());
        });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        // Kiểm tra xem các trường có trống hay không
        if (email.isEmpty() || password.isEmpty() || username.isEmpty() || phone.isEmpty()) {
            Toast.makeText(RegisterActivity.this, "Hãy nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    String userId = user.getUid();

                    // Lưu thêm thông tin vào Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("username", username);
                    userData.put("email", email);
                    userData.put("phone", phone);

                    db.collection("users").document(userId).set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(RegisterActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, "Không lưu được thông tin người dùng", Toast.LENGTH_SHORT).show());
                }
            } else {
                // Xử lý khi email đã được sử dụng
                if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                    Toast.makeText(RegisterActivity.this, "Gmail này đã được sử dụng", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(RegisterActivity.this, "Đăng ký không thành công " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
