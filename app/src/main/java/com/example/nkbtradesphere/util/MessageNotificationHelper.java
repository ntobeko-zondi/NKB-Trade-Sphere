package com.example.nkbtradesphere.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.nkbtradesphere.NotificationCenterActivity;
import com.example.nkbtradesphere.R;

public final class MessageNotificationHelper {
    private MessageNotificationHelper() {}

    public static final String CHANNEL_ID = "messages_channel";
    public static final int UNREAD_NOTIFICATION_ID = 4001;

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Notifications for unread messages");
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public static void showUnreadMessagesNotification(Context context, int unreadCount,
                                                      String otherUserId, String otherUserName, int listingId) {
        ensureChannel(context);
        // Open the notification center rather than a conversation with possibly missing extras.
        // This prevents the “conversation unavailable” screen when a system notification is tapped.
        Intent intent = new Intent(context, NotificationCenterActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = unreadCount == 1
                ? ("New message from " + otherUserName)
                : (unreadCount + " unread messages");
        String body = unreadCount == 1
                ? "Tap to open conversation."
                : "Tap to open your latest unread conversation.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                NotificationManagerCompat.from(context).notify(UNREAD_NOTIFICATION_ID, builder.build());
            }
        } else {
            NotificationManagerCompat.from(context).notify(UNREAD_NOTIFICATION_ID, builder.build());
        }
    }

    public static void cancelUnreadMessagesNotification(Context context) {
        NotificationManagerCompat.from(context).cancel(UNREAD_NOTIFICATION_ID);
    }
}
