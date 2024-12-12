package com.example.note_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReminderScheduler {

    /**
     * Lên lịch nhắc nhở sử dụng AlarmManager.
     * @param context - Ngữ cảnh ứng dụng
     * @param noteTitle - Tiêu đề ghi chú
     * @param reminderTime - Thời gian nhắc nhở (timestamp)
     * @param requestCode - Mã yêu cầu duy nhất cho mỗi lời nhắc
     */
    public static void scheduleReminder(Context context, String noteTitle, long reminderTime, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Kiểm tra quyền đặt alarm chính xác trên Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Toast.makeText(context, "Vui lòng cấp quyền thông báo chính xác", Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Intent để kích hoạt BroadcastReceiver
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("noteTitle", noteTitle);
        intent.putExtra("notificationId", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            try {
                // Sử dụng setExactAndAllowWhileIdle để nhắc nhở chính xác
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent);
                Toast.makeText(context, "Lời nhắc đã được lên lịch!", Toast.LENGTH_SHORT).show();
            } catch (SecurityException e) {
                Toast.makeText(context, "Không thể đặt lời nhắc, cần quyền thông báo chính xác!", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Khởi động lại tất cả các lời nhắc sau khi ứng dụng được mở lại.
     * @param context - Ngữ cảnh ứng dụng
     */
    public static void reRegisterReminders(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Lấy tất cả các lời nhắc chưa hết hạn từ Firestore
        db.collection("notes")
                .whereGreaterThan("reminderTime", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String title = document.getString("title");
                        Long reminderTime = document.getLong("reminderTime");

                        if (title != null && reminderTime != null) {
                            int requestCode = document.getId().hashCode();
                            scheduleReminder(context, title, reminderTime, requestCode);
                        }
                    }
                    Toast.makeText(context, "Đã khởi động lại tất cả lời nhắc.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Không thể tải lại lời nhắc!", Toast.LENGTH_SHORT).show());
    }

    /**
     * Kiểm tra và yêu cầu quyền Overlay nếu sử dụng Dialog.
     * @param context - Ngữ cảnh ứng dụng
     */
    public static void checkAndRequestOverlayPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Toast.makeText(context, "Vui lòng cấp quyền Overlay để hiển thị nhắc nhở.", Toast.LENGTH_LONG).show();
        }
    }
}
