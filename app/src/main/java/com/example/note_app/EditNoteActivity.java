package com.example.note_app;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EditNoteActivity extends AppCompatActivity {

    private EditText titleEditText, descriptionEditText;
    private TextView reminderTimeTextView;
    private Button saveButton, setReminderButton;
    private ImageButton addImageButton, addAudioButton;
    private ImageView backButton;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private ArrayList<String> selectedImages = new ArrayList<>();
    private ArrayList<String> selectedAudioFiles = new ArrayList<>();
    private ArrayList<String> existingImageUrls = new ArrayList<>();
    private ArrayList<String> existingAudioUrls = new ArrayList<>();
    private RecyclerView imageRecyclerView, audioRecyclerView;
    private ImageAdapter imageAdapter;
    private AudioAdapter audioAdapter;
    private String noteId;
    private long reminderTime;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        reminderTimeTextView = findViewById(R.id.reminderTimeTextView);
        saveButton = findViewById(R.id.saveButton);
        setReminderButton = findViewById(R.id.setReminderButton);
        addImageButton = findViewById(R.id.addImageButton);
        addAudioButton = findViewById(R.id.addAudioButton);
        backButton = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        noteId = getIntent().getStringExtra("noteId");
        if (noteId == null || noteId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy ID của ghi chú", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        audioRecyclerView = findViewById(R.id.audioRecyclerView);

        imageAdapter = new ImageAdapter(this, selectedImages, position -> {
            String removedUrl = selectedImages.remove(position);
            if (existingImageUrls.contains(removedUrl)) {
                deleteFileFromStorage(removedUrl, "images");
                existingImageUrls.remove(removedUrl);
            }
            imageAdapter.notifyDataSetChanged();
        });

        audioAdapter = new AudioAdapter(this, selectedAudioFiles, position -> {
            String removedUrl = selectedAudioFiles.remove(position);
            if (existingAudioUrls.contains(removedUrl)) {
                deleteFileFromStorage(removedUrl, "audios");
                existingAudioUrls.remove(removedUrl);
            }
            audioAdapter.notifyDataSetChanged();
        });

        imageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        audioRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageRecyclerView.setAdapter(imageAdapter);
        audioRecyclerView.setAdapter(audioAdapter);

        backButton.setOnClickListener(v -> finish());

        loadNoteData();

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

        setReminderButton.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                new TimePickerDialog(this, (timePicker, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    reminderTime = calendar.getTimeInMillis();

                    String formattedTime = DateFormat.format("MM/dd/yyyy HH:mm", calendar).toString();
                    reminderTimeTextView.setText("Đặt lời nhắc: " + formattedTime);
                    Toast.makeText(this, "Bạn đã thiết lập lời nhắc: " + formattedTime, Toast.LENGTH_LONG).show();

                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        saveButton.setOnClickListener(v -> {
            String title = titleEditText.getText().toString();
            String description = descriptionEditText.getText().toString();

            if (!title.isEmpty() && !description.isEmpty()) {
                updateNoteData(title, description);
                if (reminderTime > System.currentTimeMillis()) {
                    scheduleReminder(title, reminderTime);
                }
            } else {
                Toast.makeText(EditNoteActivity.this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNoteData() {
        DocumentReference noteRef = db.collection("notes").document(noteId);
        noteRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                titleEditText.setText(documentSnapshot.getString("title"));
                descriptionEditText.setText(documentSnapshot.getString("description"));
                reminderTime = documentSnapshot.contains("reminderTime") ? documentSnapshot.getLong("reminderTime") : 0;

                if (reminderTime > 0) {
                    String formattedTime = DateFormat.format("MM/dd/yyyy HH:mm", reminderTime).toString();
                    reminderTimeTextView.setText("Đặt lời nhắc cho: " + formattedTime);
                }

                if (documentSnapshot.contains("images")) {
                    existingImageUrls = (ArrayList<String>) documentSnapshot.get("images");
                    selectedImages.clear();
                    selectedImages.addAll(existingImageUrls);
                    imageAdapter.notifyDataSetChanged();
                }

                if (documentSnapshot.contains("audios")) {
                    existingAudioUrls = (ArrayList<String>) documentSnapshot.get("audios");
                    selectedAudioFiles.clear();
                    selectedAudioFiles.addAll(existingAudioUrls);
                    audioAdapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(EditNoteActivity.this, "Ghi chú không tồn tại", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(EditNoteActivity.this, "Lỗi khi tải ghi chú: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateNoteData(String title, String description) {
        DocumentReference noteRef = db.collection("notes").document(noteId);
        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("title", title);
        updatedData.put("description", description);
        updatedData.put("reminderTime", reminderTime);
        updatedData.put("createdTime", System.currentTimeMillis());  // Cập nhật createdTime thành thời gian hiện tại

        noteRef.update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    ArrayList<String> newImages = new ArrayList<>(selectedImages);
                    newImages.removeAll(existingImageUrls);

                    ArrayList<String> newAudios = new ArrayList<>(selectedAudioFiles);
                    newAudios.removeAll(existingAudioUrls);

                    uploadMediaToStorage(noteId, "images", newImages, existingImageUrls);
                    uploadMediaToStorage(noteId, "audios", newAudios, existingAudioUrls);

                    Toast.makeText(EditNoteActivity.this, "Đã cập nhật " + DateFormat.format("MM/dd/yyyy HH:mm", System.currentTimeMillis()), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(EditNoteActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadMediaToStorage(String noteId, String mediaType, ArrayList<String> newMediaFiles, ArrayList<String> existingUrls) {
        StorageReference storageRef = storage.getReference().child("notes/" + noteId + "/" + mediaType);

        ArrayList<String> mediaUrls = new ArrayList<>(existingUrls);

        for (String mediaUrl : newMediaFiles) {
            Uri mediaUri = Uri.parse(mediaUrl);
            if (mediaUri.getScheme().equals("content") || mediaUri.getScheme().equals("file")) {
                String fileName = UUID.randomUUID().toString();
                StorageReference fileRef = storageRef.child(fileName);

                fileRef.putFile(mediaUri)
                        .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            mediaUrls.add(uri.toString());

                            if (mediaUrls.size() == existingUrls.size() + newMediaFiles.size()) {
                                db.collection("notes").document(noteId)
                                        .update(mediaType, mediaUrls)
                                        .addOnSuccessListener(aVoid -> Toast.makeText(EditNoteActivity.this, "Cập nhật " + mediaType + " thành công", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(EditNoteActivity.this, "Lỗi khi cập nhật " + mediaType + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            }
                        }))
                        .addOnFailureListener(e -> Toast.makeText(EditNoteActivity.this, "Lỗi khi tải lên " + mediaType, Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void deleteFileFromStorage(String url, String mediaType) {
        StorageReference fileRef = storage.getReferenceFromUrl(url);
        fileRef.delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Tệp đã bị xóa", Toast.LENGTH_SHORT).show();
            updateFirestoreUrls(mediaType);  // Cập nhật lại danh sách URL trên Firestore sau khi xóa
        }).addOnFailureListener(e -> {
            Toast.makeText(this, " Lỗi khi xóa tệp bởi bộ nhớ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFirestoreUrls(String mediaType) {
        Map<String, Object> updatedUrls = new HashMap<>();
        if (mediaType.equals("images")) {
            updatedUrls.put("images", existingImageUrls);
        } else if (mediaType.equals("audios")) {
            updatedUrls.put("audios", existingAudioUrls);
        }
        db.collection("notes").document(noteId).update(updatedUrls)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Firestore đã cập nhật " + mediaType, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi cập nhật Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void scheduleReminder(String title, long reminderTime) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("noteTitle", title);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
            }
        }
    }
}
