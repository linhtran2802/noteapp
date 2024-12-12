package com.example.note_app;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class ReminderDialogActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Áp dụng layout dialog
        setContentView(R.layout.activity_reminder_dialog);

        // Lấy nội dung nhắc nhở từ Intent
        String noteTitle = getIntent().getStringExtra("noteTitle");

        // Gán nội dung vào TextView
        TextView messageTextView = findViewById(R.id.reminder_message);
        messageTextView.setText(noteTitle);

        // Nút OK để đóng dialog
        Button okButton = findViewById(R.id.ok_button);
        okButton.setOnClickListener(v -> finish());

        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.nhacnho);
        mediaPlayer.start();
    }
}
