package com.example.note_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide; // Dùng Glide để tải ảnh từ URL
import com.example.note_app.R;

public class ImageViewActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_image_view);

        imageView = findViewById(R.id.dialogImageView);

        // Lấy URL từ Intent và hiển thị ảnh
        Intent intent = getIntent();
        String imageUrl = intent.getStringExtra("imageUrl");

        if (imageUrl != null) {
            Glide.with(this).load(Uri.parse(imageUrl)).into(imageView);
        }
    }
}
