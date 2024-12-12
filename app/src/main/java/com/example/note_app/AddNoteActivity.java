package com.example.note_app;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddNoteActivity extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText;
    private TextView reminderTimeTextView;
    private Button saveButton, setReminderButton;
    private ImageButton addImageButton, addAudioButton;
    private ImageView backButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private ArrayList<String> selectedImages = new ArrayList<>();
    private ArrayList<String> selectedAudioFiles = new ArrayList<>();
    private RecyclerView imageRecyclerView, audioRecyclerView;
    private ImageAdapter imageAdapter;
    private AudioAdapter audioAdapter;
    private long reminderTime;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        reminderTimeTextView = findViewById(R.id.reminderTimeTextView);
        saveButton = findViewById(R.id.saveButton);
        setReminderButton = findViewById(R.id.setReminderButton);
        addImageButton = findViewById(R.id.addImageButton);
        addAudioButton = findViewById(R.id.addAudioButton);
        backButton = findViewById(R.id.backButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        audioRecyclerView = findViewById(R.id.audioRecyclerView);

        imageAdapter = new ImageAdapter(this, selectedImages, position -> {
            selectedImages.remove(position);
            imageAdapter.notifyDataSetChanged();
        });
        audioAdapter = new AudioAdapter(this, selectedAudioFiles, position -> {
            selectedAudioFiles.remove(position);
            audioAdapter.notifyDataSetChanged();
        });

        imageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        audioRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageRecyclerView.setAdapter(imageAdapter);
        audioRecyclerView.setAdapter(audioAdapter);

        backButton.setOnClickListener(v -> finish());

        setReminderButton.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                new TimePickerDialog(this, (timePicker, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    reminderTime = calendar.getTimeInMillis();

                    String formattedTime = DateFormat.format("MM/dd/yyyy HH:mm", calendar).toString();
                    reminderTimeTextView.setText(" Đặt lời nhắc: " + formattedTime);
                    Toast.makeText(this, "Bạn đã thiết lập lời nhắc: " + formattedTime, Toast.LENGTH_LONG).show();

                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                                selectedImages.add(imageUri.toString());
                            }
                        } else if (result.getData().getData() != null) {
                            Uri imageUri = result.getData().getData();
                            selectedImages.add(imageUri.toString());
                        }
                        imageAdapter.notifyDataSetChanged();
                    }
                });

        addImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            imagePickerLauncher.launch(intent);
        });

        ActivityResultLauncher<Intent> audioPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            for (int i = 0; i < count; i++) {
                                Uri audioUri = result.getData().getClipData().getItemAt(i).getUri();
                                selectedAudioFiles.add(audioUri.toString());
                            }
                        } else if (result.getData().getData() != null) {
                            Uri audioUri = result.getData().getData();
                            selectedAudioFiles.add(audioUri.toString());
                        }
                        audioAdapter.notifyDataSetChanged();
                    }
                });

        addAudioButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            audioPickerLauncher.launch(intent);
        });

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString();
            String description = descriptionEditText.getText().toString();

            if (!title.isEmpty() && !description.isEmpty()) {
                Map<String, Object> note = new HashMap<>();
                note.put("title", title);
                note.put("description", description);
                note.put("userId", mAuth.getCurrentUser().getUid());
                note.put("createdTime", System.currentTimeMillis());
                note.put("reminderTime", reminderTime);
                note.put("isPinned", false); // Mặc định không ghim khi tạo ghi chú

                db.collection("notes").add(note)
                        .addOnSuccessListener(documentReference -> {
                            String noteId = documentReference.getId();
                            int requestCode = noteId.hashCode(); // Unique ID for each note

                            db.collection("notes").document(noteId)
                                    .update("id", noteId)
                                    .addOnSuccessListener(aVoid -> {
                                        uploadMediaToStorage(noteId, "images", selectedImages);
                                        uploadMediaToStorage(noteId, "audios", selectedAudioFiles);

                                        if (reminderTime > System.currentTimeMillis()) {
                                            scheduleReminder(title, reminderTime, requestCode);  // Schedule each reminder with a unique requestCode
                                        }

                                        Toast.makeText(AddNoteActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                        })
                        .addOnFailureListener(e -> Toast.makeText(AddNoteActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(AddNoteActivity.this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void uploadMediaToStorage(String noteId, String mediaType, ArrayList<String> mediaFiles) {
        StorageReference storageRef = storage.getReference().child("notes/" + noteId + "/" + mediaType);
        ArrayList<String> mediaUrls = new ArrayList<>();

        for (String mediaUriString : mediaFiles) {
            Uri mediaUri = Uri.parse(mediaUriString);
            String fileName = UUID.randomUUID().toString();
            StorageReference fileRef = storageRef.child(fileName);

            fileRef.putFile(mediaUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        mediaUrls.add(uri.toString());

                        if (mediaUrls.size() == mediaFiles.size()) {
                            db.collection("notes").document(noteId)
                                    .update(mediaType, mediaUrls)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(AddNoteActivity.this, "Đã tải lên " + mediaType + " successfully", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(AddNoteActivity.this, "Lỗi tải lên " + mediaType + " trong Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }))
                    .addOnFailureListener(e -> Toast.makeText(AddNoteActivity.this, " Tải lên lỗi " + mediaType, Toast.LENGTH_SHORT).show());
        }
    }

    private void scheduleReminder(String title, long reminderTime, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Vui lòng cấp quyền để ứng dụng có thể gửi nhắc nhở chính xác", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("noteTitle", title);
        intent.putExtra("notificationId", requestCode); // Unique notification ID

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
        }
    }
}
