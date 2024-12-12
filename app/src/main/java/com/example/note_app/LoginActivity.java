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
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, googleLoginButton;
    private GoogleSignInClient googleSignInClient;
    private FirebaseFirestore db;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleLoginButton = findViewById(R.id.googleLoginButton);
        TextView registerLinkTextView = findViewById(R.id.registerLinkTextView);
        ImageView eyeIcon = findViewById(R.id.eyeIcon);

        registerLinkTextView.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        loginButton.setOnClickListener(v -> loginWithEmail());
        googleLoginButton.setOnClickListener(v -> loginWithGoogle());

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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("431260385044-4c140o05l1fnb6b0ub0bb8g5038ce1g2.apps.googleusercontent.com")
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            Toast.makeText(LoginActivity.this, "Đăng nhập với Google thất bại " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void loginWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(LoginActivity.this, "Hãy nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                updateUI(user);
            } else {
                Toast.makeText(LoginActivity.this, "Xác thực thất bại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText(this, "Bạn đã đăng nhập bằng tài khoản Google: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    createUserInFirestore(user);
                }
                updateUI(user);
            } else {
                Toast.makeText(LoginActivity.this, "Xác thực Firebase thất bại.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserInFirestore(FirebaseUser user) {
        String userId = user.getUid();
        String userName = user.getDisplayName();
        String email = user.getEmail();

        db.collection("users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!task.getResult().exists()) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("username", userName);
                    userData.put("email", email);

                    db.collection("users").document(userId).set(userData)
                            .addOnSuccessListener(aVoid -> Toast.makeText(LoginActivity.this, "User profile created in Firestore.", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Failed to create user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            } else {
                Toast.makeText(LoginActivity.this, "Error fetching user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                googleSignInClient.signOut().addOnCompleteListener(task -> {
                    mAuth.signOut();
                });
            }
        }
    }
}
