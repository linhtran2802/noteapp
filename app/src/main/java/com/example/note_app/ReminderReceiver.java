package com.example.note_app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "ReminderChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String noteTitle = intent.getStringExtra("noteTitle");

        // Hiển thị thông báo Notification
        showNotification(context, noteTitle);

        // Khởi chạy ReminderDialogActivity để hiển thị thông báo dạng Dialog
        Intent dialogIntent = new Intent(context, ReminderDialogActivity.class);
        dialogIntent.putExtra("noteTitle", noteTitle);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(dialogIntent);
    }

    private void showNotification(Context context, String noteTitle) {
        // Tạo kênh thông báo nếu cần
        createNotificationChannel(context);

        // Âm thanh thông báo
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Tạo Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.note) // Đặt icon
                .setContentTitle("Nhắc Nhở")
                .setContentText(noteTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(soundUri) // Gán âm thanh
                .setAutoCancel(true);

        // Hiển thị Notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminder Notifications";
            String description = "Channel for Reminder Notifications";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
